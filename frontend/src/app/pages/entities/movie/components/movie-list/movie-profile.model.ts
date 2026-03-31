import { IProductionEkip } from './production-ekip.model';

export interface IMovieProfile {
  id: number;
  profileCode?: string | null;
  movieName?: string | null;
  movieType?: string | null;
  classification?: string | null;
  genre?: string | null;
  productionYear?: number | null;
  startDate?: string | null;
  endDate?: string | null;
  status?: string | null;
  themeOrientation?: string | null;
  summary?: string | null;
  ekipMembers?: IProductionEkip[] | null;
}

export type NewMovieProfile = Omit<IMovieProfile, 'id'> & { id: null };
