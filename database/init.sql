CREATE TABLE IF NOT EXISTS warehouse (
  id INTEGER PRIMARY KEY,
  name TEXT,
  district TEXT,
  address TEXT,
  manager_id TEXT,
  capacity_level TEXT,
  contact_phone TEXT,
  status TEXT
);

CREATE TABLE IF NOT EXISTS supply_item (
  id INTEGER PRIMARY KEY,
  sku_code TEXT,
  name TEXT,
  category TEXT,
  unit TEXT,
  safety_stock TEXT,
  expire_days TEXT,
  storage_requirement TEXT
);

CREATE TABLE IF NOT EXISTS inventory_batch (
  id INTEGER PRIMARY KEY,
  warehouse_id TEXT,
  supply_item_id TEXT,
  batch_no TEXT,
  quantity TEXT,
  expire_at TEXT,
  inbound_source TEXT,
  quality_status TEXT
);

CREATE TABLE IF NOT EXISTS shelter (
  id INTEGER PRIMARY KEY,
  name TEXT,
  district TEXT,
  capacity TEXT,
  current_population TEXT,
  contact_person TEXT,
  risk_level TEXT,
  open_status TEXT
);

CREATE TABLE IF NOT EXISTS dispatch_order (
  id INTEGER PRIMARY KEY,
  event_id TEXT,
  source_warehouse_id TEXT,
  shelter_id TEXT,
  priority TEXT,
  status TEXT,
  requested_by TEXT,
  approved_by TEXT,
  dispatched_at TEXT
);

CREATE TABLE IF NOT EXISTS audit_log (
  id INTEGER PRIMARY KEY,
  actor TEXT,
  action TEXT,
  target_type TEXT,
  target_id TEXT,
  created_at TEXT
);

-- 值班交接单：同一仓库重叠时段只允许存在一张非 CLOSED 的有效交接单（PENDING_CLAIM / CLAIMED）
CREATE TABLE IF NOT EXISTS shift_handover (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  warehouse_id BIGINT NOT NULL,
  shift_owner VARCHAR(64) NOT NULL COMMENT '当班负责人（交班人）',
  claimed_by VARCHAR(64) NULL COMMENT '领取交接单的新值班员',
  shift_start_at DATETIME NOT NULL,
  shift_end_at DATETIME NOT NULL,
  remark TEXT NULL,
  status VARCHAR(32) NOT NULL COMMENT 'PENDING_CLAIM / CLAIMED / CLOSED',
  created_at DATETIME NULL,
  claimed_at DATETIME NULL,
  closed_at DATETIME NULL,
  KEY idx_shift_handover_warehouse (warehouse_id),
  KEY idx_shift_handover_status (status)
) COMMENT='仓库值班交接单';

-- 交接事项：逐条登记待入库/临期批次/异常库存等与接手人，全部办结才允许关闭交接单
CREATE TABLE IF NOT EXISTS handover_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  handover_id BIGINT NOT NULL,
  category VARCHAR(32) NOT NULL COMMENT 'INBOUND_PENDING / EXPIRING_BATCH / ABNORMAL_INVENTORY / OTHER',
  content TEXT NOT NULL,
  assignee VARCHAR(64) NOT NULL COMMENT '接手人',
  batch_no VARCHAR(64) NULL COMMENT '关联库存批次号，仅用于定位，处理不回写库存',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / DONE',
  processed_at DATETIME NULL,
  process_note TEXT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  KEY idx_handover_item_handover (handover_id)
) COMMENT='值班交接事项';
