import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

import { IMovieProfile } from '../movie-list/movie-profile.model';
import { IProductionEkip } from '../movie-list/production-ekip.model';

@Component({
  selector: 'app-movie-profile-detail',
  standalone: true,
  imports: [CommonModule, ButtonModule, CardModule],
  template: `
    @if (profile(); as currentProfile) {
      <p-card>
        <div class="grid gap-4">
          <div>
            <label class="font-bold">Mã dự án:</label>
            <p>{{ currentProfile.profileCode }}</p>
          </div>
          <div>
            <label class="font-bold">Tên dự án:</label>
            <p>{{ currentProfile.movieName }}</p>
          </div>
          <div>
            <label class="font-bold">Loại phim:</label>
            <p>{{ currentProfile.movieType }}</p>
          </div>
          <div>
            <label class="font-bold">Phân loại:</label>
            <p>{{ currentProfile.classification }}</p>
          </div>
          <div>
            <label class="font-bold">Thể loại:</label>
            <p>{{ currentProfile.genre }}</p>
          </div>
          <div>
            <label class="font-bold">Năm sản xuất:</label>
            <p>{{ currentProfile.productionYear }}</p>
          </div>
          <div>
            <label class="font-bold">Ngày bắt đầu:</label>
            <p>{{ currentProfile.startDate | date: 'dd/MM/yyyy' }}</p>
          </div>
          <div>
            <label class="font-bold">Ngày kết thúc:</label>
            <p>{{ currentProfile.endDate | date: 'dd/MM/yyyy' }}</p>
          </div>
          <div>
            <label class="font-bold">Trạng thái:</label>
            <p>{{ currentProfile.status }}</p>
          </div>
          <div>
            <label class="font-bold">Tóm tắt:</label>
            <p>{{ currentProfile.summary }}</p>
          </div>
          <div>
            <button pButton type="button" label="Sửa" icon="pi pi-pencil" (click)="edit.emit(currentProfile)"></button>
          </div>
        </div>
      </p-card>
      @if (productionEkips().length > 0) {
        <div class="mt-4">
        <h3 class="text-lg font-bold mb-2">Đội ngũ sản xuất</h3>
        @for (ekip of productionEkips(); track ekip.id ?? $index) {
          <p-card>
            <p><label class="font-bold">Vai trò:</label> {{ ekip.role }}</p>
            <p><label class="font-bold">Tên:</label> {{ ekip.ekipName }}</p>
        </p-card>
        }
      </div>
      }
    }
  `,
})
export default class MovieProfileDetailComponent {
  readonly profile = input<IMovieProfile | null>(null);
  readonly productionEkips = input<IProductionEkip[]>([]);
  readonly edit = output<IMovieProfile>();
}
