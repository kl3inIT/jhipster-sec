import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnChanges,
  OnInit,
  inject,
  input,
  output,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs/operators';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { TextareaModule } from 'primeng/textarea';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';

import { ISecMenuDefinition } from '../sec-menu-definition.model';
import { SecMenuDefinitionService } from '../service/sec-menu-definition.service';
import { handleHttpError } from 'app/shared/error/http-error.utils';

@Component({
  selector: 'app-menu-definition-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    InputNumberModule,
    TextareaModule,
    TranslatePipe,
    ConfirmDialogModule,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './menu-definition-dialog.component.html',
})
export default class MenuDefinitionDialogComponent implements OnInit, OnChanges {
  visible = input(false);
  definition = input<ISecMenuDefinition | null>(null);
  saved = output<void>();
  closed = output<void>();

  isSaving = false;

  private readonly service = inject(SecMenuDefinitionService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly fb = inject(FormBuilder);

  form = this.fb.group({
    menuId: ['', [Validators.required, Validators.maxLength(150)]],
    appName: ['jhipster-security-platform', [Validators.required, Validators.maxLength(100)]],
    menuName: ['', [Validators.required, Validators.maxLength(200)]],
    label: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(500)]],
    parentMenuId: ['', [Validators.maxLength(150)]],
    route: ['', [Validators.maxLength(300)]],
    icon: ['', [Validators.maxLength(100)]],
    ordering: [0, [Validators.required]],
  });

  get isEditMode(): boolean {
    return !!this.definition()?.id;
  }

  ngOnInit(): void {
    this.syncFormFromDefinition();
  }

  ngOnChanges(): void {
    this.syncFormFromDefinition();
  }

  private syncFormFromDefinition(): void {
    const def = this.definition();
    if (def) {
      this.form.patchValue({
        menuId: def.menuId,
        appName: def.appName,
        menuName: def.menuName,
        label: def.label,
        description: def.description ?? '',
        parentMenuId: def.parentMenuId ?? '',
        route: def.route ?? '',
        icon: def.icon ?? '',
        ordering: def.ordering,
      });
      this.form.get('menuId')?.disable();
    } else {
      this.form.reset({ appName: 'jhipster-security-platform', ordering: 0 });
      this.form.get('menuId')?.enable();
    }
  }

  confirmSave(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.confirmationService.confirm({
      header: this.translateService.instant('security.menuDefinitions.confirmSave.title'),
      message: this.translateService.instant('security.menuDefinitions.confirmSave.message'),
      acceptLabel: this.translateService.instant('entity.action.save'),
      rejectLabel: this.translateService.instant('entity.action.cancel'),
      accept: () => this.save(),
    });
  }

  private save(): void {
    this.isSaving = true;
    const raw = this.form.getRawValue();
    const dto: ISecMenuDefinition = {
      menuId: raw.menuId ?? '',
      appName: raw.appName ?? 'jhipster-security-platform',
      menuName: raw.menuName ?? '',
      label: raw.label ?? '',
      description: raw.description || undefined,
      parentMenuId: raw.parentMenuId || undefined,
      route: raw.route || undefined,
      icon: raw.icon || undefined,
      ordering: raw.ordering ?? 0,
    };
    const def = this.definition();
    const request$ = def?.id ? this.service.update(def.id, dto) : this.service.create(dto);
    request$
      .pipe(
        finalize(() => {
          this.isSaving = false;
          this.cdr.markForCheck();
        }),
      )
      .subscribe({
        next: () => this.saved.emit(),
        error: (err: unknown) =>
          handleHttpError(this.messageService, this.translateService, err, 'feedback.security.menuDefinitions.saveFailed'),
      });
  }

  onHide(): void {
    this.closed.emit();
  }
}
