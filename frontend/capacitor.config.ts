import type { CapacitorConfig } from '@capacitor/cli';

const liveUrl = process.env.CAPACITOR_SERVER_URL?.trim();

const config: CapacitorConfig = {
  appId: 'com.cleanit.app',
  appName: 'Clean IT',
  webDir: 'dist',
  server: liveUrl
    ? {
        url: liveUrl,
        cleartext: false,
      }
    : {
        androidScheme: 'https',
      },
};

export default config;
