import { Pipe, PipeTransform } from '@angular/core';

// "4.2 MB" from a byte count; locale only affects the decimal separator.
@Pipe({ name: 'fileSize' })
export class FileSizePipe implements PipeTransform {
  transform(bytes: number | null | undefined, locale: string): string {
    if (bytes == null || bytes < 0) {
      return '';
    }

    const units = ['B', 'KB', 'MB', 'GB'];
    let value = bytes;
    let unitIndex = 0;

    while (value >= 1024 && unitIndex < units.length - 1) {
      value /= 1024;
      unitIndex += 1;
    }

    const formatted = new Intl.NumberFormat(locale, {
      maximumFractionDigits: value >= 100 || unitIndex === 0 ? 0 : 1,
    }).format(value);

    return `${formatted} ${units[unitIndex]}`;
  }
}
