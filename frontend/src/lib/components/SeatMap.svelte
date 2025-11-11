<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import Seat from './Seat.svelte';
  import { seatsByRow, selectedSeats, showData, loading, error } from '$lib/stores/seatStore';
  import { SeatStatus } from '$lib/types';

  $: rowNames = Array.from($seatsByRow.keys()).sort();
  $: availableSeats = $seatsByRow.size > 0 ?
    Array.from($seatsByRow.values()).flat().filter(seat => seat.status === SeatStatus.AVAILABLE).length : 0;
  $: holdSeats = $seatsByRow.size > 0 ?
    Array.from($seatsByRow.values()).flat().filter(seat => seat.status === SeatStatus.HOLD).length : 0;
  $: confirmedSeats = $seatsByRow.size > 0 ?
    Array.from($seatsByRow.values()).flat().filter(seat => seat.status === SeatStatus.CONFIRMED).length : 0;

  // Responsive seat sizing to keep rows within container
  let container: HTMLDivElement;
  let seatSize = 40; // default max size
  const minSeatSize = 26;
  const seatGap = 2; // matches CSS
  const rowLabelWidth = 24; // matches CSS

  $: maxSeatsPerRow = Math.max(
    0,
    ...Array.from($seatsByRow.values()).map((row) => row.length)
  );

  function recalcSeatSize() {
    if (!container || maxSeatsPerRow === 0) return;
    const rect = container.getBoundingClientRect();
    const style = window.getComputedStyle(container);
    const pl = parseFloat(style.paddingLeft || '0');
    const pr = parseFloat(style.paddingRight || '0');
    const innerWidth = rect.width - pl - pr;
    const labelsSpace = rowLabelWidth * 2 + 8; // both sides + gap
    const available = innerWidth - labelsSpace;
    const totalGap = (maxSeatsPerRow - 1) * seatGap;
    const size = Math.floor((available - totalGap) / maxSeatsPerRow);
    seatSize = Math.max(minSeatSize, Math.min(40, size));
  }

  let resizeObs: ResizeObserver;
  onMount(() => {
    recalcSeatSize();
    resizeObs = new ResizeObserver(() => recalcSeatSize());
    if (container) resizeObs.observe(container);
  });
  onDestroy(() => { if (resizeObs && container) resizeObs.disconnect(); });
</script>

<div class="seat-map-container" bind:this={container} style={`--seat-size:${seatSize}px`}>
  {#if $loading}
    <div class="loading">Loading seat map...</div>
  {:else if $error}
    <div class="error">Error: {$error}</div>
  {:else if $showData}
    <div class="show-info">
      <h2>{$showData.showTitle}</h2>
      <p class="venue">{$showData.venue}</p>
      <p class="show-date">{new Date($showData.showDate).toLocaleString()}</p>
    </div>

    <div class="seat-stats">
      <div class="stat available">
        <span class="stat-dot"></span>
        Available: {availableSeats}
      </div>
      <div class="stat hold">
        <span class="stat-dot"></span>
        On Hold: {holdSeats}
      </div>
      <div class="stat confirmed">
        <span class="stat-dot"></span>
        Confirmed: {confirmedSeats}
      </div>
    </div>

    {#if $selectedSeats.length > 0}
      <div class="selection-info">
        <p>Selected seats: {$selectedSeats.join(', ')}</p>
      </div>
    {/if}

    <div class="stage-wrap">
      <div class="stage-label">STAGE</div>
    </div>

    <div class="seat-map">
      {#each rowNames as rowName}
        <div class="seat-row">
          <div class="row-label">{rowName}</div>
          <div class="seats">
            {#each $seatsByRow.get(rowName) || [] as seat}
              <Seat {seat} isSelected={$selectedSeats.includes(seat.seatId)} />
            {/each}
          </div>
          <div class="row-label">{rowName}</div>
        </div>
      {/each}
    </div>

    <div class="legend">
      <div class="legend-item">
        <div class="legend-seat available"></div>
        <span>Available</span>
      </div>
      <div class="legend-item">
        <div class="legend-seat hold"></div>
        <span>On Hold</span>
      </div>
      <div class="legend-item">
        <div class="legend-seat confirmed"></div>
        <span>Confirmed</span>
      </div>
      <div class="legend-item">
        <div class="legend-seat selected"></div>
        <span>Selected</span>
      </div>
    </div>
  {:else}
    <div class="no-data">No show data available</div>
  {/if}
</div>

<style>
  .seat-map-container {
    background: var(--color-surface);
    border-radius: var(--radius-m);
    padding: var(--space-4);
    border: 1px solid var(--color-border);
    box-shadow: var(--shadow-1);
    overflow-x: auto;
  }

  .show-info {
    text-align: center;
    margin-bottom: var(--space-4);
    padding-bottom: var(--space-3);
    border-bottom: 1px solid var(--color-border);
  }

  .show-info h2 {
    margin: 0 0 var(--space-2) 0;
    color: var(--color-text);
    font-size: 18px;
  }

  .venue {
    color: var(--color-muted);
    margin: 4px 0;
    font-weight: 500;
  }

  .show-date {
    color: var(--color-muted);
    margin: 4px 0;
    font-size: 12px;
  }

  .seat-stats {
    display: flex;
    justify-content: center;
    gap: var(--space-4);
    margin-bottom: var(--space-4);
    padding: var(--space-3);
    background-color: var(--color-surface-2);
    border-radius: var(--radius-s);
  }

  .stat {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    font-weight: 500;
  }

  .stat-dot { width: 10px; height: 10px; border-radius: 50%; }

  .stat.available .stat-dot { background-color: var(--color-success); }

  .stat.hold .stat-dot { background-color: var(--color-warning); }

  .stat.confirmed .stat-dot { background-color: var(--color-danger); }

  .selection-info {
    text-align: center;
    background-color: var(--color-primary-50);
    padding: var(--space-3);
    border-radius: var(--radius-s);
    margin-bottom: var(--space-4);
    color: var(--color-primary-700);
    font-weight: 500;
  }

  .stage-label {
    background: linear-gradient(45deg, #1f2937, #374151);
    color: white;
    padding: 8px 28px;
    border-radius: var(--radius-pill);
    font-weight: 700;
    display: inline-block;
    box-shadow: var(--shadow-1);
    margin-bottom: var(--space-3);
  }

  .seat-map {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
  }

  .seat-row { display: flex; align-items: center; gap: 8px; }

  .row-label {
    width: 24px;
    text-align: center;
    font-weight: 700;
    color: var(--color-muted);
    font-size: 12px;
  }

  .seats { display: flex; gap: 2px; }

  .legend { display: flex; justify-content: center; gap: var(--space-4); flex-wrap: wrap; }

  .legend-item { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--color-muted); }

  .legend-seat { width: 16px; height: 16px; border-radius: 4px; border: 2px solid; }

  .legend-seat.available { background-color: var(--color-success-50); border-color: var(--color-success); }

  .legend-seat.hold { background-color: var(--color-warning-50); border-color: var(--color-warning); }

  .legend-seat.confirmed { background-color: var(--color-danger-50); border-color: var(--color-danger); }

  .legend-seat.selected { background-color: var(--color-primary-50); border-color: var(--color-primary-600); }

  .loading, .error, .no-data { text-align: center; padding: var(--space-6); color: var(--color-muted); font-size: 14px; }

  .error { color: var(--color-danger); background-color: var(--color-danger-50); border-radius: var(--radius-s); }
</style>
