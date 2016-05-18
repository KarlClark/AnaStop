package com.clarkgarrett.stopwatchandtimer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


/**
 * Created by Karl on 3/24/2016.
 *
 * Build an alert dialog by TimerSettingsActivity when the user enters an
 * invalid value for the alarm length value.
 */
public class NoticeDialogFragment extends AppCompatDialogFragment {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use a custom TextView layout for the message part of the dialog so we
        // can set the text size and text color. The message attributes can't
        // be set by the style or any of the AlertDialog.Builder methods.
        // The dialog positive button onClick method doesn't do anything.
        // There is no need to call back to the activity in this app.
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_textview, null);
        TextView tv = (TextView)v.findViewById(R.id.dialog_textview);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),R.style.GlobalAlertDialogStyle);
        builder.setView(tv)
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return builder.create();
    }
}
