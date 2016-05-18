package com.clarkgarrett.stopwatchandtimer.Activities;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.clarkgarrett.stopwatchandtimer.Enums.StopwatchModes;
import com.clarkgarrett.stopwatchandtimer.HandAngles.StopwatchHandAngle;
import com.clarkgarrett.stopwatchandtimer.R;
import com.clarkgarrett.stopwatchandtimer.ScreenUpdater;
import com.clarkgarrett.stopwatchandtimer.Utilities;

import static com.clarkgarrett.stopwatchandtimer.Enums.StopwatchModes.LAP_TIME;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_STOPWATCH_MODE;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_STOPWATCH_RUNNING;
import static com.clarkgarrett.stopwatchandtimer.Utilities.isNewVersion;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mStopwatchMode;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mStopwatchRunning;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mStopwatchScreenUpdater;

/**
 * This activity displays the stopwatch.  It displays three views, a SurfaceView, and two Buttons.
 * The actual drawing onto the SurfaceView is handled by the ScreenUpdater class which is also
 * used by the TimerActivity thus avoiding duplicating code.  This activity can be started by
 * StartupActivity or TimerActivity.
 */

public class StopwatchActivity extends AppCompatActivity implements SurfaceHolder.Callback {

	private SharedPreferences mPrefs;
	private Button  mButton_Reset, mButton_Start;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mHolder;
	private StopwatchHandAngle mHandAngle = new StopwatchHandAngle();
	private boolean mSurfaceDestroyed=true;
	private boolean mPaused = true;
	public static boolean isRunning= false;
	private static final String PREFS_STOPWATCH_VERSIONCODE = "prefs_stopwatch_versioncode";
	private static final String PREFS_STOPWATCH_RESET ="prefs_stopwatch_reset";
	private static final String TAG = "## My Info ##";

