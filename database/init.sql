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

-- 值班交接单：同一仓库重叠时段只允许一张有效（非 CLOSED）交接单，冲突校验由服务层半开区间判定
CREATE TABLE IF NOT EXISTS shift_handover (
  id INTEGER PRIMARY KEY,
  warehouse_id INTEGER NOT NULL,
  manager_name TEXT NOT NULL,
  shift_start TEXT NOT NULL,
  shift_end TEXT NOT NULL,
  status TEXT NOT NULL,
  claimed_by TEXT,
  claimed_at TEXT,
  created_at TEXT NOT NULL,
  closed_at TEXT
);

CREATE TABLE IF NOT EXISTS shift_handover_item (
  id INTEGER PRIMARY KEY,
  handover_id INTEGER NOT NULL,
  item_seq INTEGER NOT NULL,
  type TEXT NOT NULL,
  content TEXT NOT NULL,
  assignee TEXT NOT NULL,
  status TEXT NOT NULL,
  result_note TEXT,
  completed_at TEXT
);

INSERT INTO shift_handover (id, warehouse_id, manager_name, shift_start, shift_end, status, claimed_by, claimed_at, created_at, closed_at)
VALUES
  (1, 1, '周敏（交班）', '2026-09-24T08:00:00Z', '2026-09-24T20:00:00Z', 'CLAIMED', '李航', '2026-09-24T08:05:00Z', '2026-09-24T07:50:00Z', NULL),
  (2, 2, '陈立（交班）', '2026-09-23T20:00:00Z', '2026-09-24T08:00:00Z', 'CLOSED', '赵磊', '2026-09-23T21:00:00Z', '2026-09-23T19:00:00Z', '2026-09-24T07:40:00Z');

INSERT INTO shift_handover_item (id, handover_id, item_seq, type, content, assignee, status, result_note, completed_at)
VALUES
  (1, 1, 1, 'INBOUND', '300 箱瓶装水到货待验收入库（采购单 PO-2317）', '李航', 'PENDING', NULL, NULL),
  (2, 1, 2, 'EXPIRE', '批次 BN-20260801 压缩饼干 30 天内到期，需优先安排调拨', '李航', 'DONE', '已与鼓楼避难点确认接收，明早出库', '2026-09-24T10:15:00Z'),
  (3, 1, 3, 'EXCEPTION', 'A 区货架 2 层急救包账实不符，盘亏 4 件，待复核', '王倩', 'PENDING', NULL, NULL),
  (4, 2, 1, 'EXCEPTION', '冷藏柜温度短时告警，已恢复并复测', '赵磊', 'DONE', '温度回归 4℃，记录已存档', '2026-09-24T06:40:00Z');
