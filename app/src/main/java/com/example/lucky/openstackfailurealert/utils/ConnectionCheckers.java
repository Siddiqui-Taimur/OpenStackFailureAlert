package com.example.lucky.openstackfailurealert.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * @author Taimur Siddiqui
 *
 * siddguru75@gmail.com
 */

public class ConnectionCheckers {

    // checking internet, few part of this function is taken from Stackoverflow
    public static boolean checkNetworkConnection(Context context) {
        boolean wifi = false;
        boolean mobile = false;
        ConnectivityManager cManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cManager.getAllNetworkInfo();
        for (NetworkInfo info : netInfo) {
            if (info.getTypeName().equalsIgnoreCase("WIFI"))
                if (info.isConnected())
                    wifi = true;
            if (info.getTypeName().equalsIgnoreCase("MOBILE"))
                if (info.isConnected())
                    mobile = true;
        }
        return wifi || mobile;
    }

}
