import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { IMovieProfile } from '@/app/pages/entities/movie/components/movie-list/movie-profile.model';
import { MovieProfileService } from '@/app/pages/entities/movie/components/movie-list/services/movie-profile.service';
import { handleHttpError } from 'app/shared/error/http-error.utils';
import { ITopicOrientation, NewTopicOrientation, TopicOrientationStatus } from '../topic-orientation.model';
import { TopicOrientationService } from '../service/topic-orientation.service';

interface SelectOption<T> {
  label: string;
  value: T;
}

const STATUS_VALUES: TopicOrientationStatus[] = [
  'ACTIVE',
  'PENDING',
  'APPROVED',
  'REJECTED',
  'COMPLETED',
  'CANCELED',
];

@Component({
  selector: 'app-topic-orientation-update',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslatePipe,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    DatePickerModule,
    SelectModule,
    MessageModule,
    ToastModule,
  ],
  providers: [MessageService],
  templateUrl: './topic-orientation-update.component.html',
})
export default class TopicOrientationUpdateComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly topicOrientationService = inject(TopicOrientationService);
  private readonly movieProfileService = inject(MovieProfileService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);

  form: FormGroup = this.fb.group({
    id: [null as number | null],
    code: ['', [Validators.required, Validators.maxLength(20)]],
    title: ['', [Validators.required, Validators.maxLength(255)]],
    proposer: ['', [Validators.maxLength(255)]],
    submitDate: [null as Date | null],
    status: [null as TopicOrientationStatus | null, Validators.required],
    movieProfileId: [null as number | null],
  });

  isSaving = signal(false);
  isEdit = signal(false);
  movieProfiles = signal<IMovieProfile[]>([]);

  ngOnInit(): void {
    this.loadMovieProfiles();
    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');
      this.isEdit.set(Boolean(idParam));
      if (idParam) {
        this.load(Number(idParam));
      }
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    const formValue = this.form.value;
    const submitDateValue: Date | null = formValue.submitDate;
    const selectedMovie = this.movieProfiles().find((movie) => movie.id === formValue.movieProfileId);

    const payload: ITopicOrientation | NewTopicOrientation = {
      id: formValue.id,
      code: formValue.code,
      title: formValue.title,
      proposer: formValue.proposer || undefined,
      submitDate: submitDateValue ? submitDateValue.toISOString().slice(0, 10) : undefined,
      status: formValue.status,
      movieProfile: selectedMovie
        ? {
            id: selectedMovie.id,
            profileCode: selectedMovie.profileCode,
            movieName: selectedMovie.movieName,
          }
        : undefined,
    };

    const request$ = payload.id
      ? this.topicOrientationService.update(payload as ITopicOrientation)
      : this.topicOrientationService.create(payload as NewTopicOrientation);

    request$.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: () => this.router.navigate(['/entities/topic-orientation']),
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  cancel(): void {
    this.router.navigate(['/entities/topic-orientation']);
  }

  get movieProfileOptions(): SelectOption<number>[] {
    return this.movieProfiles()
      .filter((movie) => movie.id !== undefined)
      .map((movie) => ({
        label: `${movie.profileCode ?? '-'} - ${movie.movieName ?? '-'}`,
        value: movie.id!,
      }));
  }

  get statusOptions(): SelectOption<TopicOrientationStatus>[] {
    return STATUS_VALUES.map((status) => ({
      value: status,
      label: this.translateService.instant(`angappApp.topicOrientation.status.${status}`),
    }));
  }

  private loadMovieProfiles(): void {
    this.movieProfileService.query({ page: 0, size: 1000, sort: ['id,desc'] }).subscribe({
      next: (res) => this.movieProfiles.set(res.body ?? []),
    });
  }

  private load(id: number): void {
    this.topicOrientationService.find(id).subscribe({
      next: (res) => {
        const item = res.body;
        if (!item) {
          return;
        }
        this.form.patchValue({
          id: item.id,
          code: item.code ?? '',
          title: item.title ?? '',
          proposer: item.proposer ?? '',
          submitDate: item.submitDate ? new Date(item.submitDate) : null,
          status: item.status ?? null,
          movieProfileId: item.movieProfile?.id ?? null,
        });
      },
      error: () => this.router.navigate(['/404']),
    });
  }
}
