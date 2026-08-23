import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { vi } from 'vitest';
import { AuthApi } from '../api/auth-api.service';
import { UserResponse } from '../api/models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  function setup(refresh: () => Subject<UserResponse>) {
    const api = { refresh: vi.fn(refresh) };

    TestBed.configureTestingModule({
      providers: [{ provide: AuthApi, useValue: api }],
    });

    return { api, service: TestBed.inject(AuthService) };
  }

  it('single-flights concurrent refresh calls', () => {
    const refresh$ = new Subject<UserResponse>();
    const { api, service } = setup(() => refresh$);

    const results: unknown[] = [];

    // Two 401s land at the same time: both must share one rotation request,
    // otherwise the second one voids the first one's fresh token server-side
    service.refreshOnce().subscribe((user) => results.push(user));
    service.refreshOnce().subscribe((user) => results.push(user));

    expect(api.refresh).toHaveBeenCalledTimes(1);

    refresh$.next({ id: 1, name: 'Test User', email: 'user@test.local' } as UserResponse);
    refresh$.complete();

    expect(results).toHaveLength(2);
    expect(service.user()?.email).toBe('user@test.local');

    // A later expiry starts a fresh rotation instead of replaying the old one
    service.refreshOnce().subscribe();

    expect(api.refresh).toHaveBeenCalledTimes(2);
  });

  it('clears the in-flight slot after a failed refresh', () => {
    let current$ = new Subject<UserResponse>();
    const { api, service } = setup(() => current$);

    service.refreshOnce().subscribe({ error: () => {} });
    current$.error(new Error('401'));

    current$ = new Subject<UserResponse>();

    service.refreshOnce().subscribe({ error: () => {} });

    expect(api.refresh).toHaveBeenCalledTimes(2);
  });
});
