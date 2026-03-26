import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuth = authService.isAuthenticated();
  console.log('AuthGuard check - isAuthenticated:', isAuth);

  if (isAuth) {
    return true;
  }

  router.navigate(['/auth/login']);
  return false;
};
