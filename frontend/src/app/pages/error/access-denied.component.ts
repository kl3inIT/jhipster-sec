import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CardModule, ButtonModule, RouterModule, TranslatePipe],
  templateUrl: './access-denied.component.html',
})
export default class AccessDeniedComponent {}
