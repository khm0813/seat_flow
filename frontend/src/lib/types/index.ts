export interface Seat {
  seatId: string;
  rowName: string;
  seatNumber: number;
  status: SeatStatus;
  price: number;
  holdExpiresAt?: string;
}

export interface ShowSeatsResponse {
  showId: number;
  showTitle: string;
  venue: string;
  showDate: string;
  seats: Seat[];
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
}

export enum SeatStatus {
  AVAILABLE = 'AVAILABLE',
  HOLD = 'HOLD',
  CONFIRMED = 'CONFIRMED'
}

export interface SeatStatusMessage {
  seatId: string;
  status: SeatStatus;
  userId: string;
  holdExpiresAt?: string;
}