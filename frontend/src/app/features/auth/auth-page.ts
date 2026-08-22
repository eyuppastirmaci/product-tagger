import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoPipe } from '@jsverse/transloco';
import { map } from 'rxjs';
import { AuthService, CurrentUser } from '../../core/state/auth.service';
import { LanguageToggle } from '../../core/layout/language-toggle';
import { ThemeToggle } from '../../core/layout/theme-toggle';
import { TextInput } from '../../shared/ui/text-input';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-auth-page',
  imports: [FormsModule, RouterLink, TranslocoPipe, LanguageToggle, ThemeToggle, TextInput],
  templateUrl: './auth-page.html',
  styleUrl: './auth-page.scss',
})
export class AuthPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  protected readonly mode = toSignal(
    this.route.data.pipe(map((data) => data['mode'] as 'login' | 'register')),
    { initialValue: 'login' as const },
  );

  protected readonly isRegister = computed(() => this.mode() === 'register');

  protected readonly name = signal('');
  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly error = signal<string | null>(null);
  protected readonly submitting = signal(false);

  constructor() {
    // An already signed-in user has no business on the auth screen
    if (this.auth.user() !== null) {
      this.router.navigateByUrl('/');
    }
  }

  protected submit(): void {
    if (this.submitting() || !this.email() || !this.password()) {
      return;
    }

    const request: Observable<CurrentUser> = this.isRegister()
      ? this.auth.register(this.name(), this.email(), this.password())
      : this.auth.login(this.email(), this.password());

    this.error.set(null);
    this.submitting.set(true);

    request.subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: (response: HttpErrorResponse) => {
        this.submitting.set(false);
        this.error.set(this.errorKeyFor(response.status));
      },
    });
  }

  private errorKeyFor(status: number): string {
    switch (status) {
      case 401:
        return 'auth.invalidCredentials';
      case 409:
        return 'auth.emailTaken';
      case 429:
        return 'auth.rateLimited';
      default:
        return 'auth.genericError';
    }
  }
}
