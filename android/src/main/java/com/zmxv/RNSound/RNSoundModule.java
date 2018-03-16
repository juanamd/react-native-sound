package com.zmxv.RNSound;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
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

	private ReactApplicationContext context;
	private Map<Integer, MediaPlayer> playerPool = new HashMap<>();

	public RNSoundModule(ReactApplicationContext context) {
		super(context);
		this.context = context;
	}

	@ReactMethod
	public void load(final String dataSource, final Integer key, final ReadableMap options, final Callback onSuccess, final Callback onError) {
		MediaPlayer mediaPlayer = new MediaPlayer();
		this.playerPool.put(key, mediaPlayer);

		mediaPlayer.setOnErrorListener(this.createOnErrorListener(onError));
		mediaPlayer.setOnPreparedListener(this.createOnPreparedListener(onSuccess));
		mediaPlayer.setAudioStreamType(this.getAudioStreamTypeFromOptions(options));
		
		this.setMediaPlayerDataSource(mediaPlayer, dataSource, onError);
		this.prepareMediaPlayer(mediaPlayer, onError);		
	}

	private OnErrorListener createOnErrorListener(final Callback callback) {
		return new OnErrorListener() {
			@Override
			public synchronized boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
				WritableMap errorMap = Arguments.createMap();
				errorMap.putInt("what", what);
				errorMap.putInt("extra", extra);
				try {
					callback.invoke(errorMap);
				} catch(RuntimeException runtimeException) {
					Log.e("RNSoundModule", "The callback was already invoked", runtimeException);
				}
				return true; //Return true if the error has been handled
			}
		};
	}

	private OnPreparedListener createOnPreparedListener(final Callback callback) {
		return new OnPreparedListener() {
			@Override
			public synchronized void onPrepared(MediaPlayer mediaPlayer) {
				WritableMap response = Arguments.createMap();
				response.putDouble("duration", mediaPlayer.getDuration() * .001);
				try {
					callback.invoke(response);
				} catch(RuntimeException runtimeException) {
					Log.e("RNSoundModule", "The callback was already invoked", runtimeException);
				}
			}
		};
	}

	private int getAudioStreamTypeFromOptions(final ReadableMap options) {
		boolean useAlarmChannel = options.hasKey("useAlarmChannel") ? options.getBoolean("useAlarmChannel") : false;
		if (useAlarmChannel) return AudioManager.STREAM_ALARM;
		return AudioManager.STREAM_MUSIC;
	}

	private void setMediaPlayerDataSource(final MediaPlayer mediaPlayer, final String dataSource, final Callback onError) {
		try {
			if (this.isBundledResource(dataSource)) this.setDataSourceFromUri(mediaPlayer, dataSource);
			else if (dataSource.startsWith("asset:/")) this.setDataSourceFromAsset(mediaPlayer, dataSource);
			else if (dataSource.matches("^(https?)://.*$")) this.setDataSourceFromNetwork(mediaPlayer, dataSource);
			else this.setDataSourceFromFile(mediaPlayer, dataSource);
		} catch(Exception exception) {
			Log.e("RNSoundModule", "Exception in method setMediaPlayerDataSource", exception);
			this.onException(exception, onError);
		}
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
			throw new Exception("File does not exist");
		}
	}

	private void onException(Exception exception, final Callback callback) {
		WritableMap errorMap = Arguments.createMap();
		errorMap.putString("message", exception.getMessage());
		errorMap.putString("description", exception.toString());
		try {
			callback.invoke(errorMap);
		} catch(RuntimeException runtimeException) {
			Log.e("RNSoundModule", "The callback was already invoked", runtimeException);
		}
	}

	private void prepareMediaPlayer(final MediaPlayer mediaPlayer, final Callback onError) {
		try {
			Log.i("RNSoundModule", "prepareAsync...");
			mediaPlayer.prepareAsync();
		} catch(Exception exception) {
			Log.e("RNSoundModule", "Exception in method prepareMediaPlayer", exception);
			this.onException(exception, onError);
		}
	}

	@ReactMethod
	public void play(final Integer key, final Callback onComplete) {
		MediaPlayer player = this.playerPool.get(key);
		if (player == null) onComplete.invoke(false);
		else if (!player.isPlaying()) {
			player.setOnCompletionListener(this.createOnCompletionListener(onComplete));
			player.start();
		}
	}

	private OnCompletionListener createOnCompletionListener(final Callback callback) {
		return new OnCompletionListener() {
			@Override
			public synchronized void onCompletion(MediaPlayer mediaPlayer) {
				if (!mediaPlayer.isLooping()) {
					try {
						callback.invoke();
					} catch (RuntimeException runtimeException) {
						Log.e("RNSoundModule", "The callback was already invoked", runtimeException);
					}
				}
			}
		};
	}

	@ReactMethod
	public void pause(final Integer key, final Callback callback) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null && player.isPlaying()) player.pause();
		try {
			callback.invoke();
		} catch(RuntimeException runtimeException) {
			Log.e("RNSoundModule", "The callback was already invoked", runtimeException);
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
			Log.e("RNSoundModule", "The callback was already invoked", runtimeException);
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
			player.setOnCompletionListener(null);
			player.setOnPreparedListener(null);
			player.setOnErrorListener(null);
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
			AudioManager audio = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
			int streamVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
			int streamMaxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			float volume = (float) streamVolume / streamMaxVolume;
			callback.invoke(null, volume);
		} catch (Exception exception) {
			WritableMap errorMap = Arguments.createMap();
			errorMap.putString("message", exception.getMessage());
			errorMap.putString("description", exception.toString());
			try {
				callback.invoke(errorMap, null);
			} catch(RuntimeException runtimeException) {
				Log.e("RNSoundModule", "The callback was already invoked", runtimeException);
			}
		}
	}

	@ReactMethod
	public void setSystemVolume(final Float value) {
		AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
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
		if (player != null) player.seekTo((int) Math.round(sec * 1000));
	}

	@ReactMethod
	public void getCurrentTime(final Integer key, final Callback callback) {
		MediaPlayer player = this.playerPool.get(key);
		try {
			if (player == null) callback.invoke(-1, false);
			else callback.invoke(player.getCurrentPosition() * .001, player.isPlaying());
		} catch(RuntimeException runtimeException) {
			Log.e("RNSoundModule", "The callback was already invoked", runtimeException);
		}
	}

	@ReactMethod
	public void setSpeakerphoneOn(final Integer key, final boolean speaker) {
		MediaPlayer player = this.playerPool.get(key);
		if (player != null) {
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
			audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
			audioManager.setSpeakerphoneOn(speaker);
		}
	}

	@ReactMethod
	public void setMute(final boolean isMute) {
		AudioManager audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (isMute)	audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
			else audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
		} else {
			audioManager.setStreamMute(AudioManager.STREAM_MUSIC, isMute);
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
				Log.e("RNSoundModule", "Exception when closing audios during app exit. ", exception);
			}
		}
		entries.clear();
	}

}
