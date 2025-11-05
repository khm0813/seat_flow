<script lang="ts">
  import { seatStore, selectedSeats } from '$lib/stores/seatStore';
  import { apiClient } from '$lib/api';

  const demoUsers = ['user-alice', 'user-bob', 'user-charlie'];
  let currentUser = demoUsers[0];

  const scenarios = [
    {
      id: 'select-seat',
      title: '1. Select Seat',
      description: 'Click on an available seat to select it',
      action: null
    },
    {
      id: 'hold-seat',
      title: '2. Hold Seat',
      description: 'Request to hold the selected seat',
      action: holdSelectedSeat
    },
    {
      id: 'confirm-reservation',
      title: '3. Confirm Reservation',
      description: 'Confirm the held seat reservation',
      action: confirmReservation
    },
    {
      id: 'concurrent-demo',
      title: '4. Concurrent Demo',
      description: 'Simulate multiple users trying to reserve the same seat',
      action: demonstrateConcurrency
    }
  ];

  let lastReservationId: number | null = null;

  async function holdSelectedSeat() {
    if ($selectedSeats.length === 0) {
      seatStore.addEventLog('Please select a seat first');
      return;
    }

    const seatId = $selectedSeats[0];
    const idempotencyKey = generateIdempotencyKey();

    seatStore.addEventLog(`[${currentUser}] Requesting hold for seat ${seatId}...`);

    try {
      const response = await apiClient.holdSeat(1, seatId, currentUser, idempotencyKey);

      if (response.success && response.data) {
        lastReservationId = response.data.reservationId;
        seatStore.addEventLog(`[${currentUser}] Successfully held seat ${seatId} (Reservation ID: ${lastReservationId})`);
        seatStore.clearSelection();
      } else {
        seatStore.addEventLog(`[${currentUser}] Failed to hold seat ${seatId}: ${response.error}`);
      }
    } catch (error) {
      seatStore.addEventLog(`[${currentUser}] Error holding seat: ${error}`);
    }
  }

  async function confirmReservation() {
    if (!lastReservationId) {
      seatStore.addEventLog('No reservation to confirm. Hold a seat first.');
      return;
    }

    const idempotencyKey = generateIdempotencyKey();

    seatStore.addEventLog(`[${currentUser}] Confirming reservation ${lastReservationId}...`);

    try {
      const response = await apiClient.confirmReservation(lastReservationId, idempotencyKey);

      if (response.success) {
        seatStore.addEventLog(`[${currentUser}] Successfully confirmed reservation ${lastReservationId}`);
        lastReservationId = null;
      } else {
        seatStore.addEventLog(`[${currentUser}] Failed to confirm reservation: ${response.error}`);
      }
    } catch (error) {
      seatStore.addEventLog(`[${currentUser}] Error confirming reservation: ${error}`);
    }
  }

  async function demonstrateConcurrency() {
    if ($selectedSeats.length === 0) {
      seatStore.addEventLog('Please select a seat for the concurrency demo');
      return;
    }

    const seatId = $selectedSeats[0];
    seatStore.addEventLog(`Starting concurrency demo for seat ${seatId}`);

    // Simulate multiple users trying to hold the same seat simultaneously
    const promises = demoUsers.map(async (user, index) => {
      const delay = Math.random() * 100; // Random delay 0-100ms
      await new Promise(resolve => setTimeout(resolve, delay));

      const idempotencyKey = generateIdempotencyKey();
      seatStore.addEventLog(`[${user}] Attempting to hold seat ${seatId}...`);

      try {
        const response = await apiClient.holdSeat(1, seatId, user, idempotencyKey);

        if (response.success) {
          seatStore.addEventLog(`[${user}] ✅ Successfully held seat ${seatId}`);
        } else {
          seatStore.addEventLog(`[${user}] ❌ Failed to hold seat ${seatId}: ${response.error}`);
        }
      } catch (error) {
        seatStore.addEventLog(`[${user}] ❌ Error: ${error}`);
      }
    });

    await Promise.all(promises);
    seatStore.addEventLog('Concurrency demo completed');
    seatStore.clearSelection();
  }

  function generateIdempotencyKey(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  function clearLogs() {
    seatStore.reset();
    lastReservationId = null;
  }
</script>

<div class="scenario-panel">
  <div class="panel-header">
    <h3>Demo Scenarios</h3>
    <div class="user-selector">
      <label for="user-select">Current User:</label>
      <select id="user-select" bind:value={currentUser}>
        {#each demoUsers as user}
          <option value={user}>{user}</option>
        {/each}
      </select>
    </div>
  </div>

  <div class="scenarios">
    {#each scenarios as scenario}
      <div class="scenario">
        <h4>{scenario.title}</h4>
        <p>{scenario.description}</p>
        {#if scenario.action}
          <button
            class="scenario-btn"
            on:click={scenario.action}
            disabled={scenario.id === 'hold-seat' && $selectedSeats.length === 0}
          >
            Execute
          </button>
        {/if}
      </div>
    {/each}
  </div>

  <div class="actions">
    <button class="clear-btn" on:click={clearLogs}>Clear Logs</button>
    {#if lastReservationId}
      <p class="reservation-info">
        Active Reservation: #{lastReservationId}
      </p>
    {/if}
  </div>
</div>

<style>
  .scenario-panel {
    background: white;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    height: fit-content;
  }

  .panel-header {
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid #eee;
  }

  .panel-header h3 {
    margin: 0 0 15px 0;
    color: #333;
    font-size: 20px;
  }

  .user-selector {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .user-selector label {
    font-weight: 500;
    color: #666;
  }

  .user-selector select {
    padding: 8px 12px;
    border: 1px solid #ddd;
    border-radius: 6px;
    background: white;
    font-size: 14px;
  }

  .scenarios {
    display: flex;
    flex-direction: column;
    gap: 15px;
    margin-bottom: 20px;
  }

  .scenario {
    padding: 15px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    background: #fafafa;
  }

  .scenario h4 {
    margin: 0 0 8px 0;
    color: #333;
    font-size: 16px;
  }

  .scenario p {
    margin: 0 0 12px 0;
    color: #666;
    font-size: 14px;
    line-height: 1.4;
  }

  .scenario-btn {
    background: #2196f3;
    color: white;
    border: none;
    padding: 8px 16px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: background-color 0.2s;
  }

  .scenario-btn:hover:not(:disabled) {
    background: #1976d2;
  }

  .scenario-btn:disabled {
    background: #ccc;
    cursor: not-allowed;
  }

  .actions {
    padding-top: 15px;
    border-top: 1px solid #eee;
  }

  .clear-btn {
    background: #f44336;
    color: white;
    border: none;
    padding: 10px 20px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: background-color 0.2s;
  }

  .clear-btn:hover {
    background: #d32f2f;
  }

  .reservation-info {
    margin-top: 10px;
    padding: 8px 12px;
    background: #e8f5e8;
    border: 1px solid #4caf50;
    border-radius: 6px;
    color: #2e7d32;
    font-weight: 500;
    font-size: 14px;
    margin-bottom: 0;
  }
</style>