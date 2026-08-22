import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/** Styled text input; empty text maps to null. */
@Component({
  selector: 'pt-text-input',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => TextInput), multi: true },
  ],
  templateUrl: './text-input.html',
  styleUrl: './text-input.scss',
})
export class TextInput implements ControlValueAccessor {
  readonly type = input<'text' | 'email' | 'password'>('text');
  readonly placeholder = input<string>('');

  protected readonly value = signal<string>('');
  protected readonly disabled = signal(false);

  private onChange: (value: string | null) => void = () => {};
  private onTouched: () => void = () => {};

  protected onInput(event: Event): void {
    const text = (event.target as HTMLInputElement).value;

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
