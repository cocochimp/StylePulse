-- =====================================================================
-- Reflow 表结构（H2 / MySQL 兼容写法）
-- 设计要点见 docs/02-technical-design.md §10 数据模型
-- =====================================================================

-- 款：一个聚合根对应这张表 + t_style_sku
CREATE TABLE IF NOT EXISTS t_style (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    style_code      VARCHAR(64)  NOT NULL,
    category_id     BIGINT,
    price_band      INT,
    launch_date     DATE,
    first_order_qty INT,
    stage           VARCHAR(32),
    version         INT DEFAULT 0,
    size_curve      VARCHAR(128)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_style_code ON t_style (style_code);

-- 颜色 × 尺码矩阵
CREATE TABLE IF NOT EXISTS t_style_sku (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    style_id        BIGINT NOT NULL,
    color_code      VARCHAR(32),
    size_code       VARCHAR(16),
    available_stock INT DEFAULT 0,
    recent_sales    INT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_style_sku_style ON t_style_sku (style_id);

-- 返单规则：条件用 min/max 展开，便于人看和加索引
CREATE TABLE IF NOT EXISTS t_replenish_rule (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code          VARCHAR(64) NOT NULL,
    priority           INT DEFAULT 0,
    rule_version       INT DEFAULT 1,
    enabled            INT DEFAULT 1,
    category_ids       VARCHAR(256),
    price_bands        VARCHAR(64),
    age_day_min        INT,
    age_day_max        INT,
    dos_min            INT,
    dos_max            INT,
    sell_through_min   INT,
    sell_through_max   INT,
    cycle_factor       DECIMAL(6, 2),
    growth_factor      DECIMAL(6, 2),
    moq                INT,
    max_multiplier     DECIMAL(6, 2)
);

-- 返单决策单
CREATE TABLE IF NOT EXISTS t_replenish_decision (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    style_id        BIGINT NOT NULL,
    round           INT    NOT NULL,
    rule_id         BIGINT,
    rule_code       VARCHAR(64),
    rule_version    INT,
    total_qty       INT,
    shadow          INT DEFAULT 0,
    trigger_source  VARCHAR(32),
    status          VARCHAR(32),
    version         INT DEFAULT 0
);

-- T3 第三层防护：唯一键兜底。锁会超时、CAS 会被绕过，这个约束不会失效
CREATE UNIQUE INDEX IF NOT EXISTS uk_style_round ON t_replenish_decision (style_id, round);
CREATE INDEX IF NOT EXISTS idx_decision_status ON t_replenish_decision (status);

-- 决策单尺码行
CREATE TABLE IF NOT EXISTS t_replenish_decision_item (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    decision_id BIGINT NOT NULL,
    sku_id      BIGINT,
    color_code  VARCHAR(32),
    size_code   VARCHAR(16),
    qty         INT
);

CREATE INDEX IF NOT EXISTS idx_decision_item_decision ON t_replenish_decision_item (decision_id);

-- 产能槽账本：只存总产能，已用量由 hold 汇总（事实只记一次）
CREATE TABLE IF NOT EXISTS t_capacity_slot (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    factory_id BIGINT NOT NULL,
    line_id    BIGINT NOT NULL,
    biz_date   VARCHAR(16) NOT NULL,
    total_qty  INT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_factory_line_date ON t_capacity_slot (factory_id, line_id, biz_date);

-- 产能预占凭证：可审计、可对账
CREATE TABLE IF NOT EXISTS t_capacity_hold (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    hold_id     VARCHAR(64) NOT NULL,
    slot_key    VARCHAR(64) NOT NULL,
    qty         INT,
    decision_id BIGINT,
    status      VARCHAR(32),
    expire_at   TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_hold_id ON t_capacity_hold (hold_id);
CREATE INDEX IF NOT EXISTS idx_hold_status_expire ON t_capacity_hold (status, expire_at);

-- 本地消息表：落库与下发解耦，失败可重试
CREATE TABLE IF NOT EXISTS t_local_message (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_id        VARCHAR(64) NOT NULL,
    biz_type      VARCHAR(32),
    biz_id        BIGINT,
    payload       VARCHAR(2048),
    status        VARCHAR(16),
    retry_count   INT DEFAULT 0,
    next_retry_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_msg_id ON t_local_message (msg_id);
CREATE INDEX IF NOT EXISTS idx_msg_status_retry ON t_local_message (status, next_retry_at);