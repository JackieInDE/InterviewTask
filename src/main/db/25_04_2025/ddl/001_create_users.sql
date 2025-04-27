CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  job VARCHAR(50),
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '0-Woman, 1-Man',
  birthday DATE NOT NULL,
  location_id INT NOT NULL,
  account_status VARCHAR(20) DEFAULT 'ACTIVE',
  relationship_status ENUM('Single', 'Married', 'AlreadyTaken', 'NotSpecified')  DEFAULT 'NotSpecified',
  profile_picture_id BIGINT,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(100) NOT NULL DEFAULT 'sys',
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by VARCHAR(100) NOT NULL DEFAULT 'sys',
  KEY `idx_name` (name),
  KEY `idx_created_time` (created_time)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


