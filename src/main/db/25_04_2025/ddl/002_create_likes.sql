CREATE TABLE likes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  liker_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  liked_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0-liked, 1-canceled',
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_likes_liker` (liker_id),
  KEY `idx_likes_target` (target_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE likes_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  liker_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0-liked, 1-canceled',
  KEY `idx_likes_liker` (liker_id),
  KEY `idx_likes_target` (target_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


