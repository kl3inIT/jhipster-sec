import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-error',
  standalone: true,
  imports: [CardModule, ButtonModule, RouterModule],
  templateUrl: './error.component.html',
})
export default class ErrorComponent {}
