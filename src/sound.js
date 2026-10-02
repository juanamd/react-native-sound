// @flow
// $FlowFixMe
import { NativeEventEmitter, Image } from "react-native";
import RNSound from "./NativeRNSound";

const eventEmitter = new NativeEventEmitter(RNSound);
const AUDIO_FOCUS_EVENT = "audio_focus_event";

// Subscriptions are tracked per listener so removeAudioFocusListener() keeps working
// on React Native versions where EventEmitter.removeListener() no longer exists.
const audioFocusSubscriptions: Map<Function, Array<{ remove: () => void }>> = new Map();

const isAbsolutePath = (path: string) => /^(\/|http(s?)|asset)/.test(path);

const isBundledFile = (fileName: string) => !isAbsolutePath(fileName);

const parseBundledFileName = (fileName: string) => fileName.toLowerCase().replace(/\.[^.]+$/, "");

const parseDataSource = (fileName: string, path?: string) => {
	const asset = Image.resolveAssetSource(fileName);
	if (asset) return asset.uri;
	if (!path && isBundledFile(fileName)) return parseBundledFileName(fileName);
	if (path) return `${path}/${fileName}`;
	return fileName;
};

let keyCounter = 0;

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

class Sound {
	static async getSystemVolume(options: Options = {}): Promise<number> {
		return await RNSound.getSystemVolume(options);
	}

	static async setSystemVolume(value: number, options: Options = {}) {
		if (value < 0) value = 0;
		else if (value > 1) value = 1;
		await RNSound.setSystemVolume(value, options);
	}

	static async setVolumeControlStream(options: Options = {}) {
		await RNSound.setVolumeControlStream(options);
	}

	static async resetVolumeControlStream() {
		await RNSound.resetVolumeControlStream();
	}

	static async requestAudioFocus(options: FocusOptions): Promise<"granted" | "delayed" | "failed"> {
		return (await RNSound.requestAudioFocus(options): any);
	}

	static addAudioFocusListener(onFocus: (focusType: FocusEvent) => any) {
		const subscription = eventEmitter.addListener(AUDIO_FOCUS_EVENT, onFocus);
		const subscriptions = audioFocusSubscriptions.get(onFocus) || [];
		subscriptions.push(subscription);
		audioFocusSubscriptions.set(onFocus, subscriptions);
		return subscription;
	}

	static removeAudioFocusListener(onFocus: (focusType: FocusEvent) => any) {
		const subscriptions = audioFocusSubscriptions.get(onFocus);
		if (!subscriptions || subscriptions.length === 0) return;
		const subscription = subscriptions.pop();
		subscription.remove();
		if (subscriptions.length === 0) audioFocusSubscriptions.delete(onFocus);
	}

	static async abandonAudioFocus() {
		await RNSound.abandonAudioFocus();
	}

	static async setSystemMute(value: boolean) {
		await RNSound.setMute(value);
	}

	static async getCurrentInterruptionFilter() {
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
	numberOfLoops: number;
	volume: number;
	speed: number;

	constructor() {
		this.key = ++keyCounter;
		this._initialize();
	}

	_initialize() {
		this.status = "unloaded";
		this.duration = -1;
		this.numberOfLoops = 0;
		this.volume = 1;
		this.speed = 1;
	}

	get isLoaded() {
		return this.status === "loaded";
	}

	setErrorCallback(onError: (error: PlaybackError) => void) {
		RNSound.setErrorCallback(this.key, errorData => onError(new PlaybackError(errorData)));
	}

	async load(fileName: string, path?: string, options: Options = {}) {
		if (this.status !== "unloaded") return false;
		this._initialize();
		this.status = "loading";
		try {
			const dataSource = parseDataSource(fileName, path);
			const { duration } = await RNSound.load(this.key, dataSource, options);
			if (duration) this.duration = duration;
		} catch (error) {
			// Free the native player created for this key so load() can be retried
			await RNSound.release(this.key).catch(() => {});
			this.status = "unloaded";
			throw error;
		}
		this.status = "loaded";
		return true;
	}

	async play(onEnd?: () => void) {
		if (this.isLoaded) {
			if (onEnd) RNSound.setOnCompletionListener(this.key, onEnd);
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
		if (this.isLoaded) await RNSound.reset(this.key);
	}

	async release() {
		if (this.status !== "unloaded") await RNSound.release(this.key);
		this.status = "unloaded";
	}

	async setVolume(value: number) {
		this.volume = value;
		if (this.isLoaded) await RNSound.setVolume(this.key, value, value);
	}

	async setNumberOfLoops(value: number) {
		this.numberOfLoops = value;
		if (this.isLoaded) await RNSound.setLooping(this.key, !!value);
	}

	async setSpeed(value: number) {
		this.speed = value;
		if (this.isLoaded) await RNSound.setSpeed(this.key, value);
	}

	async getCurrentMillis(): Promise<number> {
		if (this.isLoaded) return await RNSound.getCurrentMillis(this.key);
		return -1;
	}

	async setCurrentMillis(ms: number) {
		if (this.isLoaded) await RNSound.setCurrentMillis(this.key, ms);
	}

	async setSpeakerphoneOn(value: boolean) {
		await RNSound.setSpeakerphoneOn(this.key, value);
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
