import { Component, forwardRef, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

// 34x20 switch per the handoff; works with both ngModel and reactive forms.
@Component({
  selector: 'pt-switch',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => Switch), multi: true },
  ],
  templateUrl: './switch.html',
  styleUrl: './switch.scss',
})
export class Switch implements ControlValueAccessor {
  protected readonly checked = signal(false);
  protected readonly disabled = signal(false);

  private onChange: (value: boolean) => void = () => {};
  private onTouched: () => void = () => {};

  protected toggle(): void {
    this.checked.update((value) => !value);
    this.onChange(this.checked());
    this.onTouched();
  }

  writeValue(value: unknown): void {
    this.checked.set(value === true);
  }

  registerOnChange(fn: (value: boolean) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }
}
