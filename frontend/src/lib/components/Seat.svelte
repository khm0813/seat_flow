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
    width: var(--seat-size, 40px);
    height: var(--seat-size, 40px);
    margin: 2px;
    border: 2px solid var(--color-border);
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: 700;
    cursor: pointer;
    transition: transform 0.15s ease, background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
    position: relative;
    background: var(--color-surface-2);
  }

  .seat:disabled {
    cursor: not-allowed;
  }

  .seat-available { background-color: var(--color-success-50); border-color: var(--color-success); color: #166534; }

  .seat-available:hover { background-color: #dcfce7; transform: scale(1.04); }

  .seat-hold { background-color: var(--color-warning-50); border-color: var(--color-warning); color: #b45309; animation: pulse 2s infinite; }

  .seat-confirmed { background-color: var(--color-danger-50); border-color: var(--color-danger); color: #b91c1c; }

  .seat.selected {
    background-color: var(--color-primary-50) !important;
    border-color: var(--color-primary-600) !important;
    color: var(--color-primary-700) !important;
    transform: scale(1.06);
    box-shadow: 0 4px 10px rgba(79, 70, 229, 0.25);
  }

  .seat-id { font-size: clamp(9px, calc(var(--seat-size, 40px) * 0.28), 12px); line-height: 1; }

  .seat-price { font-size: clamp(7px, calc(var(--seat-size, 40px) * 0.2), 9px); line-height: 1; color: var(--color-muted); font-weight: 600; }

  .hold-countdown {
    position: absolute;
    top: -8px;
    right: -8px;
    background-color: var(--color-warning);
    color: white;
    font-size: 9px;
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

  .clickable:hover { transform: scale(1.04); }
</style>
