package com.example.lucky.openstackfailurealert.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lucky.openstackfailurealert.R;
import com.example.lucky.openstackfailurealert.utils.ConnectionCheckers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

/**
 * @author Taimur Siddiqui (siddiq.taimur@gmail.com)
 */

public class StartingServiceActivity extends AppCompatActivity {

    String serverName;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.

        savedInstanceState.putString("SERVER_NAME", serverName);
        // etc.
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting_service);
//        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        getSupportActionBar().setIcon(R.drawable.openstack_white_50);


        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                serverName = null;
            } else {
                serverName = extras.getString("SERVER_NAME");
            }
        } else {
            serverName = (String) savedInstanceState.getSerializable("SERVER_NAME");
        }

        TextView ip = (TextView) findViewById(R.id.ip);
        ip.setText(serverName+ " URL or IP:");

        TextView port = (TextView) findViewById(R.id.port);
        port.setText(serverName+ " Port Number:");


        final EditText edit_ip = (EditText) findViewById(R.id.editIP);
        final EditText edit_port = (EditText) findViewById(R.id.editPort);
        final EditText edit_interval = (EditText) findViewById(R.id.editInterval);

        Button startingButton = (Button) findViewById(R.id.startingService);

        startingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check validation
                String text_ip = edit_ip.getText().toString();
                String text_port = edit_port.getText().toString();
                String text_interval = edit_interval.getText().toString();
                if((text_ip.length()<8) || (text_port.length()<1) || (text_interval.length()<1)){
                    Toast.makeText(StartingServiceActivity.this, "Please enter all the fields carefully", Toast.LENGTH_LONG).show();
                }
                else{
                    if(ConnectionCheckers.checkNetworkConnection(StartingServiceActivity.this)) {
                        int int_interval = Integer.parseInt(text_interval);
                        if (int_interval == 0)
                            Toast.makeText(StartingServiceActivity.this, "Time interval can't be zero", Toast.LENGTH_LONG).show();
                        else
                            // check response status
                            new StartingServiceTask(("http://"+text_ip + ":" + text_port), int_interval).execute();
                    }
                    else
                        Toast.makeText(StartingServiceActivity.this, "Sorry, No internet connection!", Toast.LENGTH_LONG).show();

                }
            }
        });

    }



    private class StartingServiceTask extends AsyncTask<Void, Void, Boolean> {

        private ProgressDialog pDialog;
        private String formedURL;
        private int interval = 5; // 5 secs
        private int timeout = 30; // 30 secs

        public StartingServiceTask(String formedURL, int interval){
            this.formedURL = formedURL;
            this.interval = interval;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(StartingServiceActivity.this);
            pDialog.setMessage("Starting background service for failure check...");
            pDialog.setCanceledOnTouchOutside(false);
            pDialog.show();
        }

        @Override
        protected Boolean doInBackground (Void... params){
            try {
                long startTime = System.currentTimeMillis();
                URL url = new URL(formedURL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("HEAD");
                urlConnection.setConnectTimeout(60000); // 60 secs
                int statusCode = urlConnection.getResponseCode();

                if (statusCode == 200) {
                    timeout = (int) (System.currentTimeMillis() - startTime);
                    return true;
                }

            } catch (SocketTimeoutException e) {
                return false;
            } catch (IOException e) {
                return false;
            }

            return false;
        }


        @Override
        protected void onPostExecute (Boolean result){
            super.onPostExecute(result);
            if(pDialog!=null)
                pDialog.dismiss();

            if(result) {
                SharedPreferences myPrefs = getSharedPreferences("SIDD_FAILURE_ALERT_PREF", MODE_PRIVATE);
                String alreadySame = myPrefs.getString(formedURL, null);
                if(alreadySame==null || alreadySame.equals("")) {
                    int androidVersion = Build.VERSION.SDK_INT;
                    int requestID = myPrefs.getInt("reqID", 0);
                    requestID++;
                    SharedPreferences.Editor editor = myPrefs.edit();
                    editor.putString(formedURL, (formedURL + "~" + interval + "~" + timeout));
                    editor.putInt("reqID", requestID);
                    editor.apply();
//                    Intent intent = new Intent(getApplicationContext(), ReceiverToStartAgain.class);
                    Intent intent = new Intent("com.example.lucky.openstackfailurealert.MyReceiver");
                    intent.putExtra("startedByApp", true);
                    intent.putExtra("formedURL", formedURL);
                    intent.putExtra("interval", interval);
                    intent.putExtra("timeout", timeout);
                    intent.putExtra("reqID", requestID);

                    Toast.makeText(StartingServiceActivity.this, "...the requestID..."+requestID, Toast.LENGTH_SHORT).show();
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), requestID, intent, PendingIntent.FLAG_ONE_SHOT);
                    AlarmManager alarmManager=(AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    if(androidVersion >= 19)
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()+(interval*1000)),
                            pendingIntent);
                    else
                        alarmManager.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()+(interval*1000)),
                                pendingIntent);


                    new AlertDialog
                            .Builder(StartingServiceActivity.this)
                            .setMessage("Background service is started successfully with failure check after each " + interval + " Seconds.\n" +
                                    "So, now you would receive an automatic alert whenever your entered system (" + formedURL +
                                    ") goes down.\n" +
                                    "Note: There would be some MINOR usage of your internet connection to check failure," +
                                    "so it is recommended to keep running your internet connection all the time to get the failure alert ASAP.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
                else{
                    Toast.makeText(StartingServiceActivity.this, "Sorry, There is already the failure check service is running for " +
                            formedURL, Toast.LENGTH_LONG).show();
                }
            }
            else{
                Toast.makeText(StartingServiceActivity.this, "Sorry, Incorrect URL/IP or PORT\n" +
                        "Entered address is not responding right now", Toast.LENGTH_LONG).show();
            }
        }


    }

}
