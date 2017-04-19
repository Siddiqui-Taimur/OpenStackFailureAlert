package com.example.lucky.openstackfailurealert.broadcast_receivers;


import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;


import com.example.lucky.openstackfailurealert.utils.ConnectionCheckers;
import com.example.lucky.openstackfailurealert.R;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;
import java.util.Random;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.POWER_SERVICE;

/**
 * @author Taimur Siddiqui (siddiq.taimur@gmail.com)
 */

public class ReceiverToStartAgain extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "onReceive, It seems all is well so far", Toast.LENGTH_LONG).show();

        // first case to know about boot complete broadcast's call from system (intent would be have intent.action.boot_completed)
        if(intent!=null && !(intent.getBooleanExtra("startedByApp", false))) {
            Toast.makeText(context.getApplicationContext(), "On System Boot, Pending_Alert is set again for checking failure", Toast.LENGTH_LONG).show();
            int androidVersion = Build.VERSION.SDK_INT;
            int countReqID = 0;
            Map<String, ?> allEntries = context.getSharedPreferences("SIDD_FAILURE_ALERT_PREF", MODE_PRIVATE).getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String[] url_and_interval = entry.getValue().toString().split("~");
                if (url_and_interval.length == 3) {
                    Toast.makeText(context, "again...\n" + entry.getKey() + "->" + entry.getValue().toString(), Toast.LENGTH_LONG).show();
//                    Intent intentBoot = new Intent(context, ReceiverToStartAgain.class);
                    Intent intentBoot = new Intent("com.example.lucky.openstackfailurealert.MyReceiver");
                    intentBoot.putExtra("startedByApp", true);
                    intentBoot.putExtra("formedURL", url_and_interval[0]);
                    intentBoot.putExtra("interval", Integer.parseInt(url_and_interval[1]));
                    intentBoot.putExtra("timeout", Integer.parseInt(url_and_interval[2]));
                    intentBoot.putExtra("reqID", (++countReqID));
                    // check first time and start the pending alarm as well
//                    (new CheckingFailureTask(context, intentBoot, false)).execute();
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, countReqID, intentBoot, PendingIntent.FLAG_ONE_SHOT);
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if(androidVersion >= 19)
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()+(120*1000)),
                            pendingIntent); // set alarm for every service after 2 mins
                    else
                        alarmManager.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()+(120*1000)),
                                pendingIntent);
                }
            }
            SharedPreferences.Editor editor = context.getSharedPreferences("SIDD_FAILURE_ALERT_PREF", MODE_PRIVATE).edit();
            editor.putInt("reqID", countReqID);
            editor.apply();
        }

        else if(intent!=null && intent.getBooleanExtra("startedByApp", false)) {
            String isRemovedURL = context.getSharedPreferences("SIDD_FAILURE_ALERT_PREF", MODE_PRIVATE)
                    .getString((intent.getStringExtra("formedURL")), null);
            if(isRemovedURL!=null && !isRemovedURL.isEmpty())
                (new CheckingFailureTask(context, intent)).execute();
            else
                Toast.makeText(context, "no more checking for this address coz. it is removed", Toast.LENGTH_LONG).show();
        }

    }



    private class CheckingFailureTask extends AsyncTask<Void, Void, Boolean> {

        private Context context;
        private Intent intent;
//        boolean isContinueNext;
        private PowerManager.WakeLock wakeLock;

        CheckingFailureTask(Context context, Intent intent){
            this.intent = intent;
            this.context = context;
//            this.isContinueNext = isContinueNext;
            PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "failureCheckingTag");
        }

        @Override
        protected Boolean doInBackground (Void... params){
            wakeLock.acquire();
            String formedURL = intent.getStringExtra("formedURL");
            int interval  = intent.getIntExtra("interval", 5);
            int timeout = intent.getIntExtra("timeout", 30);

            int tries = 0;
            int sleepingTimeCount = 0;
            while(true){
                try {
                    boolean isAvailable = ConnectionCheckers.checkNetworkConnection(context);
                    if(tries >= 2) {
                        // also add pinging google over here if it is also not responding mean return true;
                        if(isAvailable){
                            URL url = new URL("http://google.com");
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.setInstanceFollowRedirects(true);
                            urlConnection.setRequestMethod("HEAD");
                            int statusCode = urlConnection.getResponseCode();
                            if (statusCode == 200)
                                return false; // show alert and stop set next alerts
                        }
                        return true; // if nothing happens then continue to set next alerts
                    }


                    if(isAvailable) {
                        URL url = new URL(formedURL);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setInstanceFollowRedirects(true);
                        urlConnection.setRequestMethod("HEAD");
                        urlConnection.setConnectTimeout((timeout + 15000)); // plus 15 sec for any possibility
                        int statusCode = urlConnection.getResponseCode();
                        if (statusCode == 200)
                            return true;
                        else
                            tries++;
                    }
                    else{
                        if(interval<180) { // if interval is lesser than 3 mins
                            return true;
                        }
                        else{ // waits 2 mins and then again check if there is any internet
                            if(sleepingTimeCount>=3) {
                                Log.d("Sleepingg", "setting next alert");
                                return true;
                            }

                            sleepingTimeCount = sleepingTimeCount + 1;
                            try {
                                Thread.sleep((120*1000));
                                Log.d("Sleepingg", "thread Sleeping");
                            } catch (InterruptedException e1) {
//                                e1.printStackTrace();
                                Log.d("Sleepingg", "interupted sleeping");
                            }
                        }
                    }

                } catch (SocketTimeoutException e) {
                    if(interval<120) { // if interval is lesser than 2 mins
                        try {
                            Thread.sleep((interval * 1000));
                        } catch (InterruptedException e1) {
//                            e1.printStackTrace();
                            Log.d("Sleepingg", "timeoutWala: interupted sleeping");
                        }
                    }
                    tries++;
                } catch (IOException e) {
                    tries++;
                }
            }

        }


        @Override
        protected void onPostExecute (Boolean result){
            super.onPostExecute(result);

            String formedURL = intent.getStringExtra("formedURL");
            int interval  = intent.getIntExtra("interval", 5);
            int reqID = intent.getIntExtra("reqID", 1);

            if(result) {
                int androidVersion = Build.VERSION.SDK_INT;
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reqID, intent, PendingIntent.FLAG_ONE_SHOT);
                AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if(androidVersion >= 19)
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()+(interval*1000)),
                            pendingIntent);
                else
                    alarmManager.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis()+(interval*1000)),
                            pendingIntent);
            }
            else{
                SharedPreferences myPrefs = context.getSharedPreferences("SIDD_FAILURE_ALERT_PREF", MODE_PRIVATE);
                // check if it is removed before doing anything else
                String isRemovedURL = myPrefs.getString(formedURL, null);
                if(isRemovedURL!=null && !isRemovedURL.isEmpty()) {
                    Toast.makeText(context, "Your System(" + formedURL + ") Failure Warning", Toast.LENGTH_LONG).show();
                    NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context)
                            .setSmallIcon(R.mipmap.openstack_icon)
                            .setTicker("WARNING\nSYSTEM FAILURE ALERT!")
                            .setContentTitle("SYSTEM FAILURE ALERT!")
                            .setContentText("Your entered system (" + formedURL + ") has stopped responding. So, go and check your system before it's too late.\n" +
                                    "Note: Might be your system is responding from some other locations or might be this device's internet is not working properly.")
                            .setAutoCancel(true)
                            .setWhen(System.currentTimeMillis())
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVibrate(new long[]{1000, 1000, 1000, 1000});

                    NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
                    bigStyle.setBigContentTitle("SYSTEM FAILURE ALERT!");
                    bigStyle.bigText("Your entered system (" + formedURL + ") has stopped responding. So, go and check your system before it's too late.\n" +
                            "Note: Might be your system is responding from some other locations or might be this device's internet is not working properly, despite of connected to it");
                    mNotifyBuilder.setStyle(bigStyle);

                    NotificationManager notificationManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);

                    Random rd = new Random();
                    notificationManager.notify(rd.nextInt(), mNotifyBuilder.build());

                    SharedPreferences.Editor editor = myPrefs.edit();
                    editor.remove(formedURL);
                    editor.apply();
                }
            }


            // release wake lock over here
            wakeLock.release();

        }


    }






}
