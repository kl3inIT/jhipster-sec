import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, DestroyRef, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { $t, updatePreset, updateSurfacePalette } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';
import Lara from '@primeuix/themes/lara';
import Nora from '@primeuix/themes/nora';
import { SelectButtonModule } from 'primeng/selectbutton';
import { ButtonModule } from 'primeng/button';

import { LANGUAGE_DEFAULT, LANGUAGES } from 'app/config/language.constants';
import { StateStorageService } from 'app/core/auth/state-storage.service';
import { LayoutService } from 'app/layout/service/layout.service';

const PRESETS = {
  Aura,
  Lara,
  Nora,
} as const;

type PresetName = keyof typeof PRESETS;

type PaletteScale = {
  0?: string;
  50?: string;
  100?: string;
  200?: string;
  300?: string;
  400?: string;
  500?: string;
  600?: string;
  700?: string;
  800?: string;
  900?: string;
  950?: string;
};

type ThemeOption = {
  name: string;
  palette?: PaletteScale;
};

const SURFACES: ThemeOption[] = [
  {
    name: 'slate',
    palette: {
      0: '#ffffff',
      50: '#f8fafc',
      100: '#f1f5f9',
      200: '#e2e8f0',
      300: '#cbd5e1',
      400: '#94a3b8',
      500: '#64748b',
      600: '#475569',
      700: '#334155',
      800: '#1e293b',
      900: '#0f172a',
      950: '#020617',
    },
  },
  {
    name: 'gray',
    palette: {
      0: '#ffffff',
      50: '#f9fafb',
      100: '#f3f4f6',
      200: '#e5e7eb',
      300: '#d1d5db',
      400: '#9ca3af',
      500: '#6b7280',
      600: '#4b5563',
      700: '#374151',
      800: '#1f2937',
      900: '#111827',
      950: '#030712',
    },
  },
  {
    name: 'zinc',
    palette: {
      0: '#ffffff',
      50: '#fafafa',
      100: '#f4f4f5',
      200: '#e4e4e7',
      300: '#d4d4d8',
      400: '#a1a1aa',
      500: '#71717a',
      600: '#52525b',
      700: '#3f3f46',
      800: '#27272a',
      900: '#18181b',
      950: '#09090b',
    },
  },
  {
    name: 'stone',
    palette: {
      0: '#ffffff',
      50: '#fafaf9',
      100: '#f5f5f4',
      200: '#e7e5e4',
      300: '#d6d3d1',
      400: '#a8a29e',
      500: '#78716c',
      600: '#57534e',
      700: '#44403c',
      800: '#292524',
      900: '#1c1917',
      950: '#0c0a09',
    },
  },
  {
    name: 'ocean',
    palette: {
      0: '#ffffff',
      50: '#fbfcfc',
      100: '#f7f9f8',
      200: '#eff3f2',
      300: '#dadeDD',
      400: '#b1b7b6',
      500: '#828787',
      600: '#5f7274',
      700: '#415b61',
      800: '#29444e',
      900: '#183240',
      950: '#0c1920',
    },
  },
];

