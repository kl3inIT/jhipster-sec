import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { CheckboxModule } from 'primeng/checkbox';
import { TagModule } from 'primeng/tag';

import { WorkspaceContextService } from 'app/pages/entities/shared/service/workspace-context.service';
import { IUser, IUserRoleRow } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import { buildAuthorityRows } from '../shared/authority-label.util';

@Component({
  selector: 'app-user-management-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslatePipe, CardModule, ButtonModule, TableModule, CheckboxModule, TagModule],
  templateUrl: './user-management-detail.component.html',
})
export default class UserManagementDetailComponent implements OnInit {
  user = signal<IUser | null>(null);
  roleRows = signal<IUserRoleRow[]>([]);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translateService = inject(TranslateService);
  private readonly userManagementService = inject(UserManagementService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as string | undefined;

  ngOnInit(): void {
    const resolvedUser = this.route.snapshot.data['user'] as IUser | null;
    this.user.set(resolvedUser);

    this.userManagementService.authorities().subscribe(allAuthorities => {
      const selected = resolvedUser?.authorities ?? [];
      this.roleRows.set(buildAuthorityRows(allAuthorities, selected, this.translateService, true));
    });
  }

  back(): void {
    const context = this.navigationNodeId ? this.workspaceContextService.get(this.navigationNodeId) : null;
    this.router.navigate(['/admin/users'], context ? { queryParams: context.queryParams } : undefined);
  }

  edit(): void {
    const user = this.user();
    if (user?.login) {
      this.router.navigate(['/admin/users', user.login, 'edit']);
    }
  }
}
