export const environment = {
  production: true,
  apiUrl: (typeof window !== 'undefined' ? window.location.origin : '') + '/api',
  wsUrl: (typeof window !== 'undefined'
    ? (window.location.protocol === 'https:' ? 'wss://' : 'ws://') + window.location.host
    : 'wss://localhost') + '/ws/websocket'
};
