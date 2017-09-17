package com.example.pxshl.yc_monitior.net.udp;


import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.inyerface.SendCallBack;
import com.example.pxshl.yc_monitior.util.Data;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by pxshl on 2017/7/28.
 */

public class UdpTool {

    //port为服务器发回的，它可以通信的port
    public static void sendUDP(final String msg, final int port, final SendCallBack callBack)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket dSocket = null;
                DatagramPacket dPacket = null;
                try {
                    dPacket = new DatagramPacket(msg.getBytes(),msg.length(), InetAddress.getByName(Data.SERVER_IP),port);
                    dSocket = new DatagramSocket();
                    Data.UDP_PORT = dSocket.getLocalPort();  //服务器返回的数据是原路返回的,端口必须一样
                    //发送三个包，防止丢包
                    for (int i = 0 ;i < 3 ;i++){
                        dSocket.send(dPacket);
                    }
                    callBack.onFinish();
                }catch (Exception e){
                    e.printStackTrace();
                    callBack.onError();
                }finally {
                    if (dSocket != null){
                        dSocket.close();
                    }
                }
            }
        }).start();

    }


    //通过UDP广播判断是否与监控器处于同一局域网
    public static synchronized void UdpBroadCast(final RequestCallBack callBack) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket dSocket = null;
                DatagramPacket dPacket = null;
                try {
                    String msg = "Monitor are you here";
                    dSocket = new DatagramSocket();
                    dPacket = new DatagramPacket(msg.getBytes(),msg.length(), InetAddress.getByName("255.255.255.255"),Data.MONITOR_WIFI_PORT);
                    dSocket.send(dPacket);

                    dSocket.setSoTimeout(6000);
                    dSocket.receive(dPacket);

                    String response = new String(dPacket.getData(),dPacket.getOffset(),dPacket.getLength());

                    if (response.contains("yes")){
                        callBack.onFinish(response.substring(response.indexOf("ip") + 3));
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    callBack.onError();
                }finally {
                    if (dSocket != null){
                        dSocket.close();
                    }
                }
            }
        }).start();
    }
}
