package com.clarkgarrett.stopwatchandtimer;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

import com.clarkgarrett.stopwatchandtimer.HandAngles.HandAngle;

import java.text.DecimalFormat;

import static com.clarkgarrett.stopwatchandtimer.Enums.StopwatchModes.LAP_TIME;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mStopwatchMode;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mTimerRunning;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mTouchEventsOk;

/**
 * Instances of this class are used by both StopwatchActivity and TimerActivity
 * to draw the clock, including running a thread to move the clock hands. It has
 * methods for starting, stopping, pausing and restarting the clock hands. It also
 * handles touch events when the user is dragging the clock hands for the timer.
 */

public class ScreenUpdater implements  Runnable, OnTouchListener{
	Canvas mCanvas;
	private SurfaceView mSurfaceView;
	private Context mContext;
	//private Ringtone mRingtone;
	private boolean mRunning,mLargeScreen, mExtraLargeScreen, mMoveSecondHand, mMoveMinuteHand, mMoveHourHand, mMoveNoHand, mCountdown, mChanged;
	private SurfaceHolder mHolder;
	private int mCenterX, mCenterY, mRadius, mTickMarkLength, mMinuteHandLength, mHourHandLength ,mHours, mMinutes, mSeconds, mDistanceFromEnd,
			    mArrowRadiusDif,mArrowTipDif,mArrowEndDif, mArrowDegs, mSecondHandRadius, mMinuteHandRadius, mHourHandRadius;
	private float mHandEndX, mHandEndY, mArrowTipX, mArrowTipY;
	private double mTotalTimeSecs =0.0;
	private double mCountDownTimeMillis;
	private long mCountDownTimeNanos;
	private long mStartTime, mTotalTimeNanos;
	private int mTextHeight, mTopMargin, mLeftMargin, mTopTextVerticalPosition;
	private String mTitle;
	private Paint mCirclePaint, mSecondHandPaint, mMinuteHandPaint, mHourHandPaint, mTickMarkPaint, mTextPaint, mTextPaint2, mArrowPaint;
	private Path mPath= new Path();
	private HandAngle mHandAngle;
	private DecimalFormat mRound3 = new DecimalFormat("00.000");
	private String mString;
	private Thread mThread;
	private Utilities mUtilities = Utilities.get();  // get the instance of the Utilities singleton.
	//private boolean mThreadRunning;
	private String[] numbers ={"15", "10", "5", "0", "55", "50", "45", "40", "35", "30", "25", "20"};
	private static final String PREFS_STOPWATCH_UPDATER_STARTTIME = "prefs_stopwatch_updater_starttime";
	private static final String PREFS_STOPWATCH_UPDATER_TOTALTIME = "prefs_stopwatch_updater_totaltime";
	private static final String PREFS_STOPWATCH_UPDATER_TOTALTIMENANOS ="prefs_stopwatch_totaltimenanos";
	private static final String PREFS_TIMER_UPDATER_STARTTIME = "prefs_timer_updater_starttime";
	private static final String PREFS_TIMER_UPDATER_TOTALTIME = "prefs_timer_updater_totaltime";
	private static final String PREFS_TIMER_UPDATER_TOTALTIMENANOS = "prefs_timer_updater_totaltimenanos";
	private static final String PREFS_TIMER_UPDATER_COUNTDOWNTIMEMILLIES = " prefs_timer_updater_countdowntimemillies";
	private static final String PREFS_TIMER_UPDATER_COUNTDOWNTIMENANOS = " prefs_timer_updater_countdowntimenanos";
	private static final String TAG = "## My Info ##";
	
