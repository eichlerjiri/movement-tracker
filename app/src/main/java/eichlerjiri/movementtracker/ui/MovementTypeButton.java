package eichlerjiri.movementtracker.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;

import eichlerjiri.movementtracker.Model;
import eichlerjiri.movementtracker.utils.Failure;

public class MovementTypeButton extends Button {

    private final Model m;
    private final Drawable originalBackground;

    public MovementTypeButton(final Context c, final String movementType) {
        super(c);
        m = Model.getInstance();

        originalBackground = getBackground();

        setText(movementType);
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    handleClick(c, v, movementType);
                } catch (Failure ignored) {
                }
            }
        });

        if (m.getActiveRecordingType().equals(movementType)) {
            setBackgroundColor(Color.GREEN);
        }
    }

    private void handleClick(Context c, View v, String movementType) throws Failure {
        String activeRecordingType = m.getActiveRecordingType();

        if (!activeRecordingType.isEmpty()) {
            m.stopRecording();
        }

        if (!activeRecordingType.equals(movementType)) {
            m.startRecording(movementType);
            setBackgroundColor(Color.GREEN);
        }
    }

    public void resetBackground() {
        setBackgroundDrawable(originalBackground);
    }
}
