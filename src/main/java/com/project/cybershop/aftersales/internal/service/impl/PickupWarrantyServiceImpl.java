package com.project.cybershop.aftersales.internal.service.impl;

import com.project.cybershop.aftersales.api.PickupWarrantyApi;
import com.project.cybershop.aftersales.internal.service.PickupWarrantyService;
import com.project.cybershop.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PickupWarrantyServiceImpl implements PickupWarrantyService {
    private final JdbcTemplate jdbc;
    public PickupWarrantyServiceImpl(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void issue(List<Unit> units, LocalDateTime start) {
        for (var unit : units) {
            Integer found = jdbc.queryForObject("SELECT count(*) FROM warranty_cards WHERE upper(trim(serial_number))=?", Integer.class, unit.serialNumber());
            if (found != null && found > 0) throw new BusinessException("Serial đã được kích hoạt bảo hành: " + unit.serialNumber());
            jdbc.update("""
                    INSERT INTO warranty_cards (order_item_id,serial_number,start_date,end_date,status)
                    VALUES (?,?,?,?,'active')
                    """, unit.orderItemId(), unit.serialNumber(), start, start.plusMonths(unit.warrantyMonths()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Card> forCustomer(Integer userId) {
        return jdbc.query("""
                SELECT w.id,o.order_code,o.user_id,oi.product_variant_id,oi.product_name,
                       w.serial_number,w.start_date,w.end_date,w.status
                FROM warranty_cards w JOIN order_items oi ON oi.id=w.order_item_id
                JOIN orders o ON o.id=oi.order_id WHERE o.user_id=? ORDER BY w.id DESC
                """, (rs, i) -> new Card(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getObject(7, LocalDateTime.class), rs.getObject(8, LocalDateTime.class), rs.getString(9)), userId);
    }
}
