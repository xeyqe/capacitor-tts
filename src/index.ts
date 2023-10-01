import { registerPlugin } from '@capacitor/core';

import type { TTSPlugin } from './definitions';

const TTS = registerPlugin<TTSPlugin>('TTS', {
  web: () => import('./web').then(m => new m.TTSWeb()),
});

export * from './definitions';
export { TTS };
