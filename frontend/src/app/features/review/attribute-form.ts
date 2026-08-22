import { Component, HostListener, inject, input, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucidePlus, LucideX } from '@lucide/angular';
import { AttributeValues, Confidences, SchemaAttribute } from '../../core/api/models';
import { LanguageService } from '../../core/i18n/language.service';
import { ConfidenceBadge } from '../../shared/ui/confidence-badge';
import { Select, SelectOption } from '../../shared/ui/select';
import { Switch } from '../../shared/ui/switch';
import { TextInput } from '../../shared/ui/text-input';

const LOW_CONFIDENCE = 0.6;

/**
 * Schema-driven attribute form: one card per definition, control chosen by type.
 * Values flow through the `values` model; validation errors come in from the
 * parent so the form itself stays presentation-only.
 */
@Component({
  selector: 'app-attribute-form',
  imports: [FormsModule, TranslocoPipe, ConfidenceBadge, Select, Switch, TextInput, LucidePlus, LucideX],
  templateUrl: './attribute-form.html',
  styleUrl: './attribute-form.scss',
})
export class AttributeForm {
  protected readonly language = inject(LanguageService);

  readonly definitions = input.required<SchemaAttribute[]>();
  readonly confidences = input<Confidences | null>(null);
  readonly errors = input<Record<string, boolean>>({});
  readonly values = model.required<AttributeValues>();

  protected readonly addOpenFor = signal<string | null>(null);

  protected labelOf(definition: SchemaAttribute): string {
    return this.language.lang() === 'tr' ? definition.label_tr : definition.label_en;
  }

  protected typeTag(definition: SchemaAttribute): string {
    return definition.type === 'enum' && definition.multi ? 'multi-enum' : definition.type;
  }

  protected valueLabel(definition: SchemaAttribute, code: string): string {
    const entry = definition.values?.find((value) => value.value === code);

    if (!entry) {
      return code;
    }

    return this.language.lang() === 'tr' ? entry.label_tr : entry.label_en;
  }

  protected confidenceOf(key: string): number | null {
    return this.confidences()?.[key] ?? null;
  }

  protected isLowConfidence(definition: SchemaAttribute): boolean {
    const confidence = this.confidenceOf(definition.key);

    return confidence != null && confidence < LOW_CONFIDENCE;
  }

  protected hasSuggestion(definition: SchemaAttribute): boolean {
    return this.confidenceOf(definition.key) != null;
  }

  protected singleValue(key: string): string {
    const value = this.values()[key];

    return typeof value === 'string' ? value : '';
  }

  protected multiValues(key: string): string[] {
    const value = this.values()[key];

    return Array.isArray(value) ? (value as string[]) : [];
  }

  protected boolValue(key: string): boolean {
    return this.values()[key] === true;
  }

  protected optionsFor(definition: SchemaAttribute): SelectOption[] {
    return (definition.values ?? []).map((value) => ({
      value: value.value,
      label: this.language.lang() === 'tr' ? value.label_tr : value.label_en,
    }));
  }

  protected onSingleChange(key: string, value: string | null): void {
    this.setValue(key, value);
  }

  protected onBoolChange(key: string, value: boolean): void {
    this.setValue(key, value);
  }

  protected onTextChange(key: string, value: string | null): void {
    this.setValue(key, value);
  }

  protected removeChip(key: string, code: string): void {
    this.setValue(key, this.multiValues(key).filter((value) => value !== code));
  }

  protected addChip(key: string, code: string): void {
    this.setValue(key, [...this.multiValues(key), code]);
    this.addOpenFor.set(null);
  }

  protected remainingValues(definition: SchemaAttribute): string[] {
    const selected = new Set(this.multiValues(definition.key));

    return (definition.values ?? [])
      .map((value) => value.value)
      .filter((code) => !selected.has(code));
  }

  protected toggleAdd(key: string, event: MouseEvent): void {
    event.stopPropagation();
    this.addOpenFor.update((open) => (open === key ? null : key));
  }

  @HostListener('document:click')
  protected closeAdd(): void {
    this.addOpenFor.set(null);
  }

  @HostListener('document:keydown.escape')
  protected closeAddOnEscape(): void {
    this.addOpenFor.set(null);
  }

  private setValue(key: string, value: unknown): void {
    this.values.update((values) => ({ ...values, [key]: value }));
  }
}
