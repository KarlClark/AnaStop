package com.clarkgarrett.stopwatchandtimer.Activities;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.clarkgarrett.stopwatchandtimer.R;

import static com.clarkgarrett.stopwatchandtimer.Utilities.applicationContext;

public class StartupActivity extends AppCompatActivity {
	private Button mButton_Stopwatch, mButton_Timer;
	
	/*
	 * This activity displays two buttons that lets the user
	 * decide whether he wants to use the stop watch or the
	 * count down timer.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_screen);

		mButton_Stopwatch = (Button)findViewById(R.id.button_stopwatch);
		mButton_Timer = (Button)findViewById(R.id.button_timer);

		//Global variable in Utilities class
		applicationContext = getApplicationContext();
		
		mButton_Timer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(StartupActivity.this,TimerActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(i);
			}
		});
		
		mButton_Stopwatch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(StartupActivity.this,StopwatchActivity.class);
				startActivity(i);
			}
		});
	}

}
