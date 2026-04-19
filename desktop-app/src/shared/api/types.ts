export interface ApiResponse<T> {
  code: string;
  message: string;
  requestId?: string;
  data: T;
}

export interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: BodyInit | object | null;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

export class ApiClientError extends Error {
  code: string;
  status: number;
  requestId?: string;

  constructor(message: string, code: string, status: number, requestId?: string) {
    super(message);
    this.name = "ApiClientError";
    this.code = code;
    this.status = status;
    this.requestId = requestId;
  }
}
