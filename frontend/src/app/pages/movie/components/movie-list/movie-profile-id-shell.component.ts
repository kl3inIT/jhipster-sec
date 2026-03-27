import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Chứa router-outlet cho /movie/:id và /movie/:id/edit */
@Component({
  selector: 'app-movie-profile-id-shell',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export default class MovieProfileIdShellComponent {}
