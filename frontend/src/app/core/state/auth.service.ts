import { Injectable, inject, signal } from '@angular/core';
import { Observable, finalize, firstValueFrom, map, shareReplay } from 'rxjs';
import { AuthApi } from '../api/auth-api.service';
import { UserResponse } from '../api/models';

export interface CurrentUser {
  id: number;
  name: string;
  email: string;
  initials: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(AuthApi);

  readonly user = signal<CurrentUser | null>(null);

  private refreshInFlight: Observable<CurrentUser> | null = null;

  /**
   * Resolves the session before the first render; an expired access token is
   * silently exchanged via the refresh cookie.
   */
  async init(): Promise<void> {
    try {
      const me = await firstValueFrom(this.api.me());

      this.user.set(this.toCurrentUser(me));
    } catch {
      try {
        await firstValueFrom(this.refreshOnce());
      } catch {
        this.user.set(null);
      }
    }
  }

  /** Single-flight refresh: concurrent 401s share one rotation request. */
  refreshOnce(): Observable<CurrentUser> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.api.refresh().pipe(
        map((user) => this.setUser(user)),
        finalize(() => {
          this.refreshInFlight = null;
        }),
        shareReplay(1),
      );
    }

    return this.refreshInFlight;
  }

  login(email: string, password: string): Observable<CurrentUser> {
    return this.api.login({ email, password }).pipe(map((user) => this.setUser(user)));
  }

  register(name: string, email: string, password: string): Observable<CurrentUser> {
    return this.api.register({ name, email, password }).pipe(map((user) => this.setUser(user)));
  }

  logout(): Observable<void> {
    return this.api.logout().pipe(map(() => this.user.set(null)));
  }

  clear(): void {
    this.user.set(null);
  }

  private setUser(user: UserResponse): CurrentUser {
    const current = this.toCurrentUser(user);

    this.user.set(current);

    return current;
  }

  private toCurrentUser(user: UserResponse): CurrentUser {
    return { ...user, initials: initialsOf(user.name) };
  }
}

function initialsOf(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}
