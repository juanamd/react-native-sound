declare module "react-native-sound" {
	export type Status = "unloaded" | "loading" | "loaded";
	export type FocusGain = "gain" | "gainTransient" | "gainTransientMayDuck" | "gainTransientExclusive";
	export type FocusLoss = "loss" | "lossTransient" | "lossTransientMayDuck";
	export type FocusEvent = "gain" | "loss" | "lossTransient" | "lossTransientMayDuck";
	export type Options = {
		useAlarmChannel?: boolean,
	};
	export type FocusOptions = {
		useAlarmChannel?: boolean,
		audioFocusType?: FocusGain,
	};

	export default class {
		static getSystemVolume(options?: Options): Promise<number>;
		static setSystemVolume(value: number, options?: Options): Promise<void>;
		static setVolumeControlStream(options?: Options): Promise<void>;
		static resetVolumeControlStream(): Promise<void>;
		static requestAudioFocus(options: FocusOptions): Promise<void | "granted" | "delayed" | "failed">;
		static addAudioFocusListener(onFocus: (focusType: FocusEvent) => void): Promise<void>;
		static removeAudioFocusListener(onFocus: (focusType: FocusEvent) => void): Promise<void>;
		static abandonAudioFocus(): Promise<void>;
		static setSystemMute(value: boolean): Promise<void>;
		static setEnabled(value: boolean): Promise<void>;
		static setActive(value: boolean): Promise<void>;
		static setMode(value: boolean): Promise<void>;
		static setCategory(value: string, mixWithOthers?: boolean): Promise<void>;
		static enableInSilenceMode(enabled: boolean): Promise<void>;

		status: Status;
		duration: number;
		numberOfChannels: number;
		numberOfLoops: number;
		volume: number;
		pan: number;
		speed: number;
		isLoaded: boolean;
		setErrorCallback(onError: (error: PlaybackError) => void): void;
		load(fileName: string, path?: string, options?: Options): Promise<void>;
		play(onEnd?: () => void): Promise<void>;
		pause(): Promise<void>;
		stop(): Promise<void>;
		reset(): Promise<void>;
		release(): Promise<void>;
		setVolume(value: number): Promise<void>;
		setPan(value: number): Promise<void>;
		setNumberOfLoops(value: number): Promise<void>;
		setSpeed(value: number): Promise<void>;
		getCurrentMillis(): Promise<number>;
		setCurrentMillis(ms: number): Promise<void>;
		setSpeakerphoneOn(value: boolean): Promise<void>;
		isPlaying(): Promise<boolean>;
	}

	export class PlaybackError {
		what: number;
		extra: number;
		toString(): string;
	}
	
	export const MAIN_BUNDLE_PATH: string;
	export const DOCUMENT_PATH: string;
	export const LIBRARY_PATH: string;
	export const CACHES_PATH: string;
}