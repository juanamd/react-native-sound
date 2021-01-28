declare module "react-native-sound" {
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
		static async getSystemVolume(options: Options = {}): Promise<number>;
		static async setSystemVolume(value: number, options: Options = {}): Promise<void>;
		static async setVolumeControlStream(options: Options = {}): Promise<void>;
		static async resetVolumeControlStream(): Promise<void>;
		static async requestAudioFocus(options: FocusOptions): Promise<void | "granted" | "delayed" | "failed">;
		static async addAudioFocusListener(onFocus: (focusType: FocusEvent) => void): Promise<void>;
		static async removeAudioFocusListener(onFocus: (focusType: FocusEvent) => void): Promise<void>;
		static async abandonAudioFocus(): Promise<void>;
		static async setSystemMute(value: boolean): Promise<void>;
		static async setEnabled(value: boolean): Promise<void>;
		static async setActive(value: boolean): Promise<void>;
		static async setMode(value: boolean): Promise<void>;
		static async setCategory(value: string, mixWithOthers: boolean = false): Promise<void>;
		static async enableInSilenceMode(enabled: boolean): Promise<void>;

		isLoaded: boolean;
		setErrorCallback(onError: (error: PlaybackError) => void): void;
		async load(fileName: string, path?: string, options: Options = {}): Promise<void>;
		async play(onEnd?: () => void): Promise<void>;
		async pause(): Promise<void>;
		async stop(): Promise<void>;
		async reset(): Promise<void>;
		async release(): Promise<void>;
		async setVolume(value: number): Promise<void>;
		async setPan(value: number): Promise<void>;
		async setNumberOfLoops(value: number): Promise<void>;
		async setSpeed(value: number): Promise<void>;
		async getCurrentMillis(): Promise<number>;
		async setCurrentMillis(ms: number): Promise<void>;
		async setSpeakerphoneOn(value: boolean): Promise<void>;
		async isPlaying(): Promise<boolean>;
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