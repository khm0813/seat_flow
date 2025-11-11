<script lang="ts">
  import { eventLog } from '$lib/stores/seatStore';

  let logContainer: HTMLDivElement;

  // Auto-scroll to bottom when new events are added
  $: if ($eventLog.length > 0 && logContainer) {
    setTimeout(() => {
      logContainer.scrollTop = logContainer.scrollHeight;
    }, 100);
  }
</script>

<div class="event-log">
  <div class="log-header">
    <h3>Real-time Event Log</h3>
    <div class="log-count">
      {$eventLog.length} events
    </div>
  </div>

  <div class="log-content" bind:this={logContainer}>
    {#if $eventLog.length === 0}
      <div class="no-events">
        <p>No events yet. Start by selecting a seat or running a demo scenario.</p>
      </div>
    {:else}
      <div class="events">
        {#each $eventLog as event, index}
          <div class="event" class:recent={index < 3}>
            {event}
          </div>
        {/each}
      </div>
    {/if}
  </div>
</div>

<style>
  .event-log {
    background: white;
    border-radius: 12px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    height: 400px;
    display: flex;
    flex-direction: column;
  }

  .log-header {
    padding: 15px 20px;
    border-bottom: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
  }

  .log-header h3 {
    margin: 0;
    color: #333;
    font-size: 18px;
  }

  .log-count {
    background: #e3f2fd;
    color: #1565c0;
    padding: 4px 8px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
  }

  .log-content {
    flex: 1;
    overflow-y: auto;
    padding: 0;
  }

  .no-events {
    padding: 40px 20px;
    text-align: center;
    color: #666;
  }

  .no-events p {
    margin: 0;
    font-style: italic;
  }

  .events {
    padding: 10px 0;
  }

  .event {
    padding: 8px 20px;
    border-bottom: 1px solid #f5f5f5;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.4;
    color: #333;
    transition: background-color 0.3s ease;
  }

  .event:last-child {
    border-bottom: none;
  }

  .event.recent {
    background-color: #f8f9fa;
    animation: highlight 1s ease-out;
  }

  .event:hover {
    background-color: #f0f0f0;
  }

  @keyframes highlight {
    from {
      background-color: #e8f5e8;
    }
    to {
      background-color: #f8f9fa;
    }
  }

  /* Custom scrollbar */
  .log-content::-webkit-scrollbar {
    width: 6px;
  }

  .log-content::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }

  .log-content::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;
  }

  .log-content::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
  }
</style>