package com.example.pxshl.yc_monitior.activity;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.adapter.WifiAdapter;
import com.example.pxshl.yc_monitior.util.Data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import static android.R.attr.enabled;


/**
 * 该activity负责将wifi密码发送给监控器
 */

public class WifiActivity extends AppCompatActivity {


   private static final String MonitorWifiName = "Pi3-AP"; //监控器的wifi名
    private RecyclerView wifiList;
    private WifiAdapter mAdapter;
    private WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        init();
    }


    public void init() {

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);



        if (!mWifiManager.isWifiEnabled()) {  //打开wifi
            mWifiManager.setWifiEnabled(true);
        }

        mWifiManager.startScan();
        wifiList = (RecyclerView) findViewById(R.id.wifi_list);


        if (hasMonitorWifi(MonitorWifiName)){

            if (isConnectMonitor()){
                return;
            }

            if (connectMonitorWifi(MonitorWifiName)){ //让手机去连接监控器的wifi

                new Thread(new Runnable() {    //子线程轮询是否连接上指定wifi
                    @Override
                    public void run() {
                        int count = 8;   //判断时间为 <8秒
                        while(count > 0){

                            if (isConnectMonitor()){
                                break;
                            }

                            count--;

                            try {
                                Thread.sleep(1000);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        if ( count <= 0){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(WifiActivity.this,"连接监控器wifi失败，请手动连接",Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); //打开系统的wifi界面
                                    finish();

                                }
                            });
                        }
                    }
                }).start();

            }else {
                Toast.makeText(this,"连接监控器wifi失败，请手动连接",Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                finish();

            }
        }else {
            Toast.makeText(this,"请靠近监控器，并打开监控器的wifi",Toast.LENGTH_SHORT).show();
        }


    }


    /**
     *
     * @param SSID   要判断时候存在的wifi的SSID
     * @return  ture表示该SSID对应的wifi在附近
     */
    private boolean hasMonitorWifi(String SSID){
        //先刷新wifi列表
        List<ScanResult> results = mWifiManager.getScanResults();
        for (ScanResult result : results){
            Log.e("SSID",result.SSID);
            if (result.SSID.contains(SSID)){
                return true;
            }
        }
        return false;
    }

    private boolean isConnectMonitor(){
             //先判断当前连接的wifi时候为监控器的wifi
            //mWifiManager.getConnectionInfo().getSSID(),就算当前没连接wifi也会返回上一次尝试连接的wifi的数据，因此可能导致错误

            if (mWifiManager.getConnectionInfo().getSSID().contains(MonitorWifiName)){  //轮询方式，判断连接上是不是监控器的wifi

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //显示wifi列表
                        mAdapter = new WifiAdapter(WifiActivity.this, mWifiManager.getScanResults());
                        wifiList.setLayoutManager(new LinearLayoutManager(WifiActivity.this));
                        wifiList.setAdapter(mAdapter);
                        findViewById(R.id.wifi_explain_tv).setVisibility(View.VISIBLE);
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
        for (WifiConfiguration config : wifiConfigurationList){
                mWifiManager.disableNetwork(config.networkId);
        }

        for (WifiConfiguration config : wifiConfigurationList){
            if (config.SSID.equals('\"' + SSID+ '\"')){
                mWifiManager.enableNetwork(config.networkId,true);
                return true;
            }
        }

        WifiConfiguration config = createWifiConfig(SSID);
        int netId = mWifiManager.addNetwork(config);

        if ( -1 == netId){
            return false;
        }else {
            mWifiManager.enableNetwork(netId,true);
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


    public void sendToMonitor(final String msg){
        new Thread(new Runnable() {
            @Override
            public void run() {



                OutputStream os;
                Socket socket = null;
                try {
                    socket = new Socket(Data.MONITOR_WIFI_IP,Data.MONITOR_PORT);
                    os = socket.getOutputStream();
                    byte[] buffer = (msg).getBytes();
                    os.write(buffer);
                    os.flush();

                    String msg;

                    if (isMonitorConnect()){
                        msg = "wifi密码错误，请重新输入";
                    }else {
                        msg = "恭喜您，监控器已经连接上wifi啦";
                    }

                    Toast.makeText(WifiActivity.this,msg,Toast.LENGTH_SHORT).show();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    try{
                        if (socket != null)
                             socket.close();
                    }catch (IOException e){

                    }

                }
            }
        }).start();
    }

    //通过判断监控器有没有重新发出wifi
    private boolean isMonitorConnect() throws InterruptedException{

        for (int i = 12 ; i > 0 ;i--){    //12秒内 如果监控器没有重新发出wifi，则可以认为它已经连接上指定wifi
            for (ScanResult result : mWifiManager.getScanResults()){
                if (result.SSID.contains(MonitorWifiName)){
                    return false;
                }
            }

            Thread.sleep(1000);
        }
        return true;
    }



}
