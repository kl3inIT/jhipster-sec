import { Injectable } from '@angular/core';
import { Params } from '@angular/router';

export interface WorkspaceContext {
  queryParams: Record<string, string>;
  page?: string;
  sort?: string;
  filters: Record<string, string>;
}

@Injectable({ providedIn: 'root' })
export class WorkspaceContextService {
  private readonly contexts = new Map<string, WorkspaceContext>();

  store(navigationNodeId: string, queryParams: Params): void {
    const normalized = Object.entries(queryParams).reduce<Record<string, string>>(
      (acc, [key, value]) => {
        if (typeof value === 'string') {
          acc[key] = value;
        } else if (
          Array.isArray(value) &&
          value.length > 0 &&
          typeof value[value.length - 1] === 'string'
        ) {
          acc[key] = value[value.length - 1] as string;
        }
        return acc;
      },
      {},
    );

    this.contexts.set(navigationNodeId, {
      queryParams: normalized,
      page: normalized['page'],
      sort: normalized['sort'],
      filters: Object.fromEntries(
        Object.entries(normalized).filter(([key]) => !['page', 'size', 'sort'].includes(key)),
      ),
    });
  }

  get(navigationNodeId: string): WorkspaceContext | null {
    return this.contexts.get(navigationNodeId) ?? null;
  }
}