	public ScreenUpdater(Context c, HandAngle ha, String title, boolean countdown) {
		mContext=c.getApplicationContext();
		mHandAngle = ha;
		mTitle=title;
		mCountdown= countdown;
		if (mCountdown) {
			// There can be more than one instance of this class and this
			// is a global variable so only do this only if mCountdown is true.
			mTouchEventsOk = mCountdown;
		}

		mLargeScreen = (mContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE;
		mExtraLargeScreen = (mContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;

		if(mExtraLargeScreen){
			mTickMarkLength= dpToPx(20);
		}else{
			mTickMarkLength= dpToPx(10);
		}
		if(mLargeScreen){
			mArrowDegs=1;
		}else{
			mArrowDegs=3;
		}
		mTopMargin = dpToPx(5);
		mLeftMargin = dpToPx(15);
		mSecondHandRadius = (mLargeScreen || mExtraLargeScreen) ? dpToPx(18) : dpToPx(12);
		mMinuteHandRadius = (mLargeScreen || mExtraLargeScreen) ? dpToPx(14) : dpToPx(10);
		mHourHandRadius = (mLargeScreen || mExtraLargeScreen) ? dpToPx(8) : dpToPx(4);
		
		mArrowRadiusDif = dpToPx(20); // Distance from end of hand that the back of the arrow head reaches to.

		mPath = new Path();
		mPath.setFillType(Path.FillType.EVEN_ODD);

		//Clock face paint
		mCirclePaint= new Paint();
		mCirclePaint.setColor(Color.WHITE);
		mCirclePaint.setStrokeWidth(4);
		
		mSecondHandPaint = new Paint();
		mSecondHandPaint.setColor(Color.RED);
		mSecondHandPaint.setStrokeWidth((mLargeScreen || mExtraLargeScreen) ? 4 : 8);
		
		mMinuteHandPaint = new Paint();
		mMinuteHandPaint.setColor(Color.BLUE);
		mMinuteHandPaint.setStrokeWidth((mLargeScreen || mExtraLargeScreen) ? 6 : 10);
		
		mHourHandPaint = new Paint();
		mHourHandPaint.setColor(Color.BLACK);
		mHourHandPaint.setStrokeWidth((mLargeScreen || mExtraLargeScreen) ? 8 : 12);
		
		mArrowPaint = new Paint();
		mArrowPaint.setStrokeWidth(1);
		mArrowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		
		mTickMarkPaint = new Paint();
		mTickMarkPaint.setColor(Color.BLACK);
		mTickMarkPaint.setStrokeWidth(4);

		// Paint for clock numbers
		mTextPaint = new Paint();
		mTextPaint.setColor(Color.BLACK);
		if (mExtraLargeScreen){
			mTextPaint.setTextSize(dpToPx(60));
		}else{
			if (mLargeScreen){
				mTextPaint.setTextSize(dpToPx(40));
			}else{
				mTextPaint.setTextSize(dpToPx(20));
			}
		}
		
		// Paint for title and digital readout
		mTextPaint2 = new Paint();
		mTextPaint2.setColor(Color.WHITE);
		if (mExtraLargeScreen){
			mTextPaint2.setTextSize(dpToPx(55));
		}else{
			if (mLargeScreen){
				mTextPaint2.setTextSize(dpToPx(40));
			}else{
				mTextPaint2.setTextSize(dpToPx(20));
			}
		}
		Rect rect = new Rect();
		mTextPaint2.getTextBounds("9p", 0, 2, rect);
		mTextHeight = rect.height();

		
		mArrowTipDif= dpToPx(3);
		mArrowEndDif = dpToPx(10);
	}
	
	private int dpToPx(int dp){
		// convert dips to pixels
		DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
		return (int)((dp *dm.density)+ 0.5 );
	}
	
	
	
	public void setSurfaceAndHolder(SurfaceView sv,SurfaceHolder holder){
		// The activities StopwatchActivity and TimerActivity provide
		// the SurfaceView and SurfaceHolder.
		mSurfaceView = sv;
		mHolder=holder;
		mSurfaceView.setOnTouchListener(this);
		drawClock();
	}
	
	
	@Override
	public boolean onTouch(View v, MotionEvent mv){
		// Touch events are used to decide which clock hand the
		// user is touching and calculating the clock hand position
		// as the user drags the clock hand.  This method sdoesn't work
		// on the stopwatch  because the sethand methods in
		// StopwatchHandAngle class don't do anything
		if (mTouchEventsOk){ // Don't allow user to move hands while timer is running.
			switch(mv.getActionMasked()){
			case MotionEvent.ACTION_DOWN:
				chooseHandPointedAt(mv);
				drawClock();
				break;
				
			case MotionEvent.ACTION_MOVE:
				// Move the clock hand based on where the user drags his finger.
				float x = mv.getX() - mCenterX;
				float y = mCenterY - mv.getY();
				if (mMoveHourHand){
					mHandAngle.setHourDegrees((float)Math.toDegrees(Math.atan2(y,x)));
				}else{
					if (mMoveMinuteHand){
						mHandAngle.setMinuteDegrees((float)Math.toDegrees(Math.atan2(y,x)));
					}else{
						if (mMoveSecondHand) {
							mHandAngle.setSecondDegrees((float) Math.toDegrees(Math.atan2(y, x)));
						}
					}
				}
				drawClock();
				break;
			}
		}
		return true;
	}

	private void chooseHandPointedAt(MotionEvent mv){
		// On ACTION_DOWN we have to decide which hand the user wants to move.
		// Once we decide decide which hand the user wants to move, set the
		// appropriate boolean and then call the appropriate HandAngle set
		// method.  This will cause the hand to move to a spot under the user's finger.

		mMoveHourHand = mMoveMinuteHand = mMoveSecondHand = false;
		double minDist = 0.15;  // little over 6 degrees, on tick mark on clock face.

		// Get distance of user touch from center of clock face.
		double xdist=mv.getX() - mCenterX;
		double ydist=mCenterY - mv.getY();
		double r = Math.sqrt(Math.pow(xdist,2.0) + Math.pow(ydist,2.0));

		// angle user touch position makes with center of clock face.
		double downAngle = Math.atan2(ydist, xdist);

		// Get current hand angles
		float secondHandAngle = mHandAngle.getSecondDegrees();
		float minuteHandAngle = mHandAngle.getMinuteDegrees();
		float hourHandAngle = mHandAngle.getHourDegrees();

		// Get distance of user touch from each clock hand.
		double hourDifference = Math.abs(Math.min(2*Math.PI - Math.abs(downAngle - Math.toRadians(hourHandAngle)), Math.abs(downAngle - Math.toRadians(hourHandAngle))));
		double minuteDifference = Math.abs(Math.min(2 * Math.PI - Math.abs(downAngle - Math.toRadians(minuteHandAngle)), Math.abs(downAngle - Math.toRadians(minuteHandAngle))));
		double secondDifference = Math.abs(Math.min(2 * Math.PI - Math.abs(downAngle - Math.toRadians(secondHandAngle)), Math.abs(downAngle - Math.toRadians(secondHandAngle))));

		// If user touch is too far away from any hand just ignore the touch.
		if (hourDifference > 0.3 && minuteDifference > 0.3 && secondDifference > 0.3) { // roughly 18 degrees or 3 tick marks on clock face
			return;
		}

		//Ignore touch if it is too far from edge of clock face.
		if (r > mRadius + dpToPx(20)) {
			return;
		}

		if (minuteHandAngle == secondHandAngle && hourHandAngle == secondHandAngle && downAngle > Math.PI/2.0){
			return;
		}

		if (minuteHandAngle == secondHandAngle && hourHandAngle == secondHandAngle){
			// Hands are aligned pointing at zero on the clock face.
			chooseBasedOnLength(r);
			return;
		}

		if (hourDifference < minuteDifference && hourDifference < secondDifference){
			// User touch is closest to the hour hand.

			if ((Math.abs(hourDifference - minuteDifference) < minDist && Math.abs(minuteDifference - secondDifference) < minDist) ||
				(Math.abs(hourDifference - secondDifference) < minDist && Math.abs(minuteDifference - secondDifference) < minDist)){
				// Even though touch is closest to hour hand the other two hands are two close by to decide, so
				chooseBasedOnLength(r);
				return;
			}

			if (Math.abs(hourDifference - minuteDifference) < minDist){
				// Even though touch is is closest to hour hand the minute hand is also very close. So
				// actually chose minute hand if touch is past the end of the hour hand.
				if(r > mHourHandLength){
					mMoveMinuteHand = true;
					mHandAngle.setMinuteDegrees((float) Math.toDegrees(downAngle));
				}else{
					mMoveHourHand = true;
					mHandAngle.setHourDegrees((float) Math.toDegrees(downAngle));
				}
				return;
			}

			if (Math.abs(hourDifference - secondDifference) < minDist){
				// Even though touch is is closest to hour hand the second hand is also very close. So
				// actually chose second hand if touch is past the end of the hour hand.
				if(r > mHourHandLength){
					mMoveSecondHand = true;
					mHandAngle.setSecondDegrees((float) Math.toDegrees(downAngle));
				}else{
					mMoveHourHand = true;
					mHandAngle.setHourDegrees((float) Math.toDegrees(downAngle));
				}
				return;
			}

			// Touch is closest to the hour hand and no other hands are close by, so
			// the user must mean the hour hand.
			mMoveHourHand=true;
			mHandAngle.setHourDegrees((float) Math.toDegrees(downAngle));
			return;
		}

		if (minuteDifference < secondDifference){
			// User touch is closest to the minute hand (since we know it isn't
			// closer to the hour hand from above);

			if((Math.abs(minuteDifference - hourDifference) < minDist && Math.abs(hourDifference - secondDifference) < minDist) ||
			   (Math.abs(minuteDifference - secondDifference) < minDist && Math.abs(hourDifference - secondDifference) < minDist)){
				// Simular to above touch is closest to the minute hand but the other two hands are also very close, so
				chooseBasedOnLength(r);
				return;
			}

			if (Math.abs(minuteDifference - hourDifference) < minDist){
				// Even though touch is is closest to minute hand the hour hand is also very close. So
				// only chose minute hand if touch is past the end of the hour hand.
				if (r > mHourHandLength){
					mMoveMinuteHand = true;
					mHandAngle.setMinuteDegrees((float) Math.toDegrees(downAngle));
				}else{
					mMoveHourHand = true;
					mHandAngle.setHourDegrees((float) Math.toDegrees(downAngle));
				}
				return;
			}

			if (Math.abs(minuteDifference - secondDifference) < minDist){
				// Even though touch is is closest to minute hand the secnd hand is also very close. So
				// actually chose second hand if touch is past the end of the hour hand.
				if (r > mMinuteHandLength){
					mMoveSecondHand = true;
					mHandAngle.setSecondDegrees((float) Math.toDegrees(downAngle));
				}else{
					mMoveMinuteHand = true;
					mHandAngle.setMinuteDegrees((float) Math.toDegrees(downAngle));
				}
				return;
			}

			// The touch is closest to the minute hand and no other hands are close by
			// to the user must mean the minute hand.
			mMoveMinuteHand=true;
			mHandAngle.setMinuteDegrees((float) Math.toDegrees(downAngle));
			return;
		}

		// At this point we know the touch is closest to the second hand. Run the same check as
		// above on the second hand.

		if((Math.abs(secondDifference - hourDifference) < minDist && Math.abs(hourDifference - minuteDifference) < minDist) ||
		    (Math.abs(secondDifference - minuteDifference) < minDist) && Math.abs(hourDifference - minuteDifference) < minDist){
			// As above, other hands are close by so
			chooseBasedOnLength(r);
			return;
		}

		if (Math.abs(secondDifference - hourDifference) < minDist){
			// Even though touch is is closest to second hand the hour hand is also very close. So
			// only chose second hand if touch is past the end of the hour hand.
			if (r > mHourHandLength) {
				mMoveSecondHand = true;
				mHandAngle.setSecondDegrees((float) Math.toDegrees(downAngle));
			}else{
				mMoveHourHand = true;
				mHandAngle.setHourDegrees((float) Math.toDegrees(downAngle));
			}
			return;
		}

		if (Math.abs(secondDifference - minuteDifference) < minDist){
			// Even though touch is is closest to second hand the minute hand is also very close. So
			// only chose second hand if touch is past the end of the minute hand.
			if (r > mMinuteHandLength){
				mMoveSecondHand = true;
				mHandAngle.setSecondDegrees((float) Math.toDegrees(downAngle));
			}else{
				mMoveMinuteHand = true;
				mHandAngle.setMinuteDegrees((float) Math.toDegrees(downAngle));
			}
			return;
		}
		mMoveSecondHand=true;
		mHandAngle.setSecondDegrees((float)Math.toDegrees(downAngle));
	}

	private void chooseBasedOnLength(double r){
		// If clock hands are align (as when they are all pointing at zero) or very close together
		// then choose the hand based on how far the touch was from the center of the clock face
		// and the length of the hands. For example, the second hand is the longest hand, so if
		// the touch event is further out than the end of the minute hand then we assume the user
		// is pointing to the second hand.
		if (r > mMinuteHandLength){
			mMoveSecondHand = true;

		}else{
			if (r > mHourHandLength){
				mMoveMinuteHand = true;
			}else{
				mMoveHourHand=true;
			}
		}
	}

	public void start(){
		// Used by StopwatchActivity and TimerActivity in response to the user
		// pressing the START button.  Set initial values and start the thread
		// that moves the clock hands
		if(mCountdown) {
			// StopwatchActivity may have another instance of this class and we
			// are setting a global variable so only do this if mCountdown is true.
			mTouchEventsOk = false;
		}

		// Get the number of seconds, minutes, and hours the user has
		// specified by dragging the clock hands.  If this instance is being
		// used by StopwatchActivity the the hands will be pointing at zero.
		mSeconds=mHandAngle.degToTickmarkNumber(mHandAngle.getSecondDegrees());
		mMinutes=mHandAngle.degToTickmarkNumber(mHandAngle.getMinuteDegrees());
		mHours=mHandAngle.degToTickmarkNumber(mHandAngle.getHourDegrees());

		// Calculate the total countdown time in milli-seconds. Only used if this
		// instance is being user by TimerActivity
		mCountDownTimeMillis = (mHours*3600 + mMinutes*60 + mSeconds) * 1000L;
		mCountDownTimeNanos = (mHours*3600 + mMinutes*60 + mSeconds) * 1000000000L;

		mTotalTimeSecs = 0;
		mTotalTimeNanos = 0;

		// Start thread to move hands.  Thread will run as long as mRunning is true.
		mRunning=true;
		mThread = new Thread (this);
		mStartTime = SystemClock.elapsedRealtimeNanos();
		mThread.start();
	}

	public void restart(){
		// Restart the thread that moves the clock hands without initializing any variables.
		// Restarts are required for several reasons.  The user may have stopped the stopwatch
		// and then started it again without resetting the time to zero.  The StopwatchActivity
		// or the TimerActivity may have been sent into the background or killed completely
		// and has now been restarted either by the user or in the case of the timer by
		// AlarmReceiver.  We want the time to continue running.

		if (mCountdown) {
			// StopwatchActivity may have another instance of this class and we
			// are setting a global variable so only do this if mCountdown is true.
			mTouchEventsOk = false;
		}

		// Start the thread that moves the clock hands.
		mRunning=true;
		mThread = new Thread(this);
		mThread.start();
		if (mCountdown) {
			// If TimerActivity was sent to background or killed then an alarm was set to wake the
			// the app and start the TimerActivity when the countdown would have completed. However the
			// activity may have been restarted by the user, so cancel the alarm.  If the alarm has already
			// gone off, no harm is done by canceling an alarm that no longer exists.
			Intent broadcastIntent = new Intent("com.clarkgarrett.stopwatchandtimer.Alarm");
			PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 1, broadcastIntent, 0);
			AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
			alarmManager.cancel(pendingIntent);
		}
	}

	public void stop(){
		mRunning = false;  // Tell thread to stop.
		try {
			if(mThread != null){
				mThread.join();  // Wait for thread to end;
			}
		}catch(InterruptedException e){}
	}

	public void stopAndWait() {
		// Stop the thread that moves the clock hands and wait until
		// thread actually stops. Then HandAngle.pause.  Only do this
		// if mRunning is true.  If mRunning is false, then the user
		// hit the stop button to stop the alarm, not the clock hands.
		if (mRunning) {
			mRunning = false;  // Tell thread to stop.
			try {
				if (mThread != null) {
					mThread.join();  // Wait for thread to end;
				}
			} catch (InterruptedException e) {
			}
			if (mCountdown) {
				mHandAngle.pause();
			}
		}
	}

	@TargetApi(23)
	public void stopAndSetAlarm(){
		// If the TimerActivity is being sent to the background or killed while the timer is actually running
		// it calls this method.  This method sets an alarm with a broadcast PendingIntent that will go off
		// when the timer would have finished it's countdown.  The broadcast is received by the AlarmReceiver
		// class.  AlarmReceiver may restart the TimerActivity but at the very least it will cause the device
		// to start chiming and or vibrating.  This method is complicated by the fact that the appropriate
		// AlarmManager method to call to set an exact alarm varies depending what which version of the
		// android SDK the device is running on.

		// First, stop the thread that moves the clock hands.
		mRunning = false;

		// This is the intent that will be broadcast when the alarm goes off.
		Intent broadcastIntent = new Intent("com.clarkgarrett.stopwatchandtimer.Alarm");
		PendingIntent broadcastPendingIntent = PendingIntent.getBroadcast(mContext, 1, broadcastIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		// Calculate how may milli-seconds in the future we want the alarm to go off.
		long  timeMillies = (long)(mCountDownTimeMillis - (mTotalTimeSecs *1000.0)); // Milli-seconds left on the timer
		long futureTime = SystemClock.elapsedRealtime() + timeMillies;

		AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);

		//Set the alarm using the method appropriate for the Android version the device is running on.
		if (Build.VERSION.SDK_INT >= 23){
			alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureTime, broadcastPendingIntent);
		}else {
			if (Build.VERSION.SDK_INT >= 21) {
				// Android versions 21 and 22 require us to use the AlarmManager seAlarmClock() method to set an exact alarm.
				// The setAlarmClock method requires an AlarmManager.AlarmClockInfo object as one of it's parameters.
				// The AlarmClockInfo constructor requires its own pending intent.  setAlarmClock() also requires using wall
				// clock time which is less desirable than total elapse because user could change wall clock time and mess us up.
				Intent alarmclockInfoIntent = new Intent("com.clarkgarrett.stopwatchandtimer.Alarm");
				PendingIntent alarmclockInfoPendingIntent = PendingIntent.getBroadcast(mContext, 2, alarmclockInfoIntent, PendingIntent.FLAG_CANCEL_CURRENT);
				long wctTime = System.currentTimeMillis() + timeMillies;
				AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(wctTime , alarmclockInfoPendingIntent);
				alarmManager.setAlarmClock(alarmClockInfo, broadcastPendingIntent);
			} else {
				if (Build.VERSION.SDK_INT >= 19) {
					alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureTime, broadcastPendingIntent);
				} else {
					alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureTime, broadcastPendingIntent);
				}
			}
		}

		// Make sure thread has ended before we return.
		try {
			if(mThread != null){
				mThread.join();  // Wait for thread to end;
			}
		}catch(InterruptedException e){}
	}

	public void storeData(SharedPreferences prefs){
		// If either the StopwatchActivity or the TimerActivity are going into background or dying
		// they call this method to save the current state of the ScreenUpdater.  This information
		// can be used when the activity is restarted to set the clock hands to the positions they
		// would have been in if the activity had remained visible.  This info is also used by
		// the countdown timer to keep track of how much time has counted down.
		SharedPreferences.Editor prefsEditor = prefs.edit();
		if (mCountdown){
			prefsEditor.putLong(PREFS_TIMER_UPDATER_STARTTIME, mStartTime);
			prefsEditor.putLong(PREFS_TIMER_UPDATER_TOTALTIMENANOS, mTotalTimeNanos);
			prefsEditor.putLong(PREFS_TIMER_UPDATER_TOTALTIME, Double.doubleToRawLongBits(mTotalTimeSecs));
			prefsEditor.putLong(PREFS_TIMER_UPDATER_COUNTDOWNTIMEMILLIES, Double.doubleToRawLongBits(mCountDownTimeMillis));
			prefsEditor.putLong(PREFS_TIMER_UPDATER_COUNTDOWNTIMENANOS, mCountDownTimeNanos);
		}else {
			prefsEditor.putLong(PREFS_STOPWATCH_UPDATER_STARTTIME, mStartTime);
			prefsEditor.putLong(PREFS_STOPWATCH_UPDATER_TOTALTIMENANOS, mTotalTimeNanos);
			prefsEditor.putLong(PREFS_STOPWATCH_UPDATER_TOTALTIME, Double.doubleToRawLongBits(mTotalTimeSecs));
		}
		prefsEditor.commit();
	}

	public void retrieveData(SharedPreferences prefs){
		// Called by TimerActivity or Stopwatch activity when it become visible again.
		// Retrieve data stored in storeData().  The thread that moves the clock hands will
		// adjust the hands to their new positions based on the difference in these stored
		// times and the current time.
		if(mCountdown){
			mStartTime = prefs.getLong(PREFS_TIMER_UPDATER_STARTTIME, 0);
			mTotalTimeNanos = prefs.getLong(PREFS_TIMER_UPDATER_TOTALTIMENANOS, 0);
			mTotalTimeSecs = Double.longBitsToDouble(prefs.getLong(PREFS_TIMER_UPDATER_TOTALTIME, Double.doubleToLongBits(0)));
			mCountDownTimeMillis = Double.longBitsToDouble(prefs.getLong(PREFS_TIMER_UPDATER_COUNTDOWNTIMEMILLIES, Double.doubleToLongBits(0)));
			mCountDownTimeNanos = prefs.getLong(PREFS_TIMER_UPDATER_COUNTDOWNTIMENANOS , 0);
		}else {
			mStartTime = prefs.getLong(PREFS_STOPWATCH_UPDATER_STARTTIME, 0);
			mTotalTimeNanos = prefs.getLong(PREFS_STOPWATCH_UPDATER_TOTALTIMENANOS, 0);
			mTotalTimeSecs = Double.longBitsToDouble(prefs.getLong(PREFS_STOPWATCH_UPDATER_TOTALTIME, Double.doubleToLongBits(0)));
		}
	}

	public void reset(){
		// Called by StopwatchActivity to reset the clock hands and the elapse
		// time to zero.  Note: this method does not stop the clock hands
		// from moving if the stopwatch is running.
		mHandAngle.reset();
		mTotalTimeSecs =0;
		mTotalTimeNanos = 0;
		drawClock();
	}

	@Override
	public void run(){
		// This run method is used in a separate thread.  It runs a loop that
		// run as long as the variable mRunning is true.  Each time through
		//the loop it calculates how long it took to run the loop and adds
		// this time to an accumulating total time.  This total time is
		// passed to the HandAngle.updateAngles method which uses the time
		// to calculate the new positions of the clock hands.  Every so
		// often the updateAngles method return true. Then we re-draw
		// the clock which shows the clock hands in their new positions.
		// It is up to the HandAngle class to determine how often to
		// re-draw the clock hands.  If this ScreenUpdater instance is
		// being used by the countdown timer and the total time exceeds
		// the countdown time then the loop is stopped and the alarm
		// is sounded/vibrated. The loop for the stopwatch is stopped
		// if the user presses the STOP button.  This thread is also
		// stopped if the app goes into the background or is killed.
		// It is restarted when the app comes back using save data.
		long newTime;
		long deltaTime;
		while(mRunning){
			newTime = SystemClock.elapsedRealtimeNanos();  // Get current time
			deltaTime=(newTime-mStartTime);  // Calculate how long it took to run the loop in nano-seconds

			// Keep total time loop has been running.
			mTotalTimeSecs += deltaTime/1000000000.0f;  //used by drawCLock();
			mTotalTimeNanos += deltaTime;  // used by HandAngle.updateAngles();

			mChanged=mHandAngle.updateAngles(mTotalTimeNanos); // pass delta time to updatAngles();
			mStartTime=newTime;  // Save this time for the next calculation
			if(mChanged){
				drawClock();  // Re-draw the clock with the hands in their new positions.
			}

			if (mCountdown && mTotalTimeNanos >= mCountDownTimeNanos) {
				// This instance of ScreenUpdater is being used by TimerActivity
				// and the timer has run down.
				mUtilities.startAlarm();  // Sound/vibrate the alarm
				mRunning=false;   // stop the loop
				mTimerRunning = false;  // Tell the rest of the app that the timer is not running
				mHandAngle.reset();  // put all hands at exactly zero.
				drawClock();
			}
		}
	}

	private void drawClock(){
		// This method draws the the clock. It is called at various points, but in particular
		// it is called by the thread that updates the clock hand angles and when the user is
		// dragging the clock hands.  As the the clock is repeatedly re-drawn with the hands
		// in different positions, they appear to move.
		mCanvas = mHolder.lockCanvas();
		if (mCanvas == null){
			return;
		}
		mCenterX = mCanvas.getWidth()/2;
		mCenterY = mCanvas.getHeight()/2;
		int verticalSpace = mCanvas.getHeight() - 2*(mTextHeight + mTopMargin);
		mRadius= Math.min(mCenterX, verticalSpace/2)-dpToPx(30);
		mTopTextVerticalPosition = Math.max(mTextHeight + mTopMargin , ((mCanvas.getHeight() - 2*mRadius)/2)/2);
		mDistanceFromEnd = mRadius/8;

		mMinuteHandLength= mRadius - 2*mTickMarkLength;
		mHourHandLength= mRadius - ((mLargeScreen) ? 6 : 4)*mTickMarkLength;

		int length;
		double angle;

		float seconthandAngle = mHandAngle.getSecondDegrees();
		float minutehandAngle = mHandAngle.getMinuteDegrees();
		float hourhandDegrees = mHandAngle.getHourDegrees();

		// Draw the circle for the clock face.
		mCanvas.drawCircle(mCenterX, mCenterY, mRadius, mCirclePaint);

		// Draw the tick marks and the numbers
		int i = 0;
		for (int deg=0; deg <360; deg += 6){
			if (deg % 30 == 0 ){
				length = mTickMarkLength*2;
				drawNumbers(mCanvas,deg,i);
				i++;
			}else{
				length = mTickMarkLength;
			}
			angle = Math.toRadians(deg);
			mCanvas.drawLine((float)mCenterX+(float)((mRadius-length)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-length)*Math.sin(angle)),
					(float)mCenterX+(float)(mRadius*Math.cos(angle)), (float)mCenterY-(float)(mRadius*Math.sin(angle)), mTickMarkPaint);
		}
		
		//Draw second hand.
		mCanvas.drawCircle(mCenterX, mCenterY, mSecondHandRadius, mSecondHandPaint);
		angle = Math.toRadians(seconthandAngle);
		mHandEndX=(float)mCenterX+(float)(mRadius*Math.cos(angle));
		mHandEndY =  (float)mCenterY-(float)(mRadius*Math.sin(angle));
		mCanvas.drawLine((float) mCenterX, (float) mCenterY, mHandEndX, mHandEndY, mSecondHandPaint);
		mArrowPaint.setColor(Color.RED);
		mPath.reset();
		mPath.moveTo((float) mCenterX + (float) (mSecondHandRadius * Math.cos(Math.PI / 2 - angle)), (float) mCenterY + (float) (mSecondHandRadius * Math.sin(Math.PI / 2 - angle)));
		mPath.lineTo((float) mCenterX + (float) (mDistanceFromEnd * Math.cos(angle)), (float) mCenterY - (float) (mDistanceFromEnd * Math.sin(angle)));
		mPath.lineTo((float) mCenterX - (float) (mSecondHandRadius * Math.cos(Math.PI / 2 - angle)), (float) mCenterY - (float) (mSecondHandRadius * Math.sin(Math.PI / 2 - angle)));
		mPath.lineTo((float) mCenterX + (float) (mSecondHandRadius * Math.cos(Math.PI / 2 - angle)), (float) mCenterY + (float) (mSecondHandRadius * Math.sin(Math.PI / 2 - angle)));
		mPath.close();
		mCanvas.drawPath(mPath, mArrowPaint);
		angle = Math.toRadians((seconthandAngle + mArrowDegs ) %360);
		mPath.reset();
		mPath.moveTo(mHandEndX, mHandEndY);
		mPath.lineTo((float) mCenterX + (float) ((mRadius - mArrowRadiusDif) * Math.cos(angle)), (float) mCenterY - (float) ((mRadius - mArrowRadiusDif) * Math.sin(angle)));
		angle = Math.toRadians(seconthandAngle);
		mPath.lineTo((float) mCenterX + (float) ((mRadius - mArrowEndDif) * Math.cos(angle)), (float) mCenterY - (float) ((mRadius - mArrowEndDif) * Math.sin(angle)));
		angle = Math.toRadians((seconthandAngle - mArrowDegs ) %360);
		mPath.lineTo((float) mCenterX + (float) ((mRadius - mArrowRadiusDif) * Math.cos(angle)), (float) mCenterY - (float) ((mRadius - mArrowRadiusDif) * Math.sin(angle)));
		mPath.close();
		mCanvas.drawPath(mPath, mArrowPaint);



		//Draw minute hand.
		mCanvas.drawCircle(mCenterX, mCenterY, mMinuteHandRadius, mMinuteHandPaint);
		angle = Math.toRadians(minutehandAngle);
		mHandEndX=(float)mCenterX+(float)((mMinuteHandLength)*Math.cos(angle));
		mHandEndY=(float)mCenterY-(float)((mMinuteHandLength)*Math.sin(angle));
		mArrowTipX=(float)mCenterX+(float)((mMinuteHandLength + mArrowTipDif)*Math.cos(angle));
		mArrowTipY=(float)mCenterY-(float)((mMinuteHandLength + mArrowTipDif)*Math.sin(angle));
		mCanvas.drawLine((float) mCenterX, (float) mCenterY, mHandEndX, mHandEndY, mMinuteHandPaint);
		mArrowPaint.setColor(Color.BLUE);
		mPath.reset();
		mPath.moveTo((float) mCenterX + (float) (mMinuteHandRadius * Math.cos(Math.PI / 2 - angle)), (float) mCenterY + (float) (mMinuteHandRadius * Math.sin(Math.PI / 2 - angle)));
		mPath.lineTo((float) mCenterX + (float) (mDistanceFromEnd * Math.cos(angle)), (float) mCenterY - (float) (mDistanceFromEnd * Math.sin(angle)));
		mPath.lineTo((float) mCenterX - (float) (mMinuteHandRadius * Math.cos(Math.PI / 2 - angle)), (float) mCenterY - (float) (mMinuteHandRadius * Math.sin(Math.PI / 2 - angle)));
		mPath.lineTo((float) mCenterX + (float) (mMinuteHandRadius * Math.cos(Math.PI / 2 - angle)), (float) mCenterY + (float) (mMinuteHandRadius * Math.sin(Math.PI / 2 - angle)));
		mPath.close();
		mCanvas.drawPath(mPath, mArrowPaint);
		angle = Math.toRadians((minutehandAngle + mArrowDegs+1 ) %360);
		mPath.reset();
		//mPath.setFillType(Path.FillType.EVEN_ODD);
		mPath.moveTo(mArrowTipX, mArrowTipY);
		mPath.lineTo((float) mCenterX + (float) ((mMinuteHandLength - mArrowRadiusDif) * Math.cos(angle)), (float) mCenterY - (float) ((mMinuteHandLength - mArrowRadiusDif) * Math.sin(angle)));
		angle = Math.toRadians(minutehandAngle);
		mPath.lineTo((float)mCenterX+(float)((mMinuteHandLength - mArrowEndDif)*Math.cos(angle)) , (float)mCenterY-(float)((mMinuteHandLength - mArrowEndDif)*Math.sin(angle)) );
		angle = Math.toRadians((minutehandAngle - mArrowDegs-1 ) %360);
		mPath.lineTo((float) mCenterX + (float) ((mMinuteHandLength - mArrowRadiusDif) * Math.cos(angle)), (float) mCenterY - (float) ((mMinuteHandLength - mArrowRadiusDif) * Math.sin(angle)));
		mPath.close();
		mCanvas.drawPath(mPath, mArrowPaint);

		//Draw hour hand if needed.
		if(mCountdown){
			mCanvas.drawCircle(mCenterX, mCenterY, mHourHandRadius, mHourHandPaint);
			angle = Math.toRadians(hourhandDegrees);
			mHandEndX=(float)mCenterX+(float)((mHourHandLength)*Math.cos(angle));
			mHandEndY=(float)mCenterY-(float)((mHourHandLength)*Math.sin(angle));
			mArrowTipX=(float)mCenterX+(float)((mHourHandLength + mArrowTipDif)*Math.cos(angle));
			mArrowTipY=(float)mCenterY-(float)((mHourHandLength + mArrowTipDif)*Math.sin(angle));
			mCanvas.drawLine((float) mCenterX, (float) mCenterY, mHandEndX, mHandEndY, mHourHandPaint);
			mArrowPaint.setColor(Color.BLACK);
			mPath.reset();
			mPath.moveTo((float) mCenterX + (float) (mHourHandRadius * Math.cos(Math.PI / 2 - angle)), (float) mCenterY + (float) (mHourHandRadius * Math.sin(Math.PI / 2 - angle)));
			mPath.lineTo((float) mCenterX + (float) (mDistanceFromEnd * Math.cos(angle)), (float) mCenterY - (float) (mDistanceFromEnd * Math.sin(angle)));
			mPath.lineTo((float) mCenterX - (float) (mHourHandRadius * Math.cos(Math.PI / 2 - angle)), (float) mCenterY - (float) (mHourHandRadius * Math.sin(Math.PI / 2 - angle)));
			mPath.lineTo((float) mCenterX + (float) (mHourHandRadius * Math.cos(Math.PI / 2 - angle)), (float) mCenterY + (float) (mHourHandRadius * Math.sin(Math.PI / 2 - angle)));
			mPath.close();
			mCanvas.drawPath(mPath, mArrowPaint);
			mPath.reset();
			mPath.moveTo(mArrowTipX, mArrowTipY);
			angle = Math.toRadians((hourhandDegrees + mArrowDegs+2 ) %360);
			mPath.lineTo((float) mCenterX + (float) ((mHourHandLength - mArrowRadiusDif) * Math.cos(angle)), (float) mCenterY - (float) ((mHourHandLength - mArrowRadiusDif) * Math.sin(angle)));
			angle = Math.toRadians(hourhandDegrees);
			mPath.lineTo((float) mCenterX + (float) ((mHourHandLength - mArrowEndDif) * Math.cos(angle)), (float) mCenterY - (float) ((mHourHandLength - mArrowEndDif) * Math.sin(angle)) );
			angle = Math.toRadians((hourhandDegrees - mArrowDegs-2 ) %360);
			mPath.lineTo((float) mCenterX + (float) ((mHourHandLength - mArrowRadiusDif) * Math.cos(angle)), (float) mCenterY - (float) ((mHourHandLength - mArrowRadiusDif) * Math.sin(angle)));
			mPath.close();
			mArrowPaint.setColor(Color.BLACK);
			mCanvas.drawPath(mPath, mArrowPaint);
		}

		// Print the title and the digital readout.
		mCanvas.drawRect(0, mCenterY + mRadius + 3 ,mCanvas.getWidth(), mCanvas.getHeight(), mTextPaint);
		if (mCountdown){
			mCanvas.drawText(mTitle, mLeftMargin, mTopTextVerticalPosition, mTextPaint2);
			mSeconds=mHandAngle.degToTickmarkNumber(mHandAngle.getSecondDegrees());
			mMinutes=mHandAngle.degToTickmarkNumber(mHandAngle.getMinuteDegrees());
			mHours=mHandAngle.degToTickmarkNumber(hourhandDegrees);
			mCanvas.drawText(("" + mHours + ":" + String.format("%02d", mMinutes) +":" + String.format("%02d", mSeconds)), dpToPx(20), mCenterY + mRadius + mTextHeight + dpToPx(5), mTextPaint2);
		}else{
			if(mStopwatchMode == LAP_TIME){
				mCanvas.drawText(mContext.getString( R.string.lap_time_mode), mLeftMargin, mTopTextVerticalPosition, mTextPaint2);
			}else{
				mCanvas.drawText(mContext.getString( R.string.split_time_mode), mLeftMargin, mTopTextVerticalPosition, mTextPaint2);
			}
			mCanvas.drawText("" + String.format("%02d", (int) mTotalTimeSecs / 60) + ":" + mRound3.format(mTotalTimeSecs % 60.0), mLeftMargin, mCenterY + mRadius + mTextHeight + mTopMargin , mTextPaint2);
		}
		mHolder.unlockCanvasAndPost(mCanvas);
	}
	
	private void drawNumbers(Canvas canvas,int deg,int i){
		double angle;
		int textDistance=mTickMarkLength*3;
		if (mExtraLargeScreen){
			switch (i) {
			case 0: //15
				angle = Math.toRadians(deg-5);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-2.0*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2.0*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 1: //10
				angle = Math.toRadians(deg +1);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-2.0*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2.0*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 2: //5
				angle = Math.toRadians(deg +2);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.7*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.7*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 3: //0
				angle = Math.toRadians(deg+3.4);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.5*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.5*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 4: //55
				angle = Math.toRadians(deg +9);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.2*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.2*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 5: //50
				angle = Math.toRadians(deg+5);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 6: //45
				angle = Math.toRadians(deg +4);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 7: //40
				angle = Math.toRadians(deg );
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-0.8*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-0.8*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 8: //35
				angle = Math.toRadians(deg -3);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-0.8*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-0.8*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 9: //30
				angle = Math.toRadians(deg -6);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 10: //25
				angle = Math.toRadians(deg -5);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.3*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.3*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			case 11: //20
				angle = Math.toRadians(deg -8);
				canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.7*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.7*textDistance)*Math.sin(angle)),mTextPaint);
				break;
			}
		}else{
			if (mLargeScreen){
				switch (i) {
				case 0: 
					angle = Math.toRadians(deg-3);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-2.5*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2.5*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 1:
					angle = Math.toRadians(deg +1);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-2.5*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2.5*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 2:
					angle = Math.toRadians(deg +2);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-2*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 3:
					angle = Math.toRadians(deg+3);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-2*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 4:
					angle = Math.toRadians(deg +4);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 5:
					angle = Math.toRadians(deg);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 6:
					angle = Math.toRadians(deg +4);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 7:
					angle = Math.toRadians(deg );
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 8:
					angle = Math.toRadians(deg -3);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 9:
					angle = Math.toRadians(deg -6);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 10:
					angle = Math.toRadians(deg -5);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.5*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.5*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 11:
					angle = Math.toRadians(deg -7);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-2*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				}
			}else{
				switch (i) {
				case 0: //15
					angle = Math.toRadians(deg-6);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.7*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2.5*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 1: //10
					angle = Math.toRadians(deg +3);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.7*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.7*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 2: //5
					angle = Math.toRadians(deg +2);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.5*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.5*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 3: //0
					angle = Math.toRadians(deg+3);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.5*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.5*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 4: //55
					angle = Math.toRadians(deg +8);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.3*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.3*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 5: //50
					angle = Math.toRadians(deg-2);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 6: //45
					angle = Math.toRadians(deg +6);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-2*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 7: //40
					angle = Math.toRadians(deg +2);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 8: //35
					angle = Math.toRadians(deg -3);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 9: //30
					angle = Math.toRadians(deg -6);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 10: //25
					angle = Math.toRadians(deg -5);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.3*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.3*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				case 11: //20
					angle = Math.toRadians(deg -7);
					canvas.drawText(numbers[i], (float)mCenterX+(float)((mRadius-1.5*textDistance)*Math.cos(angle)), (float)mCenterY-(float)((mRadius-1.5*textDistance)*Math.sin(angle)),mTextPaint);
					break;
				}
			}
		}
	}
}
