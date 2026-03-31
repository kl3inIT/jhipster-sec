import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Observable, Subscription, combineLatest, finalize, tap } from 'rxjs';
import { CardModule } from 'primeng/card';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { DatePickerModule } from 'primeng/datepicker';
import { MessageModule } from 'primeng/message';

import { SortService } from 'app/shared/sort/sort.service';
import { SortState, sortStateSignal } from 'app/shared/sort/sort-state';
import { addTranslatedMessage, handleHttpError } from 'app/shared/error/http-error.utils';
import { IMovieProfile } from '@/app/pages/entities/movie/components/movie-list/movie-profile.model';
import {
  EntityArrayResponseType as MovieProfileArrayResponseType,
  MovieProfileService,
} from '@/app/pages/entities/movie/components/movie-list/services/movie-profile.service';
import {
  EntityArrayResponseType,
  TopicOrientationService,
} from '../service/topic-orientation.service';
import { ITopicOrientation, NewTopicOrientation, TopicOrientationStatus } from '../topic-orientation.model';

const ITEMS_PER_PAGE = 20;
const TOTAL_COUNT_RESPONSE_HEADER = 'X-Total-Count';
const PAGE_HEADER = 'page';
const SORT = 'sort';
const DEFAULT_SORT_DATA = 'defaultSort';
const STATUS_VALUES: TopicOrientationStatus[] = ['ACTIVE', 'PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELED'];

@Component({
  selector: 'app-topic-orientation-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterModule,
    ReactiveFormsModule,
    TranslatePipe,
    CardModule,
    TableModule,
    ButtonModule,
    ConfirmDialogModule,
    ToastModule,
    DialogModule,
    InputTextModule,
    SelectModule,
    DatePickerModule,
    MessageModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './topic-orientation-list.component.html',
})
export default class TopicOrientationListComponent implements OnInit, OnDestroy {
  subscription: Subscription | null = null;
  topicOrientations = signal<ITopicOrientation[]>([]);
  loading = signal(true);

  sortState = sortStateSignal({});
  itemsPerPage = ITEMS_PER_PAGE;
  totalItems = signal(0);
  page = signal(1);
  dialogVisible = signal(false);
  isEditDialog = signal(false);
  savingDialog = signal(false);
  movieProfiles = signal<IMovieProfile[]>([]);

  form: FormGroup;

  readonly router = inject(Router);
  protected readonly topicOrientationService = inject(TopicOrientationService);
  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly sortService = inject(SortService);
  protected readonly movieProfileService = inject(MovieProfileService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly fb = inject(FormBuilder);

  trackId = (item: ITopicOrientation): number => this.topicOrientationService.getTopicOrientationIdentifier(item);

  dialogTitle = computed(() =>
    this.isEditDialog()
      ? this.translateService.instant('angappApp.topicOrientation.home.createOrEditLabel')
      : this.translateService.instant('angappApp.topicOrientation.home.createLabel'),
  );

  constructor() {
    this.form = this.fb.group({
      id: [null as number | null],
      title: ['', [Validators.required, Validators.maxLength(255)]],
      movieProfileId: [null as number | null],
      proposer: ['', [Validators.maxLength(255)]],
      submitDate: [null as Date | null],
      status: [null as TopicOrientationStatus | null, Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadMovieProfiles();
    this.subscription = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
        tap(() => this.load()),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  load(): void {
    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => this.onResponseSuccess(res),
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.itemsPerPage;
    const page = rows > 0 ? Math.floor(first / rows) + 1 : 1;
    const sortField = event.sortField as string | undefined;
    const sortOrder = event.sortOrder === 1 ? 'asc' : event.sortOrder === -1 ? 'desc' : undefined;
    const newSortState: SortState =
      sortField && sortOrder ? { predicate: sortField, order: sortOrder } : this.sortState();

    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams: {
        page,
        size: this.itemsPerPage,
        sort: this.sortService.buildSortParam(newSortState),
      },
    });
  }

  create(): void {
    this.isEditDialog.set(false);
    this.form.reset({
      id: null,
      title: '',
      movieProfileId: null,
      proposer: '',
      submitDate: null,
      status: null,
    });
    this.dialogVisible.set(true);
  }

  view(item: ITopicOrientation): void {
    this.router.navigate(['/entities/topic-orientation', item.id, 'view']);
  }

  edit(item: ITopicOrientation): void {
    this.isEditDialog.set(true);
    this.form.patchValue({
      id: item.id ?? null,
      title: item.title ?? '',
      movieProfileId: item.movieProfile?.id ?? null,
      proposer: item.proposer ?? '',
      submitDate: item.submitDate ? new Date(item.submitDate) : null,
      status: item.status ?? null,
    });
    this.dialogVisible.set(true);
  }

  confirmDelete(item: ITopicOrientation): void {
    this.confirmationService.confirm({
      message: this.translateService.instant('angappApp.topicOrientation.delete.question'),
      header: this.translateService.instant('angappApp.topicOrientation.delete.title'),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('entity.action.delete'),
      rejectLabel: this.translateService.instant('entity.action.cancel'),
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.delete(item),
    });
  }

  getStatusLabel(status?: TopicOrientationStatus): string {
    if (!status) {
      return '';
    }
    return this.translateService.instant(`angappApp.topicOrientation.status.${status}`);
  }

  get statusOptions(): Array<{ label: string; value: TopicOrientationStatus }> {
    return STATUS_VALUES.map((status) => ({
      value: status,
      label: this.translateService.instant(`angappApp.topicOrientation.status.${status}`),
    }));
  }

  get movieProfileOptions(): Array<{ label: string; value: number }> {
    return this.movieProfiles()
      .filter((movie) => movie.id !== undefined)
      .map((movie) => ({
        label: `${movie.profileCode ?? '-'} - ${movie.movieName ?? '-'}`,
        value: movie.id!,
      }));
  }

  closeDialog(): void {
    this.dialogVisible.set(false);
  }

  saveDialog(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const formValue = this.form.value;
    const selectedMovie = this.movieProfiles().find((movie) => movie.id === formValue.movieProfileId);
    const submitDateValue: Date | null = formValue.submitDate;
    const payload: ITopicOrientation | NewTopicOrientation = {
      id: formValue.id,
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

    this.savingDialog.set(true);
    request$.pipe(finalize(() => this.savingDialog.set(false))).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.saved',
          detail: 'feedback.entities.topicOrientations.saved',
        });
        this.dialogVisible.set(false);
        this.load();
      },
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  private delete(item: ITopicOrientation): void {
    const id = item.id;
    if (id === undefined) {
      return;
    }
    this.topicOrientationService.delete(id).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.deleted',
          detail: 'feedback.entities.topicOrientations.deleted',
        });
        this.load();
      },
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  private fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page.set(+(page ?? 1));
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  }

  private onResponseSuccess(response: EntityArrayResponseType): void {
    this.fillComponentAttributesFromResponseHeader(response.headers);
    this.topicOrientations.set(response.body ?? []);
  }

  private fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER)));
  }

  private queryBackend(): Observable<EntityArrayResponseType> {
    this.loading.set(true);
    return this.topicOrientationService
      .query({
        page: this.page() - 1,
        size: this.itemsPerPage,
        sort: this.sortService.buildSortParam(this.sortState()),
      })
      .pipe(finalize(() => this.loading.set(false)));
  }

  private loadMovieProfiles(): void {
    this.movieProfileService.query({ page: 0, size: 1000, sort: ['id,desc'] }).subscribe({
      next: (res: MovieProfileArrayResponseType) => this.movieProfiles.set(res.body ?? []),
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }
}
