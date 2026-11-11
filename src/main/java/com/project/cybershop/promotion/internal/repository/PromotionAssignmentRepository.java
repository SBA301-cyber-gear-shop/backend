package com.project.cybershop.promotion.internal.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Repository @RequiredArgsConstructor
public class PromotionAssignmentRepository {
    private final JdbcTemplate jdbc;
    public record Recipient(Integer id, String name, String email, boolean assigned, boolean used) {}
    public record WalletRow(Integer id, String code, String name, String type, BigDecimal value,
                            LocalDateTime start, LocalDateTime end, boolean active, int limit, int allocated,
                            Integer usageId, String usageStatus, String orderCode, LocalDateTime grantedAt) {}

    public boolean exists(Integer promotionId, Integer userId) {
        return jdbc.queryForObject("SELECT count(*) FROM promotion_assignments WHERE promotion_id=? AND user_id=?", Integer.class, promotionId, userId) > 0;
    }
    public void grant(Integer promotionId, Integer userId, Integer actorId, LocalDateTime now) {
        jdbc.update("INSERT INTO promotion_assignments (promotion_id,user_id,granted_by,granted_at) VALUES (?,?,?,?)", promotionId,userId,actorId,now);
    }
    public void revoke(Integer promotionId, Integer userId) {
        jdbc.update("DELETE FROM promotion_assignments WHERE promotion_id=? AND user_id=?",promotionId,userId);
    }
    public List<Recipient> recipients(Integer promotionId, String search, boolean assignedOnly) {
        String term="%"+search.toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        return jdbc.query("""
            SELECT u.id,u.full_name,u.email,a.user_id AS assigned,
                   (SELECT count(*) FROM promotion_usages pu WHERE pu.promotion_id=? AND pu.user_id=u.id) AS used
            FROM users u JOIN roles r ON r.id=u.role_id
            LEFT JOIN promotion_assignments a ON a.user_id=u.id AND a.promotion_id=?
            WHERE lower(r.name)='customer' AND u.is_active=true
              AND (?=false OR a.user_id IS NOT NULL)
              AND (lower(u.full_name) LIKE ? ESCAPE '!' OR lower(u.email) LIKE ? ESCAPE '!' OR CAST(u.id AS VARCHAR)=?)
            ORDER BY u.id DESC LIMIT 50
            """,(rs,n)->new Recipient(rs.getInt("id"),rs.getString("full_name"),rs.getString("email"),rs.getObject("assigned")!=null,rs.getInt("used")>0),
            promotionId,promotionId,assignedOnly,term,term,search);
    }
    public List<WalletRow> wallet(Integer userId) {
        return jdbc.query("""
            SELECT p.*, a.granted_at, pu.id AS usage_id, pu.status AS usage_status, o.order_code,
                   coalesce(allocated.total,0) AS allocated
            FROM promotions p
            LEFT JOIN promotion_assignments a ON a.promotion_id=p.id AND a.user_id=?
            LEFT JOIN promotion_usages pu ON pu.promotion_id=p.id AND pu.user_id=?
            LEFT JOIN orders o ON o.id=pu.order_id
            LEFT JOIN (SELECT promotion_id,count(*) AS total FROM promotion_usages
                       WHERE status IS NULL OR status NOT IN ('REVOKED','CANCELED') GROUP BY promotion_id) allocated ON allocated.promotion_id=p.id
            WHERE a.user_id IS NOT NULL OR pu.id IS NOT NULL
            ORDER BY p.end_date DESC,p.id DESC
            """,(rs,n)->new WalletRow(rs.getInt("id"),rs.getString("code"),rs.getString("name"),rs.getString("discount_type"),rs.getBigDecimal("discount_value"),
            rs.getTimestamp("start_date").toLocalDateTime(),rs.getTimestamp("end_date").toLocalDateTime(),rs.getBoolean("is_active"),rs.getInt("usage_limit"),
            rs.getInt("allocated"),(Integer)rs.getObject("usage_id"),rs.getString("usage_status"),rs.getString("order_code"),
            rs.getTimestamp("granted_at")==null ? null : rs.getTimestamp("granted_at").toLocalDateTime()),userId,userId);
    }
}
