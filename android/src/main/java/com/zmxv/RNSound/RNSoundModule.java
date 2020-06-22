package com.zmxv.RNSound;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RNSoundModule extends ReactContextBaseJavaModule {

	private static final String TAG = "RNSoundModule";

	private ReactApplicationContext context;
	private Map<Integer, MediaPlayer> playerPool = new HashMap<>();
	private Map<Integer, Callback> errorCallbackPool = new HashMap<>();
	private AudioFocusRequest focusRequest;
	private OnAudioFocusChangeListener afChangeListener;
	private Callback onAudioFocus;

	public RNSoundModule(ReactApplicationContext context) {
		super(context);
		this.context = context;
		Log.d(TAG, "Initialized");
	}

	@ReactMethod
	public void setErrorCallback(final Integer key, final Callback onError) {
		try {
			this.errorCallbackPool.put(key, onError);
			Log.d(TAG, key + " - Added error callback");
		} catch (Exception e) {
			Log.e(TAG,  key + " - Error on setErrorCallback()", e);
		}
	}

	@ReactMethod
	public void load(final Integer key, final String dataSource, final ReadableMap options, final Promise promise) {
		Log.d(TAG, key + " - Loading " + dataSource + " ...");
		try {
			MediaPlayer player = new MediaPlayer();
			this.playerPool.put(key, player);
			player.setOnErrorListener(this.createOnErrorListener(key));
			player.setOnPreparedListener(this.createOnPreparedListener(key, promise));
			this.applyAudioOptions(player, options);
			this.setMediaPlayerDataSource(player, dataSource);
			player.prepareAsync();
			Log.d(TAG, key + " - Load complete. Waiting for onPrepared...");
		} catch (Exception e) {
			Log.e(TAG,  key + " - Error on load()", e);
			promise.reject(e);
		}
	}

	private OnErrorListener createOnErrorListener(final Integer key) {
		return new OnErrorListener() {
			@Override
			public synchronized boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
				Log.e(TAG, key + " - Error. What: " + what + " extra: " + extra);
				WritableMap errorMap = Arguments.createMap();
				errorMap.putInt("what", what);
				errorMap.putInt("extra", extra);
				try {
					Callback onError = errorCallbackPool.get(key);
					if (onError != null) onError.invoke(errorMap);
					else Log.e(TAG, "OnErrorListener(): no on errror callback found for key " + key);
				} catch (Exception e) {
					Log.e(TAG, "OnErrorListener(): onError callback already dispatched!", e);
				}
				return true; //Return true if the error has been handled
			}
		};
	}

	private OnPreparedListener createOnPreparedListener(final Integer key, final Promise promise) {
		return new OnPreparedListener() {
			@Override
			public synchronized void onPrepared(MediaPlayer mediaPlayer) {
				try {
					WritableMap map = Arguments.createMap();
					map.putInt("duration", mediaPlayer.getDuration());
					promise.resolve(map);
					Log.d(TAG, key + " - Prepared completed!");
				} catch (Exception e) {
					Log.e(TAG, key + " - Error on OnPreparedListener()", e);
					promise.reject(e);
				}
			}
		};
	}

	private void applyAudioOptions(final MediaPlayer mediaPlayer, final ReadableMap options) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			mediaPlayer.setAudioStreamType(getAudioStreamType(options));
		} else {
			mediaPlayer.setAudioAttributes((AudioAttributes) getAudioAttributes(options));
		}
	}

	private Object getAudioAttributes(final ReadableMap options) {
		return new AudioAttributes.Builder()
			.setUsage(useAlarmChannel(options) ? AudioAttributes.USAGE_ALARM : AudioAttributes.USAGE_MEDIA)
			.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
			.build();
	}

	private boolean useAlarmChannel(final ReadableMap options) {
		return options.hasKey("useAlarmChannel") ? options.getBoolean("useAlarmChannel") : false;
	}

	private int getAudioStreamType(final ReadableMap options) {
		if (useAlarmChannel(options)) return AudioManager.STREAM_ALARM;
		return AudioManager.STREAM_MUSIC;
	}

	private void setMediaPlayerDataSource(final MediaPlayer mediaPlayer, final String dataSource) throws Exception {
		if (this.isBundledResource(dataSource)) this.setDataSourceFromUri(mediaPlayer, dataSource);
		else if (dataSource.startsWith("asset:/")) this.setDataSourceFromAsset(mediaPlayer, dataSource);
		else if (dataSource.matches("^(https?)://.*$")) this.setDataSourceFromNetwork(mediaPlayer, dataSource);
		else this.setDataSourceFromFile(mediaPlayer, dataSource);
	}

	private boolean isBundledResource(final String fileName) {
		int resId = this.context.getResources().getIdentifier(fileName, "raw", this.context.getPackageName());
		return resId != 0;
	}

	private void setDataSourceFromUri(final MediaPlayer mediaPlayer, final String fileName) throws Exception {
		Uri uri = Uri.parse("android.resource://" + this.context.getPackageName() + "/raw/" + fileName);
		mediaPlayer.setDataSource(this.context, uri);
	}

	private void setDataSourceFromAsset(final MediaPlayer mediaPlayer, final String fileName) throws Exception {
		AssetFileDescriptor desc = this.context.getAssets().openFd(fileName.replace("asset:/", ""));
		mediaPlayer.setDataSource(desc.getFileDescriptor(), desc.getStartOffset(), desc.getLength());
		desc.close();
	}

	private void setDataSourceFromNetwork(final MediaPlayer mediaPlayer, final String url) throws Exception {
		mediaPlayer.setDataSource(url);
	}

	private void setDataSourceFromFile(final MediaPlayer mediaPlayer, final String fileName) throws Exception {
		File file = new File(fileName);
		if (file.exists()) {
			Uri uri = Uri.fromFile(file);
			mediaPlayer.setDataSource(this.context, uri);
		} else {
			throw new Exception("File does not exist with name: " + fileName);
		}
	}

	@ReactMethod
	public void setOnCompletionListener(final Integer key, final Callback onComplete) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null && !player.isPlaying()) {
				player.setOnCompletionListener(this.createOnCompletionListener(key, onComplete));
			}
			Log.d(TAG, key + " - Set OnCompletion listener");
		} catch (Exception e) {
			Log.e(TAG, "Error on setOnCompletionListener()", e);
		}
	}

	private OnCompletionListener createOnCompletionListener(final Integer key, final Callback callback) {
		return new OnCompletionListener() {
			@Override
			public synchronized void onCompletion(MediaPlayer mediaPlayer) {
				if (!mediaPlayer.isLooping()) {
					try {
						callback.invoke();
						Log.d(TAG, key + " - Playing complete!");
					} catch (Exception e) {
						Log.e(TAG, "The on completion callback was already invoked", e);
					}
				}
			}
		};
	}

	@ReactMethod
	public void play(final Integer key, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player == null) {
				promise.reject(new Exception("Undefined player"));
				return;
			} else if (!player.isPlaying()) {
				player.start();
			}
			promise.resolve(null);
			Log.d(TAG, key + " - Started playing...");
		} catch (Exception e) {
			Log.e(TAG, "Error on play()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void pause(final Integer key, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null && player.isPlaying()) player.pause();
			promise.resolve(null);
			Log.d(TAG, key + " - Paused");
		} catch (Exception e) {
			Log.e(TAG, "Error on pause()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void stop(final Integer key, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null && player.isPlaying()) {
				player.pause();
				player.seekTo(0);
			}
			Log.d(TAG, key + " - Stopped");
			promise.resolve(null);
		} catch (Exception e) {
			Log.e(TAG, "Error on stop()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void reset(final Integer key, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null) player.reset();
			promise.resolve(null);
			Log.d(TAG, key + " - Resetted");
		} catch (Exception e) {
			Log.e(TAG, "Error on reset()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void release(final Integer key, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null) {
				player.setOnCompletionListener(null);
				player.setOnPreparedListener(null);
				player.setOnErrorListener(null);
				player.release();
				this.playerPool.remove(key);
			}
			promise.resolve(null);
			Log.d(TAG, key + " - Released!");
		} catch (Exception e) {
			Log.e(TAG, "Error on release()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void setVolume(final Integer key, final Float left, final Float right, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null) player.setVolume(left, right);
			promise.resolve(null);
			Log.d(TAG, key + " - Set volume - Left: " + left.toString() + ", Right: " + right.toString());
		} catch (Exception e) {
			Log.e(TAG, "Error on setVolume()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void getSystemVolume(final ReadableMap options, final Promise promise) {
		try {
			AudioManager audio = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
			int channel = this.getAudioStreamType(options);
			int streamVolume = audio.getStreamVolume(channel);
			int streamMaxVolume = audio.getStreamMaxVolume(channel);
			float volume = (float) streamVolume / streamMaxVolume;
			promise.resolve(volume);
			Log.d(TAG, "Get system volume");
		} catch (Exception e) {
			Log.e(TAG, "Error on getSystemVolume()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void setSystemVolume(final Float value, final ReadableMap options, final Promise promise) {
		try {
			AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
			int channel = this.getAudioStreamType(options);
			int volume = Math.round(audioManager.getStreamMaxVolume(channel) * value);
			audioManager.setStreamVolume(channel, volume, 0);
			promise.resolve(null);
			Log.d(TAG, "Set system volume to: " + volume);
		} catch (Exception e) {
			Log.e(TAG, "Error on setSystemVolume()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void setVolumeControlStream(final ReadableMap options, final Promise promise) {
		try {
			final Activity activity = getCurrentActivity();
			if (activity == null) {
				promise.reject(new Exception("Null current activity"));
			} else {
				int channel = this.getAudioStreamType(options);
				activity.setVolumeControlStream(channel);
				promise.resolve(null);
			}
			Log.d(TAG, "Set volume control stream");
		} catch (Exception e) {
			Log.e(TAG, "Error on setVolumeControlStream()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void resetVolumeControlStream(final Promise promise) {
		try {
			final Activity activity = getCurrentActivity();
			if (activity == null) {
				promise.reject(new Exception("Null current activity"));
			} else {
				activity.setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
				promise.resolve(null);
			}
			Log.d(TAG, "Reset volume control stream");
		} catch (Exception e) {
			Log.e(TAG, "Error on resetVolumeControlStream()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void requestAudioFocus(final ReadableMap options, final Promise promise) {
		try {
			AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				int result = audioManager.requestAudioFocus(
						getAudioFocusListener(),
						getAudioStreamType(options),
						getAudioFocusType(options)
					);
				if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) promise.resolve("granted");
				else promise.resolve("failed");
			} else {
				this.focusRequest = new AudioFocusRequest.Builder(getAudioFocusType(options))
					.setAudioAttributes((AudioAttributes) getAudioAttributes(options))
					.setOnAudioFocusChangeListener(getAudioFocusListener())
					.build();
				int result = audioManager.requestAudioFocus(focusRequest);
				if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) promise.resolve("granted");
				else if (result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) promise.resolve("delayed");
				else promise.resolve("failed");
			}
			Log.d(TAG, "Request audio focus");
		} catch (Exception e) {
			Log.e(TAG, "Error on requestAudioFocus()", e);
			promise.reject(e);
		}
	}

	private int getAudioFocusType(final ReadableMap options) {
		if (!options.hasKey("audioFocusType")) return AudioManager.AUDIOFOCUS_GAIN;
		String type = options.getString("audioFocusType");
		if (type == "gainTransient") return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
		if (type == "gainTransientMayDuck") return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
		if (type == "gainTransientExclusive") {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
			else return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;
		}
		return AudioManager.AUDIOFOCUS_GAIN;
	}

	private OnAudioFocusChangeListener getAudioFocusListener() {
		if (this.afChangeListener == null) {
			this.afChangeListener = new OnAudioFocusChangeListener() {
				public void onAudioFocusChange(int code) {
					String focusType = null;
					if (code == AudioManager.AUDIOFOCUS_GAIN) focusType = "gain";
					else if (code == AudioManager.AUDIOFOCUS_LOSS) focusType = "loss";
					else if (code == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) focusType = "lossTransient";
					else if (code == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) focusType = "lossTransientCanDuck";

					if (onAudioFocus != null) onAudioFocus.invoke(focusType);
				}
			};
		}
		return this.afChangeListener;
	}

	@ReactMethod
	public void setAudioFocusListener(final Callback onFocus) {
		try {
			this.onAudioFocus = onFocus;
			Log.d(TAG, "Set audio focus listener");
		} catch (Exception e) {
			Log.e(TAG, "Error on setAudioFocusListener()", e);
		}
	}

	@ReactMethod
	public void abandonAudioFocus(final Promise promise) {
		try {
			AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				audioManager.abandonAudioFocus(getAudioFocusListener());
			} else if (this.focusRequest != null) {
				audioManager.abandonAudioFocusRequest(this.focusRequest);
			}
			promise.resolve(null);
			Log.d(TAG, "Abandon audio focus");
		} catch (Exception e) {
			Log.e(TAG, "Error on abandonAudioFocus()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void setLooping(final Integer key, final boolean looping, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null) player.setLooping(looping);
			promise.resolve(null);
			Log.d(TAG, key + " - Set looping to " + looping);
		} catch (Exception e) {
			Log.e(TAG, "Error on setLooping()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void setSpeed(final Integer key, final Float speed, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null) player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
			promise.resolve(null);
			Log.d(TAG, key + " - Set speed to " + speed.toString());
		} catch (Exception e) {
			Log.e(TAG, "Error on setSpeed()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void setCurrentMillis(final Integer key, final int ms, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null) player.seekTo(ms);
			promise.resolve(null);
			Log.d(TAG, key + " - Set current millis to: " + Integer.toString(ms));
		} catch (Exception e) {
			Log.e(TAG, "Error on setCurrentMillis()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void getCurrentMillis(final Integer key, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			int ms = player != null ? player.getCurrentPosition() : -1;
			promise.resolve(ms);
			Log.d(TAG, key + " - Get current millis");
		} catch (Exception e) {
			Log.e(TAG, "Error on getCurrentMillis()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void isPlaying(final Integer key, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			boolean isPlaying = player != null ? player.isPlaying() : false;
			promise.resolve(isPlaying);
			Log.d(TAG, key + " - isPlaying");
		} catch (Exception e) {
			Log.e(TAG, "Error on isPlaying()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void setSpeakerphoneOn(final Integer key, final boolean speaker, final Promise promise) {
		try {
			MediaPlayer player = this.playerPool.get(key);
			if (player != null) {
				player.setAudioStreamType(AudioManager.STREAM_MUSIC);
				AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
				audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
				audioManager.setSpeakerphoneOn(speaker);
				Log.d(TAG, key + " - Set speakerphone to: " + speaker);
			}
			promise.resolve(null);
		} catch (Exception e) {
			Log.e(TAG, "Error on setSpeakerphoneOn()", e);
			promise.reject(e);
		}
	}

	@ReactMethod
	public void setMute(final boolean isMute, final Promise promise) {
		try {
			AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (isMute)	audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
				else audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
			} else {
				audioManager.setStreamMute(AudioManager.STREAM_MUSIC, isMute);
			}
			Log.d(TAG, "Set mute to: " + isMute);
			promise.resolve(null);
		} catch (Exception e) {
			Log.e(TAG, "Error on setMute()", e);
			promise.reject(e);
		}
	}

	@Override
	public String getName() {
		return "RNSound";
	}

	@Override
	public Map<String, Object> getConstants() {
		final Map<String, Object> constants = new HashMap<>();
		constants.put("IsAndroid", true);
		return constants;
	}

	/**
	* Ensure any audios that are playing when app exits are stopped and released
	*/
	@Override
	public void onCatalystInstanceDestroy() {
		super.onCatalystInstanceDestroy();

		Set<Map.Entry<Integer, MediaPlayer>> entries = playerPool.entrySet();
		for (Map.Entry<Integer, MediaPlayer> entry : entries) {
			MediaPlayer player = entry.getValue();
			if (player == null) continue;
			try {
				player.setOnCompletionListener(null);
				player.setOnPreparedListener(null);
				player.setOnErrorListener(null);
				if (player.isPlaying()) player.stop();
				player.reset();
				player.release();
			} catch (Exception exception) {
				Log.e(TAG, "Exception when closing audios during app exit. ", exception);
			}
		}
		entries.clear();
	}

}
