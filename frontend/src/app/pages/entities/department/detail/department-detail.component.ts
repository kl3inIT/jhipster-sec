import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';

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

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly departmentService = inject(DepartmentService);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.departmentService.find(Number(id)).subscribe({
          next: res => this.department.set(res.body ?? null),
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
    this.router.navigate(['/entities/department']);
  }

  edit(dept: IDepartment): void {
    this.router.navigate(['/entities/department', dept.id, 'edit']);
  }
}
