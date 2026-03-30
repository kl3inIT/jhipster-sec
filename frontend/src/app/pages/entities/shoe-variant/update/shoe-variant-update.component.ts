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
import { IShoe } from '../../shoe/shoe.model';
import { ShoeService } from '../../shoe/service/shoe.service';
import { IShoeVariant, NewShoeVariant } from '../shoe-variant.model';
import { ShoeVariantService } from '../service/shoe-variant.service';

@Component({
  selector: 'app-shoe-variant-update',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, RouterModule, TranslatePipe, ReactiveFormsModule, CardModule, ButtonModule, InputTextModule, MessageModule, SelectModule, ToastModule],
  providers: [MessageService],
  templateUrl: './shoe-variant-update.component.html',
})
export default class ShoeVariantUpdateComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly shoeVariantService = inject(ShoeVariantService);
  private readonly shoeService = inject(ShoeService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  form: FormGroup = this.fb.group({
    id: [null as number | null],
    name: [''],
    decription: [''],
    shoeId: [null as number | null],
  });

  isSaving = signal(false);
  isEdit = signal(false);
  shoes = signal<IShoe[]>([]);

  private readonly navigationNodeId = this.route.snapshot.data['navigationNodeId'] as string | undefined;

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      this.isEdit.set(!!idParam);
      this.loadShoes();
      if (idParam) {
        this.load(Number(idParam));
      }
    });
  }

  private loadShoes(): void {
    this.shoeService.query({ page: 0, size: 1000, sort: ['name,asc'] }).subscribe({
      next: res => this.shoes.set(res.body ?? []),
    });
  }

  private load(id: number): void {
    this.shoeVariantService.find(id).subscribe({
      next: res => {
        const shoeVariant = res.body;
        if (shoeVariant) {
          this.form.patchValue({
            id: shoeVariant.id,
            name: shoeVariant.name,
            decription: shoeVariant.decription,
            shoeId: shoeVariant.shoe?.id ?? null,
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
    const selectedShoe = formValue.shoeId ? this.shoes().find(shoe => shoe.id === formValue.shoeId) : undefined;
    const payload: IShoeVariant | NewShoeVariant = {
      id: formValue.id,
      name: formValue.name || undefined,
      decription: formValue.decription || undefined,
      shoe: selectedShoe ? { id: selectedShoe.id, name: selectedShoe.name } : undefined,
    };
    const request$ = payload.id
      ? this.shoeVariantService.update(payload as IShoeVariant)
      : this.shoeVariantService.create(payload as NewShoeVariant);
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

  get shoeOptions(): { label: string; value: number }[] {
    return this.shoes()
      .filter(shoe => shoe.id !== undefined && shoe.name !== undefined)
      .map(shoe => ({ label: shoe.name!, value: shoe.id! }));
  }

  private navigateToWorkspaceList(): void {
    const context = this.navigationNodeId ? this.workspaceContextService.get(this.navigationNodeId) : null;
    this.router.navigate(['/entities/shoe-variant'], context ? { queryParams: context.queryParams } : undefined);
  }
}
