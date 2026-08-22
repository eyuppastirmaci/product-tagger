import { Pipe, PipeTransform } from '@angular/core';

const UNITS: Array<{ unit: Intl.RelativeTimeFormatUnit; seconds: number }> = [
  { unit: 'year', seconds: 31_536_000 },
  { unit: 'month', seconds: 2_592_000 },
  { unit: 'week', seconds: 604_800 },
  { unit: 'day', seconds: 86_400 },
  { unit: 'hour', seconds: 3_600 },
  { unit: 'minute', seconds: 60 },
];

const ABSOLUTE_CUTOFF_SECONDS = 7 * 86_400;

// e.g. "3 minutes ago" in the active locale; anything under a minute reads as
// "now", anything older than 7 days falls back to an absolute date.
@Pipe({ name: 'relativeTime' })
export class RelativeTimePipe implements PipeTransform {
  transform(value: string | Date | null | undefined, locale: string): string {
    if (!value) {
      return '';
    }

    const elapsedSeconds = (Date.now() - new Date(value).getTime()) / 1000;

    if (elapsedSeconds >= ABSOLUTE_CUTOFF_SECONDS) {
      return new Intl.DateTimeFormat(locale, {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
      }).format(new Date(value));
    }

    const formatter = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });

    for (const { unit, seconds } of UNITS) {
      if (Math.abs(elapsedSeconds) >= seconds) {
        return formatter.format(-Math.round(elapsedSeconds / seconds), unit);
      }
    }

    return formatter.format(0, 'second');
  }
}
