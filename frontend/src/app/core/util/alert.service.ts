import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { translationNotFoundMessage } from 'app/config/translation.config';

export type AlertType = 'success' | 'danger' | 'warning' | 'info';

export interface Alert {
  id: number;
  type: AlertType;
  message?: string;
  translationKey?: string;
  translationParams?: Record<string, unknown>;
  timeout?: number;
  toast?: boolean;
  position?: string;
  close?: (alerts: Alert[]) => void;
}

@Injectable({
  providedIn: 'root',
})
export class AlertService {
  readonly alerts = signal<Alert[]>([]);
  timeout = 5000;
  toast = false;
  position = 'top-center';

  private alertId = 0;
  private readonly translateService = inject(TranslateService);

  clear(): void {
    this.alerts.set([]);
  }

  get(): Alert[] {
    return this.alerts();
  }

  addAlert(alertToAdd: Omit<Alert, 'id'>, extAlerts?: Alert[]): Alert {
    const translatedMessage = alertToAdd.translationKey
      ? this.translateService.instant(alertToAdd.translationKey, alertToAdd.translationParams)
      : alertToAdd.message;
    const message =
      alertToAdd.translationKey && translatedMessage !== `${translationNotFoundMessage}[${alertToAdd.translationKey}]`
        ? translatedMessage
        : (alertToAdd.message ?? alertToAdd.translationKey ?? '');

    const alert: Alert = {
      ...alertToAdd,
      id: this.alertId++,
      message,
      timeout: alertToAdd.timeout ?? this.timeout,
      toast: alertToAdd.toast ?? this.toast,
      position: alertToAdd.position ?? this.position,
    };
    alert.close = (alerts: Alert[]) => this.closeAlert(alert.id, extAlerts ?? alerts);

    if (extAlerts) {
      extAlerts.push(alert);
    } else {
      this.alerts.update(alerts => [...alerts, alert]);
    }

    const timeout = alert.timeout ?? 0;
    if (timeout > 0) {
      setTimeout(() => {
        this.closeAlert(alert.id, extAlerts);
      }, timeout);
    }

    return alert;
  }

  private closeAlert(alertId: number, extAlerts?: Alert[]): void {
    if (extAlerts) {
      const alertIndex = extAlerts.findIndex(alert => alert.id === alertId);
      if (alertIndex >= 0) {
        extAlerts.splice(alertIndex, 1);
      }
      return;
    }

    this.alerts.update(alerts => alerts.filter(alert => alert.id !== alertId));
  }
}
