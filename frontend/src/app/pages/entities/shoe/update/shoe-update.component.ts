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
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';

import { handleHttpError } from 'app/shared/error/http-error.utils';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { IBrand } from '../../brand/brand.model';
import { BrandService } from '../../brand/service/brand.service';
import { IShoe, NewShoe } from '../shoe.model';
import { ShoeService } from '../service/shoe.service';

@Component({
  selector: 'app-shoe-update',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, TranslatePipe, ReactiveFormsModule, CardModule, ButtonModule, InputTextModule, MessageModule, SelectModule, ToastModule],
  providers: [MessageService],
  templateUrl: './shoe-update.component.html',
})
export default class ShoeUpdateComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly shoeService = inject(ShoeService);
  private readonly brandService = inject(BrandService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  form: FormGroup = this.fb.group({
    id: [null as number | null],
    name: [''],
    decription: [''],
    brandId: [null as number | null],
  });

  isSaving = signal(false);
  isEdit = signal(false);
  brands = signal<IBrand[]>([]);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as string | undefined;

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      this.isEdit.set(!!idParam);
      this.loadBrands();
      if (idParam) {
        this.load(Number(idParam));
      }
    });
  }

  private loadBrands(): void {
    this.brandService.query({ page: 0, size: 1000, sort: ['name,asc'] }).subscribe({
      next: res => this.brands.set(res.body ?? []),
    });
  }

  private load(id: number): void {
    this.shoeService.find(id).subscribe({
      next: res => {
        const shoe = res.body;
        if (shoe) {
          this.form.patchValue({
            id: shoe.id,
            name: shoe.name,
            decription: shoe.decription,
            brandId: shoe.brand?.id ?? null,
          });
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
    const selectedBrand = formValue.brandId ? this.brands().find(brand => brand.id === formValue.brandId) : undefined;
    const payload: IShoe | NewShoe = {
      id: formValue.id,
      name: formValue.name || undefined,
      decription: formValue.decription || undefined,
      brand: selectedBrand ? { id: selectedBrand.id, name: selectedBrand.name } : undefined,
    };
    const request$ = payload.id ? this.shoeService.update(payload as IShoe) : this.shoeService.create(payload as NewShoe);
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

  get brandOptions(): { label: string; value: number }[] {
    return this.brands()
      .filter(brand => brand.id !== undefined && brand.name !== undefined)
      .map(brand => ({ label: brand.name!, value: brand.id! }));
  }

  private navigateToWorkspaceList(): void {
    const context = this.navigationNodeId ? this.workspaceContextService.get(this.navigationNodeId) : null;
    this.router.navigate(['/entities/shoe'], context ? { queryParams: context.queryParams } : undefined);
  }
}
