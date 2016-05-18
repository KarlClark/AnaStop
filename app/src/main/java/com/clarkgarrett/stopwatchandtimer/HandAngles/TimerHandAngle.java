package com.clarkgarrett.stopwatchandtimer.HandAngles;


import android.content.SharedPreferences;

/**
 * This class extends the abstract class HandAngle to create a class
 * used to control the hand angles for the countdown timer.  See the
 * HandAngle class for a explanation of what each method should do.
 */

public class TimerHandAngle extends HandAngle {

	private float mSecondDegrees = 90.0f;
	private float mMinuteDegrees=90.0f;
	private float mHourDegrees = 90.0f;
	private long mTickTime = 1000000000L;  // Tick time will be 1 second, expressed in nano-seconds.
	private long mTickTimeRange = mTickTime/10000L;
	private double mDegPerNanoSecond = 6.0/1000000000.0; // 6 degrees = 1 second divided by 1 second in nano-seconds
	private int mSecondsRemaining,mSeconds, mMinutes, mHours, mMinuteTickMarkDegs, mHourTickMarkDegs;
	private long mCountdownTimeNanos, mNanosRemaining , mNextTickTime;
	private int mSecondTickMark = 0, mMinuteTickMark = 0, mHourTickMark = 0;
	//private float  mPreviousSecondDegrees=90, mPreviousMinuteDegrees=90, mPreviousHourDegrees=90;
	//private long mTickTime = 1000000000L;  // hand angles will update every second
	private double mUpdateTime =0.0;
	private boolean mChanged;
	private boolean mFirstCallToUpdateAngles = true;
	int i = 0;
	private static final String PREFS_TIMER_NEXTTICKTIME = "prefs_timer_nextticktime";
	private static final String PREFS_TIMER_STARTNUMBEROFNANOS = "prefs_timer_startnumberofnanos";
	private static final String PREFS_TIMER_FIRSTCALLTOUPDATEANGLES="prefs_timer_firstcalltoupdateangles";
	private static final String TAG = "## My Info ##";
	private int [] degToFullUnits = {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1,
							          0, 59, 58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45,
							         44, 43, 42, 41, 40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 
							         29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16, 15};

	@Override
	public boolean updateAngles(long elapsedTimeNanos){

		if(mFirstCallToUpdateAngles){
			// First call since user last moved the clock hands.
			// Calculate the countdown time in nano-seconds based
			// on the position of the clock hands.
			mSeconds=degToTickmarkNumber(mSecondDegrees);
			mMinutes=degToTickmarkNumber(mMinuteDegrees);
			mHours=degToTickmarkNumber(mHourDegrees);
			mCountdownTimeNanos = (mHours*3600 + mMinutes*60 + mSeconds) * 1000000000L;

			// Initialize previous nanos remaining.
			mNextTickTime = mCountdownTimeNanos - mTickTime;
			mFirstCallToUpdateAngles = false;
		}

		mChanged = false;

		// Calculate nano-seconds remaining.  Don't let the
		// countdown go past zero.
		mNanosRemaining = mCountdownTimeNanos - elapsedTimeNanos;
		if (mNanosRemaining <0){
			mNanosRemaining = 0;
		}

		// These three variables must be kept up to date in case user pauses the timer and
		// and then restarts it again without moving any of the hands.  First multiply the
		// number of nano-seconds remaining by the number of degrees per nano-second. Usually
		// this will produce a big number so mod by 360 degrees.  Subtract this from 90 since
		// zero on the clock face is at 90 degrees.  This may produce a negative number so
		// add it to 360.  If the number was actually positive this will produce a number
		// bigger than 360, so mod by 360 again.
		mSecondDegrees = (float)(360 +(90 - (mNanosRemaining * mDegPerNanoSecond)) % 360) % 360;
		mMinuteDegrees = (float)(360 +(90 - ((mNanosRemaining/60) * mDegPerNanoSecond)) % 360) % 360;
		mHourDegrees = (float)(360 +(90 - ((mNanosRemaining/3600) * mDegPerNanoSecond)) % 360) % 360;

		// If the nano seconds remaining is less than the next tick time then
		// calculate a new next tick time and return true.  This wil cause
		// ScreenUpdate to redraw the clock.
		if (mNanosRemaining < mNextTickTime){
			mNextTickTime = (int)(mNanosRemaining / mTickTime) * mTickTime;
			mChanged = true;
		}
		return mChanged;
	}

