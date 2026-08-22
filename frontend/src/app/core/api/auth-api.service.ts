import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LoginRequest, RegisterRequest, UserResponse } from './models';

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/auth';

  register(request: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/register`, request);
  }

  login(request: LoginRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/login`, request);
  }

  refresh(): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/refresh`, null);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.base}/logout`, null);
  }

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.base}/me`);
  }
}
