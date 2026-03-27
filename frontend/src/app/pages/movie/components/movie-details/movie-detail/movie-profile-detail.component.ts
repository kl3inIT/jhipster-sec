import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';

import { IMovieProfile } from '../../movie-list/movie-profile.model';
import { IProductionEkip } from '../../movie-list/production-ekip.model';

interface EnumOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-movie-profile-detail',
  standalone: true,
  imports: [ButtonModule, TagModule],
  templateUrl: './movie-profile-detail.component.html',
  styleUrls: ['./movie-profile-detail.component.scss'],
})
export default class MovieProfileDetailComponent {
  @Input() profile: IMovieProfile | null = null;
  @Input() statusOptions: EnumOption[] = [];
  @Input() typeOptions: EnumOption[] = [];
  @Input() categoryOptions: EnumOption[] = [];
  @Input() genreOptions: EnumOption[] = [];
  @Input() productionRoleOptions: EnumOption[] = [];
  @Input() productionEkips: IProductionEkip[] = [];

  @Output() edit = new EventEmitter<IMovieProfile>();
  @Output() close = new EventEmitter<void>();

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

  genreLabel(genreVal: string | null | undefined): string {
    const found = this.genreOptions.find(opt => opt.value === genreVal);
    return found ? found.label : (genreVal ?? '—');
  }

  roleLabel(roleVal: string | null | undefined): string {
    const found = this.productionRoleOptions.find(opt => opt.value === roleVal);
    return found ? found.label : (roleVal ?? '—');
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

  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) {
      return '—';
    }
    const parts = dateStr.split('-');
    if (parts.length !== 3) {
      return dateStr;
    }
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }

  getDurationDays(startDate: string | null | undefined, endDate: string | null | undefined): string {
    if (!startDate || !endDate) {
      return '—';
    }

    const start = new Date(startDate);
    const end = new Date(endDate);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      return '—';
    }

    const oneDay = 24 * 60 * 60 * 1000;
    const diff = Math.floor((end.getTime() - start.getTime()) / oneDay) + 1;
    if (diff <= 0) {
      return '—';
    }
    return `${diff} ngày`;
  }

  onEdit(): void {
    if (!this.profile) {
      return;
    }
    this.edit.emit(this.profile);
  }
}
