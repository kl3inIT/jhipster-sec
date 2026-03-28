import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extracts a user-facing message from JHipster/RFC7807-style error bodies or HttpErrorResponse.
 */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error;
    if (body && typeof body === 'object' && 'detail' in body) {
      const detail = (body as { detail?: unknown }).detail;
      if (typeof detail === 'string' && detail.trim().length > 0) {
        return detail;
      }
    }
    if (typeof body === 'string' && body.trim().length > 0) {
      return body;
    }
    if (err.message) {
      return err.message;
    }
  }
  return fallback;
}
