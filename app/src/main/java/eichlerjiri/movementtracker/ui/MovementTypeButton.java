package eichlerjiri.movementtracker.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;

import eichlerjiri.movementtracker.Model;

public class MovementTypeButton extends Button {

    private final Model m;
    private final Drawable originalBackground;

    public MovementTypeButton(Context context, final String movementType) {
        super(context);
        m = Model.getInstance();

        originalBackground = getBackground();

        setText(movementType);
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String activeRecordingType = m.getActiveRecordingType();

                if (!activeRecordingType.isEmpty()) {
                    m.stopRecording();
                }

                if (!activeRecordingType.equals(movementType)) {
                    if (m.startRecording(movementType)) {
                        setBackgroundColor(Color.GREEN);
                    }
                }
            }
        });

        if (m.getActiveRecordingType().equals(movementType)) {
            setBackgroundColor(Color.GREEN);
        }
    }

    public void resetBackground() {
        setBackgroundDrawable(originalBackground);
    }
}
