-- =========================================================================
-- CYBERSHOP - SEED DATA: CẤU HÌNH KHUÔN PC & NGÂN SÁCH THEO MỤC ĐÍCH (PHASE 1)
-- Bảng: build_template_slots (11 slot linh kiện)
-- Bảng: budget_profiles (tỷ lệ phân bổ % và ngân sách tối thiểu cho 6 purpose)
-- =========================================================================

-- 1. Khởi tạo 11 slot linh kiện mẫu
INSERT INTO build_template_slots ("id", "category_id", "step_order", "is_required", "min_qty", "max_qty", "required_if_category_id", "required_if_spec_id", "required_if_value")
VALUES
    (9, 9, 1, true, 1, 1, null, null, null),      -- Màn hình (monitor)
    (10, 10, 2, true, 1, 1, null, null, null),    -- Bàn phím (keyboard)
    (11, 11, 3, true, 1, 1, null, null, null),    -- Chuột (mouse)
    (1, 1, 4, true, 1, 2, null, null, null),      -- CPU
    (2, 2, 5, true, 1, 1, null, null, null),      -- Mainboard
    (3, 3, 6, true, 1, 8, null, null, null),      -- RAM
    (5, 5, 7, true, 1, 6, null, null, null),      -- SSD
    (4, 4, 8, false, 0, 4, 1, 26, 'NO'),          -- GPU (bắt buộc nếu CPU không có iGPU)
    (8, 8, 9, true, 1, 2, null, null, null),      -- Tản nhiệt (cooler)
    (7, 7, 10, true, 1, 1, null, null, null),     -- Vỏ case
    (6, 6, 11, true, 1, 1, null, null, null)      -- Nguồn (psu)
ON CONFLICT (id) DO UPDATE SET
    category_id = EXCLUDED.category_id,
    step_order = EXCLUDED.step_order,
    is_required = EXCLUDED.is_required,
    min_qty = EXCLUDED.min_qty,
    max_qty = EXCLUDED.max_qty;

SELECT setval(pg_get_serial_sequence('build_template_slots', 'id'), (SELECT COALESCE(MAX(id), 1) FROM build_template_slots));

-- 2. Khởi tạo budget_profiles (6 mục đích: GAMING, CREATIVE, AI_DATA, OFFICE, DEV, GENERAL)

-- GAMING (min_budget 16.000.000)
INSERT INTO budget_profiles ("purpose", "slot_id", "share_pct", "min_budget") VALUES
    ('GAMING', 9, 12.00, 16000000),
    ('GAMING', 10, 4.00, 16000000),
    ('GAMING', 11, 3.00, 16000000),
    ('GAMING', 1, 15.00, 16000000),
    ('GAMING', 2, 9.00, 16000000),
    ('GAMING', 3, 8.00, 16000000),
    ('GAMING', 5, 6.00, 16000000),
    ('GAMING', 4, 28.00, 16000000),
    ('GAMING', 8, 3.00, 16000000),
    ('GAMING', 7, 5.00, 16000000),
    ('GAMING', 6, 7.00, 16000000)
ON CONFLICT (purpose, slot_id) DO UPDATE SET
    share_pct = EXCLUDED.share_pct,
    min_budget = EXCLUDED.min_budget;

-- CREATIVE (min_budget 16.000.000)
INSERT INTO budget_profiles ("purpose", "slot_id", "share_pct", "min_budget") VALUES
    ('CREATIVE', 9, 15.00, 16000000),
    ('CREATIVE', 10, 3.00, 16000000),
    ('CREATIVE', 11, 2.00, 16000000),
    ('CREATIVE', 1, 21.00, 16000000),
    ('CREATIVE', 2, 9.00, 16000000),
    ('CREATIVE', 3, 14.00, 16000000),
    ('CREATIVE', 5, 9.00, 16000000),
    ('CREATIVE', 4, 16.00, 16000000),
    ('CREATIVE', 8, 3.00, 16000000),
    ('CREATIVE', 7, 3.00, 16000000),
    ('CREATIVE', 6, 5.00, 16000000)
