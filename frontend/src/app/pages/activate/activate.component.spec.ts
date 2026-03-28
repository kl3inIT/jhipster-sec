import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { provideTranslateService, TranslateService } from '@ngx-translate/core';
import { firstValueFrom, of, throwError } from 'rxjs';

import ActivateComponent from './activate.component';
import { ActivateService } from './activate.service';

describe('ActivateComponent', () => {
  let component: ActivateComponent;
  let fixture: ComponentFixture<ActivateComponent>;
  let mockActivateService: {
    get: ReturnType<typeof vi.fn>;
  };

  async function setup(key: string | null, shouldFail = false): Promise<void> {
    mockActivateService = {
      get: shouldFail ? vi.fn().mockReturnValue(throwError(() => new Error('boom'))) : vi.fn().mockReturnValue(of(void 0)),
    };

    await TestBed.configureTestingModule({
      imports: [ActivateComponent],
      providers: [
        provideRouter([]),
        provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (name: string) => (name === 'key' ? key : null),
              },
            },
          },
        },
        { provide: ActivateService, useValue: mockActivateService },
      ],
    }).compileComponents();

    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation('en', {
      activate: {
        title: 'Activation',
        messages: {
          success: '<strong>Your user account has been activated.</strong> Please ',
          error: '<strong>Your user could not be activated.</strong> Please use the registration form to sign up.',
        },
      },
      global: {
        messages: {
          info: {
            authenticated: {
              link: 'sign in',
            },
          },
        },
      },
      login: {
        title: 'Sign in',
        form: {
          button: 'Sign in',
        },
      },
    });
    await firstValueFrom(translateService.use('en'));

    fixture = TestBed.createComponent(ActivateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('activates the user when a key is present', async () => {
    await setup('activation-key');

    expect(mockActivateService.get).toHaveBeenCalledWith('activation-key');
    expect(component.success()).toBe(true);
    expect(component.error()).toBe(false);
  });

  it('shows an error when the key is missing', async () => {
    await setup(null);

    expect(mockActivateService.get).not.toHaveBeenCalled();
    expect(component.success()).toBe(false);
    expect(component.error()).toBe(true);
  });

  it('shows an error when activation fails', async () => {
    await setup('bad-key', true);

    expect(mockActivateService.get).toHaveBeenCalledWith('bad-key');
    expect(component.success()).toBe(false);
    expect(component.error()).toBe(true);
  });
});
