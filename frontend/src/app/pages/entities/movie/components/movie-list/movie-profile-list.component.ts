import { ChangeDetectorRef, Component, OnInit, ViewChild, inject } from '@angular/core';
import { NgFor } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';

import { IMovieProfile } from './movie-profile.model';
import { MovieProfileService } from './services/movie-profile.service';
import MovieProfileCreateComponent from '../movie-create/movie-profile-create.component';
import {
  CLASSIFICATION_OPTIONS,
  GENRE_OPTIONS,
  MOVIE_TYPE_OPTIONS,
  PRODUCTION_ROLE_OPTIONS,
  STATUS_OPTIONS,
} from '../../constants/movie-enums.constants';

type StatusFilterValue = 'all' | string;

interface StatusOption {
  label: string;
  value: StatusFilterValue;
}
interface EnumOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-movie-profile-list',
  standalone: true,
  imports: [
    NgFor,
    FormsModule,
    CardModule,
    TableModule,
    TagModule,
    ButtonModule,
    InputTextModule,
    DialogModule,
    ToastModule,
    ConfirmDialogModule,
    MovieProfileCreateComponent,
  ],
  providers: [ConfirmationService, MessageService],
  templateUrl: './movie-profile-list.component.html',
  styleUrls: ['./movie-profile-list.component.scss'],
})
export default class MovieProfileListComponent implements OnInit {
  private readonly movieProfileService = inject(MovieProfileService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly defaultStatusOptions: StatusOption[] = [{ label: 'Tất cả trạng thái', value: 'all' }];

  searchTerm = '';
  isLoading = true;
  showCreateDialog = false;
  @ViewChild('createDialogRef') private createDialogRef?: MovieProfileCreateComponent;

  filteredProfilesData: IMovieProfile[] = [];

  statusOptions: StatusOption[] = [...this.defaultStatusOptions, ...STATUS_OPTIONS];
  typeOptions: EnumOption[] = [...MOVIE_TYPE_OPTIONS];
  categoryOptions: EnumOption[] = [...CLASSIFICATION_OPTIONS];
  genreOptions: EnumOption[] = [...GENRE_OPTIONS];
  productionRoleOptions: EnumOption[] = [...PRODUCTION_ROLE_OPTIONS];

  selectedStatus: StatusFilterValue = 'all';

  filmProfiles: IMovieProfile[] = [];

  ngOnInit(): void {
    this.loadProfiles();
  }

  loadProfiles(): void {
    this.isLoading = true;
    this.movieProfileService
      .query({ page: 0, size: 100, sort: ['profileCode,asc'] })
      .subscribe({
        next: (res) => {
          const rows = [...(res.body ?? [])].sort((a, b) => this.compareProfileCode(a.profileCode, b.profileCode));
          this.patchTableAfterStableTick(rows);
        },
        error: () => {
          this.patchTableAfterStableTick(null);
        },
      });
  }

  /**
   * Tránh NG0100: p-table đọc [value] trong cùng turn với callback HTTP.
   * Macrotask + markForCheck tách cập nhật khỏi lượt CD hiện tại (kể cả zoneless).
   */
  private patchTableAfterStableTick(rows: IMovieProfile[] | null): void {
    setTimeout(() => {
      if (rows === null) {
        this.filmProfiles = [];
        this.filteredProfilesData = [];
      } else {
        this.filmProfiles = rows;
        this.filterData();
      }
      this.isLoading = false;
      this.cdr.markForCheck();
    }, 0);
  }

  openCreateDialog(): void {
    this.showCreateDialog = true;
  }

  goToDetail(profile: IMovieProfile): void {
    if (!profile.id) {
      return;
    }
    void this.router.navigate(['/entities/movie', profile.id, 'view']);
  }

  goToEdit(profile: IMovieProfile): void {
    if (!profile.id) {
      return;
    }
    void this.router.navigate(['/entities/movie', profile.id, 'edit']);
  }

  handleCreated(): void {
    this.closeCreateDialog();
    this.loadProfiles();
    this.messageService.add({ severity: 'success', summary: 'Thành công', detail: 'Đã tạo hồ sơ phim mới.' });
  }

  closeCreateDialog(): void {
    this.showCreateDialog = false;

    this.createDialogRef?.resetForm();
  }

  handleDialogHide(): void {
    this.closeCreateDialog();
  }

  confirmDelete(profile: IMovieProfile): void {
    if (!profile.id) {
      return;
    }
    this.confirmationService.confirm({
      header: 'Xác nhận xóa',
      message: `Bạn có chắc chắn muốn xóa hồ sơ "${profile.profileCode ?? ''}"?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Xóa',
      rejectLabel: 'Hủy',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.movieProfileService.delete(profile.id).subscribe({
          next: () => {
            this.loadProfiles();
            this.messageService.add({ severity: 'success', summary: 'Thành công', detail: 'Đã xóa hồ sơ phim.' });
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Thất bại', detail: 'Xóa hồ sơ phim thất bại.' });
          },
        });
      },
    });
  }

  filterData(): void {
    const term = this.searchTerm.trim().toLowerCase();

    this.filteredProfilesData = this.filmProfiles.filter(profile => {
      const matchesStatus = this.selectedStatus === 'all' || profile.status === this.selectedStatus;
      const matchesSearch =
        !term ||
        (profile.profileCode ?? '').toLowerCase().includes(term) ||
        (profile.movieName ?? '').toLowerCase().includes(term);

      return matchesStatus && matchesSearch;
    });
  }

  formatDateStr(dateStr: string | null | undefined): string {
    if (!dateStr || dateStr === '?') return '?';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
  }

  getTimeRange(profile: IMovieProfile): string {
    if (!profile.startDate && !profile.endDate) return '—';
    const start = profile.startDate ? this.formatDateStr(profile.startDate) : '?';
    const end = profile.endDate ? this.formatDateStr(profile.endDate) : '?';
    return `${start} → ${end}`;
  }

  statusSeverity(status: string | null | undefined): 'success' | 'danger' | 'info' | 'warn' | 'secondary' | 'contrast' {
    switch (status) {
      case 'IN_PROGRESS':
      case 'ACTIVE':
        return 'info';
      case 'PENDING':
        return 'warn';
      case 'DRAFT':
        return 'secondary';
      case 'COMPLETED':
      case 'APPROVED':
        return 'success';
      case 'REJECTED':
        return 'danger';
      case 'CANCELED':
        return 'secondary';
      default:
        return 'secondary';
    }
  }

  statusLabel(status: string | null | undefined): string {
    const found = this.statusOptions.find(opt => opt.value === status);
    return found ? found.label : (status ?? '—');
  }

  typeLabel(typeVal: string | null | undefined): string {
    const found = this.typeOptions.find(opt => opt.value === typeVal);
    return found ? found.label : (typeVal ?? '—');
  }

  categoryLabel(catVal: string | null | undefined): string {
    const found = this.categoryOptions.find(opt => opt.value === catVal);
    return found ? found.label : (catVal ?? '—');
  }

  exportExcel(): void {
    // TODO: tích hợp xuất Excel thật với backend
  }

  private compareProfileCode(aCode: string | null | undefined, bCode: string | null | undefined): number {
    const a = this.parseCodeOrder(aCode);
    const b = this.parseCodeOrder(bCode);
    if (a.year !== b.year) {
      return a.year - b.year;
    }
    if (a.seq !== b.seq) {
      return a.seq - b.seq;
    }
    return (aCode ?? '').localeCompare(bCode ?? '');
  }

  private parseCodeOrder(code: string | null | undefined): { year: number; seq: number } {
    if (!code) {
      return { year: Number.MAX_SAFE_INTEGER, seq: Number.MAX_SAFE_INTEGER };
    }
    const matched = code.match(/-(\d{4})-(\d+)$/);
    if (!matched) {
      return { year: Number.MAX_SAFE_INTEGER, seq: Number.MAX_SAFE_INTEGER };
    }
    return {
      year: Number(matched[1]),
      seq: Number(matched[2]),
    };
  }
}
