import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../state/auth.service';

// A 401 anywhere outside the auth endpoints means the session expired:
// clear the user and fall back to the login screen.
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !request.url.includes('/api/auth/')) {
        auth.clear();
        router.navigate(['/login']);
      }

      return throwError(() => error);
    }),
  );
};
