import type { ApiResponse, ShowSeatsResponse } from '$lib/types';

const API_BASE = 'http://localhost:8080/api';

class ApiClient {
  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<ApiResponse<T>> {
    try {
      const response = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...options.headers,
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error('API request failed:', error);
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error occurred'
      };
    }
  }

  async getShowSeats(showId: number): Promise<ApiResponse<ShowSeatsResponse>> {
    return this.request<ShowSeatsResponse>(`/shows/${showId}/seats`);
  }

  async holdSeat(showId: number, seatId: string, userId: string, idempotencyKey: string): Promise<ApiResponse<any>> {
    return this.request<any>('/reservations/hold', {
      method: 'POST',
      headers: {
        'Idempotency-Key': idempotencyKey
      },
      body: JSON.stringify({
        showId,
        seatId,
        userId
      })
    });
  }

  async confirmReservation(reservationId: number, idempotencyKey: string): Promise<ApiResponse<any>> {
    return this.request<any>(`/reservations/${reservationId}/confirm`, {
      method: 'POST',
      headers: {
        'Idempotency-Key': idempotencyKey
      }
    });
  }
}

export const apiClient = new ApiClient();