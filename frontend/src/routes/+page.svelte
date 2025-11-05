<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import SeatMap from '$lib/components/SeatMap.svelte';
  import ScenarioPanel from '$lib/components/ScenarioPanel.svelte';
  import EventLog from '$lib/components/EventLog.svelte';
  import ConnectionStatus from '$lib/components/ConnectionStatus.svelte';
  import { seatStore } from '$lib/stores/seatStore';
  import { apiClient } from '$lib/api';
  import { webSocketClient } from '$lib/websocket';

  const SHOW_ID = 1; // Demo show ID

  onMount(async () => {
    // Load initial seat data for show ID 1 (demo show)
    await loadShowSeats();

    // Connect to WebSocket for real-time updates
    webSocketClient.connect(SHOW_ID);
  });

  onDestroy(() => {
    // Disconnect WebSocket when component is destroyed
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
</script>

<svelte:head>
  <title>SeatFlow - Real-time Seat Reservation System</title>
  <meta name="description" content="Demo of a real-time, high-concurrency seat reservation system" />
</svelte:head>

<div class="app">
  <header class="app-header">
    <h1>🎭 SeatFlow</h1>
    <p>Real-time Seat Reservation System Demo</p>
    <div class="header-controls">
      <ConnectionStatus />
      <button class="refresh-btn" on:click={refreshSeats}>
        🔄 Refresh Seats
      </button>
    </div>
  </header>

  <main class="app-main">
    <div class="left-panel">
      <ScenarioPanel />
    </div>

    <div class="right-panel">
      <div class="seat-map-section">
        <SeatMap />
      </div>

      <div class="event-log-section">
        <EventLog />
      </div>
    </div>
  </main>
</div>

<style>
  :global(body) {
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    min-height: 100vh;
  }

  :global(*) {
    box-sizing: border-box;
  }

  .app {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }

  .app-header {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    padding: 20px;
    text-align: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
    position: sticky;
    top: 0;
    z-index: 100;
  }

  .app-header h1 {
    margin: 0 0 10px 0;
    font-size: 32px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .app-header p {
    margin: 0 0 15px 0;
    color: #666;
    font-size: 16px;
  }

  .header-controls {
    display: flex;
    align-items: center;
    gap: 15px;
    justify-content: center;
  }

  .refresh-btn {
    background: #4caf50;
    color: white;
    border: none;
    padding: 10px 20px;
    border-radius: 25px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s;
    box-shadow: 0 2px 5px rgba(76, 175, 80, 0.3);
  }

  .refresh-btn:hover {
    background: #45a049;
    transform: translateY(-1px);
    box-shadow: 0 4px 10px rgba(76, 175, 80, 0.4);
  }

  .app-main {
    flex: 1;
    display: grid;
    grid-template-columns: 1fr 2fr;
    gap: 20px;
    padding: 20px;
    max-width: 1400px;
    margin: 0 auto;
    width: 100%;
  }

  .left-panel {
    display: flex;
    flex-direction: column;
  }

  .right-panel {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .seat-map-section {
    flex: 1;
  }

  .event-log-section {
    flex-shrink: 0;
  }

  @media (max-width: 1024px) {
    .app-main {
      grid-template-columns: 1fr;
      gap: 15px;
      padding: 15px;
    }

    .right-panel {
      order: -1;
    }
  }

  @media (max-width: 768px) {
    .app-header {
      padding: 15px;
    }

    .app-header h1 {
      font-size: 24px;
    }

    .app-main {
      padding: 10px;
      gap: 10px;
    }
  }
</style>
