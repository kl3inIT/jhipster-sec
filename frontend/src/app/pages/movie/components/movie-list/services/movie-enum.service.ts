import { Injectable } from '@angular/core';

import {
  CLASSIFICATION_OPTIONS,
  GENRE_OPTIONS,
  MOVIE_TYPE_OPTIONS,
  PRODUCTION_ROLE_OPTIONS,
  STATUS_OPTIONS,
  MovieEnumOption,
} from '../../../constants/movie-enums.constants';

/**
 * Nguồn enum cố định trên FE (đồng bộ với backend Java).
 */
@Injectable({ providedIn: 'root' })
export class MovieEnumService {
  getClassifications(): MovieEnumOption[] {
    return CLASSIFICATION_OPTIONS;
  }

  getMovieTypes(): MovieEnumOption[] {
    return MOVIE_TYPE_OPTIONS;
  }

  getGenres(): MovieEnumOption[] {
    return GENRE_OPTIONS;
  }

  getStatuses(): MovieEnumOption[] {
    return STATUS_OPTIONS;
  }

  getProductionRoles(): MovieEnumOption[] {
    return PRODUCTION_ROLE_OPTIONS;
  }
}
