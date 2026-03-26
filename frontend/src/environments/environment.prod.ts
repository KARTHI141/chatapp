export const environment = {
  production: true,
  apiUrl: '/api',
  wsUrl: 'wss://' + (typeof window !== 'undefined' ? window.location.host : 'localhost') + '/ws/websocket'
};
