import { NativeModules } from "react-native";
import resolveAssetSource from "react-native/Libraries/Image/resolveAssetSource";

const RNSound = NativeModules.RNSound;
const IS_ANDROID = RNSound.IsAndroid;
const IS_WINDOWS = RNSound.IsWindows;

const isNumber = (value) => value && typeof value === "number";

const isRelativePath = (path) => !/^(\/|http(s?)|asset)/.test(path);

const isBundledFile = (fileName) => IS_ANDROID && isRelativePath(fileName);

const parseBundledFileName = (fileName) => fileName.toLowerCase().replace(/\.[^.]+$/, "");

const parseDataSource = (fileName, path) => {
	const asset = resolveAssetSource(fileName);
	if (asset) return asset.uri;
	if (!path && isBundledFile(fileName)) return parseBundledFileName(fileName);
	if (path) return `${path}/${fileName}`;
	return fileName;
};

let keyCounter = 0;

class Sound {
	static getSystemVolume(callback) {
		if (IS_ANDROID) RNSound.getSystemVolume(callback);
	}
	
	static setSystemVolume(value) {
		if (IS_ANDROID) RNSound.setSystemVolume(value);
	}

	static setSystemMute(value) {
		if (IS_ANDROID) RNSound.setMute(value);
	}

	static setEnabled(value) {
		if (!IS_ANDROID && !IS_WINDOWS) RNSound.enable(value);
	}
	
	static setActive(value) {
		if (!IS_ANDROID && !IS_WINDOWS) RNSound.setActive(value);
	}

	static setMode(value) {
		if (!IS_ANDROID && !IS_WINDOWS) RNSound.setMode(value);
	}
	
	static setCategory(value, mixWithOthers = false) {
		if (!IS_ANDROID && !IS_WINDOWS) RNSound.setCategory(value, mixWithOthers);
	}
	
	static enableInSilenceMode(enabled) {
		if (!IS_ANDROID && !IS_WINDOWS) RNSound.enableInSilenceMode(enabled);
	}

	constructor() {
		this.key = ++keyCounter;
		this.initialize();
	}

	initialize() {
		this.isLoading = false;
		this.isLoaded = false;
		this.duration = -1;
		this.numberOfChannels = -1;
		this.numberOfLoops = 0;
		this.volume = 1;
		this.pan = 0;
		this.speed = 1;
	}

	load(fileName, path, onSuccess, onError, options = {}) {
		if (this.isLoading || this.isLoaded) return;
		this.initialize();
		this.isLoading = true;
		const dataSource = parseDataSource(fileName, path);
		RNSound.load(dataSource, this.key, options, (response) => {
			this.parseLoadResponse(response);
			if (onSuccess) onSuccess(response);
		}, onError);
	}

	parseLoadResponse(response) {
		if (response) {
			if (isNumber(response.duration)) this.duration = response.duration;
			if (isNumber(response.numberOfChannels)) this.numberOfChannels = response.numberOfChannels;
		}
		this.isLoading = false;
		this.isLoaded = true;
	}

	play(onEnd) {
		if (this.isLoaded) RNSound.play(this.key, (success) => onEnd && onEnd(success));
		else if (onEnd) onEnd(false);
		return this;
	}

	pause(callback) {
		if (this.isLoaded) RNSound.pause(this.key, () => callback && callback());
		return this;
	}

	stop(callback) {
		if (this.isLoaded) RNSound.stop(this.key, () => callback && callback());
		return this;
	}

	reset() {
		if (this.isLoaded && IS_ANDROID) RNSound.reset(this.key);
		return this;
	}

	release() {
		if (this.isLoading || this.isLoaded) RNSound.release(this.key);
		this.isLoading = false;
		this.isLoaded = false;
		return this;
	}

	setVolume(value) {
		this.volume = value;
		if (this.isLoaded) {
			if (IS_ANDROID || IS_WINDOWS) RNSound.setVolume(this.key, value, value);
			else RNSound.setVolume(this.key, value);
		}
		return this;
	}

	setPan(value) {
		this.pan = value;
		if (this.isLoaded) RNSound.setPan(this.key, this.pan);
		return this;
	}

	setNumberOfLoops(value) {
		this.numberOfLoops = value;
		if (this.isLoaded) {
			if (IS_ANDROID || IS_WINDOWS) RNSound.setLooping(this.key, !!value);
			else RNSound.setNumberOfLoops(this.key, value);
		}
		return this;
	}

	setSpeed(value) {
		this.speed = value;
		if (!IS_WINDOWS && this.isLoaded) RNSound.setSpeed(this.key, value);
		return this;
	}

	getCurrentTime(callback) {
		if (this.isLoaded) RNSound.getCurrentTime(this.key, callback);
	}
	
	setCurrentTime(value) {
		if (this.isLoaded) RNSound.setCurrentTime(this.key, value);
		return this;
	}

	setSpeakerphoneOn(value) {
		if (IS_ANDROID) RNSound.setSpeakerphoneOn(this.key, value);
	}
}

export default Sound;

export const MAIN_BUNDLE_PATH = RNSound.MainBundlePath;
export const DOCUMENT_PATH = RNSound.NSDocumentDirectory;
export const LIBRARY_PATH = RNSound.NSLibraryDirectory;
export const CACHES_PATH = RNSound.NSCachesDirectory;