	@Override
	public float getSecondDegrees(){

		return mSecondDegrees;
	}

	@Override
	public float getMinuteDegrees(){

		return mMinuteDegrees;
	}

	@Override
	public float getHourDegrees(){

		return mHourDegrees;
	}

	@Override
	public void setSecondDegrees(float degrees){
		// This method is called by ScreenUpdater in response to touch events.
		// The three set methods work by keeping the variable mSecondRemaining
		// set to the correct value as the user drags hands past the tick marks.
		// First we see how many tick marks the user has drug by.  On most calls
		// (there are a lot of touch events) the user won't have drug by any tick
		// marks. Occasionally he will have passed one.  However, ScreenUpdater
		// is designed so that if the user's down event is close to a hand,
		// the hand will relocate to a point under the user's finger.  In this
		// case we might move past several tick marks.

		mFirstCallToUpdateAngles = true;

		// First get closest tick mark that is less than where user is touching.
		// Negative degrees (0 to -180) are converted to values between 180 and 360.
		int newTickMark = degToTickmarkNumber((degrees < 0) ? 360+degrees : degrees);

		// Get the tick mark the second hand is pointing at now.
		mSecondTickMark = mSecondsRemaining % 60;

		// Find out how many tick marks away from second hand location the touch event
		// is.  A negative number means the user drug backwards with is perfectly valid.
		// A big number (positive of negative) means the user drug to or past zero.
		// What is a big number. Well usually it will be either -59 or 59, one tick
		// mark past zero.  However as explained above we could move more than one
		// tick mark, so maybe -57 to 57.  We use -50 and 50 as being more than safe.
		int tickMarkDifference = newTickMark - mSecondTickMark;
		if(tickMarkDifference > 50){
			tickMarkDifference = (newTickMark - 60) - mSecondTickMark;
		}
		if(tickMarkDifference < -50){
			tickMarkDifference = (60 - mSecondTickMark) + newTickMark;
		}

		// For second hand each tick mark is one second.
		mSecondsRemaining += tickMarkDifference;
		if (mSecondsRemaining < 0){ // pointing at 59 or close by.
			mSecondsRemaining += 60;
		}

		// Calculate the degrees fro each hand.  Each hand moves when the
		// second hand moves.  The expressions are explained above in
		// updateAngles();
		long nanos = mSecondsRemaining * 1000000000L;
		mSecondDegrees = (float)(360 +( + 90 - (nanos * mDegPerNanoSecond)) % 360) % 360;
		mMinuteDegrees = (float)(360 +(90 - ((nanos/60) * mDegPerNanoSecond)) % 360) %360;
		mHourDegrees = (float)(360 +(90 - ((nanos/3600) * mDegPerNanoSecond)) % 360) % 360;

	}

	@Override
	public void setMinuteDegrees(float degrees){
		// Calculate number of seconds based of user dragging minute hand.
		// See comments in setSecondDegrees().
		int newTickMark = degToTickmarkNumber((degrees < 0) ? 360+degrees : degrees);
		mFirstCallToUpdateAngles = true;
		mMinuteTickMark = (mSecondsRemaining /60) % 60;
		int tickMarkDifference = newTickMark - mMinuteTickMark;
		if(tickMarkDifference > 50){
			tickMarkDifference = (newTickMark - 60) - mMinuteTickMark;
		}
		if(tickMarkDifference < -50){
			tickMarkDifference = (60 - mMinuteTickMark) + newTickMark;
		}
		 // For minute hand each tick mark is 60 seconds.
		mSecondsRemaining += tickMarkDifference * 60;
		if (mSecondsRemaining < 0){
			mSecondsRemaining += 3600;  // number of seconds in 60 minutes.
		}
		long nanos = mSecondsRemaining * 1000000000L;
		mMinuteDegrees = (float)(360 +(90 - ((nanos/60) * mDegPerNanoSecond)) % 360) % 360;
		mHourDegrees = (float)(360 +(90 - ((nanos/3600) * mDegPerNanoSecond)) % 360) % 360;

	}

