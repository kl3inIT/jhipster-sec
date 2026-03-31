import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';

import { ITopicOrientation, TopicOrientationStatus } from '../topic-orientation.model';
import { TopicOrientationService } from '../service/topic-orientation.service';

@Component({
  selector: 'app-topic-orientation-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe, CardModule, ButtonModule],
  templateUrl: './topic-orientation-detail.component.html',
})
export default class TopicOrientationDetailComponent implements OnInit {
  topicOrientation = signal<ITopicOrientation | null>(null);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translateService = inject(TranslateService);
  private readonly topicOrientationService = inject(TopicOrientationService);

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (!id) {
        return;
      }
      this.topicOrientationService.find(Number(id)).subscribe({
        next: (res) => this.topicOrientation.set(res.body ?? null),
        error: () => this.router.navigate(['/404']),
      });
    });
  }

  getStatusLabel(status?: TopicOrientationStatus): string {
    if (!status) {
      return '';
    }
    return this.translateService.instant(`angappApp.topicOrientation.status.${status}`);
  }

  back(): void {
    this.router.navigate(['/entities/topic-orientation']);
  }

  edit(item: ITopicOrientation): void {
    this.router.navigate(['/entities/topic-orientation', item.id, 'edit']);
  }
}
