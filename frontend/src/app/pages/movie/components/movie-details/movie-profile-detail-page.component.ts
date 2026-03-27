import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { catchError, finalize, of } from 'rxjs';

import { IMovieProfile } from '../movie-list/movie-profile.model';
import { IProductionEkip } from '../movie-list/production-ekip.model';
import { MovieProfileService } from '../movie-list/services/movie-profile.service';
import { ProductionEkipService } from '../movie-list/services/production-ekip.service';
import MovieProfileDetailComponent from '../movie-details/movie-profile-detail.component';



@Component({
  selector: 'app-movie-profile-detail-page',
  standalone: true,
  imports: [RouterLink, ButtonModule, MovieProfileDetailComponent],
  templateUrl: './movie-profile-detail-page.component.html',
  styleUrls: ['./movie-profile-detail-page.component.scss'],
})
export default class MovieProfileDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly movieProfileService = inject(MovieProfileService);
  private readonly productionEkipService = inject(ProductionEkipService);
  private readonly cd = inject(ChangeDetectorRef);

  profile: IMovieProfile | null = null;
  productionEkips: IProductionEkip[] = [];
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
        this.profile = res?.body ?? null;
        this.loadError = !this.profile;
        if (this.profile?.id) {
          this.productionEkipService
            .query(this.profile.id)
            .pipe(catchError(() => of([])))
            .subscribe(items => {
              this.productionEkips = [...(items ?? [])];
              this.cd.detectChanges();
            });
        }
      });
  }

  goToEdit(p: IMovieProfile): void {
    if (!p.id) {
      return;
    }
    void this.router.navigate(['/entities/movie', p.id, 'edit']);
  }
}
