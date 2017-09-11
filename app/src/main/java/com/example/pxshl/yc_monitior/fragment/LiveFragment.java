package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.content.Context;
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
import com.example.pxshl.yc_monitior.net.tcp.TcpConnect;
import com.example.pxshl.yc_monitior.net.udp.UdpConnect;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import java.io.IOException;
import java.nio.ByteBuffer;


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
    private MediaCodec mCodec;
    private Activity mActivity;


    // Video Constants
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int VIDEO_WIDTH = 640;
    private final static int VIDEO_HEIGHT = 480;
    private final static int TIME_INTERNAL = 15;

    byte[] sps = {0, 0, 0, 1, 39, 100, 0, 40, -84, 43, 64, 80, 30, -48, 15, 18, 38, -96};
    byte[] pps = {0, 0, 0, 1, 40, -18, 2, 92, -80};


    private void startReceive() {
        initDecoder();
        if (rtp_socket == null) {
            try {
                rtp_socket = new RtpSocket(new SipdroidSocket(Data.UDP_PORT));
            } catch (Exception e) {
                e.printStackTrace();

            }

            rtp_packet = new RtpPacket(socketBuffer, 0);
        }

        isRunning = true;
        Decoder decoder = new Decoder();
        decoder.start();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, null);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.surfaceView);

        final Button play = (Button) (view.findViewById(R.id.play));
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRunning) {
                    TcpConnect.send(Data.START_PLAY + " " + Data.account + " " + Tools.pwdToMd5(Data.password));
                    TcpConnect.receive(new RequestCallBack() {
                        @Override
                        public void onFinish(String response) {
                            Log.e("play_response", response);
                            if (response.equals("no")) {
                                if (mActivity != null) {
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "监控器不在线", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                return;
                            }

                            int port = 0;
                            try {
                                port = Integer.parseInt(response.trim());
                            } catch (Exception e) {
                                e.printStackTrace();
                                return;
                            }


                            UdpConnect.sendUDP("app", port, new SendCallBack() {
                                @Override
                                public void onFinish() {
                                    startReceive();
                                }

                                @Override
                                public void onError() {
                                    Log.e("liveFragment", "UDP包发送失败");
                                }
                            });
                        }

                        @Override
                        public void onError() {

                        }
                    });

                    play.setText("停止");

                } else {
                    close();
                    TcpConnect.send(Data.STOP_PLAY + " " + Data.account + " " + Tools.pwdToMd5(Data.password));
                    play.setText("播放");

                }
            }
        });


        return view;
    }

    private void close() {
        isRunning = false;
        if (mCodec != null) {
            mCodec.release();
        }
        if (rtp_socket != null) {
            rtp_socket.close();
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); //Activity被销毁再重新生成时，该fragment不会被重新构造
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

    public void initDecoder() {

        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                VIDEO_WIDTH, VIDEO_HEIGHT);
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
        mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));

        mCodec.configure(mediaFormat, mSurfaceView.getHolder().getSurface(), null, 0);
        mCodec.start();

    }


    class Decoder extends Thread {
        public void run() {
            boolean hasStartPacket, hasEndPacket, isContinue, isRightGet;
            int lastSequence, nowSequence, frmSize;

            while (isRunning) {

                hasStartPacket = hasEndPacket = false;
                isRightGet = true;
                isContinue = true;
                frmSize = 0;
                lastSequence = -1;
                nowSequence = 0;


                while (isContinue) {

                    try {
                        rtp_socket.receive(rtp_packet);
                    } catch (IOException e) {
                        e.printStackTrace();
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
            }

            if (rtp_socket != null) {
                rtp_socket.close();
                rtp_socket = null;
            }


        }
    }

    @Override
    public void onDestroyView() {
        Log.e("DesView", "DesView");
        try {
            if (isRunning)
                 close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            super.onDestroyView();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Override
    public void onResume() {

        super.onResume();

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {

                    if (isRunning) {
                        close();
                    }

                }

                return false;
            }
        });
    }

}



