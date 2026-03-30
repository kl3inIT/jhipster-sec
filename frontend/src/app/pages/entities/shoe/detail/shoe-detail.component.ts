import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { IShoe } from '../shoe.model';
import { ShoeService } from '../service/shoe.service';

@Component({
  selector: 'app-shoe-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, TranslatePipe, CardModule, ButtonModule],
  templateUrl: './shoe-detail.component.html',
})
export default class ShoeDetailComponent implements OnInit {
  shoe = signal<IShoe | null>(null);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly shoeService = inject(ShoeService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as string | undefined;

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.shoeService.find(Number(id)).subscribe({
          next: res => this.shoe.set(res.body ?? null),
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
    this.router.navigate(['/entities/shoe'], context ? { queryParams: context.queryParams } : undefined);
  }

  edit(shoe: IShoe): void {
    this.router.navigate(['/entities/shoe', shoe.id, 'edit']);
  }
}
