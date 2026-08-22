import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../state/auth.service';

// A 401 outside the auth endpoints means the access token expired: try one
// silent refresh and replay the request; only a failed refresh logs out.
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || request.url.includes('/api/auth/')) {
        return throwError(() => error);
      }

      return auth.refreshOnce().pipe(
        switchMap(() => next(request)),
        catchError((refreshError) => {
          auth.clear();
          router.navigate(['/login']);

          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
