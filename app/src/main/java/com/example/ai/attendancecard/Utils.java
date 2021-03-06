package com.example.ai.attendancecard;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.amap.api.location.AMapLocationQualityReport;
import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenwanfeng on 2018/4/16.
 */

public class Utils {
    private static String TAG = "Utils";

    public static LatLng LatLngConverter(LatLng latLng,CoordinateConverter.CoordType coordType) {
        CoordinateConverter converter = new CoordinateConverter();
        // CoordType.GPS 待转换坐标类型
        converter.from(coordType);
        // sourceLatLng待转换坐标点 DPoint类型
        converter.coord(latLng);
        // 执行转换操作
        LatLng converterLatLng = converter.convert();
        Log.d(TAG, "原坐标：(" + latLng.latitude + "," + latLng.longitude
                + ") 转换后的坐标：(" + converterLatLng.latitude + "," + converterLatLng.longitude + ")");
        return converterLatLng;
    }

    public static List<ScanResult> getWifiList(Context applicationContext) {
        List<ScanResult> list = new ArrayList<>();
        try {
            WifiManager wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
            // WIFI不可用，直接返回空列表
            if (WifiManager.WIFI_STATE_ENABLED != wifiManager.getWifiState()) {
                Log.d("Utils", "WIFI 不可用");
                return list;
            }
            list = wifiManager.getScanResults();
            if (null == list) {
                list = new ArrayList<>();
            }
            for (ScanResult wifi : list) {
                Log.d("Utils", "WIFI SSID：" + wifi.SSID + " BSSID：" + wifi.BSSID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean checkHasWifi(List<ScanResult> wifiList, List<MWifiInfo> wifiBSSIDNames) {

        if (null == wifiBSSIDNames || wifiBSSIDNames.size() == 0) {
            return false;
        }
        if (null == wifiList || wifiList.size() == 0) {
            return false;
        }
        for (ScanResult wifi : wifiList) {
            for (MWifiInfo name : wifiBSSIDNames) {
                if (wifi.BSSID.equals(name.BSSID)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 高德地图获取GPS状态的字符串
     *
     * @param statusCode GPS状态码  AMapLocation.getLocationQualityReport().getGPSStatus()
     * @return
     */
    public static String getGPSStatusString(int statusCode) {
        String str = "";
        switch (statusCode) {
            case AMapLocationQualityReport.GPS_STATUS_OK:
                str = "GPS状态正常";
                break;
            case AMapLocationQualityReport.GPS_STATUS_NOGPSPROVIDER:
                str = "手机中没有GPS Provider，无法进行GPS定位";
                break;
            case AMapLocationQualityReport.GPS_STATUS_OFF:
                str = "GPS关闭，建议开启GPS，提高定位质量";
                break;
            case AMapLocationQualityReport.GPS_STATUS_MODE_SAVING:
                str = "选择的定位模式中不包含GPS定位，建议选择包含GPS定位的模式，提高定位质量";
                break;
            case AMapLocationQualityReport.GPS_STATUS_NOGPSPERMISSION:
                str = "没有GPS定位权限，建议开启gps定位权限";
                break;
        }
        return str;
    }


    public static boolean isNetWorkConnected(Context context) {
        if (null == context)
            return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (null != networkInfo)
            return networkInfo.isAvailable();
        return false;
    }

    /**
     * 获取手机IMEI号
     */
    public static String getIMEI(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        String imei = telephonyManager.getDeviceId();
        Log.e("utils", imei);
        return imei;
    }

}
