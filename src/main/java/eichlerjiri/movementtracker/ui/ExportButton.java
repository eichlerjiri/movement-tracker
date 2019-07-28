package eichlerjiri.movementtracker.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Locale;

import eichlerjiri.movementtracker.MovementTracker;

public class ExportButton extends Button {

    public final String format;

    public ExportButton(Context c, String format) {
        super(c);
        this.format = format;

        setText("Export " + format.toUpperCase(Locale.US));
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MovementTracker t = (MovementTracker) getContext();
                if (Build.VERSION.SDK_INT >= 23 && t.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    t.lastExportButton = ExportButton.this;
                    t.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                } else {
                    showDateExportSelector();
                }
            }
        });
    }

    public void showDateExportSelector() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar c = Calendar.getInstance();
                c.set(year, month, dayOfMonth);
                new Exporter(getContext(), c.getTimeInMillis(), format).exportTracks();
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        dialog.setMessage("Export recordings since:");
        dialog.show();
    }
}
