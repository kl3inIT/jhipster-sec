import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [CardModule, ButtonModule, RouterModule],
  templateUrl: './access-denied.component.html',
})
export default class AccessDeniedComponent {}
