package com.project.cybershop.pcbuild.internal.support;

import com.project.cybershop.catalog.api.dto.ComponentCandidateDTO;
import com.project.cybershop.pcbuild.internal.model.Archetype;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Chọn màn, phím, chuột cho chế độ "Trọn bộ": thuần tính toán, tất định, không gọi AI.
 * Các món này không có luật tương thích nên chỉ phụ thuộc vào giá và nhu cầu.
 */
public final class PeripheralSelector {

    public static final String MONITOR = "monitor";
    public static final String KEYBOARD = "keyboard";
    public static final String MOUSE = "mouse";
    public static final List<String> ROLES = List.of(MONITOR, KEYBOARD, MOUSE);
    public static final int REFRESH_RATE_HZ = 70;

    private static final BigDecimal PC_SHARE = new BigDecimal("0.25");
    private static final BigDecimal MOUSE_SHARE = new BigDecimal("0.10");
    private static final BigDecimal KEYBOARD_SHARE = new BigDecimal("0.15");
    private static final int GAMING_MIN_REFRESH_HZ = 144;
    private static final Pattern DIGITS = Pattern.compile("\\d{1,4}");

    public record BudgetRange(BigDecimal min, BigDecimal max) {
    }

    public record Selection(BigDecimal available, BigDecimal spendLimit, BigDecimal peripheralTotal,
                            Map<String, ComponentCandidateDTO> picks, boolean gamingFallback) {
    }

    private PeripheralSelector() {
    }

    /**
     * min = tổng 3 món rẻ nhất; max = số tiền bộ chọn thật sự chi được khi dư dả (B3). Món đắt nhất mỗi loại có thể nằm ngoài
     * phần chia nên tổng giá đắt nhất không dùng làm trần, nếu không thùng máy bị giữ tiền mà không tiêu.
     * null khi kho thiếu một trong 3 loại.
     */
    public static BudgetRange budgetRange(List<ComponentCandidateDTO> peripherals) {
        BigDecimal min = BigDecimal.ZERO;
        BigDecimal rawMax = BigDecimal.ZERO;
        for (String role : ROLES) {
            List<ComponentCandidateDTO> options = optionsOf(peripherals, role);
            if (options.isEmpty()) return null;
            min = min.add(options.get(0).getPrice());
            rawMax = rawMax.add(options.get(options.size() - 1).getPrice());
        }
        BigDecimal max = practicalMax(peripherals, rawMax, false).max(practicalMax(peripherals, rawMax, true));
        return new BudgetRange(min, max);
    }

    // Lặp chi(S) cho tới khi không giảm nữa: dãy giảm dần trên tập giá hữu hạn nên luôn dừng; tại điểm dừng chi(S) = S
    private static BigDecimal practicalMax(List<ComponentCandidateDTO> peripherals, BigDecimal rawMax, boolean gaming) {
        BigDecimal spend = rawMax;
        for (int guard = 0; guard < 100; guard++) {
            BigDecimal total = pick(peripherals, spend, gaming).total();
            if (total.compareTo(spend) >= 0) break;
            spend = total;
        }
        return spend;
    }

    /** Các món có giá của một loại, sắp theo giá tăng dần rồi variantId tăng dần. */
    public static List<ComponentCandidateDTO> optionsOf(List<ComponentCandidateDTO> peripherals, String role) {
        return peripherals.stream()
                .filter(item -> role.equalsIgnoreCase(item.getCategorySlug()) && item.getPrice() != null)
                .sorted(Comparator.comparing(ComponentCandidateDTO::getPrice).thenComparing(ComponentCandidateDTO::getVariantId))
                .toList();
    }

    /**
     * Chi tối đa min(tiền còn lại, max(tổng 3 món rẻ nhất, 25% thùng máy)). Chọn chuột, phím trước theo phần chia,
     * màn hình lấy toàn bộ phần còn lại; mỗi bước đều chừa đủ tiền mua món rẻ nhất của các loại sau.
     */
    public static Selection select(List<ComponentCandidateDTO> peripherals, BigDecimal budget, BigDecimal pcTotal, String archetype) {
        BudgetRange range = budgetRange(peripherals);
        if (range == null) {
            throw new BuildRuleException("PERIPHERALS_UNAVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Kho hiện chưa đủ màn hình, bàn phím và chuột để gợi ý trọn bộ", null);
        }
        BigDecimal available = budget.subtract(pcTotal);
        if (available.compareTo(range.min()) < 0) {
            throw new BuildRuleException("PERIPHERAL_BUDGET_TOO_LOW", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ngân sách còn lại chưa đủ mua màn hình, bàn phím và chuột",
                    Map.of("shortfall", range.min().subtract(available), "minTotal", range.min(), "available", available));
        }

        BigDecimal spendLimit = available.min(range.min().max(pcTotal.multiply(PC_SHARE).setScale(0, RoundingMode.DOWN)));
        Picks picks = pick(peripherals, spendLimit, Archetype.parse(archetype) == Archetype.GAMING);
        Map<String, ComponentCandidateDTO> byRole = new LinkedHashMap<>();
        byRole.put(MONITOR, picks.monitor());
        byRole.put(KEYBOARD, picks.keyboard());
        byRole.put(MOUSE, picks.mouse());
        return new Selection(available, spendLimit, picks.total(), byRole, picks.gamingFallback());
    }

