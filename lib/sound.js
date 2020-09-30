// $FlowFixMe
import { NativeModules, NativeEventEmitter } from "react-native"; // $FlowFixMe

import resolveAssetSource from "react-native/Libraries/Image/resolveAssetSource";
const RNSound = NativeModules.RNSound;
const IS_ANDROID = RNSound.IsAndroid;
const IS_WINDOWS = RNSound.IsWindows;
const eventEmitter = new NativeEventEmitter(RNSound);
const AUDIO_FOCUS_EVENT = "audio_focus_event";

const isAbsolutePath = path => /^(\/|http(s?)|asset)/.test(path);

const isBundledFile = fileName => IS_ANDROID && !isAbsolutePath(fileName);

const parseBundledFileName = fileName => fileName.toLowerCase().replace(/\.[^.]+$/, "");

const parseDataSource = (fileName, path) => {
  const asset = resolveAssetSource(fileName);
  if (asset) return asset.uri;
  if (!path && isBundledFile(fileName)) return parseBundledFileName(fileName);
  if (path) return `${path}/${fileName}`;
  return fileName;
};

let keyCounter = 0;

class Sound {
  static async getSystemVolume(options = {}) {
    if (IS_ANDROID) return await RNSound.getSystemVolume(options);
  }

  static async setSystemVolume(value, options = {}) {
    if (value < 0) value = 0;else if (value > 1) value = 1;
    if (IS_ANDROID) await RNSound.setSystemVolume(value, options);
  }

  static async setVolumeControlStream(options = {}) {
    if (IS_ANDROID) await RNSound.setVolumeControlStream(options);
  }

  static async resetVolumeControlStream() {
    if (IS_ANDROID) await RNSound.resetVolumeControlStream();
  }

  static async requestAudioFocus(options) {
    if (IS_ANDROID) return await RNSound.requestAudioFocus(options);
  }

  static async addAudioFocusListener(onFocus) {
    if (IS_ANDROID) eventEmitter.addListener(AUDIO_FOCUS_EVENT, onFocus);
  }

  static async removeAudioFocusListener(onFocus) {
    if (IS_ANDROID) eventEmitter.removeListener(AUDIO_FOCUS_EVENT, onFocus);
  }

  static async abandonAudioFocus() {
    if (IS_ANDROID) await RNSound.abandonAudioFocus();
  }

  static async setSystemMute(value) {
    if (IS_ANDROID) await RNSound.setMute(value);
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

  setErrorCallback(onError) {
    if (IS_ANDROID) RNSound.setErrorCallback(this.key, errorData => onError(new PlaybackError(errorData)));
  }

  async load(fileName, path, options = {}) {
    if (this.status !== "unloaded") return false;

    this._initialize();

    this.status = "loading";
    const dataSource = parseDataSource(fileName, path);
    const {
      duration,
      numberOfChannels
    } = await RNSound.load(this.key, dataSource, options);
    if (duration) this.duration = duration;
    if (numberOfChannels) this.numberOfChannels = numberOfChannels;
    this.status = "loaded";
    return true;
  }

  async play(onEnd) {
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

  async setVolume(value) {
    this.volume = value;

    if (this.isLoaded) {
      if (IS_ANDROID || IS_WINDOWS) await RNSound.setVolume(this.key, value, value);else await RNSound.setVolume(this.key, value);
    }
  }

  async setPan(value) {
    this.pan = value;
    if (this.isLoaded) await RNSound.setPan(this.key, this.pan);
  }

  async setNumberOfLoops(value) {
    this.numberOfLoops = value;

    if (this.isLoaded) {
      if (IS_ANDROID || IS_WINDOWS) await RNSound.setLooping(this.key, !!value);else await RNSound.setNumberOfLoops(this.key, value);
    }
  }

  async setSpeed(value) {
    this.speed = value;
    if (!IS_WINDOWS && this.isLoaded) await RNSound.setSpeed(this.key, value);
  }

  async getCurrentMillis() {
    if (this.isLoaded) return await RNSound.getCurrentMillis(this.key);
    return -1;
  }

  async setCurrentMillis(ms) {
    if (this.isLoaded) await RNSound.setCurrentMillis(this.key, ms);
  }

  async setSpeakerphoneOn(value) {
    if (IS_ANDROID) await RNSound.setSpeakerphoneOn(this.key, value);
  }

  async isPlaying() {
    if (this.isLoaded) return await RNSound.isPlaying(this.key);
    return false;
  }

}

export default Sound;
export class PlaybackError {
  constructor(errorData) {
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