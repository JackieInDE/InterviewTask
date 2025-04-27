CREATE TABLE visits (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  visitor_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  visited_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_visits_visitor` (visitor_id),
  KEY `idx_visits_target` (target_id, visited_time)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


