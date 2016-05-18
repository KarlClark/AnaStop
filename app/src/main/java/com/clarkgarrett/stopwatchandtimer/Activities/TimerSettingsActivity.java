package com.clarkgarrett.stopwatchandtimer.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.clarkgarrett.stopwatchandtimer.NoticeDialogFragment;
import com.clarkgarrett.stopwatchandtimer.R;

import static com.clarkgarrett.stopwatchandtimer.Enums.VibrateModes.VIBRATE_ALWAYS;
import static com.clarkgarrett.stopwatchandtimer.Enums.VibrateModes.VIBRATE_NEVER;
import static com.clarkgarrett.stopwatchandtimer.Enums.VibrateModes.VIBRATE_WHEN_MUTED;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_ALARM_LENGTH;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_NAME;
import static com.clarkgarrett.stopwatchandtimer.Utilities.PREFS_VIBRATE_MODE;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mAlarmLengthSecs;
import static com.clarkgarrett.stopwatchandtimer.Utilities.mVibrateMode;

/**
 *This activity is started by the TimerActivity in response to its Settings option
 * in its actionbar. It displays three interactive views.
 *
 * The first is a numeric EditText which is used to enter the lenght of time the
 * alarm should sound in seconds.
 *
 * The second is a Spinner that displays options for the vibrator. THe user can choose
 * from three options, vibrate when muted, vibrate always, or never vibrate.  This view
 * is only shown if the device has a vibrator.
 *
 * The third is a DONE Button.
 *
 * When the user is done with this activity, (back press, up arrow press or DONE Button
 * press> the chosen values are store in shared preferences, and global variables
 * in the Utilities class.
 */

public class TimerSettingsActivity extends AppCompatActivity {

    private EditText m_etAlarmLength;
    private Spinner m_spnVibrate;
    private Button m_btnDone;
    private SharedPreferences mPrefs;
    private Vibrator mVibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVibrator = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator.hasVibrator()){
            // Use the layout that has the vibrate options Spinner
            setContentView(R.layout.activity_timer_settings_vibrator);

            // Set the Spinner currnet selected value to our current vibrate mode.
            // The hard coded numbers in the switch statement correspond to the
            // values in the string array Vibrate_options in the res/values/strings file.
            // The case values are from the VibrateModes Enum.
            m_spnVibrate = (Spinner)findViewById(R.id.spnVibrateMode);
            switch (mVibrateMode) {
                case VIBRATE_WHEN_MUTED:
                    m_spnVibrate.setSelection(0);
                    break;
                case VIBRATE_ALWAYS:
                    m_spnVibrate.setSelection(1);
                    break;
                case VIBRATE_NEVER:
                    m_spnVibrate.setSelection(2);
                    break;
            }
        }else{ // use layout that doesn't have vibrate mode spinner.
            setContentView(R.layout.activity_timer_settings_no_vibrator);
        }

        // Set EditText value to the current value for alarm length.
        // Move the cursor to the end of the field, ready to use backspace
        // to edit the field.
        m_etAlarmLength = (EditText)findViewById(R.id.etAlarmLength);
        m_etAlarmLength.setText("" + mAlarmLengthSecs);
        m_etAlarmLength.setSelection(m_etAlarmLength.length());

        m_btnDone = (Button)findViewById(R.id.btnDone);
        m_btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if ( getValues() ) finish();
            }
        });


    }

    @Override
    public void onBackPressed(){
       if( getValues() )super.onBackPressed();
    }

    private boolean getValues() {
        // Read the values from the views and store them in shared preferences and
        // global variables in the Utilities class.  Display an alert dialog if the
        // value in the alarm lenght EditText view is not valid.  Return true if
        // values are valid.
        mPrefs = getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        String text = m_etAlarmLength.getText().toString();
        if (text.equals("") || text.equals("0") || text.equals("00") || text.equals("000")) {
            // Invlaid value, display alert dialog. There is no call back for this dialog.
            // It just dismisses itself when the user interacts with it.  This activity will
            // keep displaying it until the usesr enters a valid value.
            AppCompatDialogFragment dialog = new NoticeDialogFragment();
            dialog.show(getSupportFragmentManager(), "SomeTagWeDontNeed");
            return false;
        }else{
            // Valid value so store value.
            mAlarmLengthSecs = Integer.parseInt(text);
            prefsEditor.putInt(PREFS_ALARM_LENGTH, mAlarmLengthSecs).commit();
            }
        if (mVibrator.hasVibrator()) {
            // Get the selected value from the Spinner.  The hard coded numbers
            // in the switch statement correspond to the string array
            // Vibrate_options in the res/values/strings file.
            int i = m_spnVibrate.getSelectedItemPosition();
            switch (i) {
                case 0:
                    mVibrateMode = VIBRATE_WHEN_MUTED;
                    break;
                case 1:
                    mVibrateMode = VIBRATE_ALWAYS;
                    break;
                case 2:
                    mVibrateMode = VIBRATE_NEVER;
                    break;
            }
            prefsEditor.putString(PREFS_VIBRATE_MODE, mVibrateMode.name()).commit();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){  //user pressed up arrow.
            case android.R.id.home:
                if ( getValues() ) {
                    finish();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }
}
