-- ============================================================
-- 二手交易平台（flea_market）数据库初始化脚本
-- 版本：v1.1（含举报表 + 测试数据）
-- 依据：《数据模型设计 v1.1》
-- MySQL 8.0+ / 字符集 utf8mb4
-- ============================================================

-- ----------------------------
-- 1. 创建数据库
-- ----------------------------
DROP DATABASE IF EXISTS flea_market;
CREATE DATABASE flea_market
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE flea_market;

-- ----------------------------
-- 2. DDL：建表
-- ----------------------------

-- 2.1 用户表
CREATE TABLE `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt 哈希',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER | ADMIN',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    KEY idx_user_role_enabled (role, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.2 分类表
CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_name (name),
    KEY idx_category_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.3 商品表
CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL COMMENT '价格（元）',
    description TEXT NOT NULL,
    category_id BIGINT NOT NULL,
    tags VARCHAR(255) COMMENT '自定义标签，逗号分隔',
    `condition` VARCHAR(20) NOT NULL COMMENT 'NEW | LIKE_NEW | USED',
    trade_type VARCHAR(20) NOT NULL COMMENT 'PICKUP | EXPRESS | BOTH',
    trade_mode VARCHAR(10) NOT NULL DEFAULT 'SELL' COMMENT 'SELL | SWAP | BOTH',
    location VARCHAR(50) NOT NULL COMMENT '所在地',
    images VARCHAR(1024) COMMENT '图片路径，逗号分隔',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | SOLD | OFF',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_product_seller (seller_id),
    KEY idx_product_category (category_id),
    KEY idx_product_status_time (status, created_at),
    KEY idx_product_seller_status (seller_id, status),
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES `user` (id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.4 订单表
CREATE TABLE `order` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL COMMENT 'ORD-日期-序号 或 SWP-日期-序号',
    order_type VARCHAR(10) NOT NULL COMMENT 'CASH | SWAP',
    product_id BIGINT NOT NULL COMMENT '目标商品（卖家商品）',
    swap_product_id BIGINT COMMENT '交换商品（买家提供），仅SWAP时有值',
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT '见状态枚举',
    amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    logistics_info VARCHAR(500) COMMENT '物流/自提说明',
    cancel_reason VARCHAR(500),
    refund_reason VARCHAR(500),
    swap_note VARCHAR(500) COMMENT '交换意向说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_buyer_status (buyer_id, status),
    KEY idx_order_seller_status (seller_id, status),
    KEY idx_order_product (product_id),
    KEY idx_order_type_status (order_type, status),
    KEY idx_order_created (created_at),
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_order_swap_product FOREIGN KEY (swap_product_id) REFERENCES product (id),
    CONSTRAINT fk_order_buyer FOREIGN KEY (buyer_id) REFERENCES `user` (id),
    CONSTRAINT fk_order_seller FOREIGN KEY (seller_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.5 订单操作日志表
CREATE TABLE order_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    action_type VARCHAR(30) NOT NULL COMMENT 'CREATE | PAY | SHIP | RECEIVE | CANCEL 等',
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    detail VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_log_order_time (order_id, created_at),
    CONSTRAINT fk_log_order FOREIGN KEY (order_id) REFERENCES `order` (id),
    CONSTRAINT fk_log_operator FOREIGN KEY (operator_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.6 聊天消息表
CREATE TABLE message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    product_id BIGINT COMMENT '关联商品',
    content TEXT NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_msg_conversation (sender_id, receiver_id, product_id),
    KEY idx_msg_contact_list (receiver_id, is_read, created_at),
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES `user` (id),
    CONSTRAINT fk_msg_receiver FOREIGN KEY (receiver_id) REFERENCES `user` (id),
    CONSTRAINT fk_msg_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.7 评价表
CREATE TABLE rating (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    rater_id BIGINT NOT NULL COMMENT '评价人',
    rated_user_id BIGINT NOT NULL COMMENT '被评价人',
    score TINYINT NOT NULL COMMENT '1-5 星',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rating_order_rater (order_id, rater_id),
    KEY idx_rating_rated_user (rated_user_id),
    CONSTRAINT fk_rating_order FOREIGN KEY (order_id) REFERENCES `order` (id),
    CONSTRAINT fk_rating_rater FOREIGN KEY (rater_id) REFERENCES `user` (id),
    CONSTRAINT fk_rating_rated_user FOREIGN KEY (rated_user_id) REFERENCES `user` (id),
    CONSTRAINT chk_rating_score CHECK (score >= 1 AND score <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2.8 举报表
CREATE TABLE report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(10) NOT NULL COMMENT 'PRODUCT | USER | MESSAGE',
    target_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL COMMENT 'FAKE_DESC | PROHIBITED | FRAUD | HARASS | OTHER',
    description VARCHAR(500) COMMENT '补充描述',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | PROCESSING | ACCEPTED | REJECTED | APPEALING',
    admin_note VARCHAR(500) COMMENT '管理员处理备注',
    appeal_reason VARCHAR(500) COMMENT '申诉理由',
    appeal_result VARCHAR(20) COMMENT 'UPHELD | OVERTURNED',
    pre_appeal_status VARCHAR(20) COMMENT '申诉前状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_report_status (status, created_at),
    KEY idx_report_reporter (reporter_id),
    KEY idx_report_target (target_type, target_id),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ----------------------------
-- 3. 测试数据
-- ----------------------------

-- ============================================================
-- 密码说明：所有测试用户密码均为 "12345678"
-- BCrypt 哈希值（10 rounds）：$2a$10$.118E.RRVkg4m.SjLyuY4e.Yf0FHEJV4muturde2vBOwKiylZycYm
-- 所有密码均为 12345678
-- 实际项目中请在应用层使用 BCryptPasswordEncoder 生成
-- 这里使用一个固定的测试哈希便于演示
-- ============================================================

-- 3.1 用户数据（1 个管理员 + 5 个普通用户）
INSERT INTO `user` (id, username, password, role, enabled, created_at) VALUES
(1,  'admin',    '$2a$10$.118E.RRVkg4m.SjLyuY4e.Yf0FHEJV4muturde2vBOwKiylZycYm', 'ADMIN', 1, '2026-06-01 09:00:00'),
(2,  'zhangsan', '$2a$10$.118E.RRVkg4m.SjLyuY4e.Yf0FHEJV4muturde2vBOwKiylZycYm', 'USER',  1, '2026-06-15 10:30:00'),
(3,  'lisi',     '$2a$10$.118E.RRVkg4m.SjLyuY4e.Yf0FHEJV4muturde2vBOwKiylZycYm', 'USER',  1, '2026-06-20 14:00:00'),
(4,  'wangwu',   '$2a$10$.118E.RRVkg4m.SjLyuY4e.Yf0FHEJV4muturde2vBOwKiylZycYm', 'USER',  1, '2026-06-25 16:20:00'),
(5,  'zhaoliu',  '$2a$10$.118E.RRVkg4m.SjLyuY4e.Yf0FHEJV4muturde2vBOwKiylZycYm', 'USER',  1, '2026-07-01 11:00:00'),
(6,  'sunqi',    '$2a$10$.118E.RRVkg4m.SjLyuY4e.Yf0FHEJV4muturde2vBOwKiylZycYm', 'USER',  0, '2026-07-02 08:30:00'); -- 被禁用用户

-- 重置自增起点
ALTER TABLE `user` AUTO_INCREMENT = 7;

-- 3.2 分类数据（5 个默认分类 + 1 个停用分类）
INSERT INTO category (id, name, enabled, created_at) VALUES
(1, '数码', 1, '2026-06-01 09:00:00'),
(2, '服饰', 1, '2026-06-01 09:00:00'),
(3, '家居', 1, '2026-06-01 09:00:00'),
(4, '图书', 1, '2026-06-01 09:00:00'),
(5, '其他', 1, '2026-06-01 09:00:00'),
(6, '美妆', 0, '2026-06-15 10:00:00'); -- 停用分类，不可用于发布商品

ALTER TABLE category AUTO_INCREMENT = 7;

-- 3.3 商品数据（覆盖不同状态、交易模式）
-- seller_id=2: zhangsan 发布了 5 件商品
-- seller_id=3: lisi 发布了 3 件商品
-- seller_id=4: wangwu 发布了 2 件商品
INSERT INTO product (id, seller_id, title, price, description, category_id, tags, `condition`, trade_type, trade_mode, location, images, status, created_at) VALUES
-- zhangsan 的商品（在售 + 已售 + 已下架）
(1,  2, 'iPhone 14 99新 128G',          3500.00, '去年买的，使用很少，无划痕，配件齐全。电池健康度 92%。',         1, '苹果,手机',    'LIKE_NEW', 'BOTH',    'SELL', '北京市朝阳区', 'uploads/1/iphone14_1.jpg,uploads/1/iphone14_2.jpg',                         'ACTIVE', '2026-07-07 10:00:00'),
(2,  2, '闲置实木书桌 1.2米',             200.00, '搬家急售，1.2米实木书桌，有轻微使用痕迹但不影响使用。仅限自提。',      3, '书桌,实木',    'USED',     'PICKUP',  'SELL', '北京市海淀区', 'uploads/2/desk_1.jpg',                                                      'ACTIVE', '2026-07-06 15:30:00'),
(3,  2, '罗技 G502 游戏鼠标',             150.00, '几乎全新，用了不到一个月，包装盒都在。',                             1, '鼠标,罗技',    'LIKE_NEW', 'EXPRESS', 'SELL', '北京市朝阳区', 'uploads/3/mouse_1.jpg,uploads/3/mouse_2.jpg',                                 'SOLD',   '2026-07-05 09:00:00'),
(4,  2, '旧机械键盘 ikbc C87',             50.00, '樱桃红轴，用了两年，有几个键帽打油了，功能正常。',                    1, '键盘,ikbc',    'USED',     'EXPRESS', 'SWAP', '北京市朝阳区', 'uploads/4/keyboard_1.jpg',                                                   'ACTIVE', '2026-07-04 14:00:00'),
(5,  2, '九成新戴尔 27寸 4K 显示器',      1800.00, '戴尔 U2723QE，27寸 4K，Type-C 一线通。买了半年，成色很好。',         1, '显示器,戴尔',  'LIKE_NEW', 'BOTH',    'BOTH', '北京市朝阳区', 'uploads/5/monitor_1.jpg,uploads/5/monitor_2.jpg,uploads/5/monitor_3.jpg',    'OFF',    '2026-07-03 11:00:00'),

-- lisi 的商品（含可交换商品）
(6,  3, '冬季羽绒服 中长款 L码',           299.00, '只穿过一季，洗干净了。中长款，L码，适合170-180。',                     2, '羽绒服,冬季',  'LIKE_NEW', 'BOTH',    'SELL', '上海市浦东新区', 'uploads/6/jacket_1.jpg,uploads/6/jacket_2.jpg',                                'ACTIVE', '2026-07-07 08:00:00'),
(7,  3, '全新未拆封蓝牙音箱 JBL Flip6',    500.00, '年会中奖奖品，全新未拆封，JBL Flip6 蓝牙音箱。',                       1, '音箱,JBL',     'NEW',      'EXPRESS', 'BOTH', '上海市浦东新区', 'uploads/7/speaker_1.jpg',                                                     'ACTIVE', '2026-07-06 20:00:00'),
(8,  3, '编程入门经典《代码大全》',          45.00, '代码大全第二版，有少量笔记划过线，不影响阅读。',                        4, '编程,书籍',    'USED',     'EXPRESS', 'SWAP', '上海市浦东新区', 'uploads/8/book_1.jpg',                                                        'ACTIVE', '2026-07-05 12:00:00'),

-- wangwu 的商品
(9,  4, '索尼 WH-1000XM5 降噪耳机',       1800.00, '用了三个月，降噪效果一流。配件齐全，箱说全。',                         1, '耳机,索尼',    'LIKE_NEW', 'BOTH',    'SELL', '广州市天河区', 'uploads/9/headphone_1.jpg,uploads/9/headphone_2.jpg',                          'ACTIVE', '2026-07-07 16:00:00'),
(10, 4, '简约北欧风台灯',                    80.00, '宜家台灯，带暖光灯泡，成色很新。',                                     3, '台灯,宜家',    'LIKE_NEW', 'PICKUP',  'BOTH', '广州市天河区', 'uploads/10/lamp_1.jpg',                                                         'ACTIVE', '2026-07-06 10:00:00');

ALTER TABLE product AUTO_INCREMENT = 11;

-- 3.4 订单数据
-- 覆盖：完整的现金交易流程 + 以物易物流程 + 取消 + 退款纠纷

INSERT INTO `order` (id, order_no, order_type, product_id, swap_product_id, buyer_id, seller_id, status, amount, logistics_info, cancel_reason, refund_reason, swap_note, created_at) VALUES
-- === 现金订单（CASH）===

-- 订单1：完整交易流程（已完成，双方可互评）
-- zhangsan(卖家) 的罗技鼠标 被 lisi(买家) 购买
(1, 'ORD-20260705-00001', 'CASH', 3, NULL, 3, 2, 'COMPLETED', 150.00, '快递：SF1234567890', NULL, NULL, NULL, '2026-07-05 09:30:00'),

-- 订单2：待付款（刚下单）
-- wangwu 的耳机 被 zhangsan 下单
(2, 'ORD-20260707-00002', 'CASH', 9, NULL, 2, 4, 'PENDING_PAY', 1800.00, NULL, NULL, NULL, NULL, '2026-07-07 18:00:00'),

-- 订单3：已付款，等待发货
-- zhangsan 的 iPhone 被 wangwu 购买
(3, 'ORD-20260707-00003', 'CASH', 1, NULL, 4, 2, 'PAID', 3500.00, NULL, NULL, NULL, NULL, '2026-07-07 12:00:00'),

-- 订单4：已发货，等待收货
-- lisi 的羽绒服 被 zhaoliu 购买
(4, 'ORD-20260707-00004', 'CASH', 6, NULL, 5, 3, 'SHIPPED', 299.00, '快递：YT9876543210', NULL, NULL, NULL, '2026-07-07 14:00:00'),

-- 订单5：已收货，买家申请退款中（纠纷）
-- zhangsan 的显示器 被 lisi 购买（注意：实际 product 5 状态是 OFF，这里作为历史订单数据）
(5, 'ORD-20260704-00005', 'CASH', 5, NULL, 3, 2, 'RECEIVED', 1800.00, '快递：SF1111111111', NULL, '显示器有坏点，与描述不符', NULL, '2026-07-04 16:00:00'),

-- 订单6：已取消（买家未付款时取消）
-- wangwu 的台灯 被 zhangsan 下单后取消
(6, 'ORD-20260706-00006', 'CASH', 10, NULL, 2, 4, 'CANCELLED', 80.00, NULL, '不想要了，抱歉', NULL, NULL, '2026-07-06 11:00:00'),

-- 订单7：退款中（DISPUTE），买家申请退款被卖家拒绝，等待管理员裁定
-- lisi 的蓝牙音箱 被 wangwu 购买
(7, 'ORD-20260706-00007', 'CASH', 7, NULL, 4, 3, 'DISPUTE', 500.00, '快递：ZT5555555555', NULL, '收到后发现是假货', NULL, '2026-07-06 22:00:00'),

-- === 交换订单（SWAP）===

-- 订单8：交换完成（已完成，双方互评）
-- lisi(买家) 用《代码大全》交换 zhangsan(卖家) 的机械键盘
(8, 'SWP-20260705-00001', 'SWAP', 4, 8, 3, 2, 'COMPLETED', 0.00, '', NULL, NULL, '很喜欢机械键盘，愿意用代码大全交换', '2026-07-05 10:00:00'),

-- 订单9：交换待确认
-- zhaoliu 用台灯(10) 交换 wangwu 的耳机(9)
(9, 'SWP-20260707-00002', 'SWAP', 9, 10, 5, 4, 'PENDING_CONFIRM', 0.00, NULL, NULL, NULL, '我特别喜欢这款耳机，愿意用台灯交换，台灯几乎全新', '2026-07-07 19:00:00'),

-- 订单10：交换已拒绝
-- wangwu 用台灯(10) 交换 zhangsan 的 iPhone(1)，被拒绝
(10, 'SWP-20260707-00003', 'SWAP', 1, 10, 4, 2, 'REJECTED', 0.00, NULL, NULL, NULL, '愿意用台灯换你的手机，可补差价', '2026-07-07 13:00:00'),

-- 订单11：交换已确认，等待双方发货
-- zhangsan 用机械键盘(4) 交换 lisi 的蓝牙音箱(7)
(11, 'SWP-20260707-00004', 'SWAP', 7, 4, 2, 3, 'CONFIRMED', 0.00, NULL, NULL, NULL, '用我的机械键盘换你的蓝牙音箱，怎么样', '2026-07-07 15:00:00'),

-- === 额外纠纷订单（用于验收测试）===

-- 订单12：已收货，买家申请退款（纠纷）
-- zhangsan 的 iPhone 被 zhaoliu 购买后退款
(12, 'ORD-20260708-00008', 'CASH', 1, NULL, 5, 2, 'DISPUTE', 3500.00, '快递：SF2222222222', NULL, '收到手机屏幕有划痕，与描述"99新"严重不符', NULL, '2026-07-08 09:00:00'),

-- 订单13：已收货，买家申请退款被拒，进入纠纷
-- lisi 的《代码大全》被 wangwu 购买后退款
(13, 'ORD-20260708-00009', 'CASH', 8, NULL, 4, 3, 'DISPUTE', 45.00, '快递：YT1111111111', NULL, '图书缺页，第3章到第5章全部丢失', NULL, '2026-07-08 14:00:00'),

-- 订单14：已收货，买家申请退款（纠纷）
-- wangwu 的台灯被 zhangsan 购买后退款
(14, 'ORD-20260708-00010', 'CASH', 10, NULL, 2, 4, 'DISPUTE', 80.00, '快递：ZT8888888888', NULL, '台灯底座有裂痕，开关接触不良', NULL, '2026-07-08 16:00:00');

ALTER TABLE `order` AUTO_INCREMENT = 15;

-- 3.5 订单操作日志数据
INSERT INTO order_log (id, order_id, operator_id, action_type, old_status, new_status, detail, created_at) VALUES
-- 订单1（完成流程）：lisi 买 zhangsan 的鼠标
(1,  1, 3, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                     '2026-07-05 09:30:00'),
(2,  1, 3, 'PAY',             'PENDING_PAY',   'PAID',        '买家模拟付款',                                  '2026-07-05 09:35:00'),
(3,  1, 2, 'SHIP',            'PAID',          'SHIPPED',     '卖家发货：SF1234567890',                        '2026-07-05 10:00:00'),
(4,  1, 3, 'RECEIVE',         'SHIPPED',       'RECEIVED',    '买家确认收货',                                  '2026-07-06 14:00:00'),
(5,  1, 1, 'AUTO_COMPLETE',   'RECEIVED',      'COMPLETED',   '系统自动完成（收货超3天）',                      '2026-07-09 14:00:00'),

-- 订单2（待付款）：zhangsan 买 wangwu 的耳机
(6,  2, 2, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                     '2026-07-07 18:00:00'),

-- 订单3（已付款）：wangwu 买 zhangsan 的 iPhone
(7,  3, 4, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                     '2026-07-07 12:00:00'),
(8,  3, 4, 'PAY',             'PENDING_PAY',   'PAID',        '买家模拟付款',                                  '2026-07-07 12:05:00'),

-- 订单4（已发货）：zhaoliu 买 lisi 的羽绒服
(9,  4, 5, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                     '2026-07-07 14:00:00'),
(10, 4, 5, 'PAY',             'PENDING_PAY',   'PAID',        '买家模拟付款',                                  '2026-07-07 14:10:00'),
(11, 4, 3, 'SHIP',            'PAID',          'SHIPPED',     '卖家发货：YT9876543210',                        '2026-07-07 15:00:00'),

-- 订单5（已收货 + 退款申请）：lisi 买 zhangsan 的显示器
(12, 5, 3, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                     '2026-07-04 16:00:00'),
(13, 5, 3, 'PAY',             'PENDING_PAY',   'PAID',        '买家模拟付款',                                  '2026-07-04 16:10:00'),
(14, 5, 2, 'SHIP',            'PAID',          'SHIPPED',     '卖家发货：SF1111111111',                        '2026-07-04 17:00:00'),
(15, 5, 3, 'RECEIVE',         'SHIPPED',       'RECEIVED',    '买家确认收货',                                  '2026-07-05 10:00:00'),
(16, 5, 3, 'REQUEST_REFUND',  'RECEIVED',      'RECEIVED',    '买家申请退款：显示器有坏点，与描述不符',           '2026-07-05 11:00:00'),

-- 订单6（已取消）：zhangsan 买 wangwu 的台灯后取消
(17, 6, 2, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                     '2026-07-06 11:00:00'),
(18, 6, 2, 'CANCEL',          'PENDING_PAY',   'CANCELLED',   '买家取消订单：不想要了，抱歉',                     '2026-07-06 11:30:00'),

-- 订单7（退款纠纷 DISPUTE）：wangwu 买 lisi 的蓝牙音箱
(19, 7, 4, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                     '2026-07-06 22:00:00'),
(20, 7, 4, 'PAY',             'PENDING_PAY',   'PAID',        '买家模拟付款',                                  '2026-07-06 22:05:00'),
(21, 7, 3, 'SHIP',            'PAID',          'SHIPPED',     '卖家发货：ZT5555555555',                        '2026-07-07 09:00:00'),
(22, 7, 4, 'RECEIVE',         'SHIPPED',       'RECEIVED',    '买家确认收货',                                  '2026-07-07 16:00:00'),
(23, 7, 4, 'REQUEST_REFUND',  'RECEIVED',      'RECEIVED',    '买家申请退款：收到后发现是假货',                    '2026-07-07 16:30:00'),
(24, 7, 3, 'REJECT_REFUND',   'RECEIVED',      'DISPUTE',     '卖家拒绝退款：正品无疑，可以提供购买凭证',           '2026-07-07 17:00:00'),

-- 订单8（交换完成）：lisi 用《代码大全》换 zhangsan 的机械键盘
(25, 8, 3, 'CREATE',          NULL,            'PENDING_CONFIRM', '买家发起交换提议：很喜欢机械键盘，愿意用代码大全交换', '2026-07-05 10:00:00'),
(26, 8, 2, 'AGREE_SWAP',      'PENDING_CONFIRM','CONFIRMED',       '卖家同意交换',                                     '2026-07-05 11:00:00'),
(27, 8, 3, 'SHIP',            'CONFIRMED',     'CONFIRMED',        '买家(交换方)发货',                                  '2026-07-05 14:00:00'),
(28, 8, 2, 'SHIP',            'CONFIRMED',     'BOTH_SHIPPED',     '卖家(交换方)发货',                                  '2026-07-05 15:00:00'),
(29, 8, 3, 'RECEIVE',         'BOTH_SHIPPED',  'BOTH_SHIPPED',     '买家确认收货',                                     '2026-07-07 10:00:00'),
(30, 8, 2, 'RECEIVE',         'BOTH_SHIPPED',  'COMPLETED',        '卖家确认收货，交换完成',                              '2026-07-07 12:00:00'),

-- 订单9（交换待确认）：zhaoliu 用台灯换 wangwu 的耳机
(31, 9, 5, 'CREATE',          NULL,            'PENDING_CONFIRM', '买家发起交换提议：我特别喜欢这款耳机，愿意用台灯交换，台灯几乎全新', '2026-07-07 19:00:00'),

-- 订单10（交换已拒绝）：wangwu 用台灯换 zhangsan 的 iPhone
(32, 10, 4, 'CREATE',         NULL,            'PENDING_CONFIRM', '买家发起交换提议：愿意用台灯换你的手机，可补差价',       '2026-07-07 13:00:00'),
(33, 10, 2, 'REJECT_SWAP',    'PENDING_CONFIRM','REJECTED',       '卖家拒绝交换：价值差距太大，不考虑',                    '2026-07-07 13:30:00'),

-- 订单11（交换已确认，一方已发货）：zhangsan 用机械键盘换 lisi 的蓝牙音箱
(34, 11, 2, 'CREATE',         NULL,            'PENDING_CONFIRM', '买家发起交换提议：用我的机械键盘换你的蓝牙音箱，怎么样', '2026-07-07 15:00:00'),
(35, 11, 3, 'AGREE_SWAP',     'PENDING_CONFIRM','CONFIRMED',       '卖家同意交换',                                        '2026-07-07 15:30:00'),
(36, 11, 2, 'SHIP',           'CONFIRMED',     'CONFIRMED',        '买家(交换方)发货',                                    '2026-07-07 16:00:00'),

-- 订单12（纠纷）：zhaoliu 买 zhangsan 的 iPhone，申请退款
(37, 12, 5, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                               '2026-07-08 09:00:00'),
(38, 12, 5, 'PAY',             'PENDING_PAY',   'PAID',        '买家模拟付款',                                            '2026-07-08 09:10:00'),
(39, 12, 2, 'SHIP',            'PAID',          'SHIPPED',     '卖家发货：SF2222222222',                                  '2026-07-08 10:00:00'),
(40, 12, 5, 'RECEIVE',         'SHIPPED',       'RECEIVED',    '买家确认收货',                                            '2026-07-08 14:00:00'),
(41, 12, 5, 'REQUEST_REFUND',  'RECEIVED',      'RECEIVED',    '买家申请退款：收到手机屏幕有划痕，与描述"99新"严重不符',       '2026-07-08 14:30:00'),
(42, 12, 2, 'REJECT_REFUND',   'RECEIVED',      'DISPUTE',     '卖家拒绝退款：发货前已拍照留证，屏幕完好无损',                 '2026-07-08 15:00:00'),

-- 订单13（纠纷）：wangwu 买 lisi 的《代码大全》，申请退款
(43, 13, 4, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                               '2026-07-08 14:00:00'),
(44, 13, 4, 'PAY',             'PENDING_PAY',   'PAID',        '买家模拟付款',                                            '2026-07-08 14:10:00'),
(45, 13, 3, 'SHIP',            'PAID',          'SHIPPED',     '卖家发货：YT1111111111',                                  '2026-07-08 15:00:00'),
(46, 13, 4, 'RECEIVE',         'SHIPPED',       'RECEIVED',    '买家确认收货',                                            '2026-07-08 20:00:00'),
(47, 13, 4, 'REQUEST_REFUND',  'RECEIVED',      'RECEIVED',    '买家申请退款：图书缺页，第3章到第5章全部丢失',                 '2026-07-08 20:30:00'),
(48, 13, 3, 'REJECT_REFUND',   'RECEIVED',      'DISPUTE',     '卖家拒绝退款：发货时完整无缺，可能是快递损坏',                 '2026-07-08 21:00:00'),

-- 订单14（纠纷）：zhangsan 买 wangwu 的台灯，申请退款
(49, 14, 2, 'CREATE',          NULL,            'PENDING_PAY', '买家下单',                                               '2026-07-08 16:00:00'),
(50, 14, 2, 'PAY',             'PENDING_PAY',   'PAID',        '买家模拟付款',                                            '2026-07-08 16:10:00'),
(51, 14, 4, 'SHIP',            'PAID',          'SHIPPED',     '卖家发货：ZT8888888888',                                  '2026-07-08 17:00:00'),
(52, 14, 2, 'RECEIVE',         'SHIPPED',       'RECEIVED',    '买家确认收货',                                            '2026-07-09 08:00:00'),
(53, 14, 2, 'REQUEST_REFUND',  'RECEIVED',      'DISPUTE',     '买家申请退款：台灯底座有裂痕，开关接触不良',                    '2026-07-09 08:30:00');

ALTER TABLE order_log AUTO_INCREMENT = 54;

-- 3.6 聊天消息数据
INSERT INTO message (id, sender_id, receiver_id, product_id, content, is_read, created_at) VALUES
-- zhangsan 与 lisi 关于 iPhone 14 的对话
(1,  3, 2, 1, '你好，iPhone 14 还在吗？',                                                           1, '2026-07-07 10:30:00'),
(2,  2, 3, 1, '在的，还在。',                                                                         1, '2026-07-07 10:35:00'),
(3,  3, 2, 1, '价格能便宜点吗？3000 出吗？',                                                           1, '2026-07-07 10:38:00'),
(4,  2, 3, 1, '最低 3300，已经很新了，电池还有 92%',                                                    0, '2026-07-07 10:42:00'),

-- wangwu 与 zhangsan 关于耳机的对话
(5,  4, 2, 9, '这耳机还在吗？什么时候买的？',                                                           1, '2026-07-07 17:00:00'),
(6,  2, 4, 9, '你好，这耳机不是我的，我是想买的那一方😅',                                                 1, '2026-07-07 17:05:00'),
(7,  4, 2, 9, '哈哈不好意思，搞错了',                                                                   1, '2026-07-07 17:06:00'),

-- zhaoliu 与 lisi 关于羽绒服的对话
(8,  5, 3, 6, '请问这件羽绒服充绒量多少？',                                                               1, '2026-07-07 14:20:00'),
(9,  3, 5, 6, '你好，大概 200g 左右，中长款保暖效果很好',                                                  1, '2026-07-07 14:25:00'),
(10, 5, 3, 6, '好的，我下单了',                                                                         0, '2026-07-07 14:28:00');

ALTER TABLE message AUTO_INCREMENT = 11;

-- 3.7 评价数据
INSERT INTO rating (id, order_id, rater_id, rated_user_id, score, created_at) VALUES
-- 订单1（完成交易）：lisi 和 zhangsan 互评
(1, 1, 3, 2, 5, '2026-07-09 15:00:00'),  -- lisi 评 zhangsan：5星
(2, 1, 2, 3, 5, '2026-07-09 16:00:00'),  -- zhangsan 评 lisi：5星

-- 订单8（交换完成）：lisi 和 zhangsan 互评
(3, 8, 3, 2, 4, '2026-07-07 13:00:00'),  -- lisi 评 zhangsan：4星（键盘有点打油但能用）
(4, 8, 2, 3, 5, '2026-07-07 14:00:00');  -- zhangsan 评 lisi：5星

ALTER TABLE rating AUTO_INCREMENT = 5;

-- 3.8 举报数据（覆盖各种状态）
INSERT INTO report (id, reporter_id, target_type, target_id, reason, description, status, admin_note, appeal_reason, appeal_result, pre_appeal_status, created_at) VALUES
-- 举报1：wangwu 举报 lisi 的蓝牙音箱是假货（待处理 PENDING）
(1, 4, 'PRODUCT', 7, 'FAKE_DESC', '收到后发现音质很差，logo印刷模糊，怀疑是假货', 'PENDING', NULL, NULL, NULL, NULL, '2026-07-07 18:00:00'),

-- 举报2：zhaoliu 举报 sunqi 骚扰（待处理 PENDING）
(2, 5, 'USER', 6, 'HARASS', '这个人在聊天中多次发送不雅消息，已截图保留', 'PENDING', NULL, NULL, NULL, NULL, '2026-07-07 20:00:00'),

-- 举报3：zhangsan 举报一个违禁品商品（已驳回 REJECTED）
(3, 2, 'PRODUCT', 5, 'PROHIBITED', '这个显示器描述说不支持退货，存在欺诈嫌疑', 'REJECTED', '经核实，该商品信息属实，不支持退货属于卖家个人意愿，不构成违规', NULL, NULL, NULL, '2026-07-04 18:00:00'),

-- 举报4：lisi 举报虚假描述（待处理 PENDING）
(4, 3, 'PRODUCT', 10, 'FAKE_DESC', '台灯描述写"几乎全新"，但实物有明显的磕碰和划痕', 'PENDING', NULL, NULL, NULL, NULL, '2026-07-07 21:00:00'),

-- 举报5：wangwu 举报 zhangsan 的 iPhone 描述不符（已受理 ACCEPTED）
(5, 4, 'PRODUCT', 1, 'FAKE_DESC', '标题写99新，但边框有明显磕碰，电池健康度只有85%不是92%', 'ACCEPTED', '经核实属实，商品已下架', NULL, NULL, NULL, '2026-07-05 10:00:00'),

-- 举报6：zhaoliu 举报诈骗行为（已受理 ACCEPTED）
(6, 5, 'USER', 2, 'FRAUD', 'zhangsan 让我先微信转账再发货，疑似诈骗', 'ACCEPTED', '经核实属实，已对卖家进行警告处理', NULL, NULL, NULL, '2026-07-06 15:00:00'),

-- 举报7：zhangsan 申诉（申诉中 APPEALING）— 举报5已被受理，zhangsan申诉
(7, 2, 'PRODUCT', 1, 'FAKE_DESC', '这是恶意举报，手机确实99新', 'APPEALING', '经核实属实，商品已下架', '手机确实没有磕碰，电池健康度我有截图证明是92%', NULL, 'ACCEPTED', '2026-07-05 12:00:00'),

-- 举报8：sunqi 举报骚扰（已驳回 REJECTED — 申诉后维持原判）
(8, 6, 'USER', 5, 'HARASS', 'zhaoliu 不断给我发骚扰消息', 'REJECTED', '经核实不构成骚扰，属于正常交流', '他每天都给我发几十条消息问在吗', 'UPHELD', 'REJECTED', '2026-07-03 09:00:00'),

-- 举报9：wangwu 举报违禁品（待处理 PENDING）
(9, 4, 'PRODUCT', 6, 'PROHIBITED', '羽绒服里可能填充了不明材料，标签信息模糊', 'PENDING', NULL, NULL, NULL, NULL, '2026-07-08 08:00:00'),

-- 举报10：zhaoliu 举报消息骚扰（已受理 ACCEPTED，后被申诉改判）
(10, 5, 'MESSAGE', 8, 'HARASS', 'lisi 在聊天中持续发送商业推广链接', 'REJECTED', NULL, '他只是分享了一个商品链接而已', 'OVERTURNED', 'ACCEPTED', '2026-07-06 10:00:00');

ALTER TABLE report AUTO_INCREMENT = 11;


-- ----------------------------
-- 4. 验证查询（可选，用于确认数据完整性）
-- ----------------------------

-- 查看各表记录数
-- SELECT 'user' AS table_name, COUNT(*) AS cnt FROM `user`
-- UNION ALL SELECT 'category', COUNT(*) FROM category
-- UNION ALL SELECT 'product', COUNT(*) FROM product
-- UNION ALL SELECT 'order', COUNT(*) FROM `order`
-- UNION ALL SELECT 'order_log', COUNT(*) FROM order_log
-- UNION ALL SELECT 'message', COUNT(*) FROM message
-- UNION ALL SELECT 'rating', COUNT(*) FROM rating
-- UNION ALL SELECT 'report', COUNT(*) FROM report;

-- 查看全部订单（含类型和状态）
-- SELECT id, order_no, order_type, status, buyer_id, seller_id, amount, created_at FROM `order` ORDER BY created_at DESC;

-- 查看待处理举报
-- SELECT r.id, u.username AS reporter, r.target_type, r.target_id, r.reason, r.status, r.created_at
-- FROM report r JOIN `user` u ON r.reporter_id = u.id
-- WHERE r.status = 'PENDING';
