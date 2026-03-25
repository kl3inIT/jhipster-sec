import { inject, isDevMode } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { AccountService } from 'app/core/auth/account.service';
import { NavigationService } from 'app/layout/navigation/navigation.service';
import { StateStorageService } from './state-storage.service';

export const UserRouteAccessService: CanActivateFn = (next: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const accountService = inject(AccountService);
  const navigationService = inject(NavigationService);
  const router = inject(Router);
  const stateStorageService = inject(StateStorageService);

  return accountService.identity().pipe(
    switchMap(account => {
      if (account) {
        const navigationNodeId = next.data['navigationNodeId'] as string | undefined;
        const blockedLabelKey =
          (next.data['breadcrumbKey'] as string | undefined) ??
          (navigationNodeId ? navigationService.getLeaf(navigationNodeId)?.breadcrumbKey : undefined);
        const sectionId = next.data['sectionId'] as string | undefined;
        const { authorities } = next.data;

        if (navigationNodeId) {
          return navigationService.isNodeVisible(navigationNodeId).pipe(
            map(isVisible => {
              if (isVisible) {
                return true;
              }

              router.navigate(['/accessdenied'], {
                state: {
                  blockedUrl: state.url,
                  blockedLabelKey,
                  sectionId,
                },
              });
              return false;
            }),
          );
        }

        if (!authorities || authorities.length === 0 || accountService.hasAnyAuthority(authorities)) {
          return of(true);
        }

        if (isDevMode()) {
          console.error('User does not have any of the required authorities:', authorities);
        }
        router.navigate(['/accessdenied'], {
          state: {
            blockedUrl: state.url,
            blockedLabelKey,
            sectionId,
          },
        });
        return of(false);
      }

      stateStorageService.storeUrl(state.url);
      router.navigate(['/login']);
      return of(false);
    }),
  );
};
