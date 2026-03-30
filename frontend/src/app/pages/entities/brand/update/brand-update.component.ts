import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

import { handleHttpError } from 'app/shared/error/http-error.utils';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { IBrand, NewBrand } from '../brand.model';
import { BrandService } from '../service/brand.service';

@Component({
  selector: 'app-brand-update',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, TranslatePipe, ReactiveFormsModule, CardModule, ButtonModule, InputTextModule, MessageModule, ToastModule],
  providers: [MessageService],
  templateUrl: './brand-update.component.html',
})
export default class BrandUpdateComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly brandService = inject(BrandService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  form: FormGroup = this.fb.group({
    id: [null as number | null],
    name: [''],
    description: [''],
  });

  isSaving = signal(false);
  isEdit = signal(false);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as string | undefined;

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      this.isEdit.set(!!idParam);
      if (idParam) {
        this.load(Number(idParam));
      }
    });
  }

  private load(id: number): void {
    this.brandService.find(id).subscribe({
      next: res => {
        const brand = res.body;
        if (brand) {
          this.form.patchValue(brand);
        }
      },
      error: () => {
        this.router.navigate(['/404']);
      },
    });
  }

  save(): void {
    this.isSaving.set(true);
    const formValue = this.form.getRawValue();
    const payload: IBrand | NewBrand = {
      id: formValue.id,
      name: formValue.name || undefined,
      description: formValue.description || undefined,
    };
    const request$ = payload.id ? this.brandService.update(payload as IBrand) : this.brandService.create(payload as NewBrand);
    request$.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: () => {
        this.navigateToWorkspaceList();
      },
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  cancel(): void {
    this.navigateToWorkspaceList();
  }

  private navigateToWorkspaceList(): void {
    const context = this.navigationNodeId ? this.workspaceContextService.get(this.navigationNodeId) : null;
    this.router.navigate(['/entities/brand'], context ? { queryParams: context.queryParams } : undefined);
  }
}
