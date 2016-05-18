package com.clarkgarrett.stopwatchandtimer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;

import com.clarkgarrett.stopwatchandtimer.Enums.StopwatchModes;
import com.clarkgarrett.stopwatchandtimer.Enums.VibrateModes;

import static com.clarkgarrett.stopwatchandtimer.Enums.StopwatchModes.LAP_TIME;
import static com.clarkgarrett.stopwatchandtimer.Enums.VibrateModes.VIBRATE_ALWAYS;
import static com.clarkgarrett.stopwatchandtimer.Enums.VibrateModes.VIBRATE_WHEN_MUTED;


/**
 * Created by Karl on 3/11/2016.
 *
 * This class is used mostly as a place to declare variables that need to
 * be used my more than one other class.  However it is also used as a
 * singleton for running the startAlarm method.  This method is used by
 * both AlarmReceiver, ScreenUpdater, and TimerActivity to play/vibrate
 * the alarm when the timer finishes its countdown.
 */
public class Utilities implements  Runnable{
    public static ScreenUpdater mTimerScreenUpdater;
    public static ScreenUpdater mStopwatchScreenUpdater;
    public static Activity timerActivity;
    public static Context applicationContext;
    public static boolean mStartedFromAlarm = false;
    public static boolean mTimerRunning = false;
    public static boolean mStopwatchRunning = false;
    public static boolean mAlarmHasRung;
    public static boolean mTouchEventsOk;
    public static Button mButton_Start;
    public static final String PREFS_NAME= "prefs_name";
    public static final String PREFS_VIBRATE_MODE= "prefs_vibrate_mode";
    public static final String PREFS_ALARM_LENGTH = "prefs_alarm_length";
    public static final String PREFS_TIMER_RUNNING = "prefs_timer_running";
    public static final String PREFS_STOPWATCH_MODE = "prefs_stopwatch_mode";
    public static final String PREFS_STOPWATCH_RUNNING = "prefs_stopwatch_running";
    public Thread alarmThread = new Thread(this);
    private static Utilities mUtilities;
    private static long[] mPattern = {0, 1000, 500};
    public static boolean mThreadReady = false;
    public static int mAlarmLengthSecs = 30;
    public static VibrateModes mVibrateMode = VIBRATE_WHEN_MUTED;
    public static StopwatchModes mStopwatchMode = LAP_TIME;
    public static final String START_IN_BACKGROUND_EXTRA = "start_in_background_extra";
    public static boolean startAlarm = false;
    private static final String TAG = "## My Info ##";
    
    private Utilities() {};

    // Getter to create and return the singleton object.
    public static Utilities get() {
        if (mUtilities == null){
            mUtilities = new Utilities();
        }
        return mUtilities;
    }

    public  void startAlarm(){
        // Its barely possible that startalarm could be called from
        // Two threads for the same countdown so check for this.
        if ( ! alarmThread.isAlive()  && ! mAlarmHasRung) {
            alarmThread = new Thread(this);
            alarmThread.start();
        }else{
            if(mAlarmHasRung){
                mTouchEventsOk = true;
                (timerActivity).runOnUiThread(new Runnable() {
                    public void run() {
                        (timerActivity).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                });
            }
        }
    }

    public void run(){
        // If activity has been restarted by AlarmReceiver the START button
        // may be enabled.  We want disabled while the alarm is sounding.
        (timerActivity).runOnUiThread(new Runnable() {
            public void run() {
                mButton_Start.setEnabled(false);
            }
        });

        // Play the TYPE_ALARM ringtone is user has set one, otherwise play
        // the TYPE_NOTIFICATION ringtone.
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alert == null)
            alert= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone ringtone = RingtoneManager.getRingtone(timerActivity.getApplicationContext(), alert);
        ringtone.play();

        // If the device has a vibrator we may want to vibrate the device.  If the user has
        // set tha app's vibrate mode to VIBRATE_ALWAYS then vibrate the device.  If the user
        // has set the app to VIBRATE_WHEN_MUTED then check if device is muted before
        // vibrating device.
        Vibrator vibrator = (Vibrator) timerActivity.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            if (mVibrateMode == VIBRATE_ALWAYS){
                vibrator.vibrate(mPattern,0);
            }else{
                AudioManager am = (AudioManager) timerActivity.getSystemService(Context.AUDIO_SERVICE);
                if ((am.getRingerMode() == AudioManager.RINGER_MODE_SILENT || am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)
                        && mVibrateMode == VIBRATE_WHEN_MUTED){
                    vibrator.vibrate(mPattern,0);
                }
            }
        }

        mThreadReady = true; // Used by AlarmReceiver to wait until thread has actually started.

        // Have the thread sleep for the length of time the alarm is suppose to ring.
        // If user presses STOP button we will interrupt the thread.
        try {
            Thread.sleep(mAlarmLengthSecs * 1000); // Sleep method requires time in milli-seconds
        }catch (InterruptedException e){}

        // Alarm has run long enough so turn it off.
        ringtone.stop();
        vibrator.cancel();
        mAlarmHasRung = true;
        (timerActivity).runOnUiThread(new Runnable() {
            public void run() {
                mButton_Start.setEnabled(true);
                (timerActivity).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        mTouchEventsOk = true;  // Allow user to drag clock hands.
    }

    public static boolean isNewVersion(String prefsKey){
        // If the current version code of the app is greater than the version code stored
        // in SharedPreferences at prefsKey return true else return false.  This routine
        // is used by StopwatchActivity and TimerActivity to tell if it is the first run of
        // the activity on a new version of the app.

        //First get the current version code.  Assign it to zero if we can't get it for some reason.
        int currentVersionCode = 0;
        try {
            currentVersionCode = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0).versionCode;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            // handle exception
            e.printStackTrace();
            return false;
        }

        // Next get the value stored in SharedPreferences.
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, applicationContext.MODE_PRIVATE);
        int savedVersionCode = prefs.getInt(prefsKey, -1);

        // Compare the values.
        if (currentVersionCode > savedVersionCode){
            // Store the new value.
            prefs.edit().putInt(prefsKey, currentVersionCode).commit();
            return true;
        }
        return false;
    }
}
