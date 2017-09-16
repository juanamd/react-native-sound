package com.zmxv.RNSound;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.media.AudioManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.ExceptionsManagerModule;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import android.util.Log;

public class RNSoundModule extends ReactContextBaseJavaModule {

	final static Object NULL = null;

	Map<Integer, MediaPlayer> playerPool = new HashMap<>();
	ReactApplicationContext context;

	public RNSoundModule(ReactApplicationContext context) {
		super(context);
		this.context = context;
	}

	@Override
	public String getName() {
		return "RNSound";
	}

	protected OnErrorListener createOnErrorListener(final Callback callback, final boolean isPlayCallback) {
		return new OnErrorListener() {
			@Override
			public synchronized boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
				try {
					if (isPlayCallback) callback.invoke(false);
					else {
						WritableMap errorMap = Arguments.createMap();
						errorMap.putInt("what", what);
						errorMap.putInt("extra", extra);
						callback.invoke(errorMap, NULL);
					}
				} catch(RuntimeException runtimeException) {
					// The callback was already invoked
					Log.e("RNSoundModule", "Exception in method onError", runtimeException);
				}
				return true;
			}
		};
	}

	protected OnPreparedListener createOnPreparedListener(final Callback callback, final Integer key, final RNSoundModule module) {
		return new OnPreparedListener() {
			@Override
			public synchronized void onPrepared(MediaPlayer mediaPlayer) {
				module.playerPool.put(key, mediaPlayer);
				WritableMap props = Arguments.createMap();
				props.putDouble("duration", mediaPlayer.getDuration() * .001);
				try {
					callback.invoke(NULL, props);
				} catch(RuntimeException runtimeException) {
					// The callback was already invoked
					Log.e("RNSoundModule", "Exception in method onPrepared", runtimeException);
				}
			}
		};
	}

	protected OnCompletionListener createOnCompletionListener(final Callback callback) {
		return new OnCompletionListener() {
			@Override
			public synchronized void onCompletion(MediaPlayer mediaPlayer) {
				if (!mediaPlayer.isLooping()) {
					try {
						callback.invoke(true);
					} catch (RuntimeException runtimeException) {
						// The callback was already invoked
						Log.e("RNSoundModule", "Exception in method onResourceNotFound", runtimeException);
					}
				}
			}
		};
	}

	@ReactMethod
	public void prepare(final String fileName, final Integer key, final ReadableMap options, final Callback callback) {
		boolean useAlarmChannel = (options != null && options.hasKey("useAlarmChannel")) ? options.getBoolean("useAlarmChannel") : false;

		MediaPlayer mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnErrorListener(this.createOnErrorListener(callback, false));
		mediaPlayer.setOnPreparedListener(this.createOnPreparedListener(callback, key, this));
		mediaPlayer.setAudioStreamType(useAlarmChannel ? AudioManager.STREAM_ALARM : AudioManager.STREAM_MUSIC);
		this.setMediaPlayerDataSource(mediaPlayer, fileName);

		try {
			Log.i("RNSoundModule", "prepareAsync...");
			mediaPlayer.prepareAsync();
		} catch(IllegalStateException illegalStateException) {
			Log.e("RNSoundModule", "Exception in method prepare", illegalStateException);
			this.onResourceNotFound(callback);
		}
	}

	protected void onResourceNotFound(final Callback callback) {
		WritableMap err = Arguments.createMap();
		err.putInt("code", -1);
		err.putString("message", "resource not found");
		try {
			callback.invoke(err, NULL);
		} catch(RuntimeException runtimeException) {
			// The callback was already invoked
			Log.e("RNSoundModule", "Exception in method onResourceNotFound", runtimeException);
		}
	}

	protected void setMediaPlayerDataSource(final MediaPlayer mediaPlayer, final String fileName) {
		int res = this.context.getResources().getIdentifier(fileName, "raw", this.context.getPackageName());

		if (res != 0) this.setDataSourceFromUri(mediaPlayer, fileName);
		else if (fileName.startsWith("asset:/")) this.setDataSourceFromAsset(mediaPlayer, fileName);
		else if (fileName.startsWith("http://") || fileName.startsWith("https://")) this.setDataSourceFromNetwork(mediaPlayer, fileName);
		else this.setDataSourceFromFile(mediaPlayer, fileName);
	}

	protected void setDataSourceFromUri(final MediaPlayer mediaPlayer, final String fileName) {
		Uri uri = Uri.parse("android.resource://" + this.context.getPackageName() + "/raw/" + fileName);
		try {
			mediaPlayer.setDataSource(this.context, uri);
		} catch(IOException exception) {
			Log.e("RNSoundModule", "Exception in method createMediaPlayerFromUri", exception);
		}
	}

	protected void setDataSourceFromAsset(final MediaPlayer mediaPlayer, final String fileName) {
		try {
			AssetFileDescriptor desc = this.context.getAssets().openFd(fileName.replace("asset:/", ""));
			mediaPlayer.setDataSource(desc.getFileDescriptor(), desc.getStartOffset(), desc.getLength());
			desc.close();
		} catch(IOException exception) {
			Log.e("RNSoundModule", "Exception in method createMediaPlayerFromAsset", exception);
		}
	}

	protected void setDataSourceFromNetwork(final MediaPlayer mediaPlayer, final String url) {
		try {
			mediaPlayer.setDataSource(url);
		} catch(IOException exception) {
			Log.e("RNSoundModule", "Exception in method createMediaPlayerFromNetwork", exception);
		}
	}

	protected void setDataSourceFromFile(final MediaPlayer mediaPlayer, final String fileName) {
		File file = new File(fileName);
		if (file.exists()) {
			Uri uri = Uri.fromFile(file);
			try {
				mediaPlayer.setDataSource(this.context, uri);
			} catch(IOException exception) {
				Log.e("RNSoundModule", "Exception in method createMediaPlayerFromFile", exception);
			}
		}
	}

	@ReactMethod
	public void play(final Integer key, final Callback callback) {
		MediaPlayer player = this.playerPool.get(key);
		if (player == null) callback.invoke(false);
		else if (!player.isPlaying()) {
			player.setOnErrorListener(this.createOnErrorListener(callback, true));
			player.setOnCompletionListener(this.createOnCompletionListener(callback));
			player.start();
		}
	}

	@ReactMethod
	public void pause(final Integer key, final Callback callback) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null && player.isPlaying()) player.pause();
		try {
			callback.invoke();
		} catch(RuntimeException runtimeException) {
			// The callback was already invoked
			Log.e("RNSoundModule", "Exception in method pause", runtimeException);
		}
	}

	@ReactMethod
	public void stop(final Integer key, final Callback callback) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null && player.isPlaying()) {
			player.pause();
			player.seekTo(0);
		}
		try {
			callback.invoke();
		} catch(RuntimeException runtimeException) {
			// The callback was already invoked
			Log.e("RNSoundModule", "Exception in method stop", runtimeException);
		}
	}

	@ReactMethod
	public void reset(final Integer key) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null) player.reset();
	}

	@ReactMethod
	public void release(final Integer key) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null) {
			player.release();
			this.playerPool.remove(key);
		}
	}

	@ReactMethod
	public void setVolume(final Integer key, final Float left, final Float right) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null) player.setVolume(left, right);
	}

	@ReactMethod
	public void getSystemVolume(final Callback callback) {
		try {
			AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			float volume = (float)audio.getStreamVolume(AudioManager.STREAM_MUSIC) / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			callback.invoke(NULL, volume);
		} catch (Exception error) {
			WritableMap errorMap = Arguments.createMap();
			errorMap.putInt("code", -1);
			errorMap.putString("message", error.getMessage());
			try {
				callback.invoke(errorMap, NULL);
			} catch(RuntimeException runtimeException) {
				// The callback was already invoked
				Log.e("RNSoundModule", "Exception in method getSystemVolume", runtimeException);
			}
		}
	}

	@ReactMethod
	public void setSystemVolume(final Float value) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		int volume = Math.round(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * value);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
	}

	@ReactMethod
	public void setLooping(final Integer key, final boolean looping) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null) player.setLooping(looping);
	}

	@ReactMethod
	public void setSpeed(final Integer key, final Float speed) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null) player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
	}

	@ReactMethod
	public void setCurrentTime(final Integer key, final Float sec) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null) player.seekTo((int)Math.round(sec * 1000));
	}

	@ReactMethod
	public void getCurrentTime(final Integer key, final Callback callback) {
		MediaPlayer player = this.playerPool.get(key);
		try {
			if (player == null) callback.invoke(-1, false);
			else callback.invoke(player.getCurrentPosition() * .001, player.isPlaying());
		} catch(RuntimeException runtimeException) {
			// The callback was already invoked
			Log.e("RNSoundModule", "Exception in method getCurrentTime", runtimeException);
		}
	}

	//turn speaker on
	@ReactMethod
	public void setSpeakerphoneOn(final Integer key, final boolean speaker) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null) {
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			AudioManager audioManager = (AudioManager)this.context.getSystemService(this.context.AUDIO_SERVICE);
			audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
			audioManager.setSpeakerphoneOn(speaker);
		}
	}

	@ReactMethod
	public void enable(final boolean enabled) {
		// no op
	}

	@Override
	public Map<String, Object> getConstants() {
		final Map<String, Object> constants = new HashMap<>();
		constants.put("IsAndroid", true);
		return constants;
	}
}