@Component({
  selector: 'app-configurator',
  imports: [CommonModule, FormsModule, TranslatePipe, ButtonModule, SelectButtonModule],
  template: `
    <div class="config-panel flex flex-col gap-4">
      <div class="flex items-start justify-between gap-3">
        <div>
          <div class="text-base font-semibold">{{ 'layout.settings.title' | translate }}</div>
          <p class="m-0 mt-1 text-sm text-color-secondary">
            {{ 'layout.settings.subtitle' | translate }}
          </p>
        </div>
      </div>

      <div class="flex flex-col gap-2">
        <span class="config-panel-label">{{ 'layout.settings.language' | translate }}</span>
        <div class="flex items-center gap-1 rounded-full surface-100 p-1">
          @for (language of languages; track language) {
            <button
              type="button"
              class="rounded-full border-0 px-3 py-1 text-sm font-medium cursor-pointer"
              [class.bg-white]="activeLanguage() === language"
              [class.shadow-1]="activeLanguage() === language"
              [attr.aria-label]="('layout.language.' + language) | translate"
              [attr.aria-pressed]="activeLanguage() === language"
              (click)="changeLanguage(language)"
            >
              {{ ('layout.language.' + language) | translate }}
            </button>
          }
        </div>
      </div>

      <div class="flex flex-col gap-2">
        <span class="config-panel-label">{{ 'layout.settings.appearance.label' | translate }}</span>
        <p-selectbutton
          [options]="appearanceOptions()"
          [ngModel]="appearanceMode()"
          (ngModelChange)="onAppearanceChange($event)"
          optionLabel="label"
          optionValue="value"
          [allowEmpty]="false"
          size="small"
        />
      </div>

      <div class="flex flex-col gap-2">
        <span class="config-panel-label">{{ 'layout.settings.preset' | translate }}</span>
        <p-selectbutton
          [options]="presetOptions"
          [ngModel]="selectedPreset()"
          (ngModelChange)="onPresetChange($event)"
          optionLabel="label"
          optionValue="value"
          [allowEmpty]="false"
          size="small"
        />
      </div>

      <div class="config-panel-colors">
        <span class="config-panel-label">{{ 'layout.settings.primary' | translate }}</span>
        <div>
          @for (primaryColor of primaryColors(); track primaryColor.name) {
            <button
              type="button"
              [title]="primaryColor.name"
              [class.active-color]="primaryColor.name === selectedPrimaryColor()"
              (click)="updateColors($event, 'primary', primaryColor)"
              [style.background-color]="
                primaryColor.name === 'noir' ? 'var(--text-color)' : primaryColor.palette?.['500']
              "
            ></button>
          }
        </div>
      </div>

      <div class="config-panel-colors">
        <span class="config-panel-label">{{ 'layout.settings.surface' | translate }}</span>
        <div>
          @for (surface of surfaces; track surface.name) {
            <button
              type="button"
              [title]="surface.name"
              [class.active-color]="selectedSurfaceColor() === surface.name"
              (click)="updateColors($event, 'surface', surface)"
              [style.background-color]="surface.palette?.['500']"
            ></button>
          }
        </div>
      </div>

      <div class="flex flex-col gap-2">
        <span class="config-panel-label">{{ 'layout.settings.menuMode.label' | translate }}</span>
        <p-selectbutton
          [options]="menuModeOptions()"
          [ngModel]="menuMode()"
          (ngModelChange)="onMenuModeChange($event)"
          optionLabel="label"
          optionValue="value"
          [allowEmpty]="false"
          size="small"
        />
      </div>
    </div>
  `,
  host: {
    class:
      'absolute top-full right-0 mt-3 w-[22rem] max-w-[calc(100vw-2rem)] rounded-[1.5rem] border border-surface-200 bg-surface-0 p-4 shadow-4 z-[1000] layout-topbar-config-panel',
  },
})
export class AppConfigurator {
  readonly languages = LANGUAGES;
  readonly surfaces = SURFACES;
  readonly presetOptions = (Object.keys(PRESETS) as PresetName[]).map(preset => ({
    label: preset,
    value: preset,
  }));
  readonly activeLanguage = signal<string>(LANGUAGE_DEFAULT);
  readonly selectedPrimaryColor = computed(() => this.layoutService.layoutConfig().primary);
  readonly selectedSurfaceColor = computed(() => this.layoutService.layoutConfig().surface ?? 'slate');
  readonly selectedPreset = computed(() => this.layoutService.layoutConfig().preset as PresetName);
  readonly menuMode = computed(() => this.layoutService.layoutConfig().menuMode);
  readonly appearanceMode = computed(() =>
    this.layoutService.layoutConfig().darkTheme ? 'dark' : 'light',
  );

  readonly appearanceOptions = computed(() => {
    this.activeLanguage();

    return [
      {
        label: this.translateService.instant('layout.settings.appearance.light'),
        value: 'light',
      },
      {
        label: this.translateService.instant('layout.settings.appearance.dark'),
        value: 'dark',
      },
    ];
  });

