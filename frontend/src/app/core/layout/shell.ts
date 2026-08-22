import { Component, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideChevronsUpDown, LucideInbox, LucideLayoutGrid, LucideUpload } from '@lucide/angular';
import { filter, map } from 'rxjs';
import { AuthService } from '../state/auth.service';
import { ReviewStore } from '../state/review-store';
import { ThemeService } from '../theme/theme.service';
import { LanguageToggle } from './language-toggle';
import { PageTitleService } from './page-title.service';
import { ThemeToggle } from './theme-toggle';

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    TranslocoPipe,
    LanguageToggle,
    ThemeToggle,
    LucideUpload,
    LucideLayoutGrid,
    LucideInbox,
    LucideChevronsUpDown,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly router = inject(Router);

  protected readonly theme = inject(ThemeService);
  protected readonly auth = inject(AuthService);
  protected readonly reviewStore = inject(ReviewStore);
  protected readonly pageTitleService = inject(PageTitleService);

  protected readonly userMenuOpen = signal(false);

  protected readonly pageTitle = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.deepestTitle()),
    ),
    { initialValue: '' },
  );

  constructor() {
    this.reviewStore.refresh();
  }

  protected toggleUserMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.userMenuOpen.update((open) => !open);
  }

  @HostListener('document:click')
  protected closeUserMenu(): void {
    this.userMenuOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  protected closeOnEscape(): void {
    this.userMenuOpen.set(false);
  }

  protected logout(): void {
    this.auth.logout().subscribe(() => this.router.navigateByUrl('/login'));
  }

  private deepestTitle(): string {
    let route = this.router.routerState.snapshot.root;

    while (route.firstChild) {
      route = route.firstChild;
    }

    return (route.data['title'] as string) ?? '';
  }
}
