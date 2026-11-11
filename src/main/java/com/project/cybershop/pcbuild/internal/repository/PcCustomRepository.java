package com.project.cybershop.pcbuild.internal.repository;

import com.project.cybershop.pcbuild.internal.entity.PcCustom;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PcCustomRepository extends JpaRepository<PcCustom, Integer> {

    /**
     * Bộ PC khách tự lưu, mới nhất trước. order_id có giá trị là bộ do module order tạo khi đặt hàng (đã hiện ở
     * "Đơn hàng của tôi") nên không lấy. Nạp items cùng lúc để khỏi N+1.
     */
    @EntityGraph(attributePaths = "items")
    List<PcCustom> findByUserIdAndOrderIdIsNullOrderByIdDesc(Integer userId);
}
