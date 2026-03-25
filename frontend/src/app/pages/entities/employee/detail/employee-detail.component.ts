import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';

import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { IEmployee } from '../employee.model';
import { EmployeeService } from '../service/employee.service';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, CardModule, ButtonModule],
  templateUrl: './employee-detail.component.html',
})
export default class EmployeeDetailComponent implements OnInit {
  employee = signal<IEmployee | null>(null);
  capabilityLoaded = signal(false);
  canUpdate = signal(false);
  fieldVisibility = signal<Record<string, boolean>>({});

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);
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
        this.employeeService.find(Number(id)).subscribe({
          next: (res) => this.employee.set(res.body ?? null),
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
      ['/entities/employee'],
      context ? { queryParams: context.queryParams } : undefined,
    );
  }

  edit(emp: IEmployee): void {
    this.router.navigate(['/entities/employee', emp.id, 'edit']);
  }

  canViewField(fieldName: string): boolean {
    if (!this.capabilityLoaded()) {
      return true;
    }
    const vis = this.fieldVisibility();
    return vis[fieldName] !== false;
  }
}
