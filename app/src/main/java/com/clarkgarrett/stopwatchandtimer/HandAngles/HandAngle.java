package com.clarkgarrett.stopwatchandtimer.HandAngles;

import android.content.SharedPreferences;
// Abstract class that specifies requirements for a hand angle class.
public abstract class HandAngle {

	// totalTime is the time the stopwatch or the timer
	// has been running in nanoseconds.  This method must
	// set the hand angles based on this time.
	public abstract boolean updateAngles(long totalTime);

	// Must return current angle of the second hand in degrees
	public abstract float getSecondDegrees();
	
	// Must return the current angle of the minute hand in degrees
	public abstract float getMinuteDegrees();

	// Must return the current angle of the hour hand in degrees
	public abstract float getHourDegrees();

	// Use to set second hand angle corresponding to user touch events.
	public abstract void setSecondDegrees(float degrees);

	// Use to set minute hand angle corresponding to user touch events.
	public abstract void setMinuteDegrees(float degrees);

	// Use to set hour hand angle corresponding to user touch events.
	public abstract void setHourDegrees(float degrees);

	// Use to set hands to zero.
	public abstract void reset();

	// Used by the timer.  The hands will stop moving. This
	// method must adjust variables so that hands stop at
	// exactly a second interval and are ready to restart
	// from that position.
	public abstract void pause();

	// Must convert angel in degress to appropriate
	// tick mark on the clock face.  There are 60
	// tick marks and the zero tick mark is at the
	// top of the clock face (90 degrees).
	public abstract int degToTickmarkNumber(float deg);

	// This app is designed so that it can go into background
	// or even be killed and then restarted.  The user expects
	// the clock to look like it should based on the time
	// the app was inactive.  This method must store what ever
	// state data is necessary to allow it to restart.
	public abstract void storeData(SharedPreferences prefs);

	// Retrieves data stored by storeData().
	public abstract void retrieveData(SharedPreferences prefs);
}
