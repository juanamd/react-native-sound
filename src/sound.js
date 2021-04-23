// @flow
// $FlowFixMe
import { NativeModules, NativeEventEmitter } from "react-native";
// $FlowFixMe
import resolveAssetSource from "react-native/Libraries/Image/resolveAssetSource";

const RNSound = NativeModules.RNSound;
const IS_ANDROID = RNSound.IsAndroid;
const IS_WINDOWS = RNSound.IsWindows;
const eventEmitter = new NativeEventEmitter(RNSound);
const AUDIO_FOCUS_EVENT = "audio_focus_event";

const isAbsolutePath = (path: string) => /^(\/|http(s?)|asset)/.test(path);

const isBundledFile = (fileName: string) => IS_ANDROID && !isAbsolutePath(fileName);

const parseBundledFileName = (fileName: string) => fileName.toLowerCase().replace(/\.[^.]+$/, "");

const parseDataSource = (fileName: string, path?: string) => {
	const asset = resolveAssetSource(fileName);
	if (asset) return asset.uri;
	if (!path && isBundledFile(fileName)) return parseBundledFileName(fileName);
	if (path) return `${path}/${fileName}`;
	return fileName;
};

let keyCounter = 0;

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

class Sound {
	static async getSystemVolume(options: Options = {}): Promise<number | void> {
		if (IS_ANDROID) return await RNSound.getSystemVolume(options);
	}
	
	static async setSystemVolume(value: number, options: Options = {}) {
		if (value < 0) value = 0;
		else if (value > 1) value = 1;
		if (IS_ANDROID) await RNSound.setSystemVolume(value, options);
	}

	static async setVolumeControlStream(options: Options = {}) {
		if (IS_ANDROID) await RNSound.setVolumeControlStream(options);
	}

	static async resetVolumeControlStream() {
		if (IS_ANDROID) await RNSound.resetVolumeControlStream();
	}

	static async requestAudioFocus(options: FocusOptions): Promise<void | "granted" | "delayed" | "failed"> {
		if (IS_ANDROID) return await RNSound.requestAudioFocus(options);
	}

	static async addAudioFocusListener(onFocus: (focusType: FocusEvent) => any) {
		if (IS_ANDROID) eventEmitter.addListener(AUDIO_FOCUS_EVENT, onFocus);
	}

	static async removeAudioFocusListener(onFocus: (focusType: FocusEvent) => any) {
		if (IS_ANDROID) eventEmitter.removeListener(AUDIO_FOCUS_EVENT, onFocus);
	}

	static async abandonAudioFocus() {
		if (IS_ANDROID) await RNSound.abandonAudioFocus();
	}

	static async setSystemMute(value: boolean) {
		if (IS_ANDROID) await RNSound.setMute(value);
	}

	static async setEnabled(value: boolean) {
		if (!IS_ANDROID && !IS_WINDOWS) await RNSound.enable(value);
	}
	
	static async setActive(value: boolean) {
		if (!IS_ANDROID && !IS_WINDOWS) await RNSound.setActive(value);
	}

	static async setMode(value: boolean) {
		if (!IS_ANDROID && !IS_WINDOWS) await RNSound.setMode(value);
	}
	
	static async setCategory(value: string, mixWithOthers: boolean = false) {
		if (!IS_ANDROID && !IS_WINDOWS) await RNSound.setCategory(value, mixWithOthers);
	}
	
	static async enableInSilenceMode(enabled: boolean) {
		if (!IS_ANDROID && !IS_WINDOWS) await RNSound.enableInSilenceMode(enabled);
	}

	static async getCurrentInterruptionFilter() {
		if (!IS_ANDROID) return "unknown";
		const filterStatus = await RNSound.getCurrentInterruptionFilter();
		if (filterStatus === 0) return "unknown";
		if (filterStatus === 1) return "all";
		if (filterStatus === 2) return "priority";
		if (filterStatus === 3) return "none";
		if (filterStatus === 4) return "alarms";
		return "unknown";
	}

