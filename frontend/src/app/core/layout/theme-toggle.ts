import { Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideMoon, LucideSun } from '@lucide/angular';
import { ThemeService } from '../theme/theme.service';

@Component({
  selector: 'app-theme-toggle',
  imports: [TranslocoPipe, LucideSun, LucideMoon],
  templateUrl: './theme-toggle.html',
  styleUrl: './theme-toggle.scss',
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);
}
