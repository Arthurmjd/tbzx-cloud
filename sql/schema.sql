-- ============================================================
-- 瞳伴随行 · 家属守护与出行信息服务平台
-- 数据库建表脚本 (MySQL 8.0) · v2.1
-- 与 瞳伴随行-云端/sql/schema.sql v2.1 保持一致
-- 演示数据由应用启动时自动插入 (tbzx.init-demo-data=true):
--   家属 test/123456, 设备 TBZX-DEMO00001, 密钥 demo-key-123456
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tbzx_cloud` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `tbzx_cloud`;

-- ------------------------------------------------------------
-- 1. user — 家属账号
-- ------------------------------------------------------------
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(50)  NOT NULL              COMMENT '登录用户名',
  `password`    VARCHAR(100) NOT NULL              COMMENT 'BCrypt 加密密码',
  `real_name`   VARCHAR(50)  DEFAULT NULL          COMMENT '真实姓名',
  `phone`       VARCHAR(20)  DEFAULT NULL          COMMENT '手机号',
  `avatar_url`  VARCHAR(255) DEFAULT NULL          COMMENT '头像 URL',
  `status`      TINYINT      NOT NULL DEFAULT 1    COMMENT '状态: 0=禁用 1=正常',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家属账号';

-- ------------------------------------------------------------
-- 2. device — 香橙派设备及绑定关系
-- 无 status 列 — 在线状态永远是 last_heartbeat_at >= NOW()-90s 的实时计算
-- ------------------------------------------------------------
CREATE TABLE `device` (
  `id`                 BIGINT        NOT NULL AUTO_INCREMENT,
  `device_id`          VARCHAR(64)   NOT NULL              COMMENT '设备编号 (TBZX-+12位HEX)',
  `device_name`        VARCHAR(50)   DEFAULT NULL          COMMENT '设备别名 (家属可辨认)',
  `api_key`            VARCHAR(64)   NOT NULL              COMMENT '设备上报密钥 SHA-256 (明文仅注册时返回一次)',
  `user_id`            BIGINT        DEFAULT NULL          COMMENT '绑定的家属 user.id (null=未绑定, 可被认领)',
  `battery_level`      INT           DEFAULT NULL          COMMENT '电量百分比 0-100',
  `last_heartbeat_at`  DATETIME      DEFAULT NULL          COMMENT '最后心跳时间 (心跳/位置上报均刷新)',
  `last_point_at`      DATETIME      DEFAULT NULL          COMMENT '最近轨迹点时间 (=该点 point_ts)',
  `last_lat`           DECIMAL(10,7) DEFAULT NULL          COMMENT '最新纬度 (GCJ-02)',
  `last_lng`           DECIMAL(10,7) DEFAULT NULL          COMMENT '最新经度 (GCJ-02)',
  `current_session_id` BIGINT        DEFAULT NULL          COMMENT '进行中的出行 session.id',
  `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  UNIQUE KEY `uk_api_key` (`api_key`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息及绑定关系';

-- ------------------------------------------------------------
-- 3. travel_session — 一次出行记录
-- ------------------------------------------------------------
CREATE TABLE `travel_session` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `device_id`    VARCHAR(64)   NOT NULL              COMMENT '设备编号',
  `start_time`   DATETIME      DEFAULT NULL          COMMENT '出行开始时间',
  `end_time`     DATETIME      DEFAULT NULL          COMMENT '出行结束时间 (null=进行中)',
  `start_lat`    DECIMAL(10,7) DEFAULT NULL          COMMENT '起点纬度 (GCJ-02)',
  `start_lng`    DECIMAL(10,7) DEFAULT NULL          COMMENT '起点经度 (GCJ-02)',
  `end_lat`      DECIMAL(10,7) DEFAULT NULL          COMMENT '终点纬度 (GCJ-02)',
  `end_lng`      DECIMAL(10,7) DEFAULT NULL          COMMENT '终点经度 (GCJ-02)',
  `distance_m`   INT           NOT NULL DEFAULT 0    COMMENT '本次出行里程 (米, 相邻点 haversine 累加)',
  `alert_count`  INT           NOT NULL DEFAULT 0    COMMENT '本次出行告警次数 (冗余计数)',
  `status`       TINYINT       NOT NULL DEFAULT 0    COMMENT '状态: 0=进行中 1=已结束',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_status` (`device_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一次出行记录';

-- ------------------------------------------------------------
-- 4. device_location — 轨迹点 (唯一热表)
-- uk_device_point_ts: 既做 INSERT IGNORE 幂等去重, 又服务 track 的时间窗查询
-- ------------------------------------------------------------
CREATE TABLE `device_location` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `device_id`  VARCHAR(64)   NOT NULL              COMMENT '设备编号',
  `lat`        DECIMAL(10,7) NOT NULL              COMMENT '纬度 (GCJ-02)',
  `lng`        DECIMAL(10,7) NOT NULL              COMMENT '经度 (GCJ-02)',
  `accuracy`   FLOAT         DEFAULT NULL          COMMENT '定位精度 (米)',
  `speed`      FLOAT         DEFAULT NULL          COMMENT '速度 (m/s)',
  `session_id` BIGINT        DEFAULT NULL          COMMENT '所属出行 session.id (轨迹归属键, 落库时填)',
  `point_ts`   DATETIME(3)   NOT NULL              COMMENT '客户端采集时间戳 (去重键)',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端入库时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_point_ts` (`device_id`, `point_ts`),
  KEY `idx_device_session_ts` (`device_id`, `session_id`, `point_ts`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轨迹点';

-- ------------------------------------------------------------
-- 5. alert — 风险告警
-- ref_id: GEOFENCE_EXIT→safe_zone.id; OFFLINE/SOS→travel_session.id
-- ------------------------------------------------------------
CREATE TABLE `alert` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `device_id`   VARCHAR(64)   NOT NULL              COMMENT '设备编号',
  `alert_type`  VARCHAR(30)   NOT NULL              COMMENT '告警类型: VEHICLE_APPROACH / RED_LIGHT / SHORT_TIME / CONSTRUCTION / GEOFENCE_EXIT / SOS / OFFLINE',
  `severity`    TINYINT       NOT NULL DEFAULT 1    COMMENT '严重程度: 0=info 1=warning 2=danger 3=sos',
  `description` VARCHAR(255)  DEFAULT NULL          COMMENT '告警描述',
  `ref_id`      BIGINT        DEFAULT NULL          COMMENT '关联对象 id (GEOFENCE_EXIT→safe_zone.id; OFFLINE/SOS→session.id)',
  `lat`         DECIMAL(10,7) DEFAULT NULL          COMMENT '告警位置纬度 (GCJ-02)',
  `lng`         DECIMAL(10,7) DEFAULT NULL          COMMENT '告警位置经度 (GCJ-02)',
  `has_image`   TINYINT       NOT NULL DEFAULT 0    COMMENT '是否有现场图片: 0=无 1=有',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_time` (`device_id`, `created_at` DESC),
  KEY `idx_ref` (`device_id`, `alert_type`, `ref_id`),
  KEY `idx_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险告警';

-- ------------------------------------------------------------
-- 6. alert_image — 告警现场图片
-- ------------------------------------------------------------
CREATE TABLE `alert_image` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `alert_id`   BIGINT       NOT NULL              COMMENT '关联告警 alert.id',
  `image_path` VARCHAR(255) NOT NULL              COMMENT '服务器存储路径',
  `image_url`  VARCHAR(255) NOT NULL              COMMENT '外部访问 URL',
  `file_size`  INT          DEFAULT NULL          COMMENT '文件大小 (字节)',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alert_id` (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警现场图片';

-- ------------------------------------------------------------
-- 7. safe_zone — 安全区域
-- 不存 user_id — 归属由 zone.device_id → device.user_id 推导, 设备换绑自动跟随
-- ------------------------------------------------------------
CREATE TABLE `safe_zone` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `device_id`  VARCHAR(64)   NOT NULL              COMMENT '设备编号',
  `zone_name`  VARCHAR(50)   NOT NULL              COMMENT '区域名称, 如"家附近"',
  `center_lat` DECIMAL(10,7) NOT NULL              COMMENT '圆心纬度 (GCJ-02)',
  `center_lng` DECIMAL(10,7) NOT NULL              COMMENT '圆心经度 (GCJ-02)',
  `radius_m`   INT           NOT NULL DEFAULT 200  COMMENT '半径 (米)',
  `active`     TINYINT       NOT NULL DEFAULT 1    COMMENT '0=停用 1=启用',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_active` (`device_id`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全区域';
