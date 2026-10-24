# capacitor-tts

Capacitor tts plugin for Android.

## Install

```bash
npm install @xeyqe/capacitor-tts
npx cap sync
```

## Requirements

This version requires Capacitor 8 and Node.js 22 or newer. Android builds use
Java 21 and SDK 36, with Android 7.0 (API 24) as the minimum supported version.
iOS requires iOS 15 or newer and Xcode 26 or newer.

See the [Capacitor 8 migration guide](https://capacitorjs.com/docs/updating/8-0)
when upgrading an existing app.

## Android background reading

`read()` runs in a `mediaPlayback` foreground service and continues when the app
goes into the background. A reading notification includes a **Stop** action.
The service stops when all texts finish, reading fails, or `stop()` is called.
Starting another read, calling `speak()`, or switching engines cancels the current read.
Progress events and saved book progress continue to be produced by native code;
JavaScript listeners may be suspended while the WebView is in the background.

Start `read()` while the app is visible, then move it to the background.
[Android 12 and later restrict starting foreground services from the background](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).
If the system rejects the service start, the `read()` promise rejects with an error.
Reading does not automatically resume after the app process is killed.

The plugin manifest includes `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `WAKE_LOCK`, `POST_NOTIFICATIONS`, and the service declaration.
A partial wake lock keeps the native reading loop running with the screen off and
is released when reading ends. Run `npx cap sync android`
after updating the plugin. Call `requestForegroundPermissions()` from a user action
while the app is visible to request notification permission on Android 13 and later:

```typescript
const { notifications } = await TTS.requestForegroundPermissions();
```

The result is a Capacitor permission state (`granted`, `denied`, `prompt`, or
`prompt-with-rationale`). Earlier Android versions return `granted` without a prompt.
Android decides whether another prompt can be shown after denial; users may need
to enable notifications in app settings. This permission shows the reading
notification in the notification drawer and is not required to start the foreground
service. The foreground service permissions themselves are granted at install time.
This method is Android only; the web implementation rejects as unimplemented.

## API

<docgen-index>

* [`requestForegroundPermissions()`](#requestforegroundpermissions)
* [`speak(...)`](#speak)
* [`read(...)`](#read)
* [`stop()`](#stop)
* [`getSupportedLanguages()`](#getsupportedlanguages)
* [`getSupportedVoices()`](#getsupportedvoices)
* [`getSupportedEngines()`](#getsupportedengines)
* [`switchEngine(...)`](#switchengine)
* [`getMaxSpeechInputLength()`](#getmaxspeechinputlength)
* [`getDefaults()`](#getdefaults)
* [`openInstall()`](#openinstall)
* [`addListener('progressEvent' | 'progressArrayEvent' | 'audioFocusChangeEvent', ...)`](#addlistenerprogressevent--progressarrayevent--audiofocuschangeevent-)
* [`requestFocus()`](#requestfocus)
* [`abandonFocus()`](#abandonfocus)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### requestForegroundPermissions()

```typescript
requestForegroundPermissions() => Promise<{ notifications: PermissionState; }>
```

Requests notification permission for the Android foreground reading service.
Android 13+ shows a system prompt when permission can be requested.
Earlier Android versions return `granted` without prompting.
Foreground service permissions are granted at install time; notification
denial does not prevent foreground playback. Android only.

**Returns:** <code>Promise&lt;{ notifications: <a href="#permissionstate">PermissionState</a>; }&gt;</code>

--------------------


### speak(...)

```typescript
speak(options: TTSOptions) => Promise<void>
```

| Param         | Type                                              |
| ------------- | ------------------------------------------------- |
| **`options`** | <code><a href="#ttsoptions">TTSOptions</a></code> |

--------------------


### read(...)

```typescript
read(options: TTSReadOptions) => Promise<void>
```

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#ttsreadoptions">TTSReadOptions</a></code> |

--------------------


### stop()

```typescript
stop() => Promise<void>
```

--------------------


### getSupportedLanguages()

```typescript
getSupportedLanguages() => Promise<{ languages: string[]; }>
```

**Returns:** <code>Promise&lt;{ languages: string[]; }&gt;</code>

--------------------


### getSupportedVoices()

```typescript
getSupportedVoices() => Promise<{ voices: SpeechSynthesisVoice[]; }>
```

**Returns:** <code>Promise&lt;{ voices: SpeechSynthesisVoice[]; }&gt;</code>

--------------------


### getSupportedEngines()

```typescript
getSupportedEngines() => Promise<{ engines: SpeechSynthesisEngine[]; }>
```

**Returns:** <code>Promise&lt;{ engines: SpeechSynthesisEngine[]; }&gt;</code>

--------------------


### switchEngine(...)

```typescript
switchEngine(engineName: { engineName: string; }) => Promise<void>
```

| Param            | Type                                 |
| ---------------- | ------------------------------------ |
| **`engineName`** | <code>{ engineName: string; }</code> |

--------------------


### getMaxSpeechInputLength()

```typescript
getMaxSpeechInputLength() => Promise<{ maxSpeechInputLength: number; }>
```

**Returns:** <code>Promise&lt;{ maxSpeechInputLength: number; }&gt;</code>

--------------------


### getDefaults()

```typescript
getDefaults() => Promise<{ audioStreamType: 'DEFAULT_STREAM_TYPE'; engine: string; language: string; pan: number; pitch: number; rate: number; voiceURI: string; volume: number; }>
```

**Returns:** <code>Promise&lt;{ audioStreamType: 'DEFAULT_STREAM_TYPE'; engine: string; language: string; pan: number; pitch: number; rate: number; voiceURI: string; volume: number; }&gt;</code>

--------------------


### openInstall()

```typescript
openInstall() => Promise<void>
```

--------------------


### addListener('progressEvent' | 'progressArrayEvent' | 'audioFocusChangeEvent', ...)

```typescript
addListener(eventName: 'progressEvent' | 'progressArrayEvent' | 'audioFocusChangeEvent', listenerFunc: (obj: { utteranceId?: string; start?: number; end?: number; frame?: number; progress?: number; type?: 'AUDIOFOCUS_GAIN' | 'AUDIOFOCUS_LOSS' | 'AUDIOFOCUS_LOSS_TRANSIENT' | 'AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK'; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                                                                                                                                                                                            |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'progressEvent' \| 'progressArrayEvent' \| 'audioFocusChangeEvent'</code>                                                                                                                                                                 |
| **`listenerFunc`** | <code>(obj: { utteranceId?: string; start?: number; end?: number; frame?: number; progress?: number; type?: 'AUDIOFOCUS_GAIN' \| 'AUDIOFOCUS_LOSS' \| 'AUDIOFOCUS_LOSS_TRANSIENT' \| 'AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK'; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### requestFocus()

```typescript
requestFocus() => Promise<void>
```

--------------------


### abandonFocus()

```typescript
abandonFocus() => Promise<void>
```

--------------------


### Interfaces


#### TTSOptions

| Prop             | Type                                                                                                                                               | Description                                                                                                                                                                                                                            | Default          |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------- |
| **`text`**       | <code>string</code>                                                                                                                                | The text that will be synthesised when the utterance is spoken.                                                                                                                                                                        |                  |
| **`rate`**       | <code>number</code>                                                                                                                                | The speed at which the utterance will be spoken at.                                                                                                                                                                                    | <code>1.0</code> |
| **`pitch`**      | <code>number</code>                                                                                                                                | The pitch at which the utterance will be spoken at.                                                                                                                                                                                    | <code>1.0</code> |
| **`volume`**     | <code>number</code>                                                                                                                                | The volume that the utterance will be spoken at.                                                                                                                                                                                       | <code>1.0</code> |
| **`pan`**        | <code>number</code>                                                                                                                                | Parameter key to specify how the speech is panned from left to right when speaking text. Pan is specified as a float ranging from -1 to +1 where -1 maps to a hard-left pan, 0 to center (the default behavior), and +1 to hard-right. | <code>0.0</code> |
| **`voiceURI`**   | <code>number</code>                                                                                                                                | The index of the selected voice that will be used to speak the utterance. Possible voices can be queried using `getSupportedVoices`.                                                                                                   |                  |
| **`streamType`** | <code>'STREAM_ALARM' \| 'STREAM_DTMF' \| 'STREAM_MUSIC' \| 'STREAM_NOTIFICATION' \| 'STREAM_RING' \| 'STREAM_SYSTEM' \| 'STREAM_VOICE_CALL'</code> | Parameter key to specify the audio stream type to be used when speaking text or playing back a file. The value should be one of the STREAM_ constants defined in AudioManager.                                                         |                  |


#### TTSReadOptions

| Prop             | Type                                                                                                                                               | Description                                                                                                                                                                                                                            | Default          |
| ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------- |
| **`texts`**      | <code>string[]</code>                                                                                                                              | The text that will be synthesised when the utterance is spoken.                                                                                                                                                                        |                  |
| **`progress`**   | <code>number</code>                                                                                                                                |                                                                                                                                                                                                                                        |                  |
| **`rate`**       | <code>number</code>                                                                                                                                | The speed at which the utterance will be spoken at.                                                                                                                                                                                    | <code>1.0</code> |
| **`pitch`**      | <code>number</code>                                                                                                                                | The pitch at which the utterance will be spoken at.                                                                                                                                                                                    | <code>1.0</code> |
| **`volume`**     | <code>number</code>                                                                                                                                | The volume that the utterance will be spoken at.                                                                                                                                                                                       | <code>1.0</code> |
| **`pan`**        | <code>number</code>                                                                                                                                | Parameter key to specify how the speech is panned from left to right when speaking text. Pan is specified as a float ranging from -1 to +1 where -1 maps to a hard-left pan, 0 to center (the default behavior), and +1 to hard-right. | <code>0.0</code> |
| **`voiceURI`**   | <code>number</code>                                                                                                                                | The index of the selected voice that will be used to speak the utterance. Possible voices can be queried using `getSupportedVoices`.                                                                                                   |                  |
| **`streamType`** | <code>'STREAM_ALARM' \| 'STREAM_DTMF' \| 'STREAM_MUSIC' \| 'STREAM_NOTIFICATION' \| 'STREAM_RING' \| 'STREAM_SYSTEM' \| 'STREAM_VOICE_CALL'</code> | Parameter key to specify the audio stream type to be used when speaking text or playing back a file. The value should be one of the STREAM_ constants defined in AudioManager.                                                         |                  |
| **`bookId`**     | <code>number</code>                                                                                                                                | for my app only                                                                                                                                                                                                                        |                  |


#### SpeechSynthesisVoice

| Prop               | Type                 | Description                                                                                                 |
| ------------------ | -------------------- | ----------------------------------------------------------------------------------------------------------- |
| **`default`**      | <code>boolean</code> | Specifies whether the voice is the default voice for the current app (`true`) or not (`false`).             |
| **`lang`**         | <code>string</code>  | BCP 47 language tag indicating the language of the voice.                                                   |
| **`localService`** | <code>boolean</code> | Specifies whether the voice is supplied by a local (`true`) or remote (`false`) speech synthesizer service. |
| **`name`**         | <code>string</code>  | Human-readable name that represents the voice.                                                              |
| **`voiceURI`**     | <code>string</code>  | Type of URI and location of the speech synthesis service for this voice.                                    |


#### SpeechSynthesisEngine

| Prop        | Type                |
| ----------- | ------------------- |
| **`icon`**  | <code>number</code> |
| **`label`** | <code>string</code> |
| **`name`**  | <code>string</code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


### Type Aliases


#### PermissionState

<code>'prompt' | 'prompt-with-rationale' | 'granted' | 'denied'</code>

</docgen-api>