    private record Picks(ComponentCandidateDTO monitor, ComponentCandidateDTO keyboard, ComponentCandidateDTO mouse,
                         boolean gamingFallback) {
        BigDecimal total() {
            return monitor.getPrice().add(keyboard.getPrice()).add(mouse.getPrice());
        }
    }

    /** Chọn 3 món với tổng không vượt spendLimit; spendLimit luôn ≥ tổng 3 món rẻ nhất. */
    private static Picks pick(List<ComponentCandidateDTO> peripherals, BigDecimal spendLimit, boolean gaming) {
        BigDecimal cheapestMonitor = cheapest(peripherals, MONITOR);
        BigDecimal cheapestKeyboard = cheapest(peripherals, KEYBOARD);
        BigDecimal cheapestMouse = cheapest(peripherals, MOUSE);

        ComponentCandidateDTO mouse = pickWithShare(peripherals, MOUSE, spendLimit, MOUSE_SHARE,
                spendLimit.subtract(cheapestKeyboard).subtract(cheapestMonitor));
        ComponentCandidateDTO keyboard = pickWithShare(peripherals, KEYBOARD, spendLimit, KEYBOARD_SHARE,
                spendLimit.subtract(mouse.getPrice()).subtract(cheapestMonitor));
        BigDecimal monitorLimit = spendLimit.subtract(mouse.getPrice()).subtract(keyboard.getPrice());
        if (!gaming) {
            return new Picks(best(peripherals, MONITOR, monitorLimit).orElseThrow(), keyboard, mouse, false);
        }

        List<ComponentCandidateDTO> highRefresh = peripherals.stream().filter(PeripheralSelector::isHighRefresh).toList();
        Optional<ComponentCandidateDTO> monitor = best(highRefresh, MONITOR, monitorLimit);
        if (monitor.isPresent()) {
            return new Picks(monitor.get(), keyboard, mouse, false);
        }
        // B2: chơi game thì màn tần số cao quan trọng hơn phím chuột đắt; chừa đủ tiền cho phím chuột rẻ nhất rồi chọn lại
        Optional<ComponentCandidateDTO> roomy = best(highRefresh, MONITOR, spendLimit.subtract(cheapestKeyboard).subtract(cheapestMouse));
        if (roomy.isEmpty()) {
            return new Picks(best(peripherals, MONITOR, monitorLimit).orElseThrow(), keyboard, mouse, true);
        }
        BigDecimal rest = spendLimit.subtract(roomy.get().getPrice());
        ComponentCandidateDTO cheaperKeyboard = pickWithShare(peripherals, KEYBOARD, spendLimit, KEYBOARD_SHARE, rest.subtract(cheapestMouse));
        ComponentCandidateDTO cheaperMouse = pickWithShare(peripherals, MOUSE, spendLimit, MOUSE_SHARE, rest.subtract(cheaperKeyboard.getPrice()));
        return new Picks(roomy.get(), cheaperKeyboard, cheaperMouse, false);
    }

    private static ComponentCandidateDTO pickWithShare(List<ComponentCandidateDTO> peripherals, String role, BigDecimal spendLimit,
                                                       BigDecimal share, BigDecimal cap) {
        BigDecimal limit = spendLimit.multiply(share).setScale(0, RoundingMode.DOWN).max(cheapest(peripherals, role)).min(cap);
        return best(peripherals, role, limit).orElseThrow();
    }

    /** Món đắt nhất không vượt limit; giá bằng nhau thì lấy variantId nhỏ hơn. */
    private static Optional<ComponentCandidateDTO> best(List<ComponentCandidateDTO> peripherals, String role, BigDecimal limit) {
        return optionsOf(peripherals, role).stream()
                .filter(item -> item.getPrice().compareTo(limit) <= 0)
                .max(Comparator.comparing(ComponentCandidateDTO::getPrice)
                        .thenComparing(ComponentCandidateDTO::getVariantId, Comparator.reverseOrder()));
    }

    private static BigDecimal cheapest(List<ComponentCandidateDTO> peripherals, String role) {
        return optionsOf(peripherals, role).get(0).getPrice();
    }

    private static boolean isHighRefresh(ComponentCandidateDTO item) {
        String hz = item.getSpecById(REFRESH_RATE_HZ);
        return hz != null && DIGITS.matcher(hz).matches() && Integer.parseInt(hz) >= GAMING_MIN_REFRESH_HZ;
    }
}
