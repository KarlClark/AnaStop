package com.clarkgarrett.stopwatchandtimer.Activities;

/**
 * This activity displays the countdown timer.  It displays three views, a SurfaceView, and two Buttons.
 * The actual drawing onto the SurfaceView is handled by the ScreenUpdater class which is also
 * used by the StopwatchActivity thus avoiding duplicating code.  This activity can be started by
 * StartupActivity or StopwatchActivity, or from AlarmReceiver due to an alarm broadcast.
 */

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.clarkgarrett.stopwatchandtimer.Enums.VibrateModes;
import com.clarkgarrett.stopwatchandtimer.HandAngles.TimerHandAngle;
import com.clarkgarrett.stopwatchandtimer.R;
import com.clarkgarrett.stopwatchandtimer.ScreenUpdater;
import com.clarkgarrett.stopwatchandtimer.Utilities;

import static com.clarkgarrett.stopwatchandtimer.Enums.VibrateModes.VIBRATE_WHEN_MUTED;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_ALARM_LENGTH;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_TIMER_RUNNING;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_VIBRATE_MODE;
import static com.clarkgarrett.stopwatchandtimer.Utilities.START_IN_BACKGROUND_EXTRA;
import static com.clarkgarrett.stopwatchandtimer.Utilities.applicationContext;
import static com.clarkgarrett.stopwatchandtimer.Utilities.isNewVersion;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mAlarmHasRung;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mAlarmLengthSecs;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mButton_Start;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mStartedFromAlarm;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mTimerRunning;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mTimerScreenUpdater;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mTouchEventsOk;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mVibrateMode;
import static com.clarkgarrett.stopwatchandtimer.Utilities.startAlarm;
import static com.clarkgarrett.stopwatchandtimer.Utilities.timerActivity;

public class TimerActivity extends AppCompatActivity implements SurfaceHolder.Callback {
	public static boolean isRunning = false;
	private boolean mPaused = false;
	private Button mButton_Stop;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private TimerHandAngle mHandAngle = new TimerHandAngle();
	private boolean mSurfaceDestroyed=true;
	private Utilities mUtilities = Utilities.get();
	private SharedPreferences mPrefs;
	private static final String PREFS_TIMER_VERSION_CODE = "prefs_timer_version_code";
	private static final String TAG = "## My Info ##";
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		if (getIntent().getBooleanExtra(START_IN_BACKGROUND_EXTRA,false)){
			// If this flag is true, then the user is using the stopwatch.  We
			// need this activity to start and sound the alarm etc. but we don't
			// want it to appear on top of the stopwatch activity. So re-start
			// StopwatchActivity and let this activity continue in the background.
			// The way this can happens is by the user starting the timer, then sending
			// the timer activity to the background, which sets an alarm, then the
			// app getting completely killed, then the user restarts the app and
			// goes to the stopwatch and then the alarm fires and the broadcast receiver
			// starts this activity.
			Intent stopwatchIntent = new Intent(this,StopwatchActivity.class);
			startActivity(stopwatchIntent);
		}
		super.onCreate(savedInstanceState);
		// Since this can be the first activity started because it may be started by the broadcast receiver
		// we must set this variable here as well as in StartupActivity.
		Log.i(TAG,"TimerActivity onCreate called. SavedInstanceState= " + savedInstanceState + "  mTimerScreenUpdater= " + mTimerScreenUpdater +"  mAlarmHasRung= " + mAlarmHasRung);
		applicationContext = this.getApplicationContext();

		setContentView(R.layout.activity_timer);
		mButton_Stop = (Button)findViewById(R.id.button_timer_stop);
		mButton_Start = (Button)findViewById(R.id.button_timer_start);
		timerActivity = this;  // Global context in Utilities class.
		mPrefs = getSharedPreferences(Utilities.PREFS_NAME, Activity.MODE_PRIVATE);

