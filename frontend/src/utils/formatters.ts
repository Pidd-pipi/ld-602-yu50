export const formatDate = (value: string) => new Date(value).toLocaleString("zh-CN");
export const formatStatus = (value: string) => value.replace(/_/g, " ");
export const formatNumber = (value: number) => new Intl.NumberFormat("zh-CN").format(value);
export const formatRisk = (value: string) => ({ LOW: "低", MEDIUM: "中", HIGH: "高", CRITICAL: "严重", EXTREME: "极高" }[value] ?? value);

const pad2 = (value: number) => String(value).padStart(2, "0");

/** 交接时段统一展示为 MM-dd HH:mm ~ MM-dd HH:mm */
export const formatDateTimeShort = (value: string) => {
  const date = new Date(value);
  return `${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}`;
};

export const formatShiftWindow = (start: string, end: string) =>
  `${formatDateTimeShort(start)} ~ ${formatDateTimeShort(end)}`;

export const formatHandoverCode = (id: number) => `HO-${String(id).padStart(4, "0")}`;
