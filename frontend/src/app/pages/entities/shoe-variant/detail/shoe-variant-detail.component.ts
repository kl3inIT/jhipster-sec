import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { IShoeVariant } from '../shoe-variant.model';
import { ShoeVariantService } from '../service/shoe-variant.service';

@Component({
  selector: 'app-shoe-variant-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, TranslatePipe, CardModule, ButtonModule],
  templateUrl: './shoe-variant-detail.component.html',
})
export default class ShoeVariantDetailComponent implements OnInit {
  shoeVariant = signal<IShoeVariant | null>(null);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly shoeVariantService = inject(ShoeVariantService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as string | undefined;

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.shoeVariantService.find(Number(id)).subscribe({
          next: res => this.shoeVariant.set(res.body ?? null),
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
    this.router.navigate(['/entities/shoe-variant'], context ? { queryParams: context.queryParams } : undefined);
  }

  edit(shoeVariant: IShoeVariant): void {
    this.router.navigate(['/entities/shoe-variant', shoeVariant.id, 'edit']);
  }
}
