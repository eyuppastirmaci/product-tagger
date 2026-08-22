import { Pipe, PipeTransform } from '@angular/core';

// e.g. "12 Aug 2026, 14:32" — locale comes in as an argument so the pipe stays pure
// and re-evaluates when the active language signal changes.
@Pipe({ name: 'localeDate' })
export class LocaleDatePipe implements PipeTransform {
  transform(value: string | Date | null | undefined, locale: string): string {
    if (!value) {
      return '';
    }

    return new Intl.DateTimeFormat(locale, {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }
}
