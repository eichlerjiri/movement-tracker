package eichlerjiri.movementtracker.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;

import eichlerjiri.movementtracker.Model;
import eichlerjiri.movementtracker.utils.Failure;

public class MovementTypeButton extends Button {

    private final Model m;
    private final Drawable originalBackground;

    public MovementTypeButton(Context c, final String movementType) {
        super(c);
        m = Model.getInstance();

        originalBackground = getBackground();

        setText(movementType);
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    handleClick(movementType);
                } catch (Failure ignored) {
                }
            }
        });

        if (m.getActiveRecordingType().equals(movementType)) {
            setBackgroundColor(Color.GREEN);
        }
    }

    private void handleClick(final String movementType) throws Failure {
        final String activeRecordingType = m.getActiveRecordingType();

        if (!activeRecordingType.isEmpty()) {
            if (m.getActiveDistance() == 0.0) {
                m.stopAndDeleteRecording();
                tryRestartRecording(movementType, activeRecordingType);
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
                            try {
                                m.stopAndFinishRecording();
                                tryRestartRecording(movementType, activeRecordingType);
                            } catch (Failure ignored) {
                            }
                        }
                    });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", (DialogInterface.OnClickListener) null);
            alertDialog.show();
        } else {
            startRecording(movementType);
        }
    }

    private void tryRestartRecording(String movementType, String activeRecordingType) throws Failure {
        if (!movementType.equals(activeRecordingType)) {
            startRecording(movementType);
        }
    }

    private void startRecording(String movementType) throws Failure {
        m.startRecording(movementType);
        setBackgroundColor(Color.GREEN);
    }

    public void resetBackground() {
        setBackgroundDrawable(originalBackground);
    }
}
