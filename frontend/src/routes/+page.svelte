<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import SeatMap from '$lib/components/SeatMap.svelte';
  import EventLog from '$lib/components/EventLog.svelte';
  import ConnectionStatus from '$lib/components/ConnectionStatus.svelte';
  import { seatStore, selectedSeats, currentReservation } from '$lib/stores/seatStore';
  import { apiClient } from '$lib/api';
  import { webSocketClient } from '$lib/websocket';

  const SHOW_ID = 1; // Demo show ID
  let lastReservationId: number | null = null;
  const currentUser = 'guest';

  onMount(async () => {
    await loadShowSeats();
    webSocketClient.connect(SHOW_ID);
  });

  onDestroy(() => {
    webSocketClient.disconnect();
  });

  async function loadShowSeats() {
    seatStore.setLoading(true);
    seatStore.addEventLog('Loading show seats...');
    try {
      const response = await apiClient.getShowSeats(SHOW_ID);
      if (response.success && response.data) {
        seatStore.setShowData(response.data);
        seatStore.addEventLog(`Loaded ${response.data.seats.length} seats for "${response.data.showTitle}"`);
      } else {
        seatStore.setError(response.error || 'Failed to load seats');
        seatStore.addEventLog(`Error loading seats: ${response.error}`);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      seatStore.setError(errorMessage);
      seatStore.addEventLog(`Error loading seats: ${errorMessage}`);
    } finally {
      seatStore.setLoading(false);
    }
  }

  async function refreshSeats() {
    await loadShowSeats();
  }

  function generateIdempotencyKey(): string {
    return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  }

  async function holdSelectedSeat() {
    if ($selectedSeats.length === 0) return;
    const seatId = $selectedSeats[0];

    // Cancel existing hold if clicking a different seat
    if ($currentReservation && $currentReservation.seatId !== seatId) {
      seatStore.addEventLog(`[${currentUser}] Cancelling previous hold on seat ${$currentReservation.seatId}...`);
      try {
        const cancelRes = await apiClient.cancelHold($currentReservation.reservationId, currentUser);
        if (cancelRes.success) {
          seatStore.addEventLog(`✅ Cancelled hold on seat ${$currentReservation.seatId}`);
          seatStore.setCurrentReservation(null);
          lastReservationId = null;
        } else {
          seatStore.addEventLog(`⚠️ Cancel failed: ${cancelRes.error}`);
        }
      } catch (e) {
        seatStore.addEventLog(`⚠️ Cancel error: ${e}`);
      }
    }

    const key = generateIdempotencyKey();
    seatStore.addEventLog(`[${currentUser}] Requesting hold for seat ${seatId}...`);
    try {
      const res = await apiClient.holdSeat(SHOW_ID, seatId, currentUser, key);
      console.log('[holdSelectedSeat] API response:', res);
      if (res.success && res.data) {
        lastReservationId = res.data.reservationId;
        seatStore.addEventLog(`✅ Held seat ${seatId} (Reservation #${lastReservationId})`);

        // Store current reservation for countdown and auto-cancel
        const reservation = {
          reservationId: res.data.reservationId,
          seatId: res.data.seatId,
          userId: currentUser,
          holdExpiresAt: res.data.holdExpiresAt
        };
        console.log('[holdSelectedSeat] Setting currentReservation:', reservation);
        seatStore.setCurrentReservation(reservation);

        seatStore.clearSelection();
      } else {
        seatStore.addEventLog(`❌ Hold failed: ${res.error}`);
      }
    } catch (e) {
      seatStore.addEventLog(`❌ Error: ${e}`);
    }
  }

  async function confirmReservation() {
    if (!lastReservationId) return;
    const key = generateIdempotencyKey();
    seatStore.addEventLog(`Confirming reservation #${lastReservationId}...`);
    const res = await apiClient.confirmReservation(lastReservationId, key);
    if (res.success) {
      seatStore.addEventLog(`✅ Reservation #${lastReservationId} confirmed`);
      lastReservationId = null;
      seatStore.setCurrentReservation(null);
    } else {
      seatStore.addEventLog(`❌ Confirm failed: ${res.error}`);
    }
  }
</script>

<svelte:head>
  <title>SeatFlow - Real-time Seat Reservation</title>
  <meta name="description" content="Modern mobile-like seat reservation UI" />
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
</svelte:head>

<div class="screen">
  <header class="toolbar">
    <div class="row" style="gap:10px">
      <span aria-hidden>🎭</span>
      <strong>SeatFlow</strong>
    </div>
    <div class="row" style="gap:10px">
      <ConnectionStatus />
      <button class="btn btn-ghost" on:click={refreshSeats} title="Refresh seats">🔄</button>
    </div>
  </header>

  <div class="screen-content">
    <SeatMap />
    <EventLog />
  </div>

  <div class="bottom-bar">
    {#if $selectedSeats.length > 0}
      <button class="btn btn-primary" style="flex:1" on:click={holdSelectedSeat}>
        Reserve Selected Seat
      </button>
    {/if}
    {#if lastReservationId}
      <button class="btn btn-success" style="flex:1" on:click={confirmReservation}>
        Confirm Reservation
      </button>
    {/if}
  </div>
</div>

<style>
  /* Page uses global tokens and utilities. */
</style>