  readonly menuModeOptions = computed(() => {
    this.activeLanguage();

    return [
      {
        label: this.translateService.instant('layout.settings.menuMode.static'),
        value: 'static',
      },
      {
        label: this.translateService.instant('layout.settings.menuMode.overlay'),
        value: 'overlay',
      },
      {
        label: this.translateService.instant('layout.settings.menuMode.drawer'),
        value: 'drawer',
      },
    ];
  });

  readonly primaryColors = computed<ThemeOption[]>(() => {
    const selectedPreset = PRESETS[this.selectedPreset()];
    const presetPalette = selectedPreset.primitive as Record<string, PaletteScale>;
    const colors = [
      'emerald',
      'green',
      'lime',
      'orange',
      'amber',
      'yellow',
      'teal',
      'cyan',
      'sky',
      'blue',
      'indigo',
      'violet',
      'purple',
      'fuchsia',
      'pink',
      'rose',
    ];

    return [
      { name: 'noir', palette: {} },
      ...colors.map(name => ({
        name,
        palette: presetPalette[name],
      })),
    ];
  });

  private readonly destroyRef = inject(DestroyRef);
  private readonly layoutService = inject(LayoutService);
  private readonly stateStorageService = inject(StateStorageService);
  private readonly translateService = inject(TranslateService);
  private readonly platformId = inject(PLATFORM_ID);

