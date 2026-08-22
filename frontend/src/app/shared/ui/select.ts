import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { LucideChevronDown } from '@lucide/angular';

export interface SelectOption {
  value: string;
  label: string;
}

/**
 * Styled native select; empty selection maps to null. Container overrides via
 * CSS vars: --pt-select-bg, --pt-select-weight.
 */
@Component({
  selector: 'pt-select',
  imports: [LucideChevronDown],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => Select), multi: true },
  ],
  templateUrl: './select.html',
  styleUrl: './select.scss',
})
export class Select implements ControlValueAccessor {
  readonly options = input.required<SelectOption[]>();
  readonly placeholder = input<string | null>(null);

  protected readonly value = signal<string | null>(null);
  protected readonly disabled = signal(false);

  private onChange: (value: string | null) => void = () => {};
  private onTouched: () => void = () => {};

  protected onSelect(event: Event): void {
    const selected = (event.target as HTMLSelectElement).value;

    this.value.set(selected === '' ? null : selected);
    this.onChange(this.value());
    this.onTouched();
  }

  writeValue(value: unknown): void {
    this.value.set(typeof value === 'string' && value !== '' ? value : null);
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
