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
  const minSeatSize = 18; // not enforced to avoid horizontal scroll
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
    // Fit to one screen: never exceed 40, allow smaller than min if needed
    seatSize = Math.min(40, size);
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
      <div class="selection-bar">
        <span class="selection-label"><span class="sel-dot"></span>Selected ({$selectedSeats.length})</span>
        <div class="selection-chips">
          {#each $selectedSeats as id}
            <span class="chip">{id}</span>
          {/each}
        </div>
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

    <div class="legend-card">
      <div class="legend-items">
        <div class="legend-item"><span class="legend-dot available"></span><span>Available</span></div>
        <div class="legend-item"><span class="legend-dot hold"></span><span>On Hold</span></div>
        <div class="legend-item"><span class="legend-dot confirmed"></span><span>Confirmed</span></div>
        <div class="legend-item"><span class="legend-dot selected"></span><span>Selected</span></div>
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
    overflow-x: hidden;
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
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-3);
    margin: 0 auto var(--space-4);
    padding: 6px 10px;
    background-color: var(--color-surface-2);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-pill);
    white-space: nowrap;
    font-size: 11px;
  }

  .stat { display: inline-flex; align-items: center; gap: 6px; font-size: inherit; font-weight: 600; color: var(--color-text); }

  .stat-dot { width: 8px; height: 8px; border-radius: 50%; }

  .stat.available .stat-dot { background-color: var(--color-success); }

  .stat.hold .stat-dot { background-color: var(--color-warning); }

  .stat.confirmed .stat-dot { background-color: var(--color-danger); }

  .selection-bar {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 10px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-pill);
    background: var(--color-primary-50);
    color: var(--color-primary-700);
    font-weight: 600;
    font-size: 11px;
    margin: 0 auto var(--space-3);
  }

  .selection-label { display: inline-flex; align-items: center; gap: 6px; }
  .sel-dot { width: 8px; height: 8px; background: var(--color-primary-600); display: inline-block; border-radius: 999px; }
  .selection-chips { display: inline-flex; align-items: center; gap: 6px; flex-wrap: wrap; }
  .chip { padding: 2px 8px; background: #fff; border: 1px solid var(--color-border); border-radius: var(--radius-pill); color: var(--color-text); font-size: 11px; }

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

  .legend-card {
    display: block;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-s);
    background: var(--color-surface-2);
    padding: 6px 8px;
    margin-top: var(--space-3);
  }
  .legend-items {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px 14px;
    flex-wrap: wrap;
    font-size: 11px;
    color: var(--color-muted);
  }
  .legend-item { display: inline-flex; align-items: center; gap: 6px; }
  .legend-dot { width: 9px; height: 9px; border-radius: 3px; border: 2px solid; }
  .legend-dot.available { background-color: var(--color-success-50); border-color: var(--color-success); }
  .legend-dot.hold { background-color: var(--color-warning-50); border-color: var(--color-warning); }
  .legend-dot.confirmed { background-color: var(--color-danger-50); border-color: var(--color-danger); }
  .legend-dot.selected { background-color: var(--color-primary-50); border-color: var(--color-primary-600); }

  .loading, .error, .no-data { text-align: center; padding: var(--space-6); color: var(--color-muted); font-size: 14px; }

  .error { color: var(--color-danger); background-color: var(--color-danger-50); border-radius: var(--radius-s); }
</style>
