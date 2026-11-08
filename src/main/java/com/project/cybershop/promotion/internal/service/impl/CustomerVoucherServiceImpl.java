package com.project.cybershop.promotion.internal.service.impl;

import com.project.cybershop.identity.api.IdentityApi;
import com.project.cybershop.promotion.api.dto.response.CustomerVoucherResponse;
import com.project.cybershop.promotion.api.dto.response.VoucherRecipientResponse;
import com.project.cybershop.promotion.internal.service.CustomerVoucherService;
import com.project.cybershop.promotion.internal.repository.*;
import com.project.cybershop.shared.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class CustomerVoucherServiceImpl implements CustomerVoucherService {
    private final IdentityApi identity;
    private final PromotionRepository promotions;
    private final PromotionUsageRepository usages;
    private final PromotionAssignmentRepository assignments;
    private final Clock clock;
    private Integer requireRole(String email,String role) {
        var user=identity.requireActiveUser(email);
        if(!role.equalsIgnoreCase(user.getRole())) throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        return user.getId();
    }
    @Override
    @Transactional(readOnly=true)
    public List<CustomerVoucherResponse> wallet(String email) {
        int userId=requireRole(email,"Customer");
        LocalDateTime now=LocalDateTime.now(clock);
        return assignments.wallet(userId).stream().map(row->new CustomerVoucherResponse(row.id(),row.code(),row.name(),row.type(),row.value(),row.start(),row.end(),status(row,now),row.orderCode(),row.grantedAt())).toList();
    }
    private String status(PromotionAssignmentRepository.WalletRow row,LocalDateTime now) {
        if(row.usageId()!=null) {
            if("USED".equals(row.usageStatus())) return "USED";
            if(List.of("REVOKED","CANCELED").contains(row.usageStatus()==null ? "" : row.usageStatus())) return "REVOKED";
            return "RESERVED";
        }
        if(now.isAfter(row.end()))return "EXPIRED";
        if(!row.active() || row.allocated()>=row.limit())return "UNAVAILABLE";
        if(now.isBefore(row.start()))return "UPCOMING";
        return "AVAILABLE";
    }
    @Override
    @Transactional(readOnly=true)
    public List<VoucherRecipientResponse> recipients(String email,Integer id,String search,boolean assignedOnly) {
        requireRole(email,"Admin");
        if(!promotions.existsById(id))throw new ResourceNotFoundException("Không tìm thấy voucher.");
        String term=search==null ? "" : search.trim();
        if(term.length()>100)throw new BusinessException("Từ khóa tối đa 100 ký tự.");
        return assignments.recipients(id,term,assignedOnly).stream()
                .map(row -> new VoucherRecipientResponse(row.id(), row.name(), row.email(), row.assigned(), row.used()))
                .toList();
    }
    @Override
    public void grant(String email,Integer id,List<Integer> userIds) {
        int actorId=requireRole(email,"Admin");
        var promotion=promotions.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy voucher."));
        if(userIds==null || userIds.isEmpty() || userIds.size()>100 || userIds.stream().anyMatch(Objects::isNull))throw new BusinessException("Chọn từ 1 đến 100 khách hàng.");
        if(!Boolean.TRUE.equals(promotion.getIsActive()) || LocalDateTime.now(clock).isAfter(promotion.getEndDate()))throw new BusinessException("Không thể cấp voucher đã tắt hoặc hết hạn.");
        var recipients=new LinkedHashSet<>(userIds);
        for(var userId:recipients) {
            var customer=identity.getUserById(userId).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy khách hàng."));
            if(!"Customer".equalsIgnoreCase(customer.getRole()) || !Boolean.TRUE.equals(customer.getIsActive()))throw new BusinessException("Chỉ cấp voucher cho tài khoản khách hàng đang hoạt động.");
        }
        for(var userId:recipients) if(!assignments.exists(id,userId)) assignments.grant(id,userId,actorId,LocalDateTime.now(clock));
    }
    @Override
    public void revoke(String email,Integer id,Integer userId) {
        requireRole(email,"Admin");
        promotions.findByIdForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy voucher."));
        if(usages.existsByPromotionIdAndUserId(id,userId))throw new BusinessException("Voucher đã gắn với đơn hàng, không thể thu hồi hoặc cấp lại lượt dùng.");
        assignments.revoke(id,userId);
    }
}
