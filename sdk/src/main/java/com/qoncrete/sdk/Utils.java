package com.qoncrete.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.TelephonyManager;

/**
 * @author <a href='mailto:zhaotengfei9@gmail.com'>Tengfei Zhao</a>
 */

class Utils {
    /**
     * 检查是否有网络
     *
     * @param context
     * @return
     */
    static boolean isNetworkConnected(Context context) {
        // TODO check android.permission.ACCESS_NETWORK_STATE
        NetworkInfo networkInfo = getConnectivityManager(context).getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private static ConnectivityManager getConnectivityManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * 返回当前程序版本名
     *
     * @return
     */
    public static String getVersionName(Context context) {
        String versionName = "";
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * 获取设备id
     *
     * @return
     */
    public static String getDeviceId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = tm.getDeviceId();
        return deviceId;
    }

    /**
     * 获取当前的应用key
     *
     * @return
     */
    public static String getAppKey(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            appInfo.metaData.getString("meta_name");
            Bundle metaData = appInfo.metaData;
            if (null != metaData) {
                String appKey = metaData.getString("DNSCACHE_APP_KEY");
                return appKey;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
