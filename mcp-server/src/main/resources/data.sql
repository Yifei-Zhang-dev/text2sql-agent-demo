-- Initial data for MCP Server Demo Database
-- 增强的测试数据集，用于 PPT 演示和技术选型对比

-- Insert Customers (10 个客户)
INSERT INTO customers (name, email, phone) VALUES
('张三', 'zhangsan@example.com', '13800138000'),
('李四', 'lisi@example.com', '13900139000'),
('王五', 'wangwu@example.com', '13700137000'),
('赵六', 'zhaoliu@example.com', '13600136000'),
('孙七', 'sunqi@example.com', '13500135000'),
('周八', 'zhouba@example.com', '13400134000'),
('吴九', 'wujiu@example.com', '13300133000'),
('郑十', 'zhengshi@example.com', '13200132000'),
('陈一', 'chenyi@example.com', '13100131000'),
('刘二', 'liuer@example.com', '13000130000');

-- Insert Orders (15 个订单，覆盖多种状态和金额区间)
INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
-- 张三的订单
(1, '2024-01-15', 16398.00, 'completed'),
(1, '2024-02-20', 9198.00, 'completed'),
(1, '2024-03-10', 5999.00, 'shipped'),
-- 李四的订单
(2, '2024-01-16', 9797.00, 'shipped'),
(2, '2024-02-25', 15999.00, 'completed'),
-- 王五的订单
(3, '2024-01-17', 24997.00, 'completed'),
(3, '2024-03-05', 3598.00, 'paid'),
-- 赵六的订单
(4, '2024-02-10', 8998.00, 'shipped'),
(4, '2024-03-15', 12999.00, 'completed'),
-- 孙七的订单
(5, '2024-01-20', 6999.00, 'completed'),
-- 周八的订单
(6, '2024-02-05', 19999.00, 'completed'),
-- 吴九的订单
(7, '2024-01-25', 4599.00, 'pending'),
-- 郑十的订单
(8, '2024-03-01', 2899.00, 'shipped'),
-- 陈一的订单
(9, '2024-02-15', 8999.00, 'completed'),
-- 刘二的订单
(10, '2024-03-08', 11999.00, 'shipped');

-- Insert Order Items (每个订单 1-3 个商品)
INSERT INTO order_items (order_id, product_name, quantity, unit_price, subtotal) VALUES
-- 订单 1: 张三
(1, 'MacBook Pro 14', 1, 15000.00, 15000.00),
(1, 'Magic Mouse', 2, 699.00, 1398.00),
-- 订单 2: 张三
(2, 'iPad Air', 2, 4599.00, 9198.00),
-- 订单 3: 张三
(3, 'iPhone 15', 1, 5999.00, 5999.00),
-- 订单 4: 李四
(4, 'iPhone 15', 1, 5999.00, 5999.00),
(4, 'AirPods Pro', 2, 1899.00, 3798.00),
-- 订单 5: 李四
(5, 'MacBook Air', 1, 9999.00, 9999.00),
(5, 'Apple Watch', 1, 3000.00, 3000.00),
(5, 'USB-C 充电线', 4, 500.00, 2000.00),
-- 订单 6: 王五
(6, 'MacBook Pro 16', 1, 19999.00, 19999.00),
(6, 'AirPods Max', 1, 4999.00, 4999.00),
-- 订单 7: 王五
(7, 'AirPods Pro', 2, 1899.00, 3798.00),
-- 订单 8: 赵六
(8, 'iPad Pro 11', 1, 6999.00, 6999.00),
(8, 'Apple Pencil', 1, 999.00, 999.00),
(8, 'Smart Keyboard', 1, 1000.00, 1000.00),
-- 订单 9: 赵六
(9, 'iPhone 15 Pro', 1, 7999.00, 7999.00),
(9, 'MagSafe 充电器', 2, 2500.00, 5000.00),
-- 订单 10: 孙七
(10, 'iPhone 14', 1, 5999.00, 5999.00),
(10, 'AirTag 4 件装', 1, 1000.00, 1000.00),
-- 订单 11: 周八
(11, 'MacBook Pro 16', 1, 19999.00, 19999.00),
-- 订单 12: 吴九
(12, 'iPad Air', 1, 4599.00, 4599.00),
-- 订单 13: 郑十
(13, 'Apple Watch SE', 1, 1899.00, 1899.00),
(13, 'Watch 表带', 2, 500.00, 1000.00),
-- 订单 14: 陈一
(14, 'iPhone 15 Pro Max', 1, 8999.00, 8999.00),
-- 订单 15: 刘二
(15, 'MacBook Air M2', 1, 8999.00, 8999.00),
(15, 'Magic Keyboard', 1, 1500.00, 1500.00),
(15, 'Magic Trackpad', 1, 1500.00, 1500.00);
