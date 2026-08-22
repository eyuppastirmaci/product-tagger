import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/** Styled textarea; empty text maps to null. */
@Component({
  selector: 'pt-textarea',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => Textarea), multi: true },
  ],
  templateUrl: './textarea.html',
  styleUrl: './textarea.scss',
})
export class Textarea implements ControlValueAccessor {
  readonly rows = input(3);
  readonly placeholder = input<string>('');

  protected readonly value = signal<string>('');
  protected readonly disabled = signal(false);

  private onChange: (value: string | null) => void = () => {};
  private onTouched: () => void = () => {};

  protected onInput(event: Event): void {
    const text = (event.target as HTMLTextAreaElement).value;

    this.value.set(text);
    this.onChange(text === '' ? null : text);
  }

  protected onBlur(): void {
    this.onTouched();
  }

  writeValue(value: unknown): void {
    this.value.set(typeof value === 'string' ? value : '');
  }

  registerOnChange(fn: (value: string | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }
}
