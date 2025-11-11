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
    background: var(--color-surface);
    border-radius: var(--radius-m);
    border: 1px solid var(--color-border);
    box-shadow: var(--shadow-1);
    height: 280px;
    display: flex;
    flex-direction: column;
  }

  .log-header {
    padding: var(--space-3) var(--space-4);
    border-bottom: 1px solid var(--color-border);
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
  }

  .log-header h3 { margin: 0; color: var(--color-text); font-size: 16px; }

  .log-count { background: var(--color-primary-50); color: var(--color-primary-700); padding: 4px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }

  .log-content {
    flex: 1;
    overflow-y: auto;
    padding: 0;
  }

  .no-events { padding: 24px 16px; text-align: center; color: var(--color-muted); }

  .no-events p {
    margin: 0;
    font-style: italic;
  }

  .events { padding: 6px 0; }

  .event {
    padding: 8px 16px;
    border-bottom: 1px solid var(--color-border);
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.4;
    color: var(--color-text);
    transition: background-color 0.3s ease;
  }

  .event:last-child {
    border-bottom: none;
  }

  .event.recent { background-color: var(--color-surface-2); animation: highlight 1s ease-out; }

  .event:hover { background-color: #f3f4f6; }

  @keyframes highlight { from { background-color: var(--color-primary-50); } to { background-color: var(--color-surface-2); } }

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
