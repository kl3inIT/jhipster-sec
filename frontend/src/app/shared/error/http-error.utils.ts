import { MessageService } from 'primeng/api';

export function handleHttpError(messageService: MessageService, err: any, fallbackDetail?: string): void {
  if (err.status === 403) {
    messageService.add({ severity: 'warn', summary: 'Access denied', detail: 'You do not have permission to perform this action.' });
  } else {
    messageService.add({ severity: 'error', summary: 'Error', detail: fallbackDetail ?? 'An unexpected error occurred. Please try again.' });
  }
}
