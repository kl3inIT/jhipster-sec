import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { take } from 'rxjs';

import { SecuredEntityCapabilityService } from '../../shared/service/secured-entity-capability.service';
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

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);
  private readonly securedEntityCapabilityService = inject(SecuredEntityCapabilityService);

  ngOnInit(): void {
    this.loadCapability();
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.employeeService.find(Number(id)).subscribe({
          next: res => this.employee.set(res.body ?? null),
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
    this.router.navigate(['/entities/employee']);
  }

  edit(emp: IEmployee): void {
    this.router.navigate(['/entities/employee', emp.id, 'edit']);
  }

  private loadCapability(): void {
    this.securedEntityCapabilityService
      .getEntityCapability('employee')
      .pipe(take(1))
      .subscribe({
        next: capability => {
          this.canUpdate.set(capability?.canUpdate ?? false);
          this.capabilityLoaded.set(true);
        },
        error: () => {
          this.canUpdate.set(false);
          this.capabilityLoaded.set(true);
        },
      });
  }
}

