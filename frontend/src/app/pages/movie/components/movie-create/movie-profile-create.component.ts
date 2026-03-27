import { Component, EventEmitter, Output, inject, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormArray, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { catchError, of, switchMap } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';
import { MessageService } from 'primeng/api';

import { MovieProfileService } from '../movie-list/services/movie-profile.service';
import { NewMovieProfile } from '../movie-list/movie-profile.model';
import { IProductionEkip } from '../movie-list/production-ekip.model';
import { ProductionEkipService } from '../movie-list/services/production-ekip.service';
import {
  CLASSIFICATION_OPTIONS,
  GENRE_OPTIONS,
  MOVIE_TYPE_OPTIONS,
  PRODUCTION_ROLE_OPTIONS,
  STATUS_OPTIONS,
} from '../../constants/movie-enums.constants';

@Component({
  selector: 'app-movie-profile-create',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    DatePickerModule,
    MessageModule,
    TextareaModule,
  ],
  templateUrl: './movie-profile-create.component.html',
})
export default class MovieProfileCreateComponent implements OnInit {
  @Output() onCreated = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly movieProfileService = inject(MovieProfileService);
  private readonly productionEkipService = inject(ProductionEkipService);
  private readonly messageService = inject(MessageService);
  private readonly http = inject(HttpClient);

  isSaving = false;
  errorMessage: string | null = null;

  typeOptions = [...MOVIE_TYPE_OPTIONS];
  categoryOptions = [...CLASSIFICATION_OPTIONS];
  genreOptions = [...GENRE_OPTIONS];
  statusOptions = [...STATUS_OPTIONS];
  productionRoleOptions = [...PRODUCTION_ROLE_OPTIONS];

  form: FormGroup = this.fb.group({
    code:           [{ value: null, disabled: true }, [Validators.required]],
    name:           [null, [Validators.required, this.noWhitespaceValidator()]],
    type:           [null, [Validators.required]],
    category:       [null, [Validators.required]],
    genre:          [null],
    productionYear: [new Date().getFullYear(), [Validators.required, Validators.min(1900), Validators.max(2100)]],
    startDate:      [null],
    endDate:        [null],
    status:         ['PENDING', [Validators.required]],
    themeOrientation: [null],
    summary:        [null],
    ekipMembers: this.fb.array([]),
  }, { validators: this.dateRangeValidator() });

  ngOnInit() {
    this.generateCode(this.form.value.productionYear);

    this.form.get('productionYear')?.valueChanges.subscribe(year => {
      if (year && year.toString().length === 4) {
        this.generateCode(year);
      }
    });
  }

