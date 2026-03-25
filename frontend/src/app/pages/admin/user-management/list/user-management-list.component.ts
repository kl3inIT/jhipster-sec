import { ChangeDetectionStrategy, Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Subscription, combineLatest, debounceTime, distinctUntilChanged, finalize, tap } from 'rxjs';

import { CardModule } from 'primeng/card';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { TagModule } from 'primeng/tag';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { AccountService } from 'app/core/auth/account.service';
import { SortService } from 'app/shared/sort/sort.service';
import { SortState, sortStateSignal } from 'app/shared/sort/sort-state';
import { WorkspaceContextService } from 'app/pages/entities/shared/service/workspace-context.service';
import { addTranslatedMessage, handleHttpError } from 'app/shared/error/http-error.utils';
import { IUser } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import { resolveAuthorityLabel } from '../shared/authority-label.util';

const ITEMS_PER_PAGE = 20;
const TOTAL_COUNT_RESPONSE_HEADER = 'X-Total-Count';
const PAGE_HEADER = 'page';
const SORT = 'sort';
const QUERY = 'query';
const DEFAULT_SORT_DATA = 'defaultSort';

@Component({
  selector: 'app-user-management-list',
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
    TagModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './user-management-list.component.html',
})
export default class UserManagementListComponent implements OnInit, OnDestroy {
  users = signal<IUser[]>([]);
  loading = signal(false);
  totalItems = signal(0);
  page = signal(1);
  sortState = sortStateSignal({});
  loadError = signal(false);
  searchControl = new FormControl('');

  readonly itemsPerPage = ITEMS_PER_PAGE;

  readonly currentAccount = inject(AccountService).trackCurrentAccount();

  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly sortService = inject(SortService);
  private readonly userManagementService = inject(UserManagementService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  private readonly navigationNodeId = this.activatedRoute.snapshot.data['navigationNodeId'] as string | undefined;
  private routeSub: Subscription | null = null;
  private searchSub: Subscription | null = null;

  firstRow = computed(() => (this.page() - 1) * this.itemsPerPage);

  ngOnInit(): void {
    this.routeSub = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillFromRoute(params, data)),
        tap(() => this.load()),
      )
      .subscribe();

    this.searchSub = this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(query => {
        this.handleNavigation(1, this.sortState(), query ?? '');
      });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
    this.searchSub?.unsubscribe();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    const queryObj = {
      query: this.searchControl.value ?? '',
      page: this.page() - 1,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };
    this.userManagementService
      .query(queryObj)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: res => {
          this.totalItems.set(Number(res.headers.get(TOTAL_COUNT_RESPONSE_HEADER) ?? 0));
          this.users.set(res.body ?? []);
        },
        error: (err: unknown) => {
          this.loadError.set(true);
          handleHttpError(this.messageService, this.translateService, err, 'userManagement.home.loadError');
        },
      });
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.itemsPerPage;
    const newPage = rows > 0 ? Math.floor(first / rows) + 1 : 1;
    const sortField = event.sortField as string | undefined;
    const sortOrder = event.sortOrder === 1 ? 'asc' : event.sortOrder === -1 ? 'desc' : undefined;
    const newSort: SortState = sortField && sortOrder ? { predicate: sortField, order: sortOrder } : this.sortState();

    if (newPage !== this.page() || newSort.predicate !== this.sortState().predicate || newSort.order !== this.sortState().order) {
      this.handleNavigation(newPage, newSort, this.searchControl.value ?? '');
    }
  }

  view(user: IUser): void {
    this.storeWorkspaceContext();
    this.router.navigate(['/admin/users', user.login, 'view']);
  }

  edit(user: IUser): void {
    this.storeWorkspaceContext();
    this.router.navigate(['/admin/users', user.login, 'edit']);
  }

  create(): void {
    this.storeWorkspaceContext();
    this.router.navigate(['/admin/users/new']);
  }

  toggleActivation(user: IUser): void {
    const updated = { ...user, activated: !user.activated };
    this.userManagementService.update(updated).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.saved',
          detail: updated.activated ? 'userManagement.activated' : 'userManagement.deactivated',
        });
        this.load();
      },
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  confirmDelete(user: IUser): void {
    this.confirmationService.confirm({
      header: this.translateService.instant('userManagement.delete.title'),
      message: this.translateService.instant('userManagement.delete.question', { login: user.login }),
      acceptLabel: this.translateService.instant('userManagement.delete.confirm'),
      rejectLabel: this.translateService.instant('userManagement.delete.keep'),
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.deleteUser(user),
    });
  }

  isSelf(user: IUser): boolean {
    return user.login === this.currentAccount()?.login;
  }

  getAuthorityLabel(authority: string): string {
    return resolveAuthorityLabel(authority, this.translateService);
  }

  displayedAuthorities(user: IUser): string[] {
    return (user.authorities ?? []).slice(0, 2);
  }

  extraAuthorityCount(user: IUser): number {
    return Math.max((user.authorities?.length ?? 0) - 2, 0);
  }

  private deleteUser(user: IUser): void {
    this.userManagementService.delete(user.login!).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.deleted',
          detail: 'userManagement.deleted',
          detailParams: { param: user.login },
        });
        this.load();
      },
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  private fillFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page.set(+(page ?? 1));
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
    const query = params.get(QUERY) ?? '';
    if (query !== (this.searchControl.value ?? '')) {
      this.searchControl.setValue(query, { emitEvent: false });
    }
  }

  private handleNavigation(page: number, sortState: SortState, query: string): void {
    const queryParams: Record<string, string | number | string[]> = {
      page,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(sortState),
    };
    if (query) {
      queryParams['query'] = query;
    }
    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams,
    });
  }

  private storeWorkspaceContext(): void {
    if (!this.navigationNodeId) {
      return;
    }
    this.workspaceContextService.store(this.navigationNodeId, this.activatedRoute.snapshot.queryParams);
  }
}
