package com.example.pxshl.yc_monitior.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.adapter.WifiAdapter;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.tbruyelle.rxpermissions2.RxPermissions;


import java.util.List;

import io.reactivex.functions.Consumer;


/**
 * 该activity负责将wifi密码发送给监控器
 */

public class WifiActivity extends AppCompatActivity {

    private static final String MonitorWifiName = "TP-LINK_5G_FAE3"; //监控器的wifi名
    //"Pi3-AP"

    private WifiManager mWifiManager;
    private TextView mBlankTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        requestPermissions();
    }

    private void requestPermissions() {


        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {
                if (aBoolean) {
                    init();
                } else {
                    Toast.makeText(WifiActivity.this, "请授予程序获取位置信息的权限（扫描wifi列表需要）", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }


    public void init() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            LocationManager locationManager
                    = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!gps && !network) {
                Toast.makeText(WifiActivity.this, "请打开位置权限（扫描wifi列表需要）", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }

        mBlankTV = (TextView) findViewById(R.id.blank_tv);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        if (!mWifiManager.isWifiEnabled()) {  //打开wifi
            mWifiManager.setWifiEnabled(true);
        }

        mWifiManager.startScan();

        if (hasMonitorWifi(MonitorWifiName)) {

            if (isConnectMonitor()) {
                return;
            }

            if (connectMonitorWifi(MonitorWifiName)) { //让手机去连接监控器的wifi

                new Thread(new Runnable() {    //子线程轮询是否连接上指定wifi
                    @Override
                    public void run() {
                        int count = 8;   //判断时间为 <8秒
                        while (count > 0) {

                            if (isConnectMonitor()) {
                                break;
                            }

                            count--;

                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (count <= 0) {
                            showMsg("连接监控器wifi失败，请手动连接");
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); //打开系统的wifi界面
                            finish();
                        }
                    }
                }).start();

            } else {
                showMsg("连接监控器wifi失败，请手动连接");
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                finish();

            }
        } else {
            showMsg("请靠近监控器，并打开监控器的wifi");
        }


    }


    /**
     * @param SSID 要判断时候存在的wifi的SSID
     * @return ture表示该SSID对应的wifi在附近
     */
    private boolean hasMonitorWifi(String SSID) {
        //先刷新wifi列表
        List<ScanResult> results = mWifiManager.getScanResults();
        for (ScanResult result : results) {
            Log.e("SSID", result.SSID);
            if (result.SSID.contains(SSID)) {
                return true;
            }
        }
        return false;
    }

    private boolean isConnectMonitor() {
        //先判断当前连接的wifi时候为监控器的wifi
        //mWifiManager.getConnectionInfo().getSSID(),就算当前没连接wifi也会返回上一次尝试连接的wifi的数据，因此可能导致错误

        if (mWifiManager.getConnectionInfo().getSSID().contains(MonitorWifiName)) {  //轮询方式，判断连接上是不是监控器的wifi

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //显示wifi列表
                    RecyclerView wifiList = (RecyclerView) findViewById(R.id.wifi_list);
                    WifiAdapter adapter = new WifiAdapter(WifiActivity.this, mWifiManager.getScanResults());
                    wifiList.setLayoutManager(new LinearLayoutManager(WifiActivity.this));
                    wifiList.setAdapter(adapter);
                    mBlankTV.setVisibility(View.GONE);
                }
            });

            return true;
        }
        return false;
    }

    //连接SSID对应的wifi
    private boolean connectMonitorWifi(String SSID) {

        List<WifiConfiguration> wifiConfigurationList = mWifiManager.getConfiguredNetworks();

        //防止系统在你接下去的代码调用enableNetwork时候，系统自动连接其他wifi
        for (WifiConfiguration config : wifiConfigurationList) {
            mWifiManager.disableNetwork(config.networkId);
        }

        for (WifiConfiguration config : wifiConfigurationList) {
            if (config.SSID.equals('\"' + SSID + '\"')) {
                mWifiManager.enableNetwork(config.networkId, true);
                return true;
            }
        }

        WifiConfiguration config = createWifiConfig(SSID);
        int netId = mWifiManager.addNetwork(config);

        if (-1 == netId) {
            return false;
        } else {
            mWifiManager.enableNetwork(netId, true);
            return true;
        }

    }

    //此项目需要连接的wifi是不加密的
    private WifiConfiguration createWifiConfig(String SSID) {

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + SSID + "\""; //wifi名称
        config.wepKeys[0] = "\"" + "\"";
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        config.wepTxKeyIndex = 0;

        return config;
    }


    public void sendToMonitor(final String msg) {

        showMsg("请稍等十秒，不要退出APP");

        new TcpTool(Data.MONITOR_WIFI_IP, Data.MONITOR_PORT2).connect(msg, new RequestCallBack() {
            @Override
            public void onFinish(String response) {

                showMsg("请观察监控器的绿灯是否亮，绿灯亮则说明监控器已经连接上wifi"
                        + "如果15秒后监控器绿灯没有亮，请重新配置监控器");

            }

            @Override
            public void onError() {
                showMsg("配置失败，请检查当前连接的wifi是否为监控器wifi，并确保选择的您的wifi和输入正确的密码"
                        + "请重新配置监控器");
            }
        });
    }


    private void showMsg(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                mBlankTV.setVisibility(View.VISIBLE);
                mBlankTV.setText(msg);
            }
        });
    }

}
