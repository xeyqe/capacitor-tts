import { WebPlugin } from '@capacitor/core';

import type { TTSPlugin } from './definitions';

export class TTSWeb extends WebPlugin implements TTSPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
