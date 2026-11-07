package com.project.cybershop.support.internal.service.impl;

import com.project.cybershop.support.api.dto.request.AiChatRequest;
import com.project.cybershop.support.api.dto.response.SupportMessageDto;
import com.project.cybershop.support.api.dto.response.SupportSessionDto;
import com.project.cybershop.support.internal.service.StaffLiveSupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffLiveSupportServiceImpl implements StaffLiveSupportService {

    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Override
    public List<SupportSessionDto> getSessions(String statusFilter) {
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT s.id, s.conversation_id, s.customer_name, s.customer_email, s.customer_phone,
                       s.status, s.assigned_staff_id, s.assigned_staff_name, s.last_message,
                       s.created_at, s.updated_at,
                       (SELECT COUNT(*) FROM support_messages m WHERE m.conversation_id = s.conversation_id AND m.sender_type = 'CUSTOMER') as msg_count
                FROM support_sessions s
            """);

            List<Object> params = new ArrayList<>();
            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                sql.append(" WHERE UPPER(s.status) = UPPER(?) ");
                params.add(statusFilter.trim());
            }

            sql.append(" ORDER BY s.updated_at DESC LIMIT 50 ");

            return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> SupportSessionDto.builder()
                    .id(rs.getLong("id"))
                    .conversationId(rs.getString("conversation_id"))
                    .customerName(rs.getString("customer_name") != null ? rs.getString("customer_name") : "Khách hàng")
                    .customerEmail(rs.getString("customer_email") != null ? rs.getString("customer_email") : "")
                    .customerPhone(rs.getString("customer_phone") != null ? rs.getString("customer_phone") : "")
                    .status(rs.getString("status"))
                    .assignedStaffId(rs.getObject("assigned_staff_id") != null ? rs.getInt("assigned_staff_id") : null)
                    .assignedStaffName(rs.getString("assigned_staff_name"))
                    .lastMessage(rs.getString("last_message") != null ? rs.getString("last_message") : "")
                    .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().format(TIME_FMT) : "")
                    .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime().format(TIME_FMT) : "")
                    .unreadCount(rs.getInt("msg_count"))
                    .build(), params.toArray());
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách phiên hỗ trợ trực tuyến: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public SupportSessionDto getSession(String conversationId) {
        try {
            String sql = """
                SELECT s.id, s.conversation_id, s.customer_name, s.customer_email, s.customer_phone,
                       s.status, s.assigned_staff_id, s.assigned_staff_name, s.last_message,
                       s.created_at, s.updated_at
                FROM support_sessions s
                WHERE s.conversation_id = ?
                LIMIT 1
            """;
            List<SupportSessionDto> list = jdbcTemplate.query(sql, (rs, rowNum) -> SupportSessionDto.builder()
                    .id(rs.getLong("id"))
                    .conversationId(rs.getString("conversation_id"))
                    .customerName(rs.getString("customer_name") != null ? rs.getString("customer_name") : "Khách hàng")
                    .customerEmail(rs.getString("customer_email") != null ? rs.getString("customer_email") : "")
                    .customerPhone(rs.getString("customer_phone") != null ? rs.getString("customer_phone") : "")
                    .status(rs.getString("status"))
                    .assignedStaffId(rs.getObject("assigned_staff_id") != null ? rs.getInt("assigned_staff_id") : null)
                    .assignedStaffName(rs.getString("assigned_staff_name"))
                    .lastMessage(rs.getString("last_message") != null ? rs.getString("last_message") : "")
                    .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().format(TIME_FMT) : "")
                    .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime().format(TIME_FMT) : "")
                    .build(), conversationId);
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết phiên hỗ trợ {}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<SupportMessageDto> getMessages(String conversationId, Long afterId) {
        try {
            StringBuilder sql = new StringBuilder("""
                SELECT id, conversation_id, sender_type, sender_name, content, created_at
                FROM support_messages
                WHERE conversation_id = ?
            """);

            List<Object> params = new ArrayList<>();
            params.add(conversationId);

            if (afterId != null && afterId > 0) {
                sql.append(" AND id > ? ");
                params.add(afterId);
            }

            sql.append(" ORDER BY id ASC ");

            return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> SupportMessageDto.builder()
                    .id(rs.getLong("id"))
                    .conversationId(rs.getString("conversation_id"))
                    .senderType(rs.getString("sender_type"))
                    .senderName(rs.getString("sender_name"))
                    .content(rs.getString("content"))
                    .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime().format(TIME_FMT) : "")
                    .build(), params.toArray());
        } catch (Exception e) {
            log.error("Lỗi khi lấy tin nhắn phiên hỗ trợ {}: {}", conversationId, e.getMessage());
            return List.of();
        }
    }

    @Override
    @Transactional
    public SupportSessionDto assignStaff(String conversationId, String staffEmail) {
        try {
            StaffProfile staff = resolveStaffProfile(staffEmail);

            jdbcTemplate.update("""
                UPDATE support_sessions
                SET status = 'ACTIVE', assigned_staff_id = ?, assigned_staff_name = ?, updated_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ?
            """, staff.id(), staff.name(), conversationId);

            jdbcTemplate.update("""
                INSERT INTO support_messages (conversation_id, sender_type, sender_name, content)
                VALUES (?, 'BOT', 'CyberBot', ?)
            """, conversationId, "👨‍💼 Chuyên viên " + staff.name() + " đã tiếp nhận và đang tham gia phiên tư vấn trực tiếp cùng bạn.");

            return getSession(conversationId);
        } catch (Exception e) {
            log.error("Lỗi khi chuyên viên tiếp nhận phiên {}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public SupportMessageDto sendStaffMessage(String conversationId, String staffEmail, String content) {
        try {
            StaffProfile staff = resolveStaffProfile(staffEmail);

            // Ghi tin nhắn của nhân viên
            Number id = jdbcTemplate.queryForObject("""
                INSERT INTO support_messages (conversation_id, sender_type, sender_name, content)
                VALUES (?, 'STAFF', ?, ?)
                RETURNING id
            """, Long.class, conversationId, staff.name(), content);

            // Cập nhật phiên sang ACTIVE và cập nhật last_message
            jdbcTemplate.update("""
                UPDATE support_sessions
                SET status = 'ACTIVE', assigned_staff_id = ?, assigned_staff_name = ?, last_message = ?, updated_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ?
            """, staff.id(), staff.name(), content, conversationId);

            return SupportMessageDto.builder()
                    .id(id != null ? id.longValue() : System.currentTimeMillis())
                    .conversationId(conversationId)
                    .senderType("STAFF")
                    .senderName(staff.name())
                    .content(content)
                    .createdAt("Vừa xong")
                    .build();
        } catch (Exception e) {
            log.error("Lỗi khi chuyên viên gửi tin nhắn {}: {}", conversationId, e.getMessage());
            throw new RuntimeException("Không thể gửi tin nhắn của chuyên viên: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SupportSessionDto closeSession(String conversationId, String staffEmail) {
        try {
            StaffProfile staff = resolveStaffProfile(staffEmail);

            jdbcTemplate.update("""
                UPDATE support_sessions
                SET status = 'CLOSED', updated_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ?
            """, conversationId);

            jdbcTemplate.update("""
                INSERT INTO support_messages (conversation_id, sender_type, sender_name, content)
                VALUES (?, 'BOT', 'CyberBot', ?)
            """, conversationId, "Chuyên viên " + staff.name() + " đã kết thúc phiên hỗ trợ trực tuyến. CyberBot đã sẵn sàng tiếp tục đồng hành cùng bạn! 🤖");

            return getSession(conversationId);
        } catch (Exception e) {
            log.error("Lỗi khi kết thúc phiên {}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public void recordCustomerMessage(String conversationId, String customerEmail, String customerName, String content) {
        try {
            jdbcTemplate.update("""
                INSERT INTO support_messages (conversation_id, sender_type, sender_name, content)
                VALUES (?, 'CUSTOMER', ?, ?)
            """, conversationId, customerName != null ? customerName : "Khách hàng", content);

            jdbcTemplate.update("""
                UPDATE support_sessions
                SET last_message = ?, updated_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ?
            """, content, conversationId);
        } catch (Exception e) {
            log.warn("Không thể ghi nhận tin nhắn khách hàng vào bảng live support: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void createOrOpenSession(String conversationId, String customerEmail, String customerName, String phone, String initialMessage) {
        createOrOpenSession(conversationId, customerEmail, customerName, phone, initialMessage, null);
    }

    @Override
    @Transactional
    public void createOrOpenSession(String conversationId, String customerEmail, String customerName, String phone, String initialMessage, List<AiChatRequest.HistoryItem> history) {
        try {
            Integer existingCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM support_sessions WHERE conversation_id = ?", Integer.class, conversationId);

            jdbcTemplate.update("""
                INSERT INTO support_sessions (conversation_id, customer_name, customer_email, customer_phone, status, last_message, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'WAITING', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (conversation_id) DO UPDATE SET
                  status = CASE WHEN support_sessions.status = 'CLOSED' THEN 'WAITING' ELSE support_sessions.status END,
                  last_message = EXCLUDED.last_message,
                  updated_at = CURRENT_TIMESTAMP
            """, conversationId, customerName, customerEmail, phone, initialMessage);

            // Bổ sung ngữ cảnh trao đổi trước đó với CyberBot để chuyên viên CSKH nắm bắt ngay (BUG-07 & Task 5)
            if ((existingCount == null || existingCount == 0) && history != null && !history.isEmpty()) {
                StringBuilder historySummary = new StringBuilder("📝 Tóm tắt ngữ cảnh trao đổi trước đó giữa Khách hàng và CyberBot:\n");
                int validCount = 0;
                for (AiChatRequest.HistoryItem h : history) {
                    if (h.getText() == null || h.getText().isBlank()) continue;
                    String senderLabel = "user".equalsIgnoreCase(h.getSender()) ? "Khách hàng" : "CyberBot";
                    historySummary.append("- ").append(senderLabel).append(": ").append(h.getText().trim().replaceAll("\\s+", " ")).append("\n");
                    validCount++;
                }
                if (validCount > 0) {
                    jdbcTemplate.update("""
                        INSERT INTO support_messages (conversation_id, sender_type, sender_name, content)
                        VALUES (?, 'BOT', 'CyberBot', ?)
                    """, conversationId, historySummary.toString());
                }
            }

            jdbcTemplate.update("""
                INSERT INTO support_messages (conversation_id, sender_type, sender_name, content)
                VALUES (?, 'BOT', 'CyberBot', ?)
            """, conversationId, "🔔 Khách hàng yêu cầu kết nối với chuyên viên hỗ trợ trực tuyến: " + initialMessage);
        } catch (Exception e) {
            log.warn("Không thể tạo phiên hỗ trợ live support: {}", e.getMessage());
        }
    }

    private record StaffProfile(Integer id, String name) {}

    private StaffProfile resolveStaffProfile(String email) {
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return new StaffProfile(1, "Trần Hoàng Nam");
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, full_name FROM users WHERE email = ? LIMIT 1", email);
            if (!rows.isEmpty()) {
                Integer id = (Integer) rows.get(0).get("id");
                String name = (String) rows.get(0).get("full_name");
                return new StaffProfile(id, name != null ? name : "Trần Hoàng Nam");
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tìm thông tin nhân viên {}: {}", email, e.getMessage());
        }
        return new StaffProfile(1, "Trần Hoàng Nam");
    }
}
