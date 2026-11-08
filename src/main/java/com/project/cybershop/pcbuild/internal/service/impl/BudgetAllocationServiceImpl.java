package com.project.cybershop.pcbuild.internal.service.impl;

import com.project.cybershop.pcbuild.api.dto.response.BudgetAllocationItem;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.PurposeMeta;
import com.project.cybershop.pcbuild.internal.model.BuildMetadata.SlotMeta;
import com.project.cybershop.pcbuild.internal.model.BuildPlan;
import com.project.cybershop.pcbuild.internal.service.BudgetAllocationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bước 4: amount = budget × share_pct / 100, làm tròn HALF_UP tới đồng; slot 0% vẫn giữ với amount 0.
 * Chỉ /builds/auto chia tiền; /requirements/analyze không chia.
 */
@Service
public class BudgetAllocationServiceImpl implements BudgetAllocationService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public BuildPlan plan(String purpose, long budget, BuildMetadata metadata) {
        PurposeMeta purposeMeta = metadata.getPurposes().get(purpose);
        if (purposeMeta == null) {
            throw new IllegalArgumentException("Mục đích không có trong cấu hình: " + purpose);
        }
        List<SlotMeta> sortedSlots = metadata.getSlots().stream()
                .sorted(Comparator.comparingInt(SlotMeta::getStepOrder))
                .toList();
        BigDecimal budgetAmount = BigDecimal.valueOf(budget);
        List<BudgetAllocationItem> allocation = new ArrayList<>();
        for (SlotMeta slot : sortedSlots) {
            BigDecimal sharePct = purposeMeta.getSharePct(slot.getSlotId());
            long amount = budgetAmount.multiply(sharePct).divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP).longValueExact();
            allocation.add(BudgetAllocationItem.builder()
                    .component(slot.getComponentCode())
                    .sharePct(sharePct)
                    .amount(amount)
                    .build());
        }
        return BuildPlan.builder().allocation(allocation).build();
    }
}
