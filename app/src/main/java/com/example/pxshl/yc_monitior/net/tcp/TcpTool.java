package com.example.pxshl.yc_monitior.net.tcp;

import android.support.annotation.Nullable;
import android.util.Log;

import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.inyerface.SendCallBack;
import com.example.pxshl.yc_monitior.util.Data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by pxshl on 17-7-28.
 */

public class TcpTool {


    private String mIp;
    private int mPort;

    public TcpTool(String ip, int port) {
        mIp = ip;
        mPort = port;
    }

    //发送，接受信息封装到一起，优点是能避免send与receive的socket不一致的情况
    public void connect(final String message,@Nullable final RequestCallBack requestCallBack){

        new Thread(new Runnable() {
            @Override
            public void run() {

                StringBuilder sb = new StringBuilder();

                try (Socket socket = new Socket(mIp,mPort)){

                 socket.setSoTimeout(5000);
                    OutputStream os = socket.getOutputStream();
                    byte[] buffer = (message + '\n').getBytes();
                    os.write(buffer);
                    os.flush();

                    Log.e("message",message);

                    if (requestCallBack != null){
                        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                        byte[] buff = new byte[1024];

                        int len;
                        while  (  (len = in.read(buff)) != -1 ) {
                            for (int i = 0; i < len ;i++) {
                                sb.append((char) buff[i]);
                            }
                        }
                        Log.e("onFinish",sb.toString());
                        requestCallBack.onFinish(sb.toString());
                    }

                }
                catch (IOException e) {
                    e.printStackTrace();
                    if (requestCallBack != null){
                        requestCallBack.onError();
                    }
                }
            }
        }).start();
    }

}