	status: Status;
	key: number;
	duration: number;
	numberOfChannels: number;
	numberOfLoops: number;
	volume: number;
	pan: number;
	speed: number;

	constructor() {
		this.key = ++keyCounter;
		this._initialize();
	}

	_initialize() {
		this.status = "unloaded";
		this.duration = -1;
		this.numberOfChannels = -1;
		this.numberOfLoops = 0;
		this.volume = 1;
		this.pan = 0;
		this.speed = 1;
	}

	get isLoaded() {
		return this.status === "loaded";
	}

	setErrorCallback(onError: (error: PlaybackError) => void) {
		if (IS_ANDROID) RNSound.setErrorCallback(this.key, errorData => onError(new PlaybackError(errorData)));
	}

	async load(fileName: string, path?: string, options: Options = {}) {
		if (this.status !== "unloaded") return false;
		this._initialize();
		this.status = "loading";
		const dataSource = parseDataSource(fileName, path);
		const { duration, numberOfChannels } = await RNSound.load(this.key, dataSource, options);
		if (duration) this.duration = duration;
		if (numberOfChannels) this.numberOfChannels = numberOfChannels;
		this.status = "loaded";
		return true;
	}

	async play(onEnd?: () => void) {
		if (this.isLoaded) {
			if (onEnd && IS_ANDROID) RNSound.setOnCompletionListener(this.key, onEnd);
			await RNSound.play(this.key);
			return true;
		} else {
			return false;
		}
	}

	async pause() {
		if (this.isLoaded) await RNSound.pause(this.key);
	}

	async stop() {
		if (this.isLoaded) await RNSound.stop(this.key);
	}

	async reset() {
		if (this.isLoaded && IS_ANDROID) await RNSound.reset(this.key);
	}

	async release() {
		if (this.status !== "unloaded") await RNSound.release(this.key);
		this.status = "unloaded";
	}

	async setVolume(value: number) {
		this.volume = value;
		if (this.isLoaded) {
			if (IS_ANDROID || IS_WINDOWS) await RNSound.setVolume(this.key, value, value);
			else await RNSound.setVolume(this.key, value);
		}
	}

	async setPan(value: number) {
		this.pan = value;
		if (this.isLoaded) await RNSound.setPan(this.key, this.pan);
	}

	async setNumberOfLoops(value: number) {
		this.numberOfLoops = value;
		if (this.isLoaded) {
			if (IS_ANDROID || IS_WINDOWS) await RNSound.setLooping(this.key, !!value);
			else await RNSound.setNumberOfLoops(this.key, value);
		}
	}

	async setSpeed(value: number) {
		this.speed = value;
		if (!IS_WINDOWS && this.isLoaded) await RNSound.setSpeed(this.key, value);
	}

	async getCurrentMillis(): Promise<number> {
		if (this.isLoaded) return await RNSound.getCurrentMillis(this.key);
		return -1;
	}
	
	async setCurrentMillis(ms: number) {
		if (this.isLoaded) await RNSound.setCurrentMillis(this.key, ms);
	}

	async setSpeakerphoneOn(value: boolean) {
		if (IS_ANDROID) await RNSound.setSpeakerphoneOn(this.key, value);
	}

	async isPlaying(): Promise<boolean> {
		if (this.isLoaded) return await RNSound.isPlaying(this.key);
		return false;
	}
}

export default Sound;

export class PlaybackError {
	what: number;
	extra: number;

	constructor(errorData: { what: number, extra: number }) {
		this.what = errorData.what;
		this.extra = errorData.extra;
	}

	toString() {
		return `What: ${this.what}, Extra: ${this.extra}`;
	}
}

export const MAIN_BUNDLE_PATH = RNSound.MainBundlePath;
export const DOCUMENT_PATH = RNSound.NSDocumentDirectory;
export const LIBRARY_PATH = RNSound.NSLibraryDirectory;
export const CACHES_PATH = RNSound.NSCachesDirectory;
