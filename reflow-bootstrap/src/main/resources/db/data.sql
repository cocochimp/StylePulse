-- =====================================================================
-- 演示数据：3 个款（健康/爆款/滞销）+ 3 条规则 + 产能槽
-- =====================================================================

-- 款 1：观察期，12 天前上架，首单 600 件，S 码已断货 —— 应该被追单
-- 注意：4 个尺码 × MOQ 100 = 400 件，追单量必须 ≥400 才能全部尺码都下得了单，
-- 这也是真实服装返单里"追单量不能太小"的原因
INSERT INTO t_style (id, style_code, category_id, price_band, launch_date, first_order_qty, stage, version, size_curve)
VALUES (1, 'SH-2026-0001', 1, 1, DATEADD('DAY', -12, CURRENT_DATE), 600, 'OBSERVING', 0, 'S:2,M:4,L:4,XL:2');

INSERT INTO t_style_sku (id, style_id, color_code, size_code, available_stock, recent_sales) VALUES
(11, 1, 'BLACK', 'S',   0,  90),
(12, 1, 'BLACK', 'M',  30, 210),
(13, 1, 'BLACK', 'L',  25, 200),
(14, 1, 'BLACK', 'XL', 40,  50);

-- 款 2：刚上架 2 天，销量还没跑出来 —— 不该追单（动销率太低）
INSERT INTO t_style (id, style_code, category_id, price_band, launch_date, first_order_qty, stage, version, size_curve)
VALUES (2, 'SH-2026-0002', 1, 2, DATEADD('DAY', -2, CURRENT_DATE), 120, 'TESTING', 0, 'S:2,M:4,L:4,XL:2');

INSERT INTO t_style_sku (id, style_id, color_code, size_code, available_stock, recent_sales) VALUES
(21, 2, 'BEIGE', 'S', 30, 2),
(22, 2, 'BEIGE', 'M', 30, 3),
(23, 2, 'BEIGE', 'L', 30, 1),
(24, 2, 'BEIGE', 'XL', 30, 0);

-- 款 3：长尾款，卖不动，库存还厚 —— 命中慢销规则且低于 MOQ，不追
INSERT INTO t_style (id, style_code, category_id, price_band, launch_date, first_order_qty, stage, version, size_curve)
VALUES (3, 'SH-2026-0003', 2, 3, DATEADD('DAY', -60, CURRENT_DATE), 200, 'OBSERVING', 0, 'S:2,M:4,L:4,XL:2');

INSERT INTO t_style_sku (id, style_id, color_code, size_code, available_stock, recent_sales) VALUES
(31, 3, 'GREY', 'S', 45, 1),
(32, 3, 'GREY', 'M', 50, 2),
(33, 3, 'GREY', 'L', 48, 1),
(34, 3, 'GREY', 'XL', 40, 0);

-- ---------------------------------------------------------------------
-- 规则
-- 公式：追单量 = 首单量 × 动销率 × 周期系数 × 增长系数，再受上限倍数与 MOQ 约束
-- ---------------------------------------------------------------------

-- 爆款快追：上架 7 天内、DOS 极低、动销率高 —— 优先级最高，先抢
INSERT INTO t_replenish_rule (id, rule_code, priority, rule_version, enabled, category_ids, price_bands,
                              age_day_min, age_day_max, dos_min, dos_max,
                              sell_through_min, sell_through_max,
                              cycle_factor, growth_factor, moq, max_multiplier)
VALUES (1, 'RT-HOT-001', 20, 1, 1, '', '',
        0, 7, 0, 5, 6000, 100000, 1.50, 1.10, 100, 2.50);

-- 标准返单：观察期主力规则
INSERT INTO t_replenish_rule (id, rule_code, priority, rule_version, enabled, category_ids, price_bands,
                              age_day_min, age_day_max, dos_min, dos_max,
                              sell_through_min, sell_through_max,
                              cycle_factor, growth_factor, moq, max_multiplier)
VALUES (2, 'RT-STD-002', 10, 3, 1, '1,2', '1,2',
        0, 30, 0, 15, 3000, 100000, 1.20, 1.00, 100, 3.00);

-- 慢销规则：上架久、卖得差、库存厚 —— 算出来必然低于 MOQ，等于"不追"
INSERT INTO t_replenish_rule (id, rule_code, priority, rule_version, enabled, category_ids, price_bands,
                              age_day_min, age_day_max, dos_min, dos_max,
                              sell_through_min, sell_through_max,
                              cycle_factor, growth_factor, moq, max_multiplier)
VALUES (3, 'RT-SLOW-003', 5, 2, 1, '', '',
        31, 365, 20, 100000, 0, 2000, 0.80, 1.00, 100, 1.50);

-- ---------------------------------------------------------------------
-- 产能槽：从今天起第 5~10 天各留 800 件产能（覆盖默认 7 天排产日）
-- 用 DATEADD 而不是写死日期，保证任何时候跑都是"未来的排产日"
-- ---------------------------------------------------------------------
INSERT INTO t_capacity_slot (factory_id, line_id, biz_date, total_qty) VALUES
(1, 1, CAST(DATEADD('DAY', 5, CURRENT_DATE) AS VARCHAR), 800),
(1, 1, CAST(DATEADD('DAY', 6, CURRENT_DATE) AS VARCHAR), 800),
(1, 1, CAST(DATEADD('DAY', 7, CURRENT_DATE) AS VARCHAR), 800),
(1, 1, CAST(DATEADD('DAY', 8, CURRENT_DATE) AS VARCHAR), 800),
(1, 1, CAST(DATEADD('DAY', 9, CURRENT_DATE) AS VARCHAR), 800),
(1, 1, CAST(DATEADD('DAY', 10, CURRENT_DATE) AS VARCHAR), 800);