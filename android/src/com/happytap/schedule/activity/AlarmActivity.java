package com.happytap.schedule.activity;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;

import com.google.inject.Inject;
import us.wmwm.njrail.R;

public class AlarmActivity extends ScheduleActivity {

	@Inject
	Vibrator vibrator;

	private MediaPlayer mediaPlayer;

	private AudioManager audioManager;
	
	public static final String TYPE = "type";
	public static final String TYPE_ARRIVE = "arrive";
	public static final String TYPE_DEPART = "depart";
	public static final String TIME = "time";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(android.R.style.Theme_Light);
		setContentView(R.layout.timer);
		View v = findViewById(android.R.id.content);
		v.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				String ns = Context.NOTIFICATION_SERVICE;
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
				mNotificationManager.cancel(1);
				finish();
			}
			
		});
		PreferenceManager.getDefaultSharedPreferences(this).edit().remove("alarm").commit();
		vibrator.vibrate(new long[] {0,200,500},0);
		audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		if (alert == null) {
			// alert is null, using backup
			alert = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			if (alert == null) { // I can't see this ever being null (as always
									// have a default notification) but just
									// incase
				// alert backup is null, using 2nd backup
				alert = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			}
		}
		mediaPlayer = new MediaPlayer();
		try {
			mediaPlayer.setDataSource(this, alert);

			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				float vol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
				mediaPlayer.setVolume(vol, vol);
				setVolumeControlStream(AudioManager.STREAM_ALARM);
				mediaPlayer.setLooping(true);
				mediaPlayer.prepare();
				mediaPlayer.start();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mediaPlayer!=null) {
			mediaPlayer.stop();			
		}
		vibrator.cancel();
	}
}
