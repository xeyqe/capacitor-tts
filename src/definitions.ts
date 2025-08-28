import { PluginListenerHandle } from "@capacitor/core";

export interface TTSPlugin {
  speak(options: TTSOptions): Promise<void>;
  read(options: TTSReadOptions): Promise<void>;
  stop(): Promise<void>;
  getSupportedLanguages(): Promise<{ languages: string[] }>;
  getSupportedVoices(): Promise<{ voices: SpeechSynthesisVoice[] }>;
  getSupportedEngines(): Promise<{ engines: SpeechSynthesisEngine[] }>;
  switchEngine(engineName: { engineName: string }): Promise<void>;
  getMaxSpeechInputLength(): Promise<{ maxSpeechInputLength: number }>;
  getDefaults(): Promise<{
    audioStreamType: 'DEFAULT_STREAM_TYPE',
    engine: string,
    language: string,
    pan: number,
    pitch: number,
    rate: number,
    voiceURI: string,
    volume: number,
  }>;
  openInstall(): Promise<void>;
  addListener(
    eventName: 'progressEvent' | 'progressArrayEvent' | 'audioFocusChangeEvent',
    listenerFunc: (obj: {
      utteranceId?: string,
      start?: number,
      end?: number,
      frame?: number,
      progress?: number,
      type?: 'AUDIOFOCUS_GAIN' | 'AUDIOFOCUS_LOSS' | 'AUDIOFOCUS_LOSS_TRANSIENT' | 'AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK'
    }) => void,
  ): Promise<PluginListenerHandle>;
  requestFocus(): Promise<void>;
  abandonFocus(): Promise<void>;
}

export interface TTSOptions {
  /**
   * The text that will be synthesised when the utterance is spoken.
   *
   * @example "Hello world"
   */
  text: string;
  /**
   * The speed at which the utterance will be spoken at.
   *
   * @default 1.0
   */
  rate?: number;
  /**
   * The pitch at which the utterance will be spoken at.
   *
   * @default 1.0
   */
  pitch?: number;
  /**
   * The volume that the utterance will be spoken at.
   *
   * @default 1.0
   */
  volume?: number;
  /**
 * Parameter key to specify how the speech is panned from left to right when speaking text.
 * Pan is specified as a float ranging from -1 to +1 where -1 maps to a hard-left pan,
 * 0 to center (the default behavior), and +1 to hard-right.
 *
 * @default 0.0
 */
  pan?: number;
  /**
   * The index of the selected voice that will be used to speak the utterance.
   * Possible voices can be queried using `getSupportedVoices`.
   */
  voiceURI?: number;
  /**
  * Parameter key to specify the audio stream type to be used when speaking text or playing back a file.
  * The value should be one of the STREAM_ constants defined in AudioManager.
  */
  streamType?: 'STREAM_ALARM' | 'STREAM_DTMF' | 'STREAM_MUSIC' | 'STREAM_NOTIFICATION' | 'STREAM_RING' | 'STREAM_SYSTEM' | 'STREAM_VOICE_CALL';
}

export interface TTSReadOptions {
  /**
   * The text that will be synthesised when the utterance is spoken.
   *
   * @example "Hello world"
   */
  texts: string[];
  progress: number;
  /**
   * The speed at which the utterance will be spoken at.
   *
   * @default 1.0
   */
  rate?: number;
  /**
   * The pitch at which the utterance will be spoken at.
   *
   * @default 1.0
   */
  pitch?: number;
  /**
   * The volume that the utterance will be spoken at.
   *
   * @default 1.0
   */
  volume?: number;
  /**
 * Parameter key to specify how the speech is panned from left to right when speaking text.
 * Pan is specified as a float ranging from -1 to +1 where -1 maps to a hard-left pan,
 * 0 to center (the default behavior), and +1 to hard-right.
 *
 * @default 0.0
 */
  pan?: number;
  /**
   * The index of the selected voice that will be used to speak the utterance.
   * Possible voices can be queried using `getSupportedVoices`.
   */
  voiceURI?: number;
  /**
  * Parameter key to specify the audio stream type to be used when speaking text or playing back a file.
  * The value should be one of the STREAM_ constants defined in AudioManager.
  */
  streamType?: 'STREAM_ALARM' | 'STREAM_DTMF' | 'STREAM_MUSIC' | 'STREAM_NOTIFICATION' | 'STREAM_RING' | 'STREAM_SYSTEM' | 'STREAM_VOICE_CALL';
}


export interface SpeechSynthesisVoice {
  /**
   * Specifies whether the voice is the default voice for the current app (`true`) or not (`false`).
   *
   * @example false
   */
  default: boolean;
  /**
   * BCP 47 language tag indicating the language of the voice.
   *
   * @example "en-US"
   */
  lang: string;
  /**
   * Specifies whether the voice is supplied by a local (`true`) or remote (`false`) speech synthesizer service.
   *
   * @example true
   */
  localService: boolean;
  /**
   * Human-readable name that represents the voice.
   *
   * @example "Microsoft Zira Desktop - English (United States)"
   */
  name: string;
  /**
   * Type of URI and location of the speech synthesis service for this voice.
   *
   * @example "urn:moz-tts:sapi:Microsoft Zira Desktop - English (United States)?en-US"
   */
  voiceURI: string;
}

export interface SpeechSynthesisEngine {
  icon: number;
  label: string;
  name: string;
}


