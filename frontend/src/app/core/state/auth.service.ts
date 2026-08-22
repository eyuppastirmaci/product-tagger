import { Injectable, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, map } from 'rxjs';
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

  /** Resolves the session cookie into a user before the first render. */
  async init(): Promise<void> {
    try {
      const me = await firstValueFrom(this.api.me());

      this.user.set(this.toCurrentUser(me));
    } catch {
      this.user.set(null);
    }
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
