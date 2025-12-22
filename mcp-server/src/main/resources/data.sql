-- Initial data for MCP Server Demo Database

-- Insert Customers
INSERT INTO customers (name, email, phone) VALUES
('张三', 'zhangsan@example.com', '13800138000'),
('李四', 'lisi@example.com', '13900139000'),
('王五', 'wangwu@example.com', '13700137000');

-- Insert Orders
INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
(1, '2024-01-15', 2500.00, 'completed'),
(2, '2024-01-16', 3800.50, 'shipped'),
(1, '2024-01-17', 6200.00, 'paid');

-- Insert Order Items
INSERT INTO order_items (order_id, product_name, quantity, unit_price, subtotal) VALUES
(1, 'MacBook Pro', 1, 15000.00, 15000.00),
(1, 'Magic Mouse', 2, 699.00, 1398.00),
(2, 'iPhone 15', 1, 5999.00, 5999.00),
(2, 'AirPods Pro', 2, 1899.00, 3798.00),
(3, 'iPad Air', 2, 4599.00, 9198.00);