	@Override
	public void setHourDegrees(float degrees){
		// Calculate number of seconds based of user dragging hour hand.
		// See comments in setSecondDegrees().
		int newTickMark = degToTickmarkNumber((degrees < 0) ? 360+degrees : degrees);
		mFirstCallToUpdateAngles = true;
		mHourTickMark = (mSecondsRemaining /3600) % 60;
		int tickMarkDifference = newTickMark - mHourTickMark;
		if(tickMarkDifference > 50){
			tickMarkDifference = (newTickMark - 60) - mHourTickMark;
		}
		if(tickMarkDifference < -50){
			tickMarkDifference = (60 - mHourTickMark) + newTickMark;
		}

		// For hour hand each tick mark is 3600 seconds.
		mSecondsRemaining += tickMarkDifference * 3600;
		if (mSecondsRemaining < 0){
			mSecondsRemaining += 216000; // number of seconds in 60 hours.
		}
		long nanos = mSecondsRemaining * 1000000000L;
		mHourDegrees = (float)(360 +(90 - ((nanos/3600) * mDegPerNanoSecond)) % 360) % 360;
	}

	@Override
	public void reset(){
		// Set data so all hands point to zero.

		mSecondDegrees = 90.0f;
		mMinuteDegrees = 90.0f;
		mHourDegrees = 90.0f;
		mSecondsRemaining = 0;
	}

	@Override
	public void pause(){
		// The user pressed the STOP button while the timer was
		// running.  If he presses the start button we want the
		// the second hand (and other hands) to start moving
		// from where they are pointing now. The second hand in
		// particular is pointing at a tick mark.  However our
		// variable mNanosRemaining has run past the time the
		// hands are pointing at.  We must adjust our variables
		// so that they match to position the hands are pointing at.

		mFirstCallToUpdateAngles = true;

		// We want to call degToTickmarkNumber on the second hand.
		// But this will return the tick mark lower than than the
		// tick mark that the second hand is pointing at since
		// our actual time is less than the time we are pointing
		// at.  So subtract one tick time worth of degrees (move
		// clockwise) from our current second degrees.
		mSecondDegrees -= (mTickTime * mDegPerNanoSecond);
		if (mSecondDegrees < 0){
			mSecondDegrees += 360;
		}

		// Get the tick mark the second hand is pointing at.
		// Then set the variable mSecondDegrees so that the
		// second had is pointing exactly at the tick mark.
		mSeconds=degToTickmarkNumber(mSecondDegrees);
		mSecondDegrees = 90 - (mSeconds * 6);
		if (mSecondDegrees < 0) {
			mSecondDegrees += 360;
		}
		// Get mMinutes and mHours then calculate mSecondsRemaining.
		mMinutes=degToTickmarkNumber(mMinuteDegrees);
		mHours=degToTickmarkNumber(mHourDegrees);
		mSecondsRemaining = (mHours*3600 + mMinutes*60 + mSeconds);
	}

	@Override
	public int degToTickmarkNumber(float deg){
		// Convert an angle to the clock tick mark number (0 - 59)
		// Returns the closest tick mark that is <= deg. For
		// example, if the minute hand is in-between 4 and 5 on
		// the clock face this means 4 minutes and some amount of
		// seconds, not 5 minutes. In the expression below, 6 is
		// the number of degrees between tick marks on the clock face.
		int i = degToFullUnits[(int)deg/6] + ((deg % 6 == 0) ? 0 : -1);
		return (i == -1) ? 59 : i;
	}

	@Override
	public void storeData(SharedPreferences prefs){
		// Store data needed to restore state if app goes away and
		// is then restarted.
		SharedPreferences.Editor prefsEditor = prefs.edit();
		prefsEditor.putLong(PREFS_TIMER_NEXTTICKTIME, mNextTickTime);
		prefsEditor.putLong(PREFS_TIMER_STARTNUMBEROFNANOS, mCountdownTimeNanos);
		prefsEditor.putBoolean(PREFS_TIMER_FIRSTCALLTOUPDATEANGLES, mFirstCallToUpdateAngles);
		prefsEditor.commit();
	}

	@Override
	public void retrieveData(SharedPreferences prefs){
		// Retrieve data stored by storeData().
		mNextTickTime = prefs.getLong(PREFS_TIMER_NEXTTICKTIME, 0L);
		mCountdownTimeNanos = prefs.getLong(PREFS_TIMER_STARTNUMBEROFNANOS, 0);
		mFirstCallToUpdateAngles = prefs.getBoolean(PREFS_TIMER_FIRSTCALLTOUPDATEANGLES, true);
	}
}
