import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { ApiResponse } from '../models/api-response.model';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly API = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getCurrentUser(): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.API}/me`);
  }

  getUserById(id: number): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.API}/${id}`);
  }

  searchUsers(query: string): Observable<ApiResponse<User[]>> {
    return this.http.get<ApiResponse<User[]>>(`${this.API}/search`, {
      params: { q: query }
    });
  }

  getOnlineUsers(): Observable<ApiResponse<number[]>> {
    return this.http.get<ApiResponse<number[]>>(`${this.API}/online`);
  }

  updateProfile(displayName?: string, avatarUrl?: string): Observable<ApiResponse<User>> {
    const params: any = {};
    if (displayName) params.displayName = displayName;
    if (avatarUrl) params.avatarUrl = avatarUrl;
    return this.http.put<ApiResponse<User>>(`${this.API}/me/profile`, null, { params });
  }
}