	@TargetApi(11)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "StartupActivity onCreate called");
		setContentView(R.layout.activity_stop_watch);

		mStopwatchScreenUpdater = new ScreenUpdater(this, mHandAngle, getString(R.string.stopwatch), false);
		mPrefs = getSharedPreferences(Utilities.PREFS_NAME, Activity.MODE_PRIVATE);

		if (isNewVersion(PREFS_STOPWATCH_VERSIONCODE)){
			// If this is the first run of the activity on a new version of the app
			// make sure stopwatch starts over at zero and not running.  Remember this
			// app stores the stopwatch info between runs of the app so the stopwatch
			// appears to keep running.
			mPrefs.edit().putBoolean(PREFS_STOPWATCH_RUNNING, false).commit();
		}

		// Get the stopwatch mode from SharedPreferences.  If we haven't
		// stored a value in SharedPreferences use Lap Time mode as default.
		// See StopwatchSettingsActivity for explanation of stopwatch modes.
		String s =  mPrefs.getString(PREFS_STOPWATCH_MODE,"");
		if (s.equals("")){
			mStopwatchMode = LAP_TIME;
		}else{
			mStopwatchMode = Enum.valueOf(StopwatchModes.class,s);
		}

		// Find out if the stopwatch was running or paused the last time the
		// activity was destroyed.
		mStopwatchRunning = mPrefs.getBoolean(PREFS_STOPWATCH_RUNNING, false);
		mPaused = mPrefs.getBoolean(PREFS_STOPWATCH_RESET, false);

		if (mStopwatchRunning || mPaused){
			// The stopwatch was running or paused so get the screen updater and
			// the hand angel to retrieve there data.  Also set flags to keep screen on.
			mStopwatchScreenUpdater.retrieveData(mPrefs);
			mHandAngle.retrieveData(mPrefs);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}


		mButton_Start=(Button)findViewById(R.id.button_start);
		mButton_Reset=(Button)findViewById(R.id.button_reset);
		mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView1);


		mHolder=mSurfaceView.getHolder();
		mHolder.addCallback(this);
		
		mButton_Start.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// The START/PAUSE button was pressed.
				v.playSoundEffect(SoundEffectConstants.CLICK);
				if (mStopwatchRunning){
					// Stopwatch is running so pause it
					mStopwatchScreenUpdater.stop();
					mStopwatchRunning=false;
					mPaused=true;
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				}else{
					if (mPaused){
						// Stopwatch is paused, so restart it.
						if (mStopwatchMode == LAP_TIME){
							mStopwatchScreenUpdater.reset();
						}
						mStopwatchScreenUpdater.restart();
					}else {
						// Stopwatch isn't running or paused so we are starting from zero.
						mStopwatchScreenUpdater.start();
					}
					// Whether it's a start or a restart set these values.
					mStopwatchRunning=true;
					mPaused= false;
					getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);;
				}
			}
		});
		
		mButton_Reset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Move the hands back to zero.
				mStopwatchScreenUpdater.reset();
				// If  stopwatch is not running, we want it to not be paused, ie we
				// want the hands to move to zero and stay stopped. Stopwatch is in stopped
				// state. If the stopwatch is running we want the hands to move to zero and
				// then keep running.  In this case it doesn't matter what the value of the
				// mPaused variable is. So we can just do the following:
				mPaused = mStopwatchRunning;
			}
		});
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		Log.i(TAG, "StartupActivity onResume called");
		isRunning = true;  // AlarmReceiver needs to be able to tell if this activity is active.
		if (mSurfaceDestroyed){
			// Activity is being created.  If stopwatch is running
			// we will have to wait to do the restart in the
			// surfaceCreated callback method.
			mHolder=mSurfaceView.getHolder();
			mHolder.addCallback(this);
		}else{
			// Returning from background
			if (mStopwatchRunning) {
				mStopwatchScreenUpdater.restart();
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
		}
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		Log.i(TAG, "StartupActivity onPause called");
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		isRunning = false; // AlarmReceiver needs to be able to tell if this activity is active.

		mStopwatchScreenUpdater.stop();

		// Store the stopwatch state in SharedPreferences. We can use
		// this data to re-start the stopwatch when this activity runs
		// again whether it's this instance or a new instance.
		SharedPreferences.Editor prefsEditor = mPrefs.edit();
		prefsEditor.putBoolean(PREFS_STOPWATCH_RUNNING, mStopwatchRunning);
		prefsEditor.putBoolean(PREFS_STOPWATCH_RESET, mPaused);
		prefsEditor.commit();
		mStopwatchScreenUpdater.storeData(mPrefs);
		mHandAngle.storeData(mPrefs);
	}

	@Override
	public void  onStop(){
		super.onStop();
		Log.i(TAG, "StartupActivity onStop called");
	}

	@Override
	public void  onDestroy(){
		super.onDestroy();
		Log.i(TAG, "StartupActivity onDestroy called");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_stopwatch, menu);
		MenuItem settingsItem = menu.findItem(R.id.action_settings);
		settingsItem.setVisible(true);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()){
			case R.id.action_settings:
				// User chose settings from actionbar overflow menu.
				Intent intent = new Intent(this, StopwatchSettingsActivity.class);
				startActivity(intent);
				break;
			case R.id.action_timer:
				// User hit Timer option on actionbar.
				Intent activityIntent = new Intent(this, TimerActivity.class);
				activityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(activityIntent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	 
	@Override
	public void surfaceCreated(SurfaceHolder holder){
		// We have our surface, so pass the SurfaceView and the
		// SurfaceHolder to the ScreenUpdater and tell the
		// ScreenUpdater to start updating the UI i.e. start moving
		// the clock hands.
		mSurfaceDestroyed=false;
		mStopwatchScreenUpdater.setSurfaceAndHolder(mSurfaceView, mHolder);
		if (mStopwatchRunning) {
			mStopwatchScreenUpdater.restart();
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder){
		mSurfaceDestroyed = true;
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){
	}
}
