import { Component } from '@angular/core';

import { CardModule } from 'primeng/card';

@Component({
  selector: 'app-permission-matrix',
  standalone: true,
  imports: [CardModule],
  template: `
    <p-card>
      <ng-template #title>Permission Matrix</ng-template>
      <p>Permission matrix coming soon</p>
    </p-card>
  `,
})
export default class PermissionMatrixComponent {}
