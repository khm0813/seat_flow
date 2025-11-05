<script lang="ts">
  import { onMount, onDestroy } from 'svelte';
  import { webSocketClient } from '$lib/websocket';

  let connectionState = 'disconnected';
  let interval: number;

  onMount(() => {
    // Update connection state every second
    interval = setInterval(() => {
      connectionState = webSocketClient.getConnectionState();
    }, 1000);
  });

  onDestroy(() => {
    if (interval) {
      clearInterval(interval);
    }
  });

  $: statusClass = getStatusClass(connectionState);
  $: statusText = getStatusText(connectionState);

  function getStatusClass(state: string): string {
    switch (state) {
      case 'connected':
        return 'status-connected';
      case 'connecting':
        return 'status-connecting';
      case 'disconnected':
      case 'closing':
        return 'status-disconnected';
      default:
        return 'status-unknown';
    }
  }

  function getStatusText(state: string): string {
    switch (state) {
      case 'connected':
        return '🟢 Real-time Connected';
      case 'connecting':
        return '🟡 Connecting...';
      case 'disconnected':
        return '🔴 Disconnected';
      case 'closing':
        return '🟡 Disconnecting...';
      default:
        return '⚫ Unknown';
    }
  }
</script>

<div class="connection-status {statusClass}">
  <span class="status-text">{statusText}</span>
</div>

<style>
  .connection-status {
    display: inline-flex;
    align-items: center;
    padding: 6px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
    transition: all 0.3s ease;
    border: 1px solid;
  }

  .status-connected {
    background-color: #e8f5e8;
    border-color: #4caf50;
    color: #2e7d32;
  }

  .status-connecting {
    background-color: #fff8e1;
    border-color: #ff9800;
    color: #f57c00;
    animation: pulse 2s infinite;
  }

  .status-disconnected {
    background-color: #ffebee;
    border-color: #f44336;
    color: #c62828;
  }

  .status-unknown {
    background-color: #f5f5f5;
    border-color: #9e9e9e;
    color: #616161;
  }

  .status-text {
    line-height: 1;
  }

  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.7;
    }
  }
</style>