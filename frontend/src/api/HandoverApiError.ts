export class HandoverApiError extends Error {
  code: string;
  meta?: Record<string, string | number>;
  constructor(code: string, message: string, meta?: Record<string, string | number>) {
    super(message);
    this.name = "HandoverApiError";
    this.code = code;
    this.meta = meta;
  }
}
