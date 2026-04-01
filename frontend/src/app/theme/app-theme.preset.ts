import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

export const APP_PRIMITIVE_TOKENS = {
  borderRadius: {
    none: '0',
    xs: '0.25rem',
    sm: '0.375rem',
    md: '0.5rem',
    lg: '0.75rem',
    xl: '1rem',
  },
};

const APP_FORM_CONTROL_ROOT = {
  background: '{form.field.background}',
  disabledBackground: '{form.field.disabled.background}',
  filledBackground: '{form.field.filled.background}',
  filledHoverBackground: '{form.field.filled.hover.background}',
  filledFocusBackground: '{form.field.filled.focus.background}',
  borderColor: '{form.field.border.color}',
  hoverBorderColor: '{form.field.hover.border.color}',
  focusBorderColor: '{form.field.focus.border.color}',
  invalidBorderColor: '{form.field.invalid.border.color}',
  color: '{form.field.color}',
  disabledColor: '{form.field.disabled.color}',
  placeholderColor: '{form.field.placeholder.color}',
  invalidPlaceholderColor: '{form.field.invalid.placeholder.color}',
  shadow: 'var(--app-shadow-xs)',
  paddingX: '0.95rem',
  paddingY: '0.8rem',
  borderRadius: '{border.radius.sm}',
  focusRing: {
    width: '0.125rem',
    style: 'solid',
    color: '{form.field.focus.border.color}',
    offset: '0',
    shadow: 'var(--app-field-shadow)',
  },
  transitionDuration: '0.18s',
  sm: {
    fontSize: '0.875rem',
    paddingX: '0.75rem',
    paddingY: '0.625rem',
  },
  lg: {
    fontSize: '1rem',
    paddingX: '1rem',
    paddingY: '0.9rem',
  },
};

const APP_SELECT_OVERLAY = {
  background: 'var(--app-overlay-bg)',
  borderColor: '{content.border.color}',
  borderRadius: '{border.radius.md}',
  color: '{text.color}',
  shadow: 'var(--app-shadow-sm)',
};

const APP_SELECT_LIST = {
  padding: '0.375rem',
  gap: '0.25rem',
  header: {
    padding: '0.625rem 0.875rem 0.375rem',
  },
};

const APP_SELECT_OPTION = {
  focusBackground: '{content.hover.background}',
  selectedBackground: 'color-mix(in srgb, var(--p-primary-color) 12%, var(--app-surface-soft))',
  selectedFocusBackground:
    'color-mix(in srgb, var(--p-primary-color) 18%, var(--app-surface-soft))',
  color: '{text.color}',
  focusColor: '{text.hover.color}',
  selectedColor: '{primary.color}',
  selectedFocusColor: '{primary.color}',
  padding: '0.75rem 0.875rem',
  borderRadius: '{border.radius.sm}',
};

const APP_SELECT_OPTION_GROUP = {
  background: 'transparent',
  color: 'var(--app-text-muted)',
  fontWeight: '600',
  padding: '0.625rem 0.875rem 0.375rem',
};

