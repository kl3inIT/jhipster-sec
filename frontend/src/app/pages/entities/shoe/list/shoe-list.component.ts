import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  NgZone,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Observable, Subscription, combineLatest, finalize, tap } from 'rxjs';
import { debounceTime, fromEvent, startWith } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonModule } from 'primeng/button';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { CardModule } from 'primeng/card';
import { Skeleton } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';

import { addTranslatedMessage, handleHttpError } from 'app/shared/error/http-error.utils';
import { SortState, sortStateSignal } from 'app/shared/sort/sort-state';
import { SortService } from 'app/shared/sort/sort.service';
import { WorkspaceContextService } from '../../shared/service/workspace-context.service';
import { IShoe } from '../shoe.model';
import { EntityArrayResponseType, ShoeService } from '../service/shoe.service';

const ITEMS_PER_PAGE = 20;
const TOTAL_COUNT_RESPONSE_HEADER = 'X-Total-Count';
const PAGE_HEADER = 'page';
const SORT = 'sort';
const DEFAULT_SORT_DATA = 'defaultSort';

@Component({
  selector: 'app-shoe-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterModule, TranslatePipe, CardModule, TableModule, ButtonModule, ConfirmDialogModule, ToastModule, Skeleton],
  providers: [ConfirmationService, MessageService],
  templateUrl: './shoe-list.component.html',
})
export default class ShoeListComponent implements OnInit, OnDestroy, AfterViewInit {
  subscription: Subscription | null = null;
  shoes = signal<IShoe[]>([]);
  loading = signal(true);

  sortState = sortStateSignal({});
  itemsPerPage = ITEMS_PER_PAGE;
  totalItems = signal(0);
  page = signal(1);
  firstRow = computed(() => (this.page() - 1) * this.itemsPerPage);

  readonly skeletonRows = Array(5).fill(null);
  readonly tableValue = computed(() => (this.loading() && this.shoes().length === 0 ? this.skeletonRows : this.shoes()));

  private readonly destroyRef = inject(DestroyRef);
  readonly isTablet = signal(typeof window !== 'undefined' ? window.innerWidth < 1024 : false);

  showIdColumn = computed(() => !this.isTablet());
  showBrandColumn = computed(() => !this.isTablet() && this.shoes().some(shoe => shoe.brand !== undefined));
  showDescriptionColumn = computed(() => !this.isTablet() && this.shoes().some(shoe => shoe.decription !== undefined));

  readonly router = inject(Router);
  protected readonly shoeService = inject(ShoeService);
  protected readonly activatedRoute = inject(ActivatedRoute);
  protected readonly sortService = inject(SortService);
  protected readonly ngZone = inject(NgZone);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly workspaceContextService = inject(WorkspaceContextService);

  private readonly navigationNodeId = this.activatedRoute.snapshot.data['navigationNodeId'] as string | undefined;

  trackId = (item: IShoe): number => this.shoeService.getShoeIdentifier(item);

  ngOnInit(): void {
    this.subscription = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
        tap(() => this.load()),
      )
      .subscribe();
  }

  ngAfterViewInit(): void {
    fromEvent(window, 'resize')
      .pipe(debounceTime(150), startWith(null), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.isTablet.set(window.innerWidth < 1024);
      });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  load(): void {
    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
      },
    });
  }

  navigateToWithComponentValues(event: SortState): void {
    this.handleNavigation(this.page(), event);
  }

  navigateToPage(page: number): void {
    this.handleNavigation(page, this.sortState());
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.itemsPerPage;
    const page = rows > 0 ? Math.floor(first / rows) + 1 : 1;
    const sortField = event.sortField as string | undefined;
    const sortOrder = event.sortOrder === 1 ? 'asc' : event.sortOrder === -1 ? 'desc' : undefined;
    const newSortState: SortState = sortField && sortOrder ? { predicate: sortField, order: sortOrder } : this.sortState();
    if (page !== this.page()) {
      this.navigateToPage(page);
    } else if (newSortState.predicate !== this.sortState().predicate || newSortState.order !== this.sortState().order) {
      this.navigateToWithComponentValues(newSortState);
    }
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page.set(+(page ?? 1));
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    this.fillComponentAttributesFromResponseHeader(response.headers);
    this.shoes.set(response.body ?? []);
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER)));
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    const pageToLoad = this.page();
    this.loading.set(true);
    const queryObject = {
      page: pageToLoad - 1,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };
    return this.shoeService.query(queryObject).pipe(finalize(() => this.loading.set(false)));
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
    this.router.navigate(['/entities/shoe/new']);
  }

  view(shoe: IShoe): void {
    this.storeWorkspaceContext();
    this.router.navigate(['/entities/shoe', shoe.id, 'view']);
  }

  edit(shoe: IShoe): void {
    this.storeWorkspaceContext();
    this.router.navigate(['/entities/shoe', shoe.id, 'edit']);
  }

  confirmDelete(shoe: IShoe): void {
    this.confirmationService.confirm({
      message: this.translateService.instant('angappApp.shoe.delete.question'),
      header: this.translateService.instant('angappApp.shoe.delete.title'),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('entity.action.delete'),
      acceptButtonStyleClass: 'p-button-danger',
      rejectLabel: this.translateService.instant('entity.action.cancel'),
      accept: () => this.deleteShoe(shoe),
    });
  }

  private deleteShoe(shoe: IShoe): void {
    this.shoeService.delete(shoe.id ?? 0).subscribe({
      next: () => {
        addTranslatedMessage(this.messageService, this.translateService, {
          severity: 'success',
          summary: 'feedback.toast.deleted',
          detail: 'feedback.entities.shoes.deleted',
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

    this.workspaceContextService.store(this.navigationNodeId, this.activatedRoute.snapshot.queryParams);
  }
}
