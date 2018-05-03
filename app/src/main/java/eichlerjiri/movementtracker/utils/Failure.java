package eichlerjiri.movementtracker.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import eichlerjiri.movementtracker.Model;

public class Failure extends Exception {

    public Failure(String msg) {
        super(msg);
        try {
            displayDialog();
        } catch (Failure e) {
            Log.e("Failure", e.getMessage(), e);
            stopApplication();
        }
    }

    public Failure(Throwable t) {
        super(t);
        try {
            displayDialog();
        } catch (Failure e) {
            Log.e("Failure", e.getMessage(), e);
            stopApplication();
        }
    }

    private void displayDialog() throws Failure {
        Log.e("Failure", getMessage(), this);

        AlertDialog.Builder builder = new AlertDialog.Builder(Model.getInstance().getAnyContext())
                .setMessage(getMessage())
                .setTitle("Error occured");

        AlertDialog alertDialog = builder.create();

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                stopApplication();
            }
        });
        alertDialog.show();
    }

    private void stopApplication() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
