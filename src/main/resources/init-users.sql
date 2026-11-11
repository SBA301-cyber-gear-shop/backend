-- ==============================================================================
-- KHỞI TẠO DỮ LIỆU BẢNG ROLES VÀ USERS (CYBERSHOP)
-- MẬT KHẨU TOÀN BỘ TÀI KHOẢN: 123456
-- BCrypt Hash: $2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu
-- ==============================================================================

-- 1. BẢNG PHÂN QUYỀN (ROLES)
INSERT INTO roles (id, name, description) VALUES
(1, 'ADMIN', 'Quản trị viên toàn quyền hệ thống'),
(2, 'STAFF', 'Nhân viên bán hàng và xử lý đơn hàng'),
(3, 'TECHNICIAN', 'Kỹ thuật viên tiếp nhận và xử lý bảo hành'),
(4, 'CUSTOMER', 'Khách hàng mua sắm')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description;

-- 2. BẢNG NGƯỜI DÙNG (USERS)
-- Mật khẩu mặc định toàn bộ tài khoản: 123456 (đã mã hóa chuẩn BCrypt cho Spring Security)
INSERT INTO users (role_id, full_name, email, phone, password_hash, address, is_active, created_at, updated_at) VALUES
-- Tài khoản Quản trị viên (ADMIN - role_id: 1)
(1, 'Admin CyberShop', 'admin@cybershop.vn', '0900000001', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', true, now(), now()),
(1, 'System Administrator', 'admin@techzone.vn', '0900000011', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Tầng 18 TechZone Tower, Quận 1, TP. Hồ Chí Minh', true, now(), now()),
(1, 'Quản trị viên Demo', 'admin@techzone.demo', '0900000131', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Tòa Golden Park, Cầu Giấy, Hà Nội', true, now(), now()),

-- Tài khoản Nhân viên bán hàng (STAFF - role_id: 2)
(2, 'Nhân viên CyberShop', 'staff@cybershop.vn', '0900000003', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', true, now(), now()),
(2, 'Trần Bích Thảo (Staff)', 'staff@techzone.vn', '0900000012', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Cửa hàng CyberShop, Cầu Giấy, Hà Nội', true, now(), now()),
(2, 'Nhân viên cửa hàng Demo', 'staff@techzone.demo', '0900000129', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Cầu Giấy, Hà Nội', true, now(), now()),

-- Tài khoản Kỹ thuật viên bảo hành (TECHNICIAN - role_id: 3)
(3, 'Kỹ thuật viên CyberShop', 'technician@cybershop.vn', '0900000004', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', true, now(), now()),
(3, 'Lê Hoàng Nam (Technician)', 'tech@techzone.vn', '0900000013', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Trung tâm Kỹ thuật & Bảo hành, Cầu Giấy, Hà Nội', true, now(), now()),
(3, 'Kỹ thuật viên Demo', 'technician@techzone.demo', '0900000130', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Cầu Giấy, Hà Nội', true, now(), now()),

-- Tài khoản Khách hàng mua sắm (CUSTOMER - role_id: 4)
(4, 'Khách Hàng CyberShop', 'customer@cybershop.vn', '0900000002', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Số 25 ngõ 120 Hoàng Quốc Việt, Cầu Giấy, Hà Nội', true, now(), now()),
(4, 'Nguyễn Tuấn Khôi (Customer)', 'customer@techzone.vn', '0900000014', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Số 25 ngõ 120 Hoàng Quốc Việt, Cầu Giấy, Hà Nội', true, now(), now()),
(4, 'Nguyễn Minh Anh (Customer Demo)', 'customer@techzone.demo', '0900000128', '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu', 'Tòa Golden Park, Dương Đình Nghệ, Cầu Giấy, Hà Nội', true, now(), now())
ON CONFLICT (email) DO UPDATE SET
    role_id = EXCLUDED.role_id,
    full_name = EXCLUDED.full_name,
    password_hash = EXCLUDED.password_hash,
    phone = COALESCE(users.phone, EXCLUDED.phone),
    address = COALESCE(users.address, EXCLUDED.address),
    is_active = true,
    updated_at = now();

-- 3. ĐỒNG BỘ MẬT KHẨU TOÀN BỘ TÀI KHOẢN TRONG HỆ THỐNG VỀ 123456
UPDATE users 
SET password_hash = '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu' 
WHERE password_hash <> '$2a$10$kcpvbfRSlnO186L3geKZIuKMZRv6sPX0ON8begMIS9kfmXGfUjSvu';

-- 4. TỰ ĐỘNG KHỞI TẠO GIỎ HÀNG CHO CÁC TÀI KHOẢN (TRÁNH LỖI KHI MUA SẮM)
INSERT INTO carts (user_id, updated_at)
SELECT u.id, now()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM carts c WHERE c.user_id = u.id);

-- 5. CẬP NHẬT SEQUENCE ĐỂ TRÁNH XUNG ĐỘT KHI TỰ SINH ID TIẾP THEO
SELECT setval(pg_get_serial_sequence('roles', 'id'), COALESCE((SELECT MAX(id) FROM roles), 1)) WHERE pg_get_serial_sequence('roles', 'id') IS NOT NULL;
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1)) WHERE pg_get_serial_sequence('users', 'id') IS NOT NULL;
SELECT setval(pg_get_serial_sequence('carts', 'id'), COALESCE((SELECT MAX(id) FROM carts), 1)) WHERE pg_get_serial_sequence('carts', 'id') IS NOT NULL;
