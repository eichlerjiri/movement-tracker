package eichlerjiri.movementtracker.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;

import eichlerjiri.movementtracker.Model;

public class MovementTypeButton extends Button {

    public final Model m;
    public final String movementType;
    public final Drawable originalBackground;
    public final int originalTextColor;

    public MovementTypeButton(Context c, String movementType) {
        super(c);
        m = Model.getInstance(c);
        this.movementType = movementType;

        originalBackground = getBackground();
        originalTextColor = getCurrentTextColor();

        setText(movementType);
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick();
            }
        });

        if (m.activeRecordingType.equals(movementType)) {
            setBackgroundColor(Color.GREEN);
        }
    }

    public void handleClick() {
        if (!m.activeRecordingType.isEmpty()) {
            if (m.activeLocations < 2) {
                restartRecording(true);
                return;
            }

            new AlertDialog.Builder(getContext())
                    .setMessage("Really stop " + m.activeRecordingType + " recording?")
                    .setTitle("Stop recording?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            restartRecording(false);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            startRecording();
        }
    }

    public void restartRecording(boolean delete) {
        String activeRecordingType = m.activeRecordingType;
        m.stopRecording(delete);
        if (!movementType.equals(activeRecordingType)) {
            startRecording();
        }
    }

    public void startRecording() {
        m.startRecording(movementType);
        setBackgroundColor(Color.GREEN);
        setTextColor(Color.BLACK);
    }

    public void resetBackground() {
        setBackgroundDrawable(originalBackground);
        setTextColor(originalTextColor);
    }
}
