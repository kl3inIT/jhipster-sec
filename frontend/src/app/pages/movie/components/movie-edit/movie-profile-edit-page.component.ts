import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { catchError, finalize, of } from 'rxjs';

import { IMovieProfile } from '../movie-list/movie-profile.model';
import { MovieProfileService } from '../movie-list/services/movie-profile.service';
import MovieProfileEditComponent from '../movie-edit/movie-profile-edit.component';

@Component({
  selector: 'app-movie-profile-edit-page',
  standalone: true,
  providers: [MessageService],
  imports: [RouterLink, ButtonModule, ToastModule, MovieProfileEditComponent],
  templateUrl: './movie-profile-edit-page.component.html',
  styleUrls: ['./movie-profile-edit-page.component.scss'],
})
export default class MovieProfileEditPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly movieProfileService = inject(MovieProfileService);
  private readonly cd = inject(ChangeDetectorRef);

  profile: IMovieProfile | null = null;
  isLoading = true;
  loadError = false;

  ngOnInit(): void {
    const idParam = this.route.parent?.snapshot.paramMap.get('id') ?? this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (!Number.isFinite(id) || id < 1) {
      this.loadError = true;
      this.isLoading = false;
      return;
    }

    this.movieProfileService
      .find(id)
      .pipe(
        catchError(() => of(null)),
        finalize(() => {
          this.isLoading = false;
          this.cd.detectChanges();
        }),
      )
      .subscribe(res => {
        this.profile = res?.body ? { ...res.body } : null;
        this.loadError = !this.profile;
      });
  }

  onUpdated(): void {
    if (this.profile?.id) {
      void this.router.navigate(['/entities/movie', this.profile.id, 'view']);
    } else {
      void this.router.navigate(['/entities/movie']);
    }
  }

  onCancel(): void {
    if (this.profile?.id) {
      void this.router.navigate(['/entities/movie', this.profile.id, 'view']);
    } else {
      void this.router.navigate(['/entities/movie']);
    }
  }

  detailBackLink(): (string | number)[] {
    const id = this.profile?.id;
    return id != null ? ['/entities/movie', id, 'view'] : ['/entities/movie'];
  }
}
