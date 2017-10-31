package com.example.pxshl.yc_monitior.net.udp;


import android.util.Log;

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


                    Log.e("UDP SEND " , Data.UDP_PORT + " " + msg);

                    if (callBack != null){
                        if (dSocket != null){
                            dSocket.close();  //先关闭 否则会影响callBack.onFinish()里面的代码
                        }
                        Thread.sleep(60);   //让该Socket占用的端口能及时释放
                        callBack.onFinish();
                    }

                }catch (Exception e){
                    e.printStackTrace();
                    if (callBack != null){
                        callBack.onError();
                    }

                }finally {
                    if (dSocket != null){
                        dSocket.close();
                    }
                }
            }
        }).start();

    }

}
