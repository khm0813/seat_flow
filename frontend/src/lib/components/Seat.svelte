<script lang="ts">
  import { SeatStatus, type Seat } from '$lib/types';
  import { selectedSeats, seatStore } from '$lib/stores/seatStore';

  export let seat: Seat;
  export let isSelected: boolean = false;

  $: statusClass = getSeatStatusClass(seat.status);
  $: isClickable = seat.status === SeatStatus.AVAILABLE;

  function getSeatStatusClass(status: SeatStatus): string {
    switch (status) {
      case SeatStatus.AVAILABLE:
        return 'seat-available';
      case SeatStatus.HOLD:
        return 'seat-hold';
      case SeatStatus.CONFIRMED:
        return 'seat-confirmed';
      default:
        return 'seat-available';
    }
  }

  function handleSeatClick() {
    if (isClickable) {
      seatStore.toggleSeatSelection(seat.seatId);
      seatStore.addEventLog(`Seat ${seat.seatId} ${isSelected ? 'deselected' : 'selected'}`);
    }
  }

  function getHoldTimeRemaining(): string {
    if (seat.status !== SeatStatus.HOLD || !seat.holdExpiresAt) return '';

    const expiresAt = new Date(seat.holdExpiresAt);
    const now = new Date();
    const timeRemaining = Math.max(0, Math.floor((expiresAt.getTime() - now.getTime()) / 1000));

    if (timeRemaining === 0) return 'Expired';

    const minutes = Math.floor(timeRemaining / 60);
    const seconds = timeRemaining % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  // Update hold countdown every second
  let holdCountdown = '';
  $: if (seat.status === SeatStatus.HOLD) {
    const interval = setInterval(() => {
      holdCountdown = getHoldTimeRemaining();
      if (holdCountdown === 'Expired') {
        clearInterval(interval);
      }
    }, 1000);
  }
</script>

<button
  class="seat {statusClass} {isSelected ? 'selected' : ''}"
  class:clickable={isClickable}
  disabled={!isClickable}
  on:click={handleSeatClick}
  title="{seat.seatId} - ${seat.price} - {seat.status}"
>
  <span class="seat-id">{seat.seatId}</span>
  <span class="seat-price">${seat.price}</span>
  {#if seat.status === SeatStatus.HOLD && holdCountdown}
    <span class="hold-countdown">{holdCountdown}</span>
  {/if}
</button>

<style>
  .seat {
    width: 50px;
    height: 50px;
    margin: 2px;
    border: 2px solid #ddd;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: bold;
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
  }

  .seat:disabled {
    cursor: not-allowed;
  }

  .seat-available {
    background-color: #e8f5e8;
    border-color: #4caf50;
    color: #2e7d32;
  }

  .seat-available:hover {
    background-color: #c8e6c9;
    transform: scale(1.05);
  }

  .seat-hold {
    background-color: #fff3e0;
    border-color: #ff9800;
    color: #f57c00;
    animation: pulse 2s infinite;
  }

  .seat-confirmed {
    background-color: #ffebee;
    border-color: #f44336;
    color: #c62828;
  }

  .seat.selected {
    background-color: #e3f2fd !important;
    border-color: #2196f3 !important;
    color: #1565c0 !important;
    transform: scale(1.1);
    box-shadow: 0 4px 8px rgba(33, 150, 243, 0.3);
  }

  .seat-id {
    font-size: 12px;
    line-height: 1;
  }

  .seat-price {
    font-size: 8px;
    line-height: 1;
  }

  .hold-countdown {
    position: absolute;
    top: -8px;
    right: -8px;
    background-color: #ff9800;
    color: white;
    font-size: 8px;
    padding: 2px 4px;
    border-radius: 4px;
    min-width: 20px;
    text-align: center;
  }

  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.7;
    }
  }

  .clickable {
    cursor: pointer;
  }

  .clickable:hover {
    transform: scale(1.05);
  }
</style>