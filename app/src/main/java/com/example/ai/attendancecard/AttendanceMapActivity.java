package com.example.ai.attendancecard;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.AMapLocationQualityReport;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.Circle;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MyLocationStyle;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by chenwanfeng on 2018/4/16.
 */

public class AttendanceMapActivity extends AppCompatActivity implements View.OnClickListener, LocationSource, EasyPermissions.PermissionCallbacks {

    // 公司路由器mac地址
    private final String[] companyWIFIs = {"30:fc:68:18:ac:20","30:fc:68:18:ac:1e"};

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
//                 正在关闭
            } else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
//                正在打开
            } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
//                已经关闭
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkWifi();
                        startLocation();
                    }
                });
            } else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
//                已经打开
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkWifi();
                    }
                });
            } else {
//                Log.i(Config.LOG_TAG, "未知状态");
            }
        }
    };


    private final int PERMISSION_CODE = 0 * 004;

    // 公司位置
    private final LatLng companyLL = new LatLng(30.278975, 120.145913);
    // 考勤区域半径
    private final double radius = 50;
    // 考勤区域
    private Circle circle;

    //备注信息
    private EditText remind;

    private Button confirm;

    // 位置信息提示
    private TextView locationInfo;

    //地图
    private MapView mapView;
    private AMap aMap;


    //定位
    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private OnLocationChangedListener listener;

    private AMapLocation lastLocation = null;

    private boolean firstLocation;

    //是否在WIFI范围内，在WIFI范围内就提示定位失败的信息
    private boolean inWifi = false;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        firstLocation = true;
        mapView = findViewById(R.id.mapview);
        remind = findViewById(R.id.remind);
        locationInfo = findViewById(R.id.locationInfo);
        confirm = findViewById(R.id.confirm);
        findViewById(R.id.confirm).setOnClickListener(this);

        mapView.onCreate(savedInstanceState);
        if (null != mapView)
            aMap = mapView.getMap();
        EasyPermissions.requestPermissions(this, "需要定位等权限", PERMISSION_CODE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.READ_PHONE_STATE
                , Manifest.permission.BLUETOOTH
        );
        initMap();
        initCircle();
        initLocation();
        checkWifi();
        startLocation();
        Executors.newSingleThreadExecutor().execute(checkWifiRunnable);
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, wifiFilter);

    }

    private void initMap() {
        aMap.setLocationSource(this);// 设置定位监听
//        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        // 自定义系统定位蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(
                BitmapDescriptorFactory.fromResource(R.mipmap.gps_point));
        // 自定义精度范围的圆形边框颜色
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
        // 自定义精度范围的圆形边框宽度
        myLocationStyle.strokeWidth(0);
        // 设置圆形的填充颜色
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));
        // 将自定义的 myLocationStyle 对象添加到地图上
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false

    }


    private Handler handler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            if (null == msg)
                return;
            if (null != locationInfo && null != msg.obj) {
                CardInfo info = (CardInfo) msg.obj;
                locationInfo.setText(info.msg);
                if (info.inCompany) {
                    confirm.setText("确认打卡");
                } else {
                    confirm.setText("外勤打卡");
                }
            }
        }
    };

    private void showLocationInfo(CardInfo cardInfo) {
        Message message = Message.obtain();
        message.what = 0;
        message.obj = cardInfo;
        handler.sendMessage(message);
    }

    //must be in UI Thread
    private synchronized void checkWifi() {
        //一分钟重新检查是否在WIFI范围内
        boolean temp = Utils.checkHasWifi(Utils.getWifiList(getApplicationContext()), companyWIFIs);
        if (temp) {
            showLocationInfo(new CardInfo(true, "您已进入公司WIFI范围"));
            inWifi = true;
        } else {
            if (inWifi) {
                if (null != lastLocation && 0 == lastLocation.getErrorCode()) {
                    boolean in = circle.contains(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
                    showLocationInfo(new CardInfo(in ? true : false, in ?
                            "您已进入考勤范围(" + lastLocation.getAddress() + ")"
                            : "不在考勤范围内"));
                } else {
                    showLocationInfo(new CardInfo(false, "当前位置未知"));
                }
            }
            inWifi = false;
            if (null == wifiManager) {
                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            }
            if (WifiManager.WIFI_STATE_DISABLED == wifiManager.getWifiState()) {
                showTip("开启WIFI将提高定位精度");
            }
        }
    }

    private Runnable checkWifiRunnable = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            Looper.loop();
            while (true) {
                try {
                    Thread.sleep(1000 * 60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                checkWifi();
            }
        }
    };


    //绘制可打卡的范围
    private void initCircle() {
        aMap.moveCamera(CameraUpdateFactory
                .newLatLngZoom(companyLL, 17));// 设置指定的可视区域地图

        // 绘制一个圆形
        circle = aMap.addCircle(new CircleOptions().center(companyLL)
                .radius(radius)//半径
                .strokeColor(Color.argb(100, 1, 1, 255))// 设置边框颜色，ARGB格式。如果设置透明，则边框不会被绘制。默认黑色。
                .fillColor(Color.argb(50, 0, 0, 255))// 设置填充颜色。填充颜色是绘制边框以内部分的颜色，ARGB格式。默认透明。
                .strokeWidth(5));//设计边框宽度，单位像素。参数必须大于等于0，默认10
    }


    /**
     * 初始化定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void initLocation() {

        //初始化client
        locationClient = new AMapLocationClient(this.getApplicationContext());
        locationOption = getDefaultOption();
        //设置定位参数
        locationClient.setLocationOption(locationOption);
        // 设置定位监听
        locationClient.setLocationListener(locationListener);
    }

    /**
     * 默认的定位参数
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(30000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
        mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        return mOption;
    }

    private AMapLocationListener locationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation location) {
            lastLocation = location;
            if (null != location) {
                //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                if (0 == location.getErrorCode()) {
//                    stopLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                    LatLng latLng = new LatLng(30.478975, 120.245913);
                    if (firstLocation) {
                        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                        firstLocation = false;
                    }
                    if (!inWifi) {
                        boolean in = circle.contains(latLng);
                        showLocationInfo(new CardInfo(in ? true : false, in ?
                                "您已进入考勤范围(" + location.getAddress() + ")"
                                : "不在考勤范围内"));
                        if (!in && !location.getLocationQualityReport().isWifiAble()) {
                            showTip("开启WIFI可以提高定位精度");
                        }
                        if (!in && AMapLocationQualityReport.GPS_STATUS_OK != location.getLocationQualityReport().getGPSStatus()) {
                            showTip(Utils.getGPSStatusString(location.getLocationQualityReport().getGPSStatus()));
                        }
                    }
                } else {
                    if (inWifi)
                        return;
                    showTip("定位失败errCode= " + location.getErrorCode());
                    showLocationInfo(new CardInfo(false, "当前位置信息未知"));
                }

            } else {
                if (inWifi)
                    return;
                showTip("定位失败。没有定位数据");
                showLocationInfo(new CardInfo(false, "当前位置信息未知"));
            }
            if (null != listener)
                listener.onLocationChanged(location);
        }
    };

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        destroyLocation();
        unregisterReceiver(wifiReceiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.confirm:
                showTip("打卡成功", true);
                break;
        }
    }

    /**
     * 开始定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void startLocation() {
        // 设置定位参数
        if (!locationClient.isStarted()) {
            locationClient.setLocationOption(locationOption);
            // 启动定位
            locationClient.startLocation();
        }
    }

    /**
     * 停止定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void stopLocation() {
        // 停止定位
        locationClient.stopLocation();
    }

    /**
     * 销毁定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void destroyLocation() {
        if (null != locationClient) {
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            locationClient.onDestroy();
            locationClient = null;
            locationOption = null;
        }
    }

    private final void showTip(CharSequence msg) {
        showTip(msg, false);
    }

    private final void showTip(CharSequence msg, boolean llong) {
        Toast.makeText(this, msg, llong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        listener = onLocationChangedListener;
        startLocation();
    }

    @Override
    public void deactivate() {
        listener = null;
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
        }
        locationClient = null;
    }

    //6.0+权限

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        EasyPermissions.requestPermissions(this, "必要权限，请选择同意，不然会影响程序运行", PERMISSION_CODE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    }
}
