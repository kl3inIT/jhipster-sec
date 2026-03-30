import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { IBrand } from '../brand.model';
import { BrandService } from '../service/brand.service';

@Component({
  selector: 'app-brand-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, TranslatePipe, CardModule, ButtonModule],
  templateUrl: './brand-detail.component.html',
})
export default class BrandDetailComponent implements OnInit {
  brand = signal<IBrand | null>(null);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly brandService = inject(BrandService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as string | undefined;

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.brandService.find(Number(id)).subscribe({
          next: res => this.brand.set(res.body ?? null),
          error: err => {
            if (err.status === 404) {
              this.router.navigate(['/404']);
            }
          },
        });
      }
    });
  }

  back(): void {
    const context = this.navigationNodeId ? this.workspaceContextService.get(this.navigationNodeId) : null;
    this.router.navigate(['/entities/brand'], context ? { queryParams: context.queryParams } : undefined);
  }

  edit(brand: IBrand): void {
    this.router.navigate(['/entities/brand', brand.id, 'edit']);
  }
}
