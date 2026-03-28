import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';

import { ActivateService } from './activate.service';

@Component({
  selector: 'app-activate',
  imports: [RouterModule, TranslatePipe, CardModule, MessageModule],
  templateUrl: './activate.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class ActivateComponent implements OnInit {
  readonly error = signal(false);
  readonly success = signal(false);

  private readonly route = inject(ActivatedRoute);
  private readonly activateService = inject(ActivateService);

  ngOnInit(): void {
    const key = this.route.snapshot.queryParamMap.get('key');
    if (!key) {
      this.error.set(true);
      return;
    }

    this.activateService.get(key).subscribe({
      next: () => {
        this.success.set(true);
        this.error.set(false);
      },
      error: () => {
        this.success.set(false);
        this.error.set(true);
      },
    });
  }
}
