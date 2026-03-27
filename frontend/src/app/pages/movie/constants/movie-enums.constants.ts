/**
 * Đồng bộ với enum Java (value = name(), label = getDisplayName).
 * Không gọi API /api/enums/* — dùng trực tiếp trên FE.
 */
export interface MovieEnumOption {
  label: string;
  value: string;
}

export const MOVIE_TYPE_OPTIONS: MovieEnumOption[] = [
  { value: 'DIGITAL', label: 'Phim kĩ thuật số' },
  { value: 'MOVIE', label: 'Phim nhựa' },
];

export const CLASSIFICATION_OPTIONS: MovieEnumOption[] = [
  { value: 'MOVIE', label: 'Phim truyện' },
  { value: 'DOCUMENTARY', label: 'Tài liệu' },
  { value: 'SCIENCE', label: 'Khoa học' },
  { value: 'ANIMATION', label: 'Hoạt hình' },
];

export const GENRE_OPTIONS: MovieEnumOption[] = [
  { value: 'ACTION', label: 'Hành động' },
  { value: 'COMEDY', label: 'Hài' },
  { value: 'DRAMA', label: 'Tâm lý' },
  { value: 'HORROR', label: 'Kinh dị' },
  { value: 'ROMANCE', label: 'Tình cảm' },
  { value: 'SCI_FI', label: 'Khoa học viễn tưởng' },
  { value: 'ANIMATION', label: 'Hoạt hình' },
];

export const STATUS_OPTIONS: MovieEnumOption[] = [
  { value: 'ACTIVE', label: 'Đang hoạt động' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'REJECTED', label: 'Từ chối' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'CANCELED', label: 'Đã hủy' },
];

export const PRODUCTION_ROLE_OPTIONS: MovieEnumOption[] = [
  { value: 'DIRECTOR', label: 'Đạo diễn' },
  { value: 'PRODUCER', label: 'Nhà sản xuất' },
  { value: 'ASSISTANT_DIRECTOR', label: 'Trợ lý đạo diễn' },
  { value: 'CINEMATOGRAPHER', label: 'Quay phim' },
  { value: 'ACTOR', label: 'Diễn viên' },
  { value: 'EDITOR', label: 'Biên tập' },
];
