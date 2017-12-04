package com.example.pxshl.yc_monitior.util;

import com.example.pxshl.yc_monitior.fragment.LiveFragment;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;

/**
 * Created by pxshl on 17-11-29.
 * 播放时发送心跳的任务类
 */

public class HBTask {


    private LiveFragment.LiveFrom mLiveFrom;
    //tcp
    private Socket mSocket;
    private OutputStream mOs;
    //udp
    private DatagramSocket mDSocket;
    private DatagramPacket mDPacket;

    public HBTask(LiveFragment.LiveFrom liveFrom) {
        mLiveFrom = liveFrom;
    }

    void run() {

        switch (mLiveFrom) {
            case Monitor:
                HBForMonitor();
                break;
            case Server:
                HBForServer();
                break;
            default:
                break;
        }
    }

    private void HBForServer() {

        try {
            if (mSocket == null || !mSocket.isConnected() || mSocket.isClosed()) {
                mSocket = new Socket(Data.SERVER_IP, Data.SERVER_PORT3);
            }

            if (mOs == null && mSocket != null) {
                mOs = mSocket.getOutputStream();
            }

            String msg = Data.HEART_BEAT + " " + Data.account + " " + Tools.pwdToMd5(Data.password);
            mOs.write(msg.getBytes());
            mOs.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void HBForMonitor() {

        try {
            if (mDPacket == null) {

                if (Data.MONITOR_IP != null) {
                 String msg = Data.HEART_BEAT + " " + Data.account + " " + Tools.pwdToMd5(Data.password);
                 mDPacket = new DatagramPacket(msg.getBytes(), msg.length(), Data.MONITOR_IP, Data.MONITOR_PORT1);
                } else {
                    return;
                }
            }

            if (mDSocket == null || !mDSocket.isConnected() || mDSocket.isClosed()){
                mDSocket = new DatagramSocket();
            }

            mDSocket.send(mDPacket);       //发送两个包，防止丢包
            mDSocket.send(mDPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void cancel() {

        try {
            if (mSocket != null) {
                mSocket.close();
            }
            if (mDSocket != null){
                mDSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
