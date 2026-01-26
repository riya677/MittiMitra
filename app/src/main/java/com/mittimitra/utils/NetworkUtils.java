package com.mittimitra.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Utility class for checking network connectivity.
 * Use this before making API calls to show appropriate offline messages.
 */
public class NetworkUtils {

    /**
     * Check if device has active internet connection.
     * @param context Application context
     * @return true if connected to internet, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if (caps == null) return false;
            
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                   caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } else {
            // Fallback for older devices
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    /**
     * Check if connected via WiFi.
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) return false;
        
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifi != null && wifi.isConnected();
        }
    }

    /**
     * Check if connected via mobile data.
     */
    public static boolean isMobileDataConnected(Context context) {
        if (context == null) return false;
        
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        } else {
            NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return mobile != null && mobile.isConnected();
        }
    }

    /**
     * Get a user-friendly network status message.
     */
    public static String getNetworkStatusMessage(Context context) {
        if (!isNetworkAvailable(context)) {
            return "No internet connection. Please check your network.";
        } else if (isWifiConnected(context)) {
            return "Connected via WiFi";
        } else if (isMobileDataConnected(context)) {
            return "Connected via Mobile Data";
        } else {
            return "Connected";
        }
    }
}
