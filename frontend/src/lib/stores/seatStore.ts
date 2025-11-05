import { writable, derived } from 'svelte/store';
import type { Seat, ShowSeatsResponse, SeatStatus } from '$lib/types';

interface SeatStoreState {
  showData: ShowSeatsResponse | null;
  seats: Seat[];
  selectedSeats: string[];
  loading: boolean;
  error: string | null;
  eventLog: string[];
}

const initialState: SeatStoreState = {
  showData: null,
  seats: [],
  selectedSeats: [],
  loading: false,
  error: null,
  eventLog: []
};

function createSeatStore() {
  const { subscribe, set, update } = writable<SeatStoreState>(initialState);

  return {
    subscribe,

    setLoading: (loading: boolean) => update(state => ({ ...state, loading })),

    setError: (error: string | null) => update(state => ({ ...state, error })),

    setShowData: (showData: ShowSeatsResponse) => update(state => ({
      ...state,
      showData,
      seats: showData.seats,
      loading: false,
      error: null
    })),

    updateSeatStatus: (seatId: string, status: SeatStatus, holdExpiresAt?: string) =>
      update(state => ({
        ...state,
        seats: state.seats.map(seat =>
          seat.seatId === seatId
            ? { ...seat, status, holdExpiresAt }
            : seat
        )
      })),

    toggleSeatSelection: (seatId: string) => update(state => {
      const isSelected = state.selectedSeats.includes(seatId);
      return {
        ...state,
        selectedSeats: isSelected
          ? state.selectedSeats.filter(id => id !== seatId)
          : [...state.selectedSeats, seatId]
      };
    }),

    clearSelection: () => update(state => ({ ...state, selectedSeats: [] })),

    addEventLog: (message: string) => update(state => ({
      ...state,
      eventLog: [
        `[${new Date().toLocaleTimeString()}] ${message}`,
        ...state.eventLog.slice(0, 49) // Keep only last 50 events
      ]
    })),

    reset: () => set(initialState)
  };
}

export const seatStore = createSeatStore();

// Derived stores for easier access
export const seats = derived(seatStore, $store => $store.seats);
export const showData = derived(seatStore, $store => $store.showData);
export const selectedSeats = derived(seatStore, $store => $store.selectedSeats);
export const loading = derived(seatStore, $store => $store.loading);
export const error = derived(seatStore, $store => $store.error);
export const eventLog = derived(seatStore, $store => $store.eventLog);

// Helper derived stores
export const seatsByRow = derived(seats, $seats => {
  const rowMap = new Map<string, Seat[]>();
  $seats.forEach(seat => {
    if (!rowMap.has(seat.rowName)) {
      rowMap.set(seat.rowName, []);
    }
    rowMap.get(seat.rowName)!.push(seat);
  });

  // Sort seats within each row by seat number
  for (const [, seats] of rowMap) {
    seats.sort((a, b) => a.seatNumber - b.seatNumber);
  }

  return rowMap;
});