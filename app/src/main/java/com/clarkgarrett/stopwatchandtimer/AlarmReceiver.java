package com.clarkgarrett.stopwatchandtimer;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.clarkgarrett.stopwatchandtimer.Activities.StartupActivity;
import com.clarkgarrett.stopwatchandtimer.Activities.StopwatchActivity;
import com.clarkgarrett.stopwatchandtimer.Activities.TimerActivity;

import static com.clarkgarrett.stopwatchandtimer.Utilities.START_IN_BACKGROUND_EXTRA;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mStartedFromAlarm;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mThreadReady;
import static com.clarkgarrett.stopwatchandtimer.Utilities.startAlarm;
import static com.clarkgarrett.stopwatchandtimer.Utilities.timerActivity;

/**
 * If TimerActivity enters onPause while the timer is running, this app sets an alarm
 * that goes off at the time the timer would have finished its countdown.  The alarm
 * broadcasts an intent that is received by this BroadcastReceiver.  This receiver either
 * sounds the alarm directly or starts/restarts the TimerActivity which will then sound the
 * alarm.
 */

public class AlarmReceiver extends WakefulBroadcastReceiver{
    private Utilities mUtilities = Utilities.get();  // the instance of Utilities singleton.
    private static final String TAG = "## My Info ##";

    @Override
    public void onReceive(Context context, Intent intent) {

        if(StopwatchActivity.isRunning && timerActivity != null){
            // If the user is using the stopwatch we don't want the
            // timer activity to appear on interrupt him.  So we
            // want to just sound the alarm.  But we need a viable
            // TimerActivity because the Utilities.startAlarm() method
            // needs to run some commands on the UI thread.
            startAlarm = true;
            startAlarm();
        }else {
            startAlarm = true; // used by TimerActivity
            mStartedFromAlarm = true; // used by TimerActivity

            //First start the StartupActivity so we will have the proper activity back stack.
            Intent startupIntent = new Intent(context, StartupActivity.class);
            startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(startupIntent);

            // Now start the TimerActivity.
            Intent activityIntent = new Intent(context, TimerActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (StopwatchActivity.isRunning) {
                // As explained above, we don't want the TimerActivity to replace
                // the StopwatchActivity.  This extra tell the TimerActivity to
                // immediately restart the StopwatchActivity.
                activityIntent.putExtra(START_IN_BACKGROUND_EXTRA, true);
            }
            context.startActivity(activityIntent);

            // If device is sleeping and this receiver finishes before the TimerActivity starts, then the
            // TimerActivity never starts.  So start a thread to keep this instance alive until
            //the TimerActivity starts.
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    //Pole until TimerActivity starts.
                    int i =0;
                    while ( ! TimerActivity.isRunning  && i < 10) {
                        try {
                            Thread.sleep(20);
                        }catch(InterruptedException e){}
                        i++;
                    }
                }
            });
            t.start();
        }

        }

    private void startAlarm(){
        mThreadReady = false;
        mUtilities.startAlarm();
        // As explained above, keep this instance alive until
        // alarm thread is actually running.
        while (! mThreadReady) {
            try {
                Thread.sleep(10);
            }catch(InterruptedException e) {}
        }
        startAlarm = false;
    }

}
