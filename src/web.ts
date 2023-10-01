import { WebPlugin } from '@capacitor/core';

import type { SpeechSynthesisEngine, TTSPlugin } from './definitions';

export class TTSWeb extends WebPlugin implements TTSPlugin {
  async speak(options: {
    text: string,
    rate?: number,
    pitch?: number,
    volume?: number,
    pan?: number,
    voice?: number
  }): Promise<void> {
    console.log(options)
    return Promise.resolve();
  };
  async stop(): Promise<void> {
    return Promise.resolve();
  };
  async getSupportedLanguages(): Promise<{ languages: string[] }> {
    return Promise.resolve({ languages: [] });
  };
  async getSupportedVoices(): Promise<{ voices: SpeechSynthesisVoice[] }> {
    return Promise.resolve({ voices: [] });
  };
  async getSupportedEngines(): Promise<{ engines: SpeechSynthesisEngine[] }> {
    return Promise.resolve({ engines: [] });
  };
  async switchEngine(engineName: { engineName: string }): Promise<void> {
    console.log(engineName)
    return Promise.resolve();
  };
  async getMaxSpeechInputLength(): Promise<{ maxSpeechInputLength: number }> {
    return Promise.resolve({ maxSpeechInputLength: NaN });
  };
  async getDefaults(): Promise<{
    audioStreamType: 'DEFAULT_STREAM_TYPE',
    engine: string,
    language: string,
    pan: number,
    pitch: number,
    rate: number,
    voiceURI: string,
    volume: number,
  }> {
    return Promise.resolve({
      audioStreamType: 'DEFAULT_STREAM_TYPE',
      engine: '',
      language: '',
      pan: NaN,
      pitch: NaN,
      rate: NaN,
      voiceURI: '',
      volume: NaN,
    });
  };
  async openInstall(): Promise<void> {
    return Promise.resolve();
  };
}
