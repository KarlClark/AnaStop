package com.clarkgarrett.stopwatchandtimer.HandAngles;

import android.content.SharedPreferences;

/**
 * This class extends the abstract class HandAngle to create a class
 * used to control the hand angles for the stopwatch.  See the
 * HandAngle class for a explanation of what each method should do.
 */
public class StopwatchHandAngle extends HandAngle {
	private double mSecondDegrees = 90.0f;
	private double mMinuteDegrees=90.0f;
	private long mPreviousTotalTime = 0;  // nano-seconds
	private long mTickTime = 1000000000/40;
	private double mDegPerNanoSecond = 6.0/1000000000.0; // 6 degrees = 1 second divided by 1 second in nano-seconds
	private float mAngleDelta = (float)((mTickTime*6.0f)/1000000000.0f);  // The amount the second hand would move for one tick time.
	private double mUpdateTime =0.0;
	private boolean mChanged;
	private static final String PREFS_STOPWATCH_PREVIOUSTOTALTIME = "prefs_stopwatch_previoustotaltime";
	private static final String TAG = "## My Info ##";
	
	@Override
	public boolean updateAngles(long totalTimeNanos){

		mChanged= false;

		// If the time since the last update is greater than
		// the tickTime then update the hand again.
		// The 90 constant below is do to the fact that zero on the
		// clock face is at 90 degrees trigonometrically speaking.
		// The mod 360 accounts for the hands passing zero.
		mSecondDegrees = (90 - (totalTimeNanos * mDegPerNanoSecond)) % 360;
		mMinuteDegrees = (90 - ((totalTimeNanos/60) * mDegPerNanoSecond)) % 360;
		if (totalTimeNanos - mPreviousTotalTime >= mTickTime){
			mPreviousTotalTime = totalTimeNanos;
			mChanged = true;
		}
		return mChanged;
	}
	
	@Override
	public float getSecondDegrees(){
		return (float)mSecondDegrees;
	}
	
	@Override
	public float getMinuteDegrees(){
		return (float)mMinuteDegrees;
	}
	
	@Override
	public float getHourDegrees(){
		// no hour hand on stopwatch
		return 0;
	}

	// The next three methods do nothing so that the
	// stopwatch doesn't respond to touch events.
	@Override
	public void setSecondDegrees(float degrees) {}
	
	@Override
	public void setMinuteDegrees(float degrees) {}
	
	@Override
	public void setHourDegrees(float degrees) {}
	
	@Override
	public void reset(){
		// Set both hands straight up pointing at zero.
		mSecondDegrees=90.0f;
		mMinuteDegrees=90.0f;
		mPreviousTotalTime = 0;
	}

	@Override
	// Not needed for stop watch.
	public void pause(){};
	
	@Override
	public int degToTickmarkNumber(float deg){
		// Not used by stopwatch so doesn't matter what we return.
		return 0;
	}

	@Override
	public void storeData(SharedPreferences prefs){
		// The data we need to restart is the previous update time.
		SharedPreferences.Editor prefsEditor = prefs.edit();
		prefsEditor.putLong(PREFS_STOPWATCH_PREVIOUSTOTALTIME, mPreviousTotalTime);
		prefsEditor.commit();
	}

	@Override
	public void retrieveData(SharedPreferences prefs){
		// Retrieve data to restart.
		mPreviousTotalTime = prefs.getLong(PREFS_STOPWATCH_PREVIOUSTOTALTIME, 0);
	}
}
