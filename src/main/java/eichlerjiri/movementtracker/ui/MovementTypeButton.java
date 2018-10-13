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

    private final Model m;
    private final Drawable originalBackground;
    private final int originalTextColor;

    public MovementTypeButton(Context c, final String movementType) {
        super(c);
        m = Model.getInstance(c);

        originalBackground = getBackground();
        originalTextColor = getCurrentTextColor();

        setText(movementType);
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick(movementType);
            }
        });

        if (m.activeRecordingType.equals(movementType)) {
            setBackgroundColor(Color.GREEN);
        }
    }

    private void handleClick(final String movementType) {
        final String activeRecordingType = m.activeRecordingType;

        if (!activeRecordingType.isEmpty()) {
            if (m.activeLocations < 2) {
                restartRecording(movementType, activeRecordingType, true);
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                    .setMessage("Really stop " + activeRecordingType + " recording?")
                    .setTitle("Stop recording?");

            AlertDialog alertDialog = builder.create();
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            restartRecording(movementType, activeRecordingType, false);
                        }
                    });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", (DialogInterface.OnClickListener) null);
            alertDialog.show();
        } else {
            startRecording(movementType);
        }
    }

    private void restartRecording(String movementType, String activeRecordingType, boolean delete) {
        m.stopRecording(delete);
        if (!movementType.equals(activeRecordingType)) {
            startRecording(movementType);
        }
    }

    private void startRecording(String movementType) {
        m.startRecording(movementType);
        setBackgroundColor(Color.GREEN);
        setTextColor(Color.BLACK);
    }

    public void resetBackground() {
        setBackgroundDrawable(originalBackground);
        setTextColor(originalTextColor);
    }
}
