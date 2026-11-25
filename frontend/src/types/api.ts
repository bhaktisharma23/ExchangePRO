/** RFC 9457 Problem Details body produced by the backend GlobalExceptionHandler. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  code?: string;
  correlationId?: string;
  timestamp?: string;
  errors?: { field: string; message: string }[];
}
