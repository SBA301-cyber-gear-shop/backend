package com.project.cybershop.support.internal.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class LiveSupportTableInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Khởi tạo cấu trúc bảng Live Support (support_sessions, support_messages)...");

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS support_sessions (
                    id SERIAL PRIMARY KEY,
                    conversation_id VARCHAR(100) NOT NULL UNIQUE,
                    customer_name VARCHAR(150),
                    customer_email VARCHAR(150),
                    customer_phone VARCHAR(50),
                    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
                    assigned_staff_id INTEGER REFERENCES users(id),
                    assigned_staff_name VARCHAR(150),
                    last_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS support_messages (
                    id SERIAL PRIMARY KEY,
                    conversation_id VARCHAR(100) NOT NULL,
                    sender_type VARCHAR(30) NOT NULL,
                    sender_name VARCHAR(150),
                    content TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_support_messages_conv ON support_messages(conversation_id);
            """);

            jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_support_sessions_status ON support_sessions(status);
            """);

            log.info("Cấu trúc bảng Live Support đã sẵn sàng!");
        } catch (Exception e) {
            log.warn("Lỗi khi khởi tạo bảng Live Support: {}", e.getMessage());
        }
    }
}
