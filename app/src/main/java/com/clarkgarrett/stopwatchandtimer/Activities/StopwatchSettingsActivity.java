package com.clarkgarrett.stopwatchandtimer.Activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import com.clarkgarrett.stopwatchandtimer.R;

import static com.clarkgarrett.stopwatchandtimer.Enums.StopwatchModes.LAP_TIME;
import static com.clarkgarrett.stopwatchandtimer.Enums.StopwatchModes.SPLIT_TIME;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_NAME;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_STOPWATCH_MODE;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mStopwatchMode;

/**
 * This activity is started by StopwatchActivity in response to it's Settings option
 * in the actionbar.  It shows a DONE Button and one Spinner that allows the user to choose which
 * of two modes the stopwatch operates in.  When the user pauses the stopwatch, it
 * continues to accumulate time while the system is paused.  In lap time mode the
 * time restarts at zero, i.e. starting the time for a new lap.  In split time mode
 * the time continues to accumulate the from the time the stopwatch was paused,
 * allowing the user to view the split time and then continue with the total
 * accumulated time. When the user is done with this activity, (back press, up arrow press or DONE Button
 * press> the chosen value is store in shared preferences, and a global variable in the Utilities class.
*/


public class StopwatchSettingsActivity extends AppCompatActivity {

    private Spinner m_spnStopwatchOptions;
    private SharedPreferences mPrefs;
    Button m_btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch_settings);
        m_spnStopwatchOptions = (Spinner)findViewById(R.id.spnStopwatchMode);

        // Display the current stopwatch mode as the spinner's current selected item.
        // The 0 and 1 in the below switch statement correspond to the values in the
        // stopwatch_options array on the res/values/strings file.  The case values
        // are from the StopwatchModes Enum.
        switch (mStopwatchMode) {
            case LAP_TIME:
                m_spnStopwatchOptions.setSelection(0);
                break;
            case SPLIT_TIME:
                m_spnStopwatchOptions.setSelection(1);
                break;
        }

        m_btnDone = (Button)findViewById(R.id.btnDone);
        m_btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getValues();
                finish();
            }
        });

    }

    private void getValues() {
        // Get the mode value and store it in the mStopwatchMode variable
        // (from Utilities class) and also in shared preferences.
        mPrefs = getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        int i = m_spnStopwatchOptions.getSelectedItemPosition();

        // The 0 and 1 in the below switch statement correspond to the values in the
        // stopwatch_options array on the res/values/strings file.
        switch (i) {
            case 0:
                mStopwatchMode = LAP_TIME;
                break;
            case 1:
                mStopwatchMode = SPLIT_TIME;
                break;
        }
        prefsEditor.putString(PREFS_STOPWATCH_MODE, mStopwatchMode.name()).commit();
    }

    @Override
    public void onBackPressed(){
        getValues();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){  // up arrow pressed
            case android.R.id.home:
                getValues();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
