import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';

@Component({
  standalone: true,
  imports: [CardModule, TranslatePipe],
  template: `
    <p-card>
      <h1 class="m-0 mb-2 text-2xl font-semibold">{{ 'userManagement.home.title' | translate }}</h1>
      <p class="m-0">{{ 'userManagement.placeholder.phase6' | translate }}</p>
    </p-card>
  `,
})
export default class UserManagementPlaceholderComponent {}
