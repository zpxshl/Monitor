package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.application.MyApplication;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.net.rtp.RtpPacket;
import com.example.pxshl.yc_monitior.net.rtp.RtpSocket;
import com.example.pxshl.yc_monitior.net.rtp.SipdroidSocket;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.HBTask;
import com.example.pxshl.yc_monitior.util.HBTimer;
import com.example.pxshl.yc_monitior.util.Tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import static android.content.Context.MODE_PRIVATE;


/**
 * 直播
 */

public class LiveFragment extends Fragment {

    private static final String TAG = "LiveFragment";

    private transient boolean isRunning;

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

    private HBTimer mTimer;
    public enum LiveFrom {Monitor, Server}
    private LiveFrom mLiveFrom;
    private DatagramPacket mPacket;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, null);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);
        mSurfaceView.setKeepScreenOn(true);
        mSurfaceView.setVisibility(View.INVISIBLE);

        //设置surfaceview的大小为 4：3 避免不按比例拉伸导致变形
        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int widthPixels = dm.widthPixels;
        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = widthPixels;
        lp.height =widthPixels / 4 * 3;
        mSurfaceView.setLayoutParams(lp);

        mPlay = (Button) (view.findViewById(R.id.play));
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (!isRunning) {

                    if (System.currentTimeMillis() - mCurrentTime < 2000) {  //服务器要求的
                        Toast.makeText(MyApplication.getContext(), "请休息下，别按得太快哦～", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    live();
                } else {
                    mCurrentTime = System.currentTimeMillis();
                    String msg = Data.STOP_PLAY + " " + Data.account + " " + Tools.pwdToMd5(Data.password);
                    new TcpTool(Data.SERVER_IP, Data.SERVER_PORT1).connect(msg, null);
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

        ask_BSSID();
    }


    //播放视频
    private void live() {

        if (Tools.canLiveFromMoniotr(MyApplication.getContext())) {
            liveFromMonitor();
        } else {
            liveFromServer();
        }

        mPlay.setText("停止");
        mSurfaceView.setVisibility(View.VISIBLE);
        showMsg("正在努力加载，请稍等数秒～");

    }

    private void liveFromMonitor() {
        mLiveFrom = LiveFrom.Monitor;
        isRunning = true;
        String msg = "play_lan " + Data.account + " P " + Tools.pwdToMd5(Data.password);

        try {
            mPacket = new DatagramPacket(msg.getBytes(), msg.length(), InetAddress.getByName("255.255.255.255"), Data.MONITOR_PORT1);
        } catch (Exception e) {
            close();
            showMsg("网络异常，请稍后再试");
            e.printStackTrace();
        }

    }

    private void liveFromServer() {
        mLiveFrom = LiveFrom.Server;

        isRunning = true;

        new TcpTool(Data.SERVER_IP, Data.SERVER_PORT1).connect(Data.START_PLAY + " " + Data.account + " " + Tools.pwdToMd5(Data.password), new RequestCallBack() {
            @Override
            public void onFinish(String response) {

                if (response.contains("offline")) {
                    Log.e(TAG, "offLine");
                    close();
                    showMsg("监控器不在线");
                } else if (response.equals("")) {
                    close();
                    showMsg("服务器异常，请稍后再试");
                } else {

                    int port;
                    try {
                        port = Integer.parseInt(response.trim());
                        String msg = "app";
                        mPacket = new DatagramPacket(msg.getBytes(), msg.length(), InetAddress.getByName(Data.SERVER_IP), port);
                        startReceive();
                    } catch (Exception e) {
                        close();
                        showMsg("网络异常，请稍后再试");
                        e.printStackTrace();
                    }


                }

            }

            @Override
            public void onError() {
                close();
                showMsg("网络异常，请检查网络");
            }

        });
    }

    private void startReceive() throws Exception {


        isRunning = true;
        initDecoder();

        SipdroidSocket sSocket = new SipdroidSocket(0);
        sSocket.setSoTimeout(7000);
        sSocket.send(mPacket);   //发送个包给视频源（监控器or服务器），让对方找到你
        sSocket.send(mPacket);   //防止丢包多发一个
        sSocket.receive(mPacket);
        Data.MONITOR_IP = mPacket.getAddress();   //或者对方ip，在局域网内播放时，心跳包需要该ip

        rtp_socket = new RtpSocket(sSocket);
        rtp_packet = new RtpPacket(socketBuffer, 0);


        startDecoder();
    }


    public void initDecoder() {

        String MIME_TYPE = "video/avc";
        byte[] sps = {0, 0, 0, 1, 39, 100, 0, 40, -84, 43, 64, 80, 30, -48, 15, 18, 38, -96};
        byte[] pps = {0, 0, 0, 1, 40, -18, 2, 92, -80};

        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                    640, 480);
            mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
            mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));

            mCodec.configure(mediaFormat, mSurfaceView.getHolder().getSurface(), null, 0);
            mCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void startDecoder() throws Exception {
        boolean hasStartPacket, hasEndPacket, isContinue, isRightGet;
        int lastSequence, nowSequence, frmSize;

        mTimer = new HBTimer(new HBTask(mLiveFrom));
        mTimer.schedule(5000);

        while (isRunning) {

            hasStartPacket = hasEndPacket = false;
            isRightGet = true;
            isContinue = true;
            frmSize = 0;
            lastSequence = Integer.MIN_VALUE;
            nowSequence = 0;


            while (isContinue) {

                rtp_socket.receive(rtp_packet);

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
                            Log.d(TAG, "Begin: " + nowSequence + "");
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
                            Log.d(TAG, "Middle" + nowSequence + "");
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
                            Log.d(TAG, "End" + nowSequence + "");
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

            if (!(hasStartPacket && hasEndPacket && isRightGet))  //该帧残缺，丢弃
                continue;


            if (frmSize <= 0)
                continue;

            byte[] h254Header = {0, 0, 0, 1};
            byte[] h264Frmbuf = new byte[65536];
            System.arraycopy(h254Header, 0, h264Frmbuf, 0, 4);
            System.arraycopy(frmbuf, 0, h264Frmbuf, 4, frmSize);
            onFrame(h264Frmbuf, 0, frmSize + 4);  //播放


            Thread.sleep(33);    //30帧是硬件的极限，将播放帧率控制在30帧以下，避免由于网络抖动导致的“异常高速播放”
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
                    * 15L, 0);
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


    private void close() {
        Log.e(TAG, "close");
        isRunning = false;

        try {                   //实践证明，mCodec.release(),rtp_socket.close()很容易抛出异常
            Thread.sleep(60);   //让正在播放的视频完全播放完
            if (mCodec != null) {
                mCodec.release();
                mCodec = null;
            }
            if (rtp_socket != null) {
                rtp_socket.close();
                rtp_socket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.gc();

        if (mTimer != null) {
            mTimer.cancel();  //关闭心跳包(
            Log.e(TAG, "mTimer cancel");
        }

        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mPlay.setText("实时监控");
                    mSurfaceView.setVisibility(View.INVISIBLE);
                }
            });
        }



    }

    //得到当前帐号对应的监控器所连接的路由器的mac地址
    private void ask_BSSID() {

        String msg = Data.ASK_BSSID + " " + Data.account + " " + Tools.pwdToMd5(Data.password);
        new TcpTool(Data.SERVER_IP, Data.SERVER_PORT1).connect(msg, new RequestCallBack() {
            @Override
            public void onFinish(String response) {
                if (response.length() > 16) {
                    String bssid = response.trim();

                    //如果当前app储存的bssid与服务器一致，无需操作
                    if (Data.BSSID != null && Data.BSSID.equals(bssid)){
                        return;
                    }

                    //储存帐号密码
                    Data.BSSID = bssid;
                    SharedPreferences preferences = MyApplication.getContext().getSharedPreferences("properties", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("bssid", Data.BSSID);
                    editor.commit();
                }
            }

            @Override
            public void onError() {

            }
        });

    }

    private void showMsg(final String msg) {
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MyApplication.getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override   //兼容低版本安卓系统
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = getActivity();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isRunning) {
            close();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }



}