  constructor() {
    this.activeLanguage.set(
      this.translateService.currentLang || this.stateStorageService.getLocale() || LANGUAGE_DEFAULT,
    );
    this.translateService.onLangChange
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ lang }) => this.activeLanguage.set(lang));
  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.applyStoredTheme();
    }
  }

  changeLanguage(language: string): void {
    this.stateStorageService.storeLocale(language);
    this.translateService.use(language);
    this.activeLanguage.set(language);
  }

  onAppearanceChange(mode: 'light' | 'dark'): void {
    this.layoutService.layoutConfig.update(state => ({
      ...state,
      darkTheme: mode === 'dark',
    }));
  }

  onPresetChange(preset: PresetName): void {
    this.layoutService.layoutConfig.update(state => ({
      ...state,
      preset,
    }));
    this.applyThemeSystem(preset);
  }

  onMenuModeChange(menuMode: 'static' | 'overlay' | 'drawer'): void {
    this.layoutService.layoutConfig.update(state => ({
      ...state,
      menuMode,
    }));
    this.layoutService.closeMenu();
  }

  updateColors(event: MouseEvent, type: 'primary' | 'surface', color: ThemeOption): void {
    if (type === 'primary') {
      this.layoutService.layoutConfig.update(state => ({
        ...state,
        primary: color.name,
      }));
      updatePreset(this.getPresetExtension());
    } else {
      this.layoutService.layoutConfig.update(state => ({
        ...state,
        surface: color.name,
      }));
      if (color.palette) {
        updateSurfacePalette(color.palette);
      }
    }

    event.stopPropagation();
  }

  private applyStoredTheme(): void {
    this.applyThemeSystem(this.selectedPreset());
  }

  private applyThemeSystem(presetName: PresetName): void {
    const surfacePalette =
      this.surfaces.find(surface => surface.name === this.selectedSurfaceColor())?.palette ??
      this.surfaces[0]?.palette;

    $t()
      .preset(PRESETS[presetName])
      .preset(this.getPresetExtension())
      .surfacePalette(surfacePalette)
      .use({ useDefaultOptions: true });
  }

  private getPresetExtension(): any {
    const primaryColor = this.primaryColors().find(
      color => color.name === this.selectedPrimaryColor(),
    );

    if (primaryColor?.name === 'noir') {
      return {
        primitive: {
          borderRadius: {
            none: '0',
            xs: '6px',
            sm: '10px',
            md: '14px',
            lg: '18px',
            xl: '24px',
          },
        },
        semantic: {
          primary: {
            50: '{surface.50}',
            100: '{surface.100}',
            200: '{surface.200}',
            300: '{surface.300}',
            400: '{surface.400}',
            500: '{surface.500}',
            600: '{surface.600}',
            700: '{surface.700}',
            800: '{surface.800}',
            900: '{surface.900}',
            950: '{surface.950}',
          },
          focusRing: {
            width: '2px',
            style: 'solid',
            color: '{primary.300}',
            offset: '2px',
            shadow: 'none',
          },
          colorScheme: {
            light: {
              primary: {
                color: '{primary.950}',
                contrastColor: '#ffffff',
                hoverColor: '{primary.800}',
                activeColor: '{primary.700}',
              },
              highlight: {
                background: '{primary.950}',
                focusBackground: '{primary.700}',
                color: '#ffffff',
                focusColor: '#ffffff',
              },
            },
            dark: {
              primary: {
                color: '{primary.50}',
                contrastColor: '{primary.950}',
                hoverColor: '{primary.200}',
                activeColor: '{primary.300}',
              },
              highlight: {
                background: '{primary.50}',
                focusBackground: '{primary.300}',
                color: '{primary.950}',
                focusColor: '{primary.950}',
              },
            },
          },
        },
      };
    }

    const defaultPalette =
      (PRESETS.Aura.primitive as Record<string, PaletteScale> | undefined)?.['teal'] ?? {};
    const primaryPalette = primaryColor?.palette ?? defaultPalette;

    return {
      primitive: {
        borderRadius: {
          none: '0',
          xs: '6px',
          sm: '10px',
          md: '14px',
          lg: '18px',
          xl: '24px',
        },
      },
      semantic: {
        primary: primaryPalette,
        focusRing: {
          width: '2px',
          style: 'solid',
          color: '{primary.300}',
          offset: '2px',
          shadow: 'none',
        },
        colorScheme: {
          light: {
            primary: {
              color: '{primary.600}',
              contrastColor: '#ffffff',
              hoverColor: '{primary.700}',
              activeColor: '{primary.800}',
            },
            highlight: {
              background: '{primary.600}',
              focusBackground: '{primary.700}',
              color: '#ffffff',
              focusColor: '#ffffff',
            },
            formField: {
              background: '{surface.0}',
              disabledBackground: '{surface.200}',
              filledBackground: '{surface.0}',
              filledHoverBackground: '{surface.0}',
              filledFocusBackground: '{surface.0}',
              borderColor: '{surface.300}',
              hoverBorderColor: '{surface.400}',
              focusBorderColor: '{primary.500}',
              color: '{surface.900}',
              disabledColor: '{surface.500}',
              placeholderColor: '{surface.500}',
              floatLabelColor: '{surface.500}',
              floatLabelFocusColor: '{primary.color}',
              floatLabelActiveColor: '{surface.500}',
              iconColor: '{surface.500}',
              shadow: 'none',
            },
          },
          dark: {
            primary: {
              color: '{primary.400}',
              contrastColor: '{surface.950}',
              hoverColor: '{primary.300}',
              activeColor: '{primary.200}',
            },
            highlight: {
              background: '{primary.400}',
              focusBackground: '{primary.300}',
              color: '{surface.950}',
              focusColor: '{surface.950}',
            },
            formField: {
              background: '{surface.950}',
              disabledBackground: '{surface.800}',
              filledBackground: '{surface.900}',
              filledHoverBackground: '{surface.900}',
              filledFocusBackground: '{surface.900}',
              borderColor: '{surface.700}',
              hoverBorderColor: '{surface.600}',
              focusBorderColor: '{primary.400}',
              color: '{surface.0}',
              disabledColor: '{surface.500}',
              placeholderColor: '{surface.500}',
              floatLabelColor: '{surface.500}',
              floatLabelFocusColor: '{primary.color}',
              floatLabelActiveColor: '{surface.500}',
              iconColor: '{surface.400}',
              shadow: 'none',
            },
          },
        },
      },
    };
  }
}
