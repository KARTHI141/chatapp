import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '@env/environment';
import { ApiResponse } from '../models/api-response.model';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = environment.apiUrl;
  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(this.getStoredUser());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  register(request: RegisterRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.API}/auth/register`, request);
  }

  verifyOtp(email: string, otp: string): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.API}/auth/verify-otp`, { email, otp })
      .pipe(tap((res: ApiResponse<AuthResponse>) => this.handleAuthResponse(res.data)));
  }

  resendOtp(email: string): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.API}/auth/resend-otp?email=${encodeURIComponent(email)}`, {});
  }

  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${this.API}/auth/login`, request)
      .pipe(tap((res: ApiResponse<AuthResponse>) => this.handleAuthResponse(res.data)));
  }

  logout(): void {
    const user = this.getStoredUser();
    if (user?.accessToken) {
      // Fire-and-forget: notify backend to blacklist tokens
      this.http.post(`${this.API}/auth/logout`, {
        refreshToken: user.refreshToken
      }, {
        headers: { Authorization: `Bearer ${user.accessToken}` }
      }).subscribe({
        error: () => {} // Ignore errors — we're logging out regardless
      });
    }
    localStorage.removeItem('auth');
    this.currentUserSubject.next(null);
  }

  refreshToken(): Observable<ApiResponse<AuthResponse>> {
    const user = this.getStoredUser();
    return this.http.post<ApiResponse<AuthResponse>>(`${this.API}/auth/refresh`, {
      refreshToken: user?.refreshToken
    }).pipe(tap((res: ApiResponse<AuthResponse>) => this.handleAuthResponse(res.data)));
  }

  getToken(): string | null {
    const user = this.getStoredUser();
    return user?.accessToken ?? null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  private handleAuthResponse(auth: AuthResponse): void {
    localStorage.setItem('auth', JSON.stringify(auth));
    this.currentUserSubject.next(auth);
  }

  private getStoredUser(): AuthResponse | null {
    const stored = localStorage.getItem('auth');
    if (!stored) return null;
    try {
      return JSON.parse(stored);
    } catch {
      return null;
    }
  }
}
