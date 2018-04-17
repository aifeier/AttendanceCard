package com.example.ai.attendancecard;

/**
 * Created by chenwanfeng on 2018/4/17.
 */

public class MWifiInfo {
    public String SSID;
    public String BSSID;

    public MWifiInfo(String SSID, String BSSID) {
        this.SSID = SSID;
        this.BSSID = BSSID;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj)
            return false;
        if (obj instanceof MWifiInfo) {
            return this.SSID.equals(((MWifiInfo) obj).SSID) && this.BSSID.equals(((MWifiInfo) obj).BSSID);
        }
        return super.equals(obj);
    }
}
