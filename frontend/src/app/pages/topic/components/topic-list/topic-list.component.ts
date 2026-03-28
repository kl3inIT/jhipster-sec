import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-topic-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule
  ],
  templateUrl: './topic-list.component.html',
  styleUrls: ['./topic-list.component.scss']
})
export class TopicListComponent {

  keyword = '';
  statusFilter = '';

  topics: any[] = [];

  trackById(index: number, item: any) {
    return item.id;
  }

  exportExcel() {
    console.log('export excel');
  }

  getStatusLabel(status: string) {
    switch (status) {
      case 'APPROVED':
        return 'Đã duyệt';
      case 'PENDING':
        return 'Đang xem xét';
      case 'SUBMITTED':
        return 'Đã gửi';
      case 'REJECTED':
        return 'Từ chối';
      default:
        return status;
    }
  }

}