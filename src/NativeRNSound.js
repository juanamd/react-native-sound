// @flow
// $FlowFixMe
import type { TurboModule } from "react-native";
// $FlowFixMe
import { TurboModuleRegistry } from "react-native";

type Options = $ReadOnly<{
	useAlarmChannel?: boolean,
}>;

type FocusOptions = $ReadOnly<{
	useAlarmChannel?: boolean,
	audioFocusType?: string,
}>;

type PlaybackErrorData = {
	what: number,
	extra: number,
};

type LoadResult = $ReadOnly<{
	duration: number,
}>;

/**
 * Codegen spec for the Android RNSound TurboModule.
 *
 * Notes:
 * - Every `number` here becomes a Java `double`, so the player key arrives as a double.
 * - Callbacks (setErrorCallback / setOnCompletionListener) can only be invoked once per registration.
 */
export interface Spec extends TurboModule {
	// Player lifecycle
	+setErrorCallback: (key: number, onError: (error: PlaybackErrorData) => void) => void;
	+load: (key: number, dataSource: string, options: Options) => Promise<LoadResult>;
	+setOnCompletionListener: (key: number, onComplete: () => void) => void;
	+play: (key: number) => Promise<void>;
	+pause: (key: number) => Promise<void>;
	+stop: (key: number) => Promise<void>;
	+reset: (key: number) => Promise<void>;
	+release: (key: number) => Promise<void>;

	// Player properties
	+setVolume: (key: number, left: number, right: number) => Promise<void>;
	+setLooping: (key: number, looping: boolean) => Promise<void>;
	+setSpeed: (key: number, speed: number) => Promise<void>;
	+setCurrentMillis: (key: number, ms: number) => Promise<void>;
	+getCurrentMillis: (key: number) => Promise<number>;
	+isPlaying: (key: number) => Promise<boolean>;
	+setSpeakerphoneOn: (key: number, speaker: boolean) => Promise<void>;

	// System audio
	+getSystemVolume: (options: Options) => Promise<number>;
	+setSystemVolume: (value: number, options: Options) => Promise<void>;
	+setVolumeControlStream: (options: Options) => Promise<void>;
	+resetVolumeControlStream: () => Promise<void>;
	+setMute: (isMute: boolean) => Promise<void>;
	+getCurrentInterruptionFilter: () => Promise<number>;

	// Audio focus
	+requestAudioFocus: (options: FocusOptions) => Promise<string>;
	+abandonAudioFocus: () => Promise<void>;

	// Required by NativeEventEmitter (audio focus events)
	+addListener: (eventName: string) => void;
	+removeListeners: (count: number) => void;
}

export default (TurboModuleRegistry.getEnforcing<Spec>("RNSound"): Spec);
