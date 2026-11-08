package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.api.dto.response.BudgetAllocationItem;
import com.project.cybershop.pcbuild.internal.model.BuildContext;
import com.project.cybershop.pcbuild.internal.model.BuildLine;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.SlotMeta;
import com.project.cybershop.pcbuild.internal.model.BuildPlan;
import com.project.cybershop.pcbuild.internal.service.BuildSolver;
import com.project.cybershop.pcbuild.internal.service.CompatibilityEngine;
import com.project.cybershop.pcbuild.internal.support.BuildIds;
import com.project.cybershop.pcbuild.internal.support.PcBuildException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bước 5 của docx: đi lần lượt các slot theo step_order, mỗi lượt lọc món tương thích với các món đã chọn
 * rồi lấy món đắt nhất còn vừa ngân sách khả dụng; lọc ra rỗng thì quay lui về lượt trước.
 *
 * <pre>
 * softCap = share + carry                      (lượt cuối: chỉ share, không nhận tiền dư)
 * cap     = min(max(softCap, cheapest), budget − spent − reserve)
 * carry'  = softCap − giá món đã chọn          (âm khi bị chặn dưới nâng lên)
 * </pre>
 */
@Slf4j
@Service
public class BuildSolverImpl implements BuildSolver {

    private final CompatibilityEngine compatibilityEngine;
    private final int maxIterations;

    public BuildSolverImpl(CompatibilityEngine compatibilityEngine,
                           @Value("${app.pcbuild.max-solver-iterations:200}") int maxIterations) {
        this.compatibilityEngine = compatibilityEngine;
        this.maxIterations = maxIterations;
    }

    @Override
    public List<BuildLine> solve(long budget, BuildPlan plan, BuildContext context) {
        Map<String, Long> shareByCode = new HashMap<>();
        for (BudgetAllocationItem item : plan.getAllocation()) {
            shareByCode.put(item.getComponent(), item.getAmount());
        }
        List<Turn> turns = new ArrayList<>();
        List<SlotMeta> skippedSlots = new ArrayList<>();
        for (SlotMeta slot : context.metadata().getSlots().stream()
                .sorted(Comparator.comparingInt(SlotMeta::getStepOrder)).toList()) {
            long share = shareByCode.getOrDefault(slot.getComponentCode(), 0L);
            // Slot tùy chọn có tỉ lệ 0% (GPU của OFFICE) thì bỏ qua; slot bắt buộc vẫn chọn, chặn dưới lo phần tiền
            if (!slot.isRequired() && share == 0) {
                skippedSlots.add(slot);
                continue;
            }
            turns.add(new Turn(slot, share));
        }

        SolverState state = new SolverState(budget, turns, reserveAfter(turns, context.candidates()), skippedSlots,
                context);
        List<BuildLine> result = pick(state, 0, List.of(), 0L, 0L);
        if (result == null) {
            throw buildNotFound(state);
        }
        long total = result.stream().mapToLong(BuildLine::lineTotal).sum();
        log.info("solver done budget={} total={} leftover={} iterations={}", budget, total, budget - total,
                state.iterations);
        return result;
    }

    private List<BuildLine> pick(SolverState state, int turnIndex, List<BuildLine> chosen, long spent, long carry) {
        if (turnIndex == state.turns.size()) {
            return chosen;
        }
        Turn turn = state.turns.get(turnIndex);
        String code = turn.slot().getComponentCode();
        boolean lastTurn = turnIndex == state.turns.size() - 1;

        List<BuildLine> valid = validLines(state, turn, chosen);
        if (valid.isEmpty()) {
            log.info("solver deadEnd slot={} reason=không còn món tương thích", code);
            state.recordDeadEnd(code);
            return null;
        }
        long cheapest = valid.stream().mapToLong(BuildLine::lineTotal).min().orElseThrow();
        long softCap = turn.share() + (lastTurn ? 0 : carry);
        long reserve = state.reserveAfter[turnIndex];
        long cap = Math.min(Math.max(softCap, cheapest), state.budget - spent - reserve);
        if (cap < cheapest) {
            log.info("solver deadEnd slot={} reason=hết tiền cheapest={} cap={} reserve={}", code, cheapest, cap, reserve);
            state.recordDeadEnd(code);
            return null;
        }
        List<BuildLine> affordable = valid.stream()
                .filter(line -> line.lineTotal() <= cap)
                .sorted(Comparator.comparingLong(BuildLine::lineTotal).reversed()
                        .thenComparing(line -> stockOf(line.part()), Comparator.reverseOrder())
                        .thenComparing(line -> line.part().getVariantId()))
                .toList();
        for (BuildLine line : affordable) {
            state.countIteration();
            long nextCarry = softCap - line.lineTotal();
            log.info("solver slot={} share={} carry={} softCap={} cheapest={} reserve={} cap={} valid={} picked={} qty={} "
                            + "lineTotal={} nextCarry={}", code, turn.share(), carry, softCap, cheapest, reserve, cap,
                    valid.size(), line.part().getVariantId(), line.quantity(), line.lineTotal(), nextCarry);
            List<BuildLine> next = new ArrayList<>(chosen);
            next.add(line);
            List<BuildLine> result = pick(state, turnIndex + 1, next, spent + line.lineTotal(), nextCarry);
            if (result != null) {
                return result;
            }
            log.info("solver backtrack slot={} variant={}", code, line.part().getVariantId());
        }
        return null;
    }

