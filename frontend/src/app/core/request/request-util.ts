import { HttpParams } from '@angular/common/http';

export const createRequestOption = (req?: any): HttpParams => {
  let options: HttpParams = new HttpParams();

  if (req) {
    Object.entries(req).forEach(([key, val]) => {
      if (val !== undefined && val !== null) {
        for (const value of ([] as any[]).concat(req[key]).filter((v: any) => v !== '')) {
          options = options.append(key, value);
        }
      }
    });
  }

  return options;
};
