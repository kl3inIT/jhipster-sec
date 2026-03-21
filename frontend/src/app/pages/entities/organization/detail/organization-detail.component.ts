import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';

import { IOrganization } from '../organization.model';
import { OrganizationService } from '../service/organization.service';

@Component({
  selector: 'app-organization-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, CardModule, ButtonModule],
  templateUrl: './organization-detail.component.html',
})
export default class OrganizationDetailComponent implements OnInit {
  organization = signal<IOrganization | null>(null);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly organizationService = inject(OrganizationService);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.organizationService.find(Number(id)).subscribe({
          next: res => this.organization.set(res.body ?? null),
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
    this.router.navigate(['/entities/organization']);
  }

  edit(org: IOrganization): void {
    this.router.navigate(['/entities/organization', org.id, 'edit']);
  }
}
