import { HttpErrorResponse } from '@angular/common/http';
import { Component, EventEmitter, Input, Output, inject, OnInit, ChangeDetectorRef } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';

import { MovieProfileService } from '../movie-list/services/movie-profile.service';
import { IMovieProfile } from '../movie-list/movie-profile.model';
import {
  CLASSIFICATION_OPTIONS,
  GENRE_OPTIONS,
  MOVIE_TYPE_OPTIONS,
  STATUS_OPTIONS,
} from '../../constants/movie-enums.constants';
import { getApiErrorMessage } from 'app/core/util/api-error.util';

@Component({
  selector: 'app-movie-profile-edit',
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
  templateUrl: './movie-profile-edit.component.html',
  styleUrls: ['./movie-profile-edit.component.scss'],
})
export default class MovieProfileEditComponent implements OnInit {
  @Input() profile: IMovieProfile | null = null;
  @Output() onUpdated = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  private readonly movieProfileService = inject(MovieProfileService);
  private readonly messageService = inject(MessageService);
  private readonly cd = inject(ChangeDetectorRef);

  isSaving = false;
  errorMessage: string | null = null;

  typeOptions = [...MOVIE_TYPE_OPTIONS];
  categoryOptions = [...CLASSIFICATION_OPTIONS];
  genreOptions = [...GENRE_OPTIONS];
  statusOptions = [...STATUS_OPTIONS];

  form: FormGroup = this.fb.group({
    code: [{ value: null, disabled: true }],
    name: [null, [Validators.required]],
    type: [null, [Validators.required]],
    category: [null, [Validators.required]],
    genre: [null],
    productionYear: [new Date().getFullYear(), [Validators.required]],
    startDate: [null],
    endDate: [null],
    status: ['PENDING', [Validators.required]],
    themeOrientation: [null],
    summary: [null],
  });

  ngOnInit(): void {
    if (this.profile) {
      this.form.patchValue({
        code: this.profile.profileCode,
        name: this.profile.movieName,
        type: this.profile.movieType,
        category: this.profile.classification,
        genre: this.profile.genre,
        productionYear: this.profile.productionYear,
        startDate: this.profile.startDate,
        endDate: this.profile.endDate,
        status: this.profile.status,
        summary: this.profile.summary,
      });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.profile?.id) {
      this.messageService.add({ severity: 'error', summary: 'Lỗi', detail: 'ID dự án không hợp lệ' });
      return;
    }

    this.isSaving = true;
    this.errorMessage = null;

    const updateData = {
      ...this.profile,
      movieName: this.form.value.name,
      movieType: this.form.value.type,
      classification: this.form.value.category,
      genre: this.form.value.genre,
      productionYear: this.form.value.productionYear,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate,
      status: this.form.value.status,
      summary: this.form.value.summary,
    };

    this.movieProfileService
      .update(updateData)
      .pipe(finalize(() => {
        this.isSaving = false;
        this.cd.detectChanges();
      }))
      .subscribe({
        next: () => {
          this.messageService.add({ severity: 'success', summary: 'Thành công', detail: 'Cập nhật dự án thành công' });
          this.onUpdated.emit();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = getApiErrorMessage(error, 'Có lỗi xảy ra khi cập nhật');
          this.messageService.add({ severity: 'error', summary: 'Lỗi', detail: this.errorMessage ?? undefined, life: 8000 });
        },
      });
  }

  cancel(): void {
    this.onCancel.emit();
  }
}
