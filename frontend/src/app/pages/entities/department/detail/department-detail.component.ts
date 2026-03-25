import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';

import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { IDepartment } from '../department.model';
import { DepartmentService } from '../service/department.service';

@Component({
  selector: 'app-department-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, CardModule, ButtonModule],
  templateUrl: './department-detail.component.html',
})
export default class DepartmentDetailComponent implements OnInit {
  department = signal<IDepartment | null>(null);
  capabilityLoaded = signal(false);
  canUpdate = signal(false);
  fieldVisibility = signal<Record<string, boolean>>({});

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly departmentService = inject(DepartmentService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as
    | string
    | undefined;

  ngOnInit(): void {
    const capability = (this.route.snapshot.data['capability'] ??
      null) as ISecuredEntityCapability | null;
    this.canUpdate.set(capability?.canUpdate ?? false);
    if (capability?.attributes) {
      const vis: Record<string, boolean> = {};
      for (const attr of capability.attributes) {
        vis[attr.name] = attr.canView;
      }
      this.fieldVisibility.set(vis);
    }
    this.capabilityLoaded.set(true);

    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.departmentService.find(Number(id)).subscribe({
          next: (res) => this.department.set(res.body ?? null),
          error: (err: any) => {
            if (err.status === 404) {
              this.router.navigate(['/404']);
            }
          },
        });
      }
    });
  }

  back(): void {
    const context = this.navigationNodeId
      ? this.workspaceContextService.get(this.navigationNodeId)
      : null;
    this.router.navigate(
      ['/entities/department'],
      context ? { queryParams: context.queryParams } : undefined,
    );
  }

  edit(dept: IDepartment): void {
    this.router.navigate(['/entities/department', dept.id, 'edit']);
  }

  canViewField(fieldName: string): boolean {
    if (!this.capabilityLoaded()) {
      return true;
    }
    const vis = this.fieldVisibility();
    return vis[fieldName] !== false;
  }
}
