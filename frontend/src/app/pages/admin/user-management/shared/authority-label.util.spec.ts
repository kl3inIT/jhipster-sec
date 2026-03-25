import { TranslateService } from '@ngx-translate/core';

import { translationNotFoundMessage } from 'app/config/translation.config';

import { buildAuthorityRows, resolveAuthorityLabel } from './authority-label.util';

describe('authority-label.util', () => {
  it('returns the translated authority label when present', () => {
    const translateService = {
      instant: vi.fn().mockReturnValue('Quản trị viên'),
    } as unknown as TranslateService;

    expect(resolveAuthorityLabel('ROLE_ADMIN', translateService)).toBe('Quản trị viên');
  });

  it('falls back to the authority code when the translation is missing', () => {
    const translateService = {
      instant: vi.fn().mockReturnValue(`${translationNotFoundMessage}[userManagement.authorityLabels.ROLE_ACCOUNTANT]`),
    } as unknown as TranslateService;

    expect(resolveAuthorityLabel('ROLE_ACCOUNTANT', translateService)).toBe('ROLE_ACCOUNTANT');
  });

  it('builds rows without exposing translation-not-found labels', () => {
    const translateService = {
      instant: vi
        .fn()
        .mockImplementation((key: string) =>
          key === 'userManagement.authorityLabels.ROLE_ADMIN'
            ? 'Quản trị viên'
            : `${translationNotFoundMessage}[${key}]`,
        ),
    } as unknown as TranslateService;

    expect(buildAuthorityRows(['ROLE_ADMIN', 'ROLE_ACCOUNTANT'], ['ROLE_ADMIN'], translateService, false)).toEqual([
      {
        authority: 'ROLE_ADMIN',
        label: 'Quản trị viên',
        selected: true,
        disabled: false,
      },
      {
        authority: 'ROLE_ACCOUNTANT',
        label: 'ROLE_ACCOUNTANT',
        selected: false,
        disabled: false,
      },
    ]);
  });
});
