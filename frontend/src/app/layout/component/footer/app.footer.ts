import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  standalone: true,
  selector: 'app-footer',
  imports: [TranslatePipe],
  template: `<div class="layout-footer">{{ 'layout.footer.productName' | translate }}</div>`,
})
export class AppFooter {}
