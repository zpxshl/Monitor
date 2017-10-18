package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Toast;


import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.inyerface.SendCallBack;
import com.example.pxshl.yc_monitior.net.rtp.RtpPacket;
import com.example.pxshl.yc_monitior.net.rtp.RtpSocket;
import com.example.pxshl.yc_monitior.net.rtp.SipdroidSocket;

import com.example.pxshl.yc_monitior.net.tcp.TcpTool;

import com.example.pxshl.yc_monitior.net.udp.UdpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;


/**
 * Created by pxshl on 17-7-26.
 */

public class LiveFragment extends Fragment {

    private boolean isRunning;

    private RtpSocket rtp_socket = null;
    private RtpPacket rtp_packet = null;
    private byte[] socketBuffer = new byte[2048];
    private byte[] buffer = new byte[2048];
    private byte[] frmbuf = new byte[655360];
    private int frmbufLen = 0;
    private SurfaceView mSurfaceView;
    private Button mPlay;
    private MediaCodec mCodec;
    private Activity mActivity;
    private long mCurrentTime;

    // Video Constants
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int VIDEO_WIDTH = 640;
    private final static int VIDEO_HEIGHT = 480;
    private final static int TIME_INTERNAL = 15;

    byte[] sps = {0, 0, 0, 1, 39, 100, 0, 40, -84, 43, 64, 80, 30, -48, 15, 18, 38, -96};
    byte[] pps = {0, 0, 0, 1, 40, -18, 2, 92, -80};


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, null);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);
        mSurfaceView.setVisibility(View.INVISIBLE);
        mPlay = (Button) (view.findViewById(R.id.play));
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (System.currentTimeMillis() - mCurrentTime < 2000){  //服务器要求的
                    Toast.makeText(getContext(),"正在加载，请稍后再按",Toast.LENGTH_SHORT).show();
                    return;
                }

                mCurrentTime = System.currentTimeMillis();

                if (!isRunning) {
                    mSurfaceView.setVisibility(View.VISIBLE);
                    liveFromMonitor();   //默认先从监控器加载视频数据，失败再从服务器加载
                    showMsg("正在努力加载，请稍等数秒～");
                } else {
                    mSurfaceView.setVisibility(View.INVISIBLE);
                    mPlay.setText("播放");
                    close();
                }

            }
        });


        return view;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); //Activity被销毁再重新生成时，该fragment不会被重新构造
    }



    private void liveFromMonitor() {
        isRunning = true;
        UdpTool.sendUDP("255.255.255.255",Data.MONITOR_PORT,new SendCallBack() {
            @Override
            public void onFinish() {
                try {
                    startReceive();
                }catch (TimeoutException e){  //当局域网超过3秒没回应时
                    e.printStackTrace();
                    close();
                    liveFromServer();   //说明无法直接从监控器那里获取直播数据。所以将数据源改为服务器
                }
            }

            @Override
            public void onError() {
                liveFromServer();
            }
        });
    }

    private void liveFromServer(){

        isRunning = true;

        new TcpTool(Data.SERVER_IP,Data.SERVER_PORT1).connect(Data.START_PLAY + " " + Data.account + " " + Tools.pwdToMd5(Data.password),new RequestCallBack() {
            @Override
            public void onFinish(String response) {

                if (response.contains("offline")) {
                    showMsg("监控器不在线");
                }else if (response.equals("")){
                    showMsg("服务器异常，请稍后再试");
                }else {

                    int port = 0;
                    try {
                        port = Integer.parseInt(response.trim());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }


                    UdpTool.sendUDP(Data.START_PLAY + " " +Data.account + " " + Tools.pwdToMd5(Data.password), port, new SendCallBack() {  //子线程回调
                        @Override
                        public void onFinish() {
                            try {
                                startReceive();
                            }catch (Exception e){
                                close();
                                showMsg("网络异常，请检查网络");
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onError() {
                            showMsg("网络异常，请检查网络");
                        }
                    });
                }

        }

            @Override
            public void onError() {
                showMsg("网络异常，请检查网络");
            }

        });
    }

    private void startReceive() throws TimeoutException{
        isRunning = true;
        initDecoder();
        if (rtp_socket == null) {
            try {
                SipdroidSocket sSocket = new SipdroidSocket(Data.UDP_PORT);
                sSocket.setSoTimeout(3000);
                rtp_socket = new RtpSocket(sSocket);
            } catch (Exception e) {
                e.printStackTrace();
                return;

            }

            rtp_packet = new RtpPacket(socketBuffer, 0);
        }


        startDecoder();  //当它抛出IOE异常，意味着4秒内没收到任何数据
    }


    public void initDecoder() {

        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                    VIDEO_WIDTH, VIDEO_HEIGHT);
            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
            mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));

            mCodec.configure(mediaFormat, mSurfaceView.getHolder().getSurface(), null, 0);
            mCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void startDecoder() throws TimeoutException{
        boolean hasStartPacket, hasEndPacket, isContinue, isRightGet;
        int lastSequence, nowSequence, frmSize;

        while (isRunning) {

            hasStartPacket = hasEndPacket = false;
            isRightGet = true;
            isContinue = true;
            frmSize = 0;
            lastSequence = Integer.MIN_VALUE;
            nowSequence = 0;


            while (isContinue) {


                try {
                    rtp_socket.receive(rtp_packet);
                }catch (IOException e){
                    throw new TimeoutException("time out");
                }


                int packetSize = rtp_packet.getPayloadLength();
                if (packetSize <= 2)
                    continue;

                if (rtp_packet.getPayloadType() != 96) {
                    continue;
                }

                System.arraycopy(socketBuffer, 12, buffer, 0, packetSize); //socketBuffer->buffer


                nowSequence = rtp_packet.getSequenceNumber();


                if ((buffer[0] & 0x1F) < 24) {
                    System.arraycopy(buffer, 0, frmbuf, 0, packetSize);
                    frmSize = packetSize;
                    frmbufLen = 0;
                }
                if ((buffer[0] & 0x1F) == 28) {
                    switch ((buffer[1] >> 6) & 0x03) {
                        case 2:
                            Log.e("Begin", nowSequence + "");
                            if (hasStartPacket) {
                                isContinue = false;
                                isRightGet = false;
                                break;
                            } else {
                                hasStartPacket = true;
                                lastSequence = nowSequence;
                            }
                            byte frameHeader = (byte) ((buffer[0] & 0xE0) | (buffer[1] & 0x1F));
                            frmbuf[0] = frameHeader;
                            System.arraycopy(buffer, 2, frmbuf, 1, packetSize - 2);
                            frmbufLen = packetSize - 1;
                            break;

                        case 0:
                            Log.e("Middle", nowSequence + "");
                            if (nowSequence - lastSequence != 1 || !hasStartPacket) {
                                isContinue = false;
                                isRightGet = false;
                                break;
                            } else {
                                lastSequence = nowSequence;
                            }
                            System.arraycopy(buffer, 2, frmbuf, frmbufLen, packetSize - 2);
                            frmbufLen += packetSize - 2;
                            break;

                        case 1:
                            Log.e("END", nowSequence + "");
                            if (hasEndPacket || (nowSequence - lastSequence != 1) || !hasStartPacket) {
                                isContinue = false;
                                isRightGet = false;
                                break;
                            } else {
                                hasEndPacket = true;
                                lastSequence = nowSequence;
                                isContinue = false;
                            }
                            System.arraycopy(buffer, 2, frmbuf, frmbufLen, packetSize - 2);
                            frmbufLen += packetSize - 2;
                            frmSize = frmbufLen;
                            frmbufLen = 0;
                            break;
                    }
                }
            }

            if (!(hasStartPacket && hasEndPacket && isRightGet))
                continue;


            if (frmSize <= 0)
                continue;

            byte[] h254Header = {0, 0, 0, 1};
            byte[] h264Frmbuf = new byte[65536];
            System.arraycopy(h254Header, 0, h264Frmbuf, 0, 4);
            System.arraycopy(frmbuf, 0, h264Frmbuf, 4, frmSize);
            onFrame(h264Frmbuf, 0, frmSize + 4);

            try {
                Thread.sleep(33);    //30帧是硬件的极限，将播放帧率控制在30帧以下，避免由于网络抖动导致的“异常高帧率播放”
            }catch (InterruptedException e){
                e.printStackTrace();
            }

        }
    }

    int mCount = 0;

    public boolean onFrame(byte[] buf, int offset, int length) {

        // Get input buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(100);


        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount
                    * TIME_INTERNAL, 0);
            mCount++;
        } else {
            return false;
        }

        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);
        while (outputBufferIndex >= 0) {
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }

        return true;
    }

    private void close() {  //再添加关闭心跳包
        isRunning = false;
        try {                   //实践证明，mCodec.release(),rtp_socket.close()很容易抛出异常
            if (mCodec != null) {
                mCodec.release();
            }
            if (rtp_socket != null) {
                rtp_socket.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    private void showMsg(final String msg){
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    mPlay.setText(isRunning?"停止":"播放");
                    mSurfaceView.setVisibility(isRunning?View.VISIBLE:View.INVISIBLE);
                }
            });
        }
    }




    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isRunning){
            close();
        }
        Log.e("LiveFragment","onDestroyView");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.e("LiveFragment","onDetach");
        mActivity = null;
    }

}

