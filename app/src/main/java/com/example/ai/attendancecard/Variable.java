package com.example.ai.attendancecard;

import com.amap.api.maps2d.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chenwanfeng on 2018/4/17.
 */

public class Variable {

    // 公司位置
    public static LatLng companyLL = new LatLng(30.278975, 120.145913);
    // 考勤区域半径
    public static double radius = 50;
    // 上班时间
    private static final String DefauleStartTime = "9:00";
    private static final String DefaultEndTime = "17:30";

    public static String startTime = DefauleStartTime;
    public static String endTime = DefaultEndTime;

    // 公司路由器mac地址
    public static List<MWifiInfo> companyWIFIs = new ArrayList<>();
}
