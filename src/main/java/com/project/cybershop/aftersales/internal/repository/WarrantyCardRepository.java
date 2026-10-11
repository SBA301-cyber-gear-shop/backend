package com.project.cybershop.aftersales.internal.repository;

import com.project.cybershop.aftersales.internal.entity.WarrantyCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface WarrantyCardRepository extends JpaRepository<WarrantyCard, Integer> {
    /** Các thẻ mới đã thay thế những thẻ trong danh sách. */
    List<WarrantyCard> findByReplacesCardIdIn(Collection<Integer> cardIds);

    boolean existsBySerialNumberIgnoreCase(String serialNumber);
}
