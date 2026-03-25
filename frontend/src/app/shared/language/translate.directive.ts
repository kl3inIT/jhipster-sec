import { Directive, ElementRef, OnChanges, OnDestroy, OnInit, inject, input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { translationNotFoundMessage } from 'app/config/translation.config';

@Directive({
  selector: '[jhiTranslate]',
})
export class TranslateDirective implements OnInit, OnChanges, OnDestroy {
  readonly jhiTranslate = input.required<string>();
  readonly translateValues = input<Record<string, unknown>>();

  private readonly directiveDestroyed = new Subject<void>();
  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly translateService = inject(TranslateService);

  ngOnInit(): void {
    this.translateService.onLangChange.pipe(takeUntil(this.directiveDestroyed)).subscribe(() => this.getTranslation());
    this.translateService.onTranslationChange.pipe(takeUntil(this.directiveDestroyed)).subscribe(() => this.getTranslation());
  }

  ngOnChanges(): void {
    this.getTranslation();
  }

  ngOnDestroy(): void {
    this.directiveDestroyed.next();
    this.directiveDestroyed.complete();
  }

  private getTranslation(): void {
    this.translateService
      .get(this.jhiTranslate(), this.translateValues())
      .pipe(takeUntil(this.directiveDestroyed))
      .subscribe({
        next: value => {
          this.el.nativeElement.innerHTML = value;
        },
        error: () => {
          this.el.nativeElement.innerHTML = `${translationNotFoundMessage}[${this.jhiTranslate()}]`;
        },
      });
  }
}
