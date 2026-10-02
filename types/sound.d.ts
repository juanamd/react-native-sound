declare module "react-native-sound" {
	export type Status = "unloaded" | "loading" | "loaded";
	export type FocusGain = "gain" | "gainTransient" | "gainTransientMayDuck" | "gainTransientExclusive";
	export type FocusLoss = "loss" | "lossTransient" | "lossTransientMayDuck";
	export type FocusEvent = "gain" | "loss" | "lossTransient" | "lossTransientCanDuck";
	export type Options = {
		useAlarmChannel?: boolean,
	};
	export type FocusOptions = {
		useAlarmChannel?: boolean,
		audioFocusType?: FocusGain,
	};

	export type FocusEventSubscription = { remove: () => void };

	export default class {
		static getSystemVolume(options?: Options): Promise<number>;
		static setSystemVolume(value: number, options?: Options): Promise<void>;
		static setVolumeControlStream(options?: Options): Promise<void>;
		static resetVolumeControlStream(): Promise<void>;
		static requestAudioFocus(options: FocusOptions): Promise<"granted" | "delayed" | "failed">;
		static addAudioFocusListener(onFocus: (focusType: FocusEvent) => void): FocusEventSubscription;
		static removeAudioFocusListener(onFocus: (focusType: FocusEvent) => void): void;
		static abandonAudioFocus(): Promise<void>;
		static setSystemMute(value: boolean): Promise<void>;
		static getCurrentInterruptionFilter(): Promise<"unknown" | "all" | "priority" | "none" | "alarms">;

		status: Status;
		duration: number;
		numberOfLoops: number;
		volume: number;
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
}