const APP_TABLE_TOKENS = {
  root: {
    transitionDuration: '0.18s',
    borderColor: '{content.border.color}',
  },
  header: {
    background: '{content.background}',
    borderColor: '{content.border.color}',
    color: '{text.color}',
    borderWidth: '0 0 1px 0',
    padding: '1rem 1.25rem',
  },
  headerCell: {
    background: 'var(--app-surface-muted)',
    hoverBackground: 'var(--app-surface-muted)',
    selectedBackground: '{content.background}',
    borderColor: '{content.border.color}',
    color: 'var(--app-text-muted)',
    hoverColor: '{text.color}',
    selectedColor: '{text.color}',
    gap: '0.5rem',
    padding: '0.95rem 1rem',
    focusRing: {
      width: '{focus.ring.width}',
      style: '{focus.ring.style}',
      color: '{focus.ring.color}',
      offset: '{focus.ring.offset}',
      shadow: '{focus.ring.shadow}',
    },
    sm: {
      padding: '0.75rem 0.875rem',
    },
    lg: {
      padding: '1rem 1.125rem',
    },
  },
  columnTitle: {
    fontWeight: '700',
  },
  row: {
    background: '{content.background}',
    hoverBackground: 'var(--app-surface-muted)',
    selectedBackground: 'color-mix(in srgb, var(--p-primary-color) 10%, var(--app-surface-soft))',
    color: '{text.color}',
    hoverColor: '{text.color}',
    selectedColor: '{text.color}',
    focusRing: {
      width: '0',
      style: 'none',
      color: 'transparent',
      offset: '0',
      shadow: 'none',
    },
    stripedBackground: 'color-mix(in srgb, var(--app-surface-muted) 82%, transparent)',
  },
  bodyCell: {
    borderColor: 'color-mix(in srgb, var(--app-border-soft) 60%, transparent)',
    padding: '1rem',
    sm: {
      padding: '0.875rem',
    },
    lg: {
      padding: '1.125rem',
    },
    selectedBorderColor: '{primary.color}',
  },
  footerCell: {
    background: '{content.background}',
    borderColor: '{content.border.color}',
    color: '{text.color}',
    padding: '0.95rem 1rem',
  },
  columnFooter: {
    fontWeight: '600',
  },
  footer: {
    background: '{content.background}',
    borderColor: '{content.border.color}',
    color: '{text.color}',
    borderWidth: '1px 0 0 0',
    padding: '0.95rem 1rem',
  },
  dropPoint: {
    color: '{primary.color}',
  },
  columnResizer: {
    width: '0.125rem',
  },
  resizeIndicator: {
    width: '0.125rem',
    color: '{primary.color}',
  },
  sortIcon: {
    color: 'var(--app-text-muted)',
    hoverColor: '{text.color}',
    size: '0.875rem',
  },
  loadingIcon: {
    size: '1rem',
  },
  filter: {
    inlineGap: '0.5rem',
    overlaySelect: APP_SELECT_OVERLAY,
    overlayPopover: {
      background: 'var(--app-overlay-bg)',
      borderColor: '{content.border.color}',
      borderRadius: '{border.radius.md}',
      color: '{text.color}',
      shadow: 'var(--app-shadow-sm)',
      padding: '0.75rem',
      gap: '0.625rem',
    },
    rule: {
      borderColor: '{content.border.color}',
    },
    constraintList: {
      padding: '0.25rem',
      gap: '0.25rem',
    },
    constraint: {
      focusBackground: '{content.hover.background}',
      selectedBackground: 'color-mix(in srgb, var(--p-primary-color) 12%, var(--app-surface-soft))',
      selectedFocusBackground:
        'color-mix(in srgb, var(--p-primary-color) 18%, var(--app-surface-soft))',
      color: '{text.color}',
      focusColor: '{text.hover.color}',
      selectedColor: '{primary.color}',
      selectedFocusColor: '{primary.color}',
      separator: {
        borderColor: '{content.border.color}',
      },
      padding: '0.625rem 0.75rem',
      borderRadius: '{border.radius.sm}',
    },
  },
  paginatorTop: {
    borderColor: '{content.border.color}',
    borderWidth: '0 0 1px 0',
  },
  paginatorBottom: {
    borderColor: '{content.border.color}',
    borderWidth: '1px 0 0 0',
  },
};

