import dayjs from 'dayjs/esm';
import 'dayjs/esm/locale/en';
import 'dayjs/esm/locale/vi';

const normalizeDayjsLocale = (language: string): 'en' | 'vi' => (language.toLowerCase().startsWith('vi') ? 'vi' : 'en');

export const applyDayjsLocale = (language: string): void => {
  dayjs.locale(normalizeDayjsLocale(language));
};
