import { HttpParams } from '@angular/common/http';

import { Pagination, Search, SearchWithPagination } from './request.model';

type RequestScalar = string | number | boolean;
type RequestArray = readonly RequestScalar[];
type RequestValue = RequestScalar | RequestArray | null | undefined;
type RequestOptions = Pagination | Search | SearchWithPagination | Record<string, RequestValue>;

const asArray = (value: RequestScalar | RequestArray): readonly RequestScalar[] => {
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return [value];
  }
  return value;
};

export const createRequestOption = (req?: RequestOptions): HttpParams => {
  let options: HttpParams = new HttpParams();

  if (req) {
    Object.entries(req).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        for (const entry of asArray(value)) {
          if (entry !== '') {
            options = options.append(key, `${entry}`);
          }
        }
      }
    });
  }

  return options;
};
