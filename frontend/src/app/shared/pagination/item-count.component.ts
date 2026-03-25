import { Component, computed, input } from '@angular/core';

import { TranslateDirective } from '../language/translate.directive';

@Component({
  selector: 'jhi-item-count',
  standalone: true,
  imports: [TranslateDirective],
  template: `<div jhiTranslate="global.item-count" [translateValues]="{ first: first(), second: second(), total: total() }"></div>`,
})
export class ItemCountComponent {
  readonly params = input<{
    page?: number;
    totalItems?: number;
    itemsPerPage?: number;
  }>();

  readonly first = computed(() => {
    const params = this.params();
    if (params?.page && params.totalItems !== undefined && params.itemsPerPage) {
      return (params.page - 1) * params.itemsPerPage + 1;
    }
    return undefined;
  });

  readonly second = computed(() => {
    const params = this.params();
    if (params?.page && params.totalItems !== undefined && params.itemsPerPage) {
      return params.page * params.itemsPerPage < params.totalItems ? params.page * params.itemsPerPage : params.totalItems;
    }
    return undefined;
  });

  readonly total = computed(() => this.params()?.totalItems);
}
