import { seatStore } from '$lib/stores/seatStore';
import { SeatStatus, type SeatStatusMessage } from '$lib/types';

class WebSocketClient {
  private socket: WebSocket | null = null;
  private showId: number | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectInterval = 1000; // Start with 1 second
  private isConnecting = false;

  connect(showId: number): void {
    if (this.socket?.readyState === WebSocket.OPEN && this.showId === showId) {
      console.log('WebSocket already connected for show', showId);
      return;
    }

    if (this.isConnecting) {
      console.log('WebSocket connection already in progress');
      return;
    }

    this.disconnect(); // Close existing connection if any
    this.showId = showId;
    this.isConnecting = true;

    const wsUrl = `ws://localhost:8080/ws/seats/${showId}`;
    console.log('Connecting to WebSocket:', wsUrl);

    try {
      this.socket = new WebSocket(wsUrl);
      this.setupEventHandlers();
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      this.isConnecting = false;
      this.scheduleReconnect();
    }
  }

  disconnect(): void {
    if (this.socket) {
      console.log('Disconnecting WebSocket');
      this.socket.close();
      this.socket = null;
    }
    this.showId = null;
    this.isConnecting = false;
    this.reconnectAttempts = 0;
  }

  private setupEventHandlers(): void {
    if (!this.socket) return;

    this.socket.onopen = () => {
      console.log('WebSocket connected successfully');
      this.isConnecting = false;
      this.reconnectAttempts = 0;
      this.reconnectInterval = 1000; // Reset reconnect interval
      seatStore.addEventLog('🔗 Connected to real-time updates');
    };

    this.socket.onmessage = (event) => {
      try {
        const message: SeatStatusMessage = JSON.parse(event.data);
        this.handleSeatStatusMessage(message);
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
        seatStore.addEventLog(`❌ Failed to parse real-time message: ${error}`);
      }
    };

    this.socket.onclose = (event) => {
      console.log('WebSocket closed:', event.code, event.reason);
      this.isConnecting = false;

      if (event.code !== 1000) { // Not a normal closure
        seatStore.addEventLog('🔌 Real-time connection lost, attempting to reconnect...');
        this.scheduleReconnect();
      } else {
        seatStore.addEventLog('🔌 Real-time connection closed');
      }
    };

    this.socket.onerror = (error) => {
      console.error('WebSocket error:', error);
      this.isConnecting = false;
      seatStore.addEventLog('❌ Real-time connection error');
    };
  }

  private handleSeatStatusMessage(message: SeatStatusMessage): void {
    console.log('Received seat status update:', message);

    // Update the seat status in the store
    seatStore.updateSeatStatus(message.seatId, message.status, message.holdExpiresAt);

    // Add log entry
    const statusText = this.getStatusDisplayText(message.status);
    const userText = message.userId === 'system' ? 'System' : message.userId;

    let logMessage = `🎭 Seat ${message.seatId} ${statusText} by ${userText}`;

    if (message.status === SeatStatus.HOLD && message.holdExpiresAt) {
      const expiresAt = new Date(message.holdExpiresAt);
      const holdDuration = Math.round((expiresAt.getTime() - Date.now()) / 1000 / 60);
      logMessage += ` (expires in ${holdDuration}min)`;
    }

    seatStore.addEventLog(logMessage);
  }

  private getStatusDisplayText(status: SeatStatus): string {
    switch (status) {
      case SeatStatus.AVAILABLE:
        return 'became available';
      case SeatStatus.HOLD:
        return 'was held';
      case SeatStatus.CONFIRMED:
        return 'was confirmed';
      default:
        return 'status changed';
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.log('Max reconnection attempts reached');
      seatStore.addEventLog('❌ Failed to reconnect after multiple attempts');
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(this.reconnectInterval * Math.pow(2, this.reconnectAttempts - 1), 30000);

    console.log(`Scheduling reconnection attempt ${this.reconnectAttempts} in ${delay}ms`);

    setTimeout(() => {
      if (this.showId !== null) {
        console.log(`Reconnection attempt ${this.reconnectAttempts}`);
        this.connect(this.showId);
      }
    }, delay);
  }

  isConnected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  getConnectionState(): string {
    if (!this.socket) return 'disconnected';

    switch (this.socket.readyState) {
      case WebSocket.CONNECTING:
        return 'connecting';
      case WebSocket.OPEN:
        return 'connected';
      case WebSocket.CLOSING:
        return 'closing';
      case WebSocket.CLOSED:
        return 'disconnected';
      default:
        return 'unknown';
    }
  }

  // Send a ping message to keep connection alive (optional)
  sendPing(): void {
    if (this.isConnected()) {
      this.socket?.send(JSON.stringify({ type: 'ping', timestamp: Date.now() }));
    }
  }
}

export const webSocketClient = new WebSocketClient();

// Optional: Set up periodic ping to keep connection alive
if (typeof window !== 'undefined') {
  setInterval(() => {
    webSocketClient.sendPing();
  }, 30000); // Ping every 30 seconds
}