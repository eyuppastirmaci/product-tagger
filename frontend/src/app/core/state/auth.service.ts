import { Injectable, signal } from '@angular/core';

export interface CurrentUser {
  name: string;
  email: string;
  initials: string;
}

// Placeholder until real authentication exists; the shell only displays it.
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal<CurrentUser>({
    name: 'Admin',
    email: 'admin@tagger.local',
    initials: 'AD',
  });
}