		// Get the alarm length and the vibrate mode from SharedPreferences. If there
		// are no values in SharedPreferences use 30 seconds and vibrate when muted as
		// the defaults.
		mAlarmLengthSecs = mPrefs.getInt(PREFS_ALARM_LENGTH, 30);
		String s =  mPrefs.getString(PREFS_VIBRATE_MODE,"");
		if (s.equals("")){
			mVibrateMode = VIBRATE_WHEN_MUTED;
		}else{
			mVibrateMode = Enum.valueOf(VibrateModes.class,s);
		}
		mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView2);
		mTimerScreenUpdater = new ScreenUpdater(this, mHandAngle, getString(R.string.timer) + ":  " + getString(R.string.drag), true);
		mSurfaceDestroyed = true;

		if(isNewVersion(PREFS_TIMER_VERSION_CODE)){
			// First run of a new version of the app.  Make sure the timer is not running.
			mPrefs.edit().putBoolean(PREFS_TIMER_RUNNING, false).commit();
		}

		if (mAlarmHasRung){
			// The alarm was rung without starting this activity because
			// the stopwatch activity was showing. Then the user switched
			// to the timer (this activity).
			mButton_Start.setEnabled(true);
			mAlarmHasRung = false;
			mTimerRunning = false;
		}else {
			// find out if timer was running when the last instance of
			// this activity was destroyed.
			mTimerRunning = mPrefs.getBoolean(PREFS_TIMER_RUNNING, false);
		}
		Log.i(TAG,"TimerActivity onCreate mTimerRunning= " +mTimerRunning);
		if (mTimerRunning){
			// Timer is was running so retrieve data necessary for restart
			// and disable START button.
			mTimerScreenUpdater.retrieveData(mPrefs);
			mHandAngle.retrieveData(mPrefs);
			mButton_Start.setEnabled(false);
		}

		mButton_Start.setOnClickListener(new View.OnClickListener() {
			// User pressed the START button.
			@Override
			public void onClick(View v) {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				mTimerScreenUpdater.start(); // start timer countdown.
				mTimerRunning=true;
				mAlarmHasRung = false;
				mStartedFromAlarm = false;
				mButton_Start.setEnabled(false);  // prevent starting twice
			}
		});
		
		mButton_Stop.setOnClickListener(new View.OnClickListener() {
			// User pressed the STOP button.  Usually done to turn off alarm.
			@Override
			public void onClick(View v) {
				mUtilities.alarmThread.interrupt(); // sleeping thread will proceed and turn off alarm
				mTimerScreenUpdater.stopAndWait(); // freeze clock hands if user stopped timer before alarm sounds
				mTouchEventsOk = true;  // let the user drag the clock hands
				mTimerRunning = false;
				mAlarmHasRung = false;
				mButton_Start.setEnabled(true);   // Let the user use the START button.
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
		});
	}

	@Override
	public void onResume(){
		super.onResume();
		Log.i(TAG,"TimerActivity onResume called");
		mPaused= false;
		isRunning = true; // used by AlarmReceiver to tell when activity has started.
		if (mSurfaceDestroyed){
			// The surface holder was destroyed. Get a new instance.
			// We will have to wait until surfaceCreated() callback method
			// is called before we restart the screen updater.
			mHolder=mSurfaceView.getHolder();
			mHolder.addCallback(this);
		}else{
			if(mTimerRunning) {
				// Probably returning from background.
				mTimerScreenUpdater.restart();
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
		}

		if (startAlarm) {
			mUtilities.startAlarm();
			startAlarm=false;
		}
	}
	
	@Override
	public void onPause(){
		super.onPause();
		Log.i(TAG, "TimerActivity onPause called");
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mPaused = true;
		mTimerScreenUpdater.stop();  // Stop thread. stop() won't return until thread is done.

		// Save state of timer so we can restart it
		SharedPreferences.Editor prefsEditor = mPrefs.edit();
		prefsEditor.putBoolean(PREFS_TIMER_RUNNING, mTimerRunning);
		prefsEditor.commit();
		mTimerScreenUpdater.storeData(mPrefs);
		mHandAngle.storeData(mPrefs);

		if(isFinishing()){
			if (mTimerRunning) {
				mTimerScreenUpdater.stopAndSetAlarm();
			}
		}else {
			if (mStartedFromAlarm) {
				// If this instance was started from the alarm receiver
				// we don't want to set another alarm. But reset the boolean.
				mStartedFromAlarm = false;
			} else {
				if (mTimerRunning) {
					mTimerScreenUpdater.stopAndSetAlarm();
				}
			}
		}
		//isRunning = false;
	}

	@Override
	public void onStop(){
		super.onStop();
		Log.i(TAG, "TimerActivity onStop called");
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		Log.i(TAG, "TimerActivity onDestroy called");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_timer, menu);
		MenuItem settingsItem = menu.findItem(R.id.action_settings);
		settingsItem.setVisible(true);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()){
			case R.id.action_settings:
				Intent intent = new Intent(this, TimerSettingsActivity.class);
				startActivity(intent);
				break;
			case R.id.action_stopwatch:
				Intent activityIntent = new Intent(this, StopwatchActivity.class);
				activityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(activityIntent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder){
		mSurfaceDestroyed=false;
		mTimerScreenUpdater.setSurfaceAndHolder(mSurfaceView, mHolder);
		if(mTimerRunning  && ! mPaused){
			// mPaused may be true if AlarmReceiver started activity while device
			// screen is off.  In this case activity moves from onResume to onPause
			// immediately.
			mTimerScreenUpdater.restart();
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder){
		mSurfaceDestroyed=true;
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){
	}
	
}
