import { TranslateService } from '@ngx-translate/core';

import { translationNotFoundMessage } from 'app/config/translation.config';

import { IUserRoleRow } from '../user-management.model';

const TRANSLATION_NOT_FOUND_PREFIX = `${translationNotFoundMessage}[`;

export function resolveAuthorityLabel(authority: string, translateService: TranslateService): string {
  const key = `userManagement.authorityLabels.${authority}`;
  const translated = translateService.instant(key);
  return translated === key || translated.startsWith(TRANSLATION_NOT_FOUND_PREFIX) ? authority : translated;
}

export function buildAuthorityRows(
  allAuthorities: string[],
  selectedAuthorities: string[],
  translateService: TranslateService,
  disabled: boolean,
): IUserRoleRow[] {
  const selectedSet = new Set(selectedAuthorities);
  return allAuthorities.map(authority => ({
    authority,
    label: resolveAuthorityLabel(authority, translateService),
    selected: selectedSet.has(authority),
    disabled,
  }));
}
