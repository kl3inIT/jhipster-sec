import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ApplicationConfigService } from './core/config/application-config.service';
import { environment } from 'environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule],
  template: '<router-outlet></router-outlet>',
})
export class App {
  private readonly applicationConfigService = inject(ApplicationConfigService);

  constructor() {
    this.applicationConfigService.setEndpointPrefix(environment.SERVER_API_URL ?? '');
  }
}
