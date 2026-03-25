import { Component, NgZone, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { Observable, Subscription, combineLatest, finalize, tap } from 'rxjs';
import { CardModule } from 'primeng/card';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { SortService } from 'app/shared/sort/sort.service';
import { SortState, sortStateSignal } from 'app/shared/sort/sort-state';
import { ISecuredEntityCapability } from '../../shared/secured-entity-capability.model';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { IDepartment } from '../department.model';
import { EntityArrayResponseType, DepartmentService } from '../service/department.service';
import { addTranslatedMessage, handleHttpError } from 'app/shared/error/http-error.utils';

const ITEMS_PER_PAGE = 20;
const TOTAL_COUNT_RESPONSE_HEADER = 'X-Total-Count';
const PAGE_HEADER = 'page';
const SORT = 'sort';
const DEFAULT_SORT_DATA = 'defaultSort';

@Component({
  selector: 'app-department-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    CardModule,
    TableModule,
    ButtonModule,
    ConfirmDialogModule,
    ToastModule,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './department-list.component.html',
})
export default class DepartmentListComponent implements OnInit, OnDestroy {
  subscription: Subscription | null = null;
  departments = signal<IDepartment[]>([]);
  capability = signal<ISecuredEntityCapability | null>(null);
  loading = signal(false);

  sortState = sortStateSignal({});
  itemsPerPage = ITEMS_PER_PAGE;
  totalItems = 0;
  page = 1;
  capabilityLoaded = computed(() => this.capability() !== null);
  canCreate = computed(() => this.capability()?.canCreate ?? false);
  canRead = computed(() => this.capability()?.canRead ?? false);
  canUpdate = computed(() => this.capability()?.canUpdate ?? false);
  canDelete = computed(() => this.capability()?.canDelete ?? false);
  showListDeniedState = computed(() => this.capabilityLoaded() && !this.canRead());
  showRowActions = computed(() => this.canRead() || this.canUpdate() || this.canDelete());

  readonly router = inject(Router);
  protected readonly departmentService = inject(DepartmentService);
  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly sortService = inject(SortService);
  protected readonly ngZone = inject(NgZone);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  private readonly navigationNodeId = this.activatedRoute.snapshot.data['navigationNodeId'] as
    | string
    | undefined;

  trackId = (item: IDepartment): number => this.departmentService.getDepartmentIdentifier(item);

  ngOnInit(): void {
    const capability = (this.activatedRoute.snapshot.data['capability'] ??
      null) as ISecuredEntityCapability | null;
    this.capability.set(capability);
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
    if (this.showListDeniedState()) {
      this.departments.set([]);
      this.totalItems = 0;
      this.loading.set(false);
      return;
    }

    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
      },
    });
  }

  navigateToWithComponentValues(event: SortState): void {
    this.handleNavigation(this.page, event);
  }

  navigateToPage(page: number): void {
    this.handleNavigation(page, this.sortState());
  }

  get firstRow(): number {
    return (this.page - 1) * this.itemsPerPage;
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.itemsPerPage;
    const page = rows > 0 ? Math.floor(first / rows) + 1 : 1;
    const sortField = event.sortField as string | undefined;
    const sortOrder = event.sortOrder === 1 ? 'asc' : event.sortOrder === -1 ? 'desc' : undefined;
    const newSortState: SortState =
      sortField && sortOrder ? { predicate: sortField, order: sortOrder } : this.sortState();
    if (page !== this.page) {
      this.navigateToPage(page);
    } else if (
      newSortState.predicate !== this.sortState().predicate ||
      newSortState.order !== this.sortState().order
    ) {
      this.navigateToWithComponentValues(newSortState);
    }
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page = +(page ?? 1);
    this.sortState.set(
      this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]),
    );
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    this.fillComponentAttributesFromResponseHeader(response.headers);
    const dataFromBody = response.body ?? [];
    this.departments.set(dataFromBody);
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    this.totalItems = Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER));
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    const { page } = this;
    this.loading.set(true);
    const pageToLoad: number = page;
    const queryObject: any = {
      page: pageToLoad - 1,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };
    return this.departmentService.query(queryObject).pipe(finalize(() => this.loading.set(false)));
  }

  protected handleNavigation(page: number, sortState: SortState): void {
    const queryParamsObj = {
      page,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(sortState),
    };
    this.ngZone.run(() => {
      this.router.navigate(['./'], {
        relativeTo: this.activatedRoute,
        queryParams: queryParamsObj,
      });
    });
  }

  create(): void {
    this.storeWorkspaceContext();
    this.router.navigate(['/entities/department/new']);
  }

  view(dept: IDepartment): void {
    this.storeWorkspaceContext();
    this.router.navigate(['/entities/department', dept.id, 'view']);
  }

  edit(dept: IDepartment): void {
    this.storeWorkspaceContext();
    this.router.navigate(['/entities/department', dept.id, 'edit']);
  }

  confirmDelete(dept: IDepartment): void {
    this.confirmationService.confirm({
      message: 'Are you sure you want to delete this department? This action cannot be undone.',
      header: 'Delete Department',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Delete',
      acceptButtonStyleClass: 'p-button-danger',
      rejectLabel: 'Keep Department',
      accept: () => this.deleteDept(dept),
    });
  }

  private deleteDept(dept: IDepartment): void {
    this.departmentService.delete(dept.id ?? 0).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.deleted',
          detail: 'feedback.entities.departments.deleted',
        });
        this.load();
      },
      error: (err: unknown) => handleHttpError(this.messageService, this.translateService, err),
    });
  }

  private storeWorkspaceContext(): void {
    if (!this.navigationNodeId) {
      return;
    }

    this.workspaceContextService.store(
      this.navigationNodeId,
      this.activatedRoute.snapshot.queryParams,
    );
  }
}