ON CONFLICT (purpose, slot_id) DO UPDATE SET
    share_pct = EXCLUDED.share_pct,
    min_budget = EXCLUDED.min_budget;

-- AI_DATA (min_budget 16.000.000)
INSERT INTO budget_profiles ("purpose", "slot_id", "share_pct", "min_budget") VALUES
    ('AI_DATA', 9, 8.00, 16000000),
    ('AI_DATA', 10, 2.00, 16000000),
    ('AI_DATA', 11, 2.00, 16000000),
    ('AI_DATA', 1, 11.00, 16000000),
    ('AI_DATA', 2, 8.00, 16000000),
    ('AI_DATA', 3, 12.00, 16000000),
    ('AI_DATA', 5, 7.00, 16000000),
    ('AI_DATA', 4, 39.00, 16000000),
    ('AI_DATA', 8, 3.00, 16000000),
    ('AI_DATA', 7, 2.00, 16000000),
    ('AI_DATA', 6, 6.00, 16000000)
ON CONFLICT (purpose, slot_id) DO UPDATE SET
    share_pct = EXCLUDED.share_pct,
    min_budget = EXCLUDED.min_budget;

-- OFFICE (min_budget 12.500.000)
INSERT INTO budget_profiles ("purpose", "slot_id", "share_pct", "min_budget") VALUES
    ('OFFICE', 9, 16.00, 12500000),
    ('OFFICE', 10, 4.00, 12500000),
    ('OFFICE', 11, 3.00, 12500000),
    ('OFFICE', 1, 23.00, 12500000),
    ('OFFICE', 2, 14.00, 12500000),
    ('OFFICE', 3, 12.00, 12500000),
    ('OFFICE', 5, 12.00, 12500000),
    ('OFFICE', 4, 0.00, 12500000),
    ('OFFICE', 8, 4.00, 12500000),
    ('OFFICE', 7, 5.00, 12500000),
    ('OFFICE', 6, 7.00, 12500000)
ON CONFLICT (purpose, slot_id) DO UPDATE SET
    share_pct = EXCLUDED.share_pct,
    min_budget = EXCLUDED.min_budget;

-- DEV (min_budget 16.000.000)
INSERT INTO budget_profiles ("purpose", "slot_id", "share_pct", "min_budget") VALUES
    ('DEV', 9, 12.00, 16000000),
    ('DEV', 10, 4.00, 16000000),
    ('DEV', 11, 2.00, 16000000),
    ('DEV', 1, 23.00, 16000000),
    ('DEV', 2, 11.00, 16000000),
    ('DEV', 3, 16.00, 16000000),
    ('DEV', 5, 10.00, 16000000),
    ('DEV', 4, 10.00, 16000000),
    ('DEV', 8, 3.00, 16000000),
    ('DEV', 7, 4.00, 16000000),
    ('DEV', 6, 5.00, 16000000)
ON CONFLICT (purpose, slot_id) DO UPDATE SET
    share_pct = EXCLUDED.share_pct,
    min_budget = EXCLUDED.min_budget;

-- GENERAL (min_budget 16.000.000)
INSERT INTO budget_profiles ("purpose", "slot_id", "share_pct", "min_budget") VALUES
    ('GENERAL', 9, 11.00, 16000000),
    ('GENERAL', 10, 3.00, 16000000),
    ('GENERAL', 11, 2.00, 16000000),
    ('GENERAL', 1, 18.00, 16000000),
    ('GENERAL', 2, 10.00, 16000000),
    ('GENERAL', 3, 10.00, 16000000),
    ('GENERAL', 5, 9.00, 16000000),
    ('GENERAL', 4, 22.00, 16000000),
    ('GENERAL', 8, 3.00, 16000000),
    ('GENERAL', 7, 5.00, 16000000),
    ('GENERAL', 6, 7.00, 16000000)
ON CONFLICT (purpose, slot_id) DO UPDATE SET
    share_pct = EXCLUDED.share_pct,
    min_budget = EXCLUDED.min_budget;