    /** Lọc một lượt: đúng loại, đủ hàng, điều kiện cố định của docx, rồi luật tương thích với các món đã chọn. */
    private List<BuildLine> validLines(SolverState state, Turn turn, List<BuildLine> chosen) {
        SlotMeta slot = turn.slot();
        int minQty = Math.max(1, slot.getMinQty() != null ? slot.getMinQty() : 1);
        List<BuildLine> valid = new ArrayList<>();
        for (ComponentCandidateDTO part : state.context.candidates()) {
            if (!Objects.equals(part.getCategoryId(), slot.getCategoryId()) || part.getPrice() == null
                    || !passesFixedFilters(state, slot, part)) {
                continue;
            }
            int quantity = Math.min(autoQuantity(slot),
                    compatibilityEngine.maxQuantity(slot, part, chosen, state.context.rules()));
            if (quantity < minQty || stockOf(part) < quantity) {
                continue;
            }
            BuildLine line = new BuildLine(slot, part, quantity);
            List<BuildLine> withLine = new ArrayList<>(chosen);
            withLine.add(line);
            if (compatibilityEngine.checkRules(withLine, state.context.rules()).isEmpty()) {
                valid.add(line);
            }
        }
        return valid;
    }

    private boolean passesFixedFilters(SolverState state, SlotMeta slot, ComponentCandidateDTO part) {
        // Tự build chỉ dùng bo mạch 1 socket; bo 2 socket để khách tự chọn (docx bước 5)
        if (Objects.equals(slot.getCategoryId(), BuildIds.CATEGORY_MAINBOARD)
                && !"1".equals(trim(part.getSpecById(BuildIds.SPEC_CPU_SOCKET_COUNT)))) {
            return false;
        }
        // Slot tùy chọn đã bỏ qua (GPU 0%) thì không được chọn món làm nó thành bắt buộc (CPU không có iGPU)
        for (SlotMeta skipped : state.skippedSlots) {
            if (compatibilityEngine.triggersRequiredSlot(skipped, part)) {
                return false;
            }
        }
        return true;
    }

    /** RAM lấy 2 thanh cùng mẫu (kênh đôi), còn lại 1; sau đó bị kẹp bởi maxQuantity (main 1 khe → 1 thanh). */
    private static int autoQuantity(SlotMeta slot) {
        return Objects.equals(slot.getCategoryId(), BuildIds.CATEGORY_RAM) ? BuildIds.RAM_AUTO_QUANTITY : 1;
    }

    /**
     * reserveAfter[i] = Σ giá rẻ nhất còn hàng của các lượt sau i.
     * shortcut: không xét tương thích nên chỉ là cận dưới; thiếu thì quay lui lo, đổi sang tính theo món tương thích nếu quay lui quá nhiều.
     */
    private static long[] reserveAfter(List<Turn> turns, List<ComponentCandidateDTO> candidates) {
        long[] cheapestByTurn = new long[turns.size()];
        for (int index = 0; index < turns.size(); index++) {
            SlotMeta slot = turns.get(index).slot();
            int quantity = autoQuantity(slot);
            cheapestByTurn[index] = candidates.stream()
                    .filter(part -> Objects.equals(part.getCategoryId(), slot.getCategoryId()) && part.getPrice() != null)
                    .mapToLong(part -> part.getPrice().longValueExact() * quantity)
                    .min()
                    .orElse(0L);
        }
        long[] reserve = new long[turns.size()];
        long sum = 0;
        for (int index = turns.size() - 1; index >= 0; index--) {
            reserve[index] = sum;
            sum += cheapestByTurn[index];
        }
        return reserve;
    }

    private static PcBuildException buildNotFound(SolverState state) {
        Map.Entry<String, Integer> worst = state.deadEnds.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("UNKNOWN", 0));
        String label = state.turns.stream()
                .filter(turn -> turn.slot().getComponentCode().equals(worst.getKey()))
                .map(turn -> turn.slot().getLabel())
                .findFirst()
                .orElse(worst.getKey());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("slot", worst.getKey());
        data.put("deadEnds", worst.getValue());
        data.put("iterations", state.iterations);
        log.warn("solver BUILD_NOT_FOUND budget={} data={}", state.budget, data);
        return new PcBuildException("BUILD_NOT_FOUND", HttpStatus.UNPROCESSABLE_CONTENT,
                "Shop chưa ráp được bộ PC phù hợp với kho hiện tại, vướng ở " + label + ".", data, null);
    }

    private static int stockOf(ComponentCandidateDTO part) {
        return part.getStockQuantity() != null ? part.getStockQuantity() : 0;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private record Turn(SlotMeta slot, long share) {
    }

    /** Trạng thái dùng chung trong một lần solve: đếm lần thử và ngõ cụt theo slot. */
    private final class SolverState {
        private final long budget;
        private final List<Turn> turns;
        private final long[] reserveAfter;
        private final List<SlotMeta> skippedSlots;
        private final BuildContext context;
        private final Map<String, Integer> deadEnds = new HashMap<>();
        private int iterations;

        private SolverState(long budget, List<Turn> turns, long[] reserveAfter, List<SlotMeta> skippedSlots,
                            BuildContext context) {
            this.budget = budget;
            this.turns = turns;
            this.reserveAfter = reserveAfter;
            this.skippedSlots = skippedSlots;
            this.context = context;
        }

        private void recordDeadEnd(String slotCode) {
            deadEnds.merge(slotCode, 1, Integer::sum);
        }

        private void countIteration() {
            iterations++;
            if (iterations > maxIterations) {
                throw buildNotFound(this);
            }
        }
    }
}
