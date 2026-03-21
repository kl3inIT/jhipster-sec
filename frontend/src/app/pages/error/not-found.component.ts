import { Component } from '@angular/core';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CardModule, ButtonModule, RouterModule],
  templateUrl: './not-found.component.html',
})
export default class NotFoundComponent {}