export const APP_COMPONENT_TOKENS = {
  button: {
    root: {
      borderRadius: '{border.radius.sm}',
      roundedBorderRadius: '9999px',
      gap: '0.5rem',
      paddingX: '0.875rem',
      paddingY: '0.5rem',
      iconOnlyWidth: '2.5rem',
      sm: {
        fontSize: '0.875rem',
        paddingX: '0.75rem',
        paddingY: '0.375rem',
        iconOnlyWidth: '2.125rem',
      },
      lg: {
        fontSize: '1rem',
        paddingX: '1rem',
        paddingY: '0.625rem',
        iconOnlyWidth: '2.75rem',
      },
      label: {
        fontWeight: '600',
      },
      raisedShadow: 'none',
      focusRing: {
        width: '0.125rem',
        style: 'solid',
        offset: '0.125rem',
      },
      badgeSize: '1.125rem',
      transitionDuration: '0.18s',
      primary: {
        background: '{primary.color}',
        hoverBackground: '{primary.hover.color}',
        activeBackground: '{primary.active.color}',
        borderColor: '{primary.color}',
        hoverBorderColor: '{primary.hover.color}',
        activeBorderColor: '{primary.active.color}',
        color: '{primary.contrast.color}',
        hoverColor: '{primary.contrast.color}',
        activeColor: '{primary.contrast.color}',
        focusRing: {
          color: '{focus.ring.color}',
          shadow: 'var(--app-field-shadow)',
        },
      },
      secondary: {
        background: '{content.background}',
        hoverBackground: '{content.hover.background}',
        activeBackground: 'var(--app-surface-strong)',
        borderColor: '{content.border.color}',
        hoverBorderColor: '{content.border.color}',
        activeBorderColor: '{content.border.color}',
        color: '{text.color}',
        hoverColor: '{text.color}',
        activeColor: '{text.color}',
        focusRing: {
          color: '{focus.ring.color}',
          shadow: 'var(--app-field-shadow)',
        },
      },
      success: {
        background: '#166534',
        hoverBackground: '#14532d',
        activeBackground: '#14532d',
        borderColor: '#166534',
        hoverBorderColor: '#14532d',
        activeBorderColor: '#14532d',
        color: '#ffffff',
        hoverColor: '#ffffff',
        activeColor: '#ffffff',
        focusRing: {
          color: '#16a34a',
          shadow: 'var(--app-field-shadow)',
        },
      },
      warn: {
        background: '#d97706',
        hoverBackground: '#b45309',
        activeBackground: '#92400e',
        borderColor: '#d97706',
        hoverBorderColor: '#b45309',
        activeBorderColor: '#92400e',
        color: '#ffffff',
        hoverColor: '#ffffff',
        activeColor: '#ffffff',
        focusRing: {
          color: '#f59e0b',
          shadow: 'var(--app-field-shadow)',
        },
      },
      danger: {
        background: '#b91c1c',
        hoverBackground: '#991b1b',
        activeBackground: '#7f1d1d',
        borderColor: '#b91c1c',
        hoverBorderColor: '#991b1b',
        activeBorderColor: '#7f1d1d',
        color: '#ffffff',
        hoverColor: '#ffffff',
        activeColor: '#ffffff',
        focusRing: {
          color: '#ef4444',
          shadow: 'var(--app-field-shadow)',
        },
      },
    },
    outlined: {
      primary: {
        hoverBackground: '{content.hover.background}',
        activeBackground: 'var(--app-surface-strong)',
        borderColor: '{content.border.color}',
        color: '{text.color}',
      },
      secondary: {
        hoverBackground: '{content.hover.background}',
        activeBackground: 'var(--app-surface-strong)',
        borderColor: '{content.border.color}',
        color: '{text.color}',
      },
      success: {
        hoverBackground: 'rgba(22, 101, 52, 0.08)',
        activeBackground: 'rgba(22, 101, 52, 0.12)',
        borderColor: '#166534',
        color: '#166534',
      },
      warn: {
        hoverBackground: 'rgba(217, 119, 6, 0.08)',
        activeBackground: 'rgba(217, 119, 6, 0.12)',
        borderColor: '#d97706',
        color: '#d97706',
      },
      danger: {
        hoverBackground: 'rgba(185, 28, 28, 0.08)',
        activeBackground: 'rgba(185, 28, 28, 0.12)',
        borderColor: '#b91c1c',
        color: '#b91c1c',
      },
      plain: {
        hoverBackground: '{content.hover.background}',
        activeBackground: 'var(--app-surface-strong)',
        borderColor: '{content.border.color}',
        color: '{text.color}',
      },
    },
    text: {
      primary: {
        hoverBackground: '{content.hover.background}',
        activeBackground: 'var(--app-surface-strong)',
        color: '{text.color}',
      },
      secondary: {
        hoverBackground: '{content.hover.background}',
        activeBackground: 'var(--app-surface-strong)',
        color: '{text.color}',
      },
      success: {
        hoverBackground: 'rgba(22, 101, 52, 0.08)',
        activeBackground: 'rgba(22, 101, 52, 0.12)',
        color: '#166534',
      },
      warn: {
        hoverBackground: 'rgba(217, 119, 6, 0.08)',
        activeBackground: 'rgba(217, 119, 6, 0.12)',
        color: '#d97706',
      },
      danger: {
        hoverBackground: 'rgba(185, 28, 28, 0.08)',
        activeBackground: 'rgba(185, 28, 28, 0.12)',
        color: '#b91c1c',
      },
      plain: {
        hoverBackground: '{content.hover.background}',
        activeBackground: 'var(--app-surface-strong)',
        color: '{text.color}',
      },
    },
    link: {
      color: '{primary.color}',
      hoverColor: '{primary.hover.color}',
      activeColor: '{primary.active.color}',
    },
  },
  card: {
    root: {
      background: '{content.background}',
      borderRadius: '{border.radius.lg}',
      color: '{text.color}',
      shadow: 'var(--app-shadow-xs)',
    },
    body: {
      padding: '1.35rem 1.5rem',
      gap: '0.875rem',
    },
    caption: {
      gap: '0.25rem',
    },
    title: {
      fontSize: '1.125rem',
      fontWeight: '700',
    },
    subtitle: {
      color: 'var(--app-text-muted)',
    },
  },
  checkbox: {
    root: {
      borderRadius: '{border.radius.xs}',
      width: '1.25rem',
      height: '1.25rem',
      background: '{form.field.background}',
      checkedBackground: '{primary.color}',
      checkedHoverBackground: '{primary.hover.color}',
      disabledBackground: '{form.field.disabled.background}',
      filledBackground: '{form.field.filled.background}',
      borderColor: '{form.field.border.color}',
      hoverBorderColor: '{form.field.hover.border.color}',
      focusBorderColor: '{form.field.focus.border.color}',
      checkedBorderColor: '{primary.color}',
      checkedHoverBorderColor: '{primary.hover.color}',
      checkedFocusBorderColor: '{primary.color}',
      checkedDisabledBorderColor: '{form.field.border.color}',
      invalidBorderColor: '{form.field.invalid.border.color}',
      shadow: 'var(--app-shadow-xs)',
      focusRing: {
        width: '0.125rem',
        style: 'solid',
        color: '{form.field.focus.border.color}',
        offset: '0',
        shadow: 'var(--app-field-shadow)',
      },
      transitionDuration: '0.18s',
    },
    icon: {
      size: '0.875rem',
      color: '{form.field.color}',
      checkedColor: '{primary.contrast.color}',
      checkedHoverColor: '{primary.contrast.color}',
      disabledColor: '{form.field.disabled.color}',
    },
  },
  confirmdialog: {
    icon: {
      size: '1.25rem',
      color: '{primary.color}',
    },
    content: {
      gap: '0.75rem',
    },
  },
  datatable: {
    ...APP_TABLE_TOKENS,
    rowToggleButton: {
      hoverBackground: '{content.hover.background}',
      selectedHoverBackground:
        'color-mix(in srgb, var(--p-primary-color) 12%, var(--app-surface-soft))',
      color: 'var(--app-text-muted)',
      hoverColor: '{text.color}',
      selectedHoverColor: '{primary.color}',
      size: '2rem',
      borderRadius: '{border.radius.sm}',
      focusRing: {
        width: '{focus.ring.width}',
        style: '{focus.ring.style}',
        color: '{focus.ring.color}',
        offset: '{focus.ring.offset}',
        shadow: '{focus.ring.shadow}',
      },
    },
  },
  dialog: {
    root: {
      background: 'var(--app-overlay-bg)',
      borderColor: '{content.border.color}',
      color: '{text.color}',
      borderRadius: '{border.radius.lg}',
      shadow: 'var(--app-shadow-md)',
    },
    header: {
      padding: '1.2rem 1.35rem 0.8rem',
      gap: '0.75rem',
    },
    title: {
      fontSize: '1.125rem',
      fontWeight: '700',
    },
    content: {
      padding: '1.2rem 1.35rem',
    },
    footer: {
      padding: '0.85rem 1.35rem 1.35rem',
      gap: '0.75rem',
    },
  },
  inputnumber: {
    root: {
      transitionDuration: '0.18s',
    },
    button: {
      width: '2.5rem',
      borderRadius: '{border.radius.sm}',
      verticalPadding: '0',
      background: '{content.background}',
      hoverBackground: '{content.hover.background}',
      activeBackground: 'var(--app-surface-strong)',
      borderColor: '{content.border.color}',
      hoverBorderColor: '{content.border.color}',
      activeBorderColor: '{content.border.color}',
      color: '{text.color}',
      hoverColor: '{text.color}',
      activeColor: '{text.color}',
    },
  },
  inputtext: {
    root: APP_FORM_CONTROL_ROOT,
  },
  message: {
    root: {
      borderRadius: '{border.radius.lg}',
      borderWidth: '0',
      transitionDuration: '0.18s',
    },
    content: {
      padding: '0.875rem 1rem',
      gap: '0.55rem',
    },
    text: {
      fontSize: '0.875rem',
      fontWeight: '500',
    },
    icon: {
      size: '1rem',
    },
    closeButton: {
      width: '2rem',
      height: '2rem',
      borderRadius: '{border.radius.sm}',
      focusRing: {
        width: '0.125rem',
        style: 'solid',
        offset: '0',
      },
    },
    closeIcon: {
      size: '0.875rem',
    },
    info: {
      background: '#eff6ff',
      borderColor: '#bfdbfe',
      color: '#1d4ed8',
      shadow: 'none',
    },
    success: {
      background: '#f0fdf4',
      borderColor: '#bbf7d0',
      color: '#166534',
      shadow: 'none',
    },
    warn: {
      background: '#fffbeb',
      borderColor: '#fde68a',
      color: '#b45309',
      shadow: 'none',
    },
    error: {
      background: '#fef2f2',
      borderColor: '#fecaca',
      color: '#b91c1c',
      shadow: 'none',
    },
    secondary: {
      background: 'var(--app-surface-muted)',
      borderColor: '{content.border.color}',
      color: '{text.color}',
      shadow: 'none',
    },
  },
  multiselect: {
    root: APP_FORM_CONTROL_ROOT,
    dropdown: {
      width: '2.5rem',
      color: 'var(--app-text-muted)',
    },
    overlay: APP_SELECT_OVERLAY,
    list: APP_SELECT_LIST,
    option: {
      ...APP_SELECT_OPTION,
      gap: '0.5rem',
    },
    optionGroup: APP_SELECT_OPTION_GROUP,
    clearIcon: {
      color: 'var(--app-text-muted)',
    },
    chip: {
      borderRadius: '{border.radius.sm}',
    },
    emptyMessage: {
      padding: '0.75rem 0.875rem',
    },
  },
  paginator: {
    root: {
      padding: '0.85rem 1rem 1rem',
      gap: '0.25rem',
      borderRadius: '0',
      background: 'color-mix(in srgb, var(--p-content-background) 86%, var(--app-surface-soft))',
      color: '{text.color}',
      transitionDuration: '0.18s',
    },
    navButton: {
      background: 'transparent',
      hoverBackground: '{content.hover.background}',
      selectedBackground: 'color-mix(in srgb, var(--p-primary-color) 14%, transparent)',
      color: 'var(--app-text-muted)',
      hoverColor: '{text.color}',
      selectedColor: '{primary.color}',
      width: '2.25rem',
      height: '2.25rem',
      borderRadius: '{border.radius.sm}',
      focusRing: {
        width: '{focus.ring.width}',
        style: '{focus.ring.style}',
        color: '{focus.ring.color}',
        offset: '{focus.ring.offset}',
        shadow: '{focus.ring.shadow}',
      },
    },
    currentPageReport: {
      color: 'var(--app-text-muted)',
    },
    jumpToPageInput: {
      maxWidth: '3rem',
    },
  },
  password: {
    meter: {
      background: 'var(--app-surface-strong)',
      borderRadius: '{border.radius.sm}',
      height: '0.375rem',
    },
    icon: {
      color: 'var(--app-text-muted)',
    },
    overlay: {
      background: 'var(--app-overlay-bg)',
      borderColor: '{content.border.color}',
      borderRadius: '{border.radius.md}',
      color: '{text.color}',
      padding: '0.875rem',
      shadow: 'var(--app-shadow-sm)',
    },
    content: {
      gap: '0.75rem',
    },
    strength: {
      weakBackground: '#ef4444',
      mediumBackground: '#f59e0b',
      strongBackground: '#10b981',
    },
  },
  select: {
    root: APP_FORM_CONTROL_ROOT,
    dropdown: {
      width: '2.5rem',
      color: 'var(--app-text-muted)',
    },
    overlay: APP_SELECT_OVERLAY,
    list: APP_SELECT_LIST,
    option: APP_SELECT_OPTION,
    optionGroup: APP_SELECT_OPTION_GROUP,
    clearIcon: {
      color: 'var(--app-text-muted)',
    },
    checkmark: {
      color: '{primary.color}',
      gutterStart: '0.5rem',
      gutterEnd: '0',
    },
    emptyMessage: {
      padding: '0.75rem 0.875rem',
    },
  },
  selectbutton: {
    root: {
      borderRadius: '{border.radius.sm}',
    },
  },
  tabs: {
    root: {
      transitionDuration: '0.18s',
    },
    tablist: {
      borderWidth: '0 0 1px 0',
      background: 'transparent',
      borderColor: '{content.border.color}',
    },
    tab: {
      background: 'transparent',
      hoverBackground: '{content.hover.background}',
      activeBackground: '{content.background}',
      borderWidth: '1px',
      borderColor: 'transparent',
      hoverBorderColor: 'transparent',
      activeBorderColor: '{content.border.color}',
      color: 'var(--app-text-muted)',
      hoverColor: '{text.color}',
      activeColor: '{primary.color}',
      padding: '0.875rem 1rem',
      fontWeight: '600',
      margin: '0 0 -1px 0',
      gap: '0.5rem',
      focusRing: {
        width: '{focus.ring.width}',
        style: '{focus.ring.style}',
        color: '{focus.ring.color}',
        offset: '{focus.ring.offset}',
        shadow: '{focus.ring.shadow}',
      },
    },
    tabpanel: {
      background: 'transparent',
      color: '{text.color}',
      padding: '1rem 0 0',
      focusRing: {
        width: '0',
        style: 'none',
        color: 'transparent',
        offset: '0',
        shadow: 'none',
      },
    },
    navButton: {
      background: 'transparent',
      color: 'var(--app-text-muted)',
      hoverColor: '{text.color}',
      width: '2rem',
      focusRing: {
        width: '{focus.ring.width}',
        style: '{focus.ring.style}',
        color: '{focus.ring.color}',
        offset: '{focus.ring.offset}',
        shadow: '{focus.ring.shadow}',
      },
      shadow: 'none',
    },
    activeBar: {
      height: '0',
      bottom: '0',
      background: 'transparent',
    },
  },
  tag: {
    root: {
      fontSize: '0.75rem',
      fontWeight: '600',
      padding: '0.25rem 0.5rem',
      gap: '0.25rem',
      borderRadius: '{border.radius.sm}',
      roundedBorderRadius: '9999px',
    },
    icon: {
      size: '0.75rem',
    },
    primary: {
      background: 'color-mix(in srgb, var(--p-primary-color) 12%, var(--app-surface-soft))',
      color: '{primary.color}',
    },
    secondary: {
      background: 'var(--app-surface-muted)',
      color: '{text.color}',
    },
    success: {
      background: '#dcfce7',
      color: '#166534',
    },
    info: {
      background: '#dbeafe',
      color: '#1d4ed8',
    },
    warn: {
      background: '#fef3c7',
      color: '#b45309',
    },
    danger: {
      background: '#fee2e2',
      color: '#b91c1c',
    },
    contrast: {
      background: '{text.color}',
      color: '{content.background}',
    },
  },
  textarea: {
    root: APP_FORM_CONTROL_ROOT,
  },
  toast: {
    root: {
      width: '24rem',
      borderRadius: '{border.radius.md}',
      borderWidth: '1px',
      transitionDuration: '0.18s',
      blur: '0',
    },
    icon: {
      size: '1rem',
    },
    content: {
      padding: '1rem',
      gap: '0.8rem',
    },
    text: {
      gap: '0.3rem',
    },
    summary: {
      fontWeight: '700',
      fontSize: '0.9375rem',
    },
    detail: {
      fontWeight: '500',
      fontSize: '0.875rem',
    },
    closeButton: {
      width: '2rem',
      height: '2rem',
      borderRadius: '{border.radius.sm}',
      focusRing: {
        width: '0.125rem',
        style: 'solid',
        offset: '0',
      },
    },
    closeIcon: {
      size: '0.875rem',
    },
    info: {
      background: '{content.background}',
      borderColor: '#bfdbfe',
      color: '#1d4ed8',
      detailColor: 'var(--app-text-muted)',
      shadow: 'var(--app-shadow-sm)',
    },
    success: {
      background: '{content.background}',
      borderColor: '#bbf7d0',
      color: '#166534',
      detailColor: 'var(--app-text-muted)',
      shadow: 'var(--app-shadow-sm)',
    },
    warn: {
      background: '{content.background}',
      borderColor: '#fde68a',
      color: '#b45309',
      detailColor: 'var(--app-text-muted)',
      shadow: 'var(--app-shadow-sm)',
    },
    error: {
      background: '{content.background}',
      borderColor: '#fecaca',
      color: '#b91c1c',
      detailColor: 'var(--app-text-muted)',
      shadow: 'var(--app-shadow-sm)',
    },
    secondary: {
      background: '{content.background}',
      borderColor: '{content.border.color}',
      color: '{text.color}',
      detailColor: 'var(--app-text-muted)',
      shadow: 'var(--app-shadow-sm)',
    },
  },
  treetable: {
    ...APP_TABLE_TOKENS,
    bodyCell: {
      ...APP_TABLE_TOKENS.bodyCell,
      gap: '0.5rem',
    },
    nodeToggleButton: {
      hoverBackground: '{content.hover.background}',
      selectedHoverBackground:
        'color-mix(in srgb, var(--p-primary-color) 12%, var(--app-surface-soft))',
      color: 'var(--app-text-muted)',
      hoverColor: '{text.color}',
      selectedHoverColor: '{primary.color}',
      size: '2rem',
      borderRadius: '{border.radius.sm}',
      focusRing: {
        width: '{focus.ring.width}',
        style: '{focus.ring.style}',
        color: '{focus.ring.color}',
        offset: '{focus.ring.offset}',
        shadow: '{focus.ring.shadow}',
      },
    },
  },
};

