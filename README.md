# react-native-sound

React Native module for playing sound clips on Android, built as a **TurboModule** for the React Native **New Architecture**.

> Android only. The legacy architecture, iOS and Windows are not supported by this branch.

## Requirements

- React Native >= 0.76 with the New Architecture enabled
- Android minSdk 24

## Feature matrix

Feature | Android
---|---
Load sound from the app bundle (`res/raw`) | ✓
Load sound from other directories | ✓
Load sound from the network | ✓
Play sound | ✓
Playback completion callback | ✓
Pause / resume | ✓
Stop | ✓
Reset | ✓
Release resource | ✓
Get duration | ✓
Get/set volume | ✓
Get/set system volume | ✓
Get/set loops | ✓
Get/set current time | ✓
Set speed | ✓
Audio focus (request / abandon / events) | ✓

## Installation

```sh
npm https://github.com/juanamd/react-native-sound
```

The module is autolinked. The native spec is processed by React Native codegen (see `codegenConfig` in `package.json`), so no manual setup is needed.

## Basic usage

Save your sound clip files under `android/app/src/main/res/raw`. File names must be lowercase and underscored (e.g. `my_file_name.mp3`) and subdirectories are not supported.

```js
import Sound from "react-native-sound";

const whoosh = new Sound();
whoosh.setErrorCallback(error => console.log("playback error", error.toString()));

// Load 'whoosh.mp3' from res/raw
await whoosh.load("whoosh.mp3");
console.log("duration in ms:", whoosh.duration);

// Play with an onEnd callback
await whoosh.play(() => console.log("finished playing"));

await whoosh.setVolume(0.5);
await whoosh.setNumberOfLoops(-1); // loop until stop() is called
await whoosh.setSpeed(1.5);
await whoosh.setCurrentMillis(2500);
console.log("at", await whoosh.getCurrentMillis());

await whoosh.pause();
await whoosh.stop();

// Release the native player when you are done
await whoosh.release();
```

Load from other locations:

```js
await sound.load("https://example.com/clip.mp3");   // network
await sound.load("clip.mp3", "/path/to/directory");  // file system
```

## Notes

- To minimize playback delay, preload a sound with `load()` during app initialization.
- You can play multiple sounds at the same time and reuse a `Sound` instance for multiple playbacks.
- The module wraps `android.media.MediaPlayer`. Supported formats: https://developer.android.com/guide/topics/media/media-formats.html
- `load()` rejects if the media cannot be loaded. The player is released automatically, so you can call `load()` again.
- `setErrorCallback` and the `onEnd` callback passed to `play` are native callbacks and can each be invoked only once per registration.
- Audio focus changes are delivered through `Sound.addAudioFocusListener(...)`, which returns a subscription with `remove()`.
