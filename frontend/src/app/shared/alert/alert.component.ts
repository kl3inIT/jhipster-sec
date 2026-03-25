import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MessageModule } from 'primeng/message';

import { Alert, AlertService, AlertType } from 'app/core/util/alert.service';

@Component({
  selector: 'jhi-alert',
  standalone: true,
  imports: [CommonModule, MessageModule],
  template: `
    <div class="mb-3 flex flex-column gap-2">
      @for (alert of alertService.alerts(); track alert.id) {
        <p-message
          [severity]="resolveSeverity(alert.type)"
          [text]="alert.message"
          [closable]="true"
          (onClose)="close(alert)"
        />
      }
    </div>
  `,
})
export class AlertComponent {
  readonly alertService = inject(AlertService);

  close(alert: Alert): void {
    alert.close?.(this.alertService.get());
  }

  resolveSeverity(type: AlertType): 'success' | 'info' | 'warn' | 'error' {
    if (type === 'danger') {
      return 'error';
    }
    if (type === 'warning') {
      return 'warn';
    }
    return type;
  }
}
