import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-error',
  standalone: true,
  imports: [CardModule, ButtonModule, RouterModule, TranslatePipe],
  templateUrl: './error.component.html',
})
export default class ErrorComponent {}
