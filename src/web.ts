import { PluginListenerHandle, WebPlugin } from '@capacitor/core';

import type { SpeechSynthesisEngine, TTSOptions, TTSReadOptions, TTSPlugin } from './definitions';

export class TTSWeb extends WebPlugin implements TTSPlugin {
  async speak(options: TTSOptions): Promise<void> {
    console.log(options)
    return Promise.resolve();
  };
  async read(options: TTSReadOptions): Promise<void> {
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
  async addListener(
    eventName: 'progressEvent' | 'progressArrayEvent',
    listenerFunc: (obj: {
      utteranceId?: string,
      start?: number,
      end?: number,
      frame?: number,
      progress?: number,
    }) => void,
  ): Promise<PluginListenerHandle> {
    console.log(eventName);
    console.log(listenerFunc);
    return Promise.resolve(null as any);
  };
}
