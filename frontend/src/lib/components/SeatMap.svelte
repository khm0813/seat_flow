<script lang="ts">
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
</script>

<div class="seat-map-container">
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

    <div class="stage">
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
    background: white;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    min-height: 500px;
  }

  .show-info {
    text-align: center;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid #eee;
  }

  .show-info h2 {
    margin: 0 0 10px 0;
    color: #333;
    font-size: 24px;
  }

  .venue {
    color: #666;
    margin: 5px 0;
    font-weight: 500;
  }

  .show-date {
    color: #888;
    margin: 5px 0;
    font-size: 14px;
  }

  .seat-stats {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin-bottom: 20px;
    padding: 10px;
    background-color: #f8f9fa;
    border-radius: 8px;
  }

  .stat {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 14px;
    font-weight: 500;
  }

  .stat-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
  }

  .stat.available .stat-dot {
    background-color: #4caf50;
  }

  .stat.hold .stat-dot {
    background-color: #ff9800;
  }

  .stat.confirmed .stat-dot {
    background-color: #f44336;
  }

  .selection-info {
    text-align: center;
    background-color: #e3f2fd;
    padding: 10px;
    border-radius: 8px;
    margin-bottom: 20px;
    color: #1565c0;
    font-weight: 500;
  }

  .stage {
    text-align: center;
    margin-bottom: 30px;
  }

  .stage-label {
    background: linear-gradient(45deg, #333, #666);
    color: white;
    padding: 10px 40px;
    border-radius: 25px;
    font-weight: bold;
    display: inline-block;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  }

  .seat-map {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
    margin-bottom: 30px;
  }

  .seat-row {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .row-label {
    width: 30px;
    text-align: center;
    font-weight: bold;
    color: #666;
    font-size: 14px;
  }

  .seats {
    display: flex;
    gap: 2px;
  }

  .legend {
    display: flex;
    justify-content: center;
    gap: 20px;
    flex-wrap: wrap;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #666;
  }

  .legend-seat {
    width: 20px;
    height: 20px;
    border-radius: 4px;
    border: 2px solid;
  }

  .legend-seat.available {
    background-color: #e8f5e8;
    border-color: #4caf50;
  }

  .legend-seat.hold {
    background-color: #fff3e0;
    border-color: #ff9800;
  }

  .legend-seat.confirmed {
    background-color: #ffebee;
    border-color: #f44336;
  }

  .legend-seat.selected {
    background-color: #e3f2fd;
    border-color: #2196f3;
  }

  .loading, .error, .no-data {
    text-align: center;
    padding: 40px;
    color: #666;
    font-size: 16px;
  }

  .error {
    color: #d32f2f;
    background-color: #ffebee;
    border-radius: 8px;
  }
</style>