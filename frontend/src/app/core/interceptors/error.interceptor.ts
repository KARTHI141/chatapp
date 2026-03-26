import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Intercepts 401/403 responses and redirects to login.
 * In a production app, you could add automatic token refresh here
 * using the refresh token before giving up and redirecting.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        // Don't redirect if we're already on an auth page
        if (!req.url.includes('/auth/')) {
          authService.logout();
          router.navigate(['/auth/login']);
        }
      }
      return throwError(() => error);
    })
  );
};