  generateCode(year: number): void {
    this.http.get<{ code?: string }>(`/api/movie-profiles/next-code?year=${year}`).subscribe({
      next: (res) => {
        this.form.patchValue({ code: res.code ?? null });
      },
      error: () => {}
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;

    const value = this.form.getRawValue();
    const trimmedName = this.normalizeText(value.name);
    const trimmedThemeOrientation = this.normalizeText(value.themeOrientation);
    const trimmedSummary = this.normalizeText(value.summary);
    const payload: NewMovieProfile = {
      id: null,
      profileCode:    value.code,
      movieName:      trimmedName,
      movieType:      value.type,
      classification: value.category,
      genre:          value.genre ?? null,
      productionYear: value.productionYear,
      startDate:      this.formatNullableDate(value.startDate),
      endDate:        this.formatNullableDate(value.endDate),
      status:         value.status,
      themeOrientation: trimmedThemeOrientation,
      summary:        trimmedSummary,
      ekipMembers:    this.buildEkipPayload(),
    };

    this.movieProfileService.create(payload).pipe(
      switchMap(created => {
        const createdId = created.body?.id;
        if (createdId) {
          return this.productionEkipService.replace(createdId, this.buildEkipPayload());
        }
        return of([]);
      }),
      catchError((error) => {
        this.errorMessage = 'Có lỗi xảy ra khi tạo hồ sơ phim. Vui lòng thử lại.';
        return of(null);
      })
    ).subscribe({
      next: () => {
        if (!this.isSaving) {
          return;
        }
        this.isSaving = false;
        this.messageService.add({ severity: 'success', summary: 'Thành công', detail: 'Tạo hồ sơ phim thành công.' });
        this.resetForm();
        this.onCreated.emit();
      },
      error: () => {
        this.isSaving = false;
        this.errorMessage = 'Có lỗi xảy ra khi tạo hồ sơ phim. Vui lòng thử lại.';
      },
    });
  }

  cancel(): void {
    this.form.reset({
        productionYear: new Date().getFullYear(),
        status: 'PENDING'
    });
    this.ekipMembers.clear();
    this.errorMessage = null;
    this.onCancel.emit();
  }

  resetForm(): void {
    this.form.reset({
      productionYear: new Date().getFullYear(),
      status: 'PENDING'
    });
    this.ekipMembers.clear();
    this.errorMessage = null;
    this.generateCode(new Date().getFullYear());
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.invalid && control.touched);
  }

  isDateRangeInvalid(): boolean {
    return !!(this.form.errors?.['invalidDateRange'] && (this.form.touched || this.form.dirty));
  }

  get ekipMembers(): FormArray {
    return this.form.get('ekipMembers') as FormArray;
  }

  addEkipMember(member?: IProductionEkip): void {
    this.ekipMembers.push(this.fb.group({
      ekipName: [member?.ekipName ?? null, [Validators.required, this.noWhitespaceValidator()]],
      role: [member?.role ?? null, [Validators.required]],
    }));
    this.messageService.add({ severity: 'success', summary: 'Thành công', detail: 'Đã thêm thành viên ekip.' });
  }

  removeEkipMember(index: number): void {
    this.ekipMembers.removeAt(index);
    this.messageService.add({ severity: 'success', summary: 'Thành công', detail: 'Đã xoá thành viên ekip.' });
  }

  private formatDate(date: Date | string): string {
    const d = this.toDate(date);
    if (!d) {
      return '';
    }
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  private formatNullableDate(date: Date | string | null | undefined): string | null {
    if (!date) {
      return null;
    }
    const formatted = this.formatDate(date);
    return formatted || null;
  }

  private normalizeText(value: string | null | undefined): string | null {
    if (value == null) {
      return null;
    }
    const normalized = value.trim();
    return normalized.length > 0 ? normalized : null;
  }

  private buildEkipPayload(): IProductionEkip[] {
    const payload: IProductionEkip[] = [];
    this.ekipMembers.controls.forEach(control => {
      const ekipName = this.normalizeText(control.get('ekipName')?.value);
      const role = control.get('role')?.value as string | null;
      if (ekipName && role) {
        payload.push({ ekipName, role });
      }
    });
    return payload;
  }

  private noWhitespaceValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (typeof control.value !== 'string') {
        return null;
      }
      return control.value.trim().length > 0 ? null : { whitespace: true };
    };
  }

  private dateRangeValidator(): ValidatorFn {
    return (group: AbstractControl): ValidationErrors | null => {
      const startDate = group.get('startDate')?.value as Date | string | null;
      const endDate = group.get('endDate')?.value as Date | string | null;
      if (!startDate || !endDate) {
        return null;
      }
      const start = this.toDate(startDate);
      const end = this.toDate(endDate);
      if (!start || !end) {
        return { invalidDateRange: true };
      }
      return start.getTime() <= end.getTime() ? null : { invalidDateRange: true };
    };
  }

  private toDate(value: Date | string): Date | null {
    if (value instanceof Date && !Number.isNaN(value.getTime())) {
      return value;
    }
    if (typeof value === 'string') {
      const vnMatch = value.trim().match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
      if (vnMatch) {
        const day = Number(vnMatch[1]);
        const month = Number(vnMatch[2]);
        const year = Number(vnMatch[3]);
        const d = new Date(year, month - 1, day);
        return Number.isNaN(d.getTime()) ? null : d;
      }
      const isoMatch = value.trim().match(/^(\d{4})-(\d{2})-(\d{2})$/);
      if (isoMatch) {
        const year = Number(isoMatch[1]);
        const month = Number(isoMatch[2]);
        const day = Number(isoMatch[3]);
        const d = new Date(year, month - 1, day);
        return Number.isNaN(d.getTime()) ? null : d;
      }
    }
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

}