-- Chỉ dùng cho test tích hợp pcbuild (DB H2 riêng). Bổ sung các cột mà entity catalog đọc nhưng purchase-schema.sql
-- không có. Không sửa trực tiếp purchase-schema.sql vì PurchaseApiTests, PickupOrderApiTests, PickupConcurrencyTests
-- chèn dữ liệu bằng INSERT ... VALUES không ghi tên cột: thêm cột sẽ làm các test đó lỗi số cột.
ALTER TABLE brands ADD COLUMN IF NOT EXISTS logo_url VARCHAR(255);
ALTER TABLE brands ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS parent_id INTEGER;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS slug VARCHAR(150);
-- Khớp numeric(12,2) của schema thật (DD 12.4) để kiểm được giới hạn cột
ALTER TABLE product_variants ALTER COLUMN price SET DATA TYPE NUMERIC(12,2);
ALTER TABLE specification_options ADD COLUMN IF NOT EXISTS specification_id INTEGER REFERENCES specifications(id);
ALTER TABLE specification_options ADD COLUMN IF NOT EXISTS code VARCHAR(100);
ALTER TABLE category_specifications ADD COLUMN IF NOT EXISTS id INTEGER;
ALTER TABLE category_specifications ADD COLUMN IF NOT EXISTS show_in_menu BOOLEAN;
ALTER TABLE category_specifications ADD COLUMN IF NOT EXISTS is_compatibility BOOLEAN;
