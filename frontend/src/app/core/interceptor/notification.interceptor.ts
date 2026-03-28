import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { AlertService } from 'app/core/util/alert.service';

@Injectable()
export class NotificationInterceptor implements HttpInterceptor {
  private readonly alertService = inject(AlertService);

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      tap((event: HttpEvent<any>) => {
        if (event instanceof HttpResponse) {
          let alert: string | null = null;
          let alertParams: string | null = null;

          for (const headerKey of event.headers.keys()) {
            if (headerKey.toLowerCase().endsWith('app-alert')) {
              alert = event.headers.get(headerKey);
            } else if (headerKey.toLowerCase().endsWith('app-params')) {
              alertParams = decodeURIComponent((event.headers.get(headerKey) ?? '').replace(/\+/g, ' '));
            }
          }

          if (alert) {
            this.alertService.addAlert({
              type: 'success',
              translationKey: this.normalizeAlertTranslationKey(alert),
              translationParams: { param: alertParams ?? '' },
            });
          }
        }
      }),
    );
  }

  private normalizeAlertTranslationKey(key: string): string {
    const mapping: Record<string, string> = {
      'jhipstersec.userManagement.created': 'userManagement.created',
      'jhipstersec.userManagement.updated': 'userManagement.updated',
      'jhipstersec.userManagement.deleted': 'userManagement.deleted',
      'jhipstersec.adminAuthority.created': 'angappApp.adminAuthority.created',
      'jhipstersec.adminAuthority.updated': 'angappApp.adminAuthority.updated',
      'jhipstersec.adminAuthority.deleted': 'angappApp.adminAuthority.deleted',
      'jhipstersec.organization.created': 'angappApp.organization.created',
      'jhipstersec.organization.updated': 'angappApp.organization.updated',
      'jhipstersec.organization.deleted': 'angappApp.organization.deleted',
      'jhipstersec.department.created': 'angappApp.department.created',
      'jhipstersec.department.updated': 'angappApp.department.updated',
      'jhipstersec.department.deleted': 'angappApp.department.deleted',
      'jhipstersec.secRole.created': 'security.alert.secRole.created',
      'jhipstersec.secRole.updated': 'security.alert.secRole.updated',
      'jhipstersec.secRole.deleted': 'security.alert.secRole.deleted',
      'jhipstersec.secPermission.created': 'security.alert.secPermission.created',
      'jhipstersec.secPermission.updated': 'security.alert.secPermission.updated',
      'jhipstersec.secPermission.deleted': 'security.alert.secPermission.deleted',
    };

    return mapping[key] ?? key;
  }
}