export const AppThemePreset = definePreset(Aura, {
  primitive: APP_PRIMITIVE_TOKENS,
  semantic: {
    primary: {
      50: '{teal.50}',
      100: '{teal.100}',
      200: '{teal.200}',
      300: '{teal.300}',
      400: '{teal.400}',
      500: '{teal.500}',
      600: '{teal.600}',
      700: '{teal.700}',
      800: '{teal.800}',
      900: '{teal.900}',
      950: '{teal.950}',
    },
    focusRing: {
      width: '0.125rem',
      style: 'solid',
      color: '{primary.300}',
      offset: '0.125rem',
      shadow: 'none',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '{slate.50}',
          100: '{slate.100}',
          200: '{slate.200}',
          300: '{slate.300}',
          400: '{slate.400}',
          500: '{slate.500}',
          600: '{slate.600}',
          700: '{slate.700}',
          800: '{slate.800}',
          900: '{slate.900}',
          950: '{slate.950}',
        },
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
        content: {
          background: '{surface.0}',
          hoverBackground: '{primary.50}',
          borderColor: '{surface.200}',
          color: '{text.color}',
          hoverColor: '{text.hover.color}',
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
        content: {
          background: '{surface.900}',
          hoverBackground: '{surface.800}',
          borderColor: '{surface.700}',
          color: '{text.color}',
          hoverColor: '{text.hover.color}',
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
  components: APP_COMPONENT_TOKENS,
});
