import { IMovieProfile } from '@/app/pages/entities/movie/components/movie-list/movie-profile.model';

export type TopicOrientationStatus = 'ACTIVE' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | 'CANCELED';

export interface ITopicOrientation {
  id?: number;
  code?: string;
  title?: string;
  proposer?: string;
  submitDate?: string;
  status?: TopicOrientationStatus;
  movieProfile?: Pick<IMovieProfile, 'id' | 'profileCode' | 'movieName'>;
}

export type NewTopicOrientation = Omit<ITopicOrientation, 'id'> & { id: null };
