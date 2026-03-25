import { TranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';

import { translationNotFoundMessage } from 'app/config/translation.config';

type MessageSeverity = 'success' | 'info' | 'warn' | 'error';

interface TranslatedMessageConfig {
  severity: MessageSeverity;
  summary: string;
  detail: string;
  summaryParams?: Record<string, unknown>;
  detailParams?: Record<string, unknown>;
}

const TRANSLATION_NOT_FOUND_PREFIX = `${translationNotFoundMessage}[`;

function resolveTranslation(
  translateService: TranslateService,
  keyOrText: string,
  params?: Record<string, unknown>,
): string {
  const translated = translateService.instant(keyOrText, params);

  return translated.startsWith(TRANSLATION_NOT_FOUND_PREFIX) ? keyOrText : translated;
}

export function buildTranslatedMessage(
  translateService: TranslateService,
  config: TranslatedMessageConfig,
): { severity: MessageSeverity; summary: string; detail: string } {
  return {
    severity: config.severity,
    summary: resolveTranslation(translateService, config.summary, config.summaryParams),
    detail: resolveTranslation(translateService, config.detail, config.detailParams),
  };
}

export function addTranslatedMessage(
  messageService: MessageService,
  translateService: TranslateService,
  config: TranslatedMessageConfig,
): void {
  messageService.add(buildTranslatedMessage(translateService, config));
}

export function handleHttpError(
  messageService: MessageService,
  translateService: TranslateService,
  err: unknown,
  fallbackDetail = 'feedback.httpError.unexpected.detail',
  fallbackDetailParams?: Record<string, unknown>,
): void {
  const status =
    typeof err === 'object' && err !== null && 'status' in err
      ? (err as { status?: number }).status
      : undefined;

  if (status === 403) {
    addTranslatedMessage(messageService, translateService, {
      severity: 'warn',
      summary: 'feedback.httpError.accessDenied.summary',
      detail: 'feedback.httpError.accessDenied.detail',
    });
    return;
  }

  addTranslatedMessage(messageService, translateService, {
    severity: 'error',
    summary: 'feedback.httpError.unexpected.summary',
    detail: fallbackDetail,
    detailParams: fallbackDetailParams,
  });
}
