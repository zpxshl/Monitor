package com.example.pxshl.yc_monitior.net.tcp;

import android.util.Log;

import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.util.Data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by pxshl on 17-7-28.
 */

public class TcpConnect {

    private static Socket mSocket;
    //isConnected方法....多读源码啊！！！
    private static synchronized void setSocketConnected() throws IOException{
        if ( mSocket== null || mSocket.isClosed() || !mSocket.isConnected()){
                mSocket = new Socket(Data.SERVER_IP,8890);
        }
    }

    //发送信息
    public static void send(final String message){
        Log.e("message",message + "");
        new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os;
                try {
                    setSocketConnected();
                    os = mSocket.getOutputStream();
                    byte[] buffer = (message + '\n').getBytes();
                    os.write(buffer);
                    os.flush();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void receive(final RequestCallBack requestCallBack){
        new Thread(new Runnable() {
            BufferedInputStream in = null;
            StringBuilder sb = new StringBuilder();

            @Override
            public void run() {
                try {

                    setSocketConnected();
                    in = new BufferedInputStream(mSocket.getInputStream());
                    byte[] buff = new byte[1024];

                    int len;
                    while  (  (len = in.read(buff)) != -1 ) {
                        for (int i = 0; i < len ;i++) {
                            sb.append((char) buff[i]);
                        }

                    }
                    Log.e("onFinish",sb.toString());
                    requestCallBack.onFinish(sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    requestCallBack.onError();
                }finally {
                    if (in != null){
                        try {
                            in.close();
                            //mSocket.close();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

}
