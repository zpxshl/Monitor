package com.example.pxshl.yc_monitior.util;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.example.pxshl.yc_monitior.application.MyApplication;
import com.example.pxshl.yc_monitior.model.FileInfo;
import com.example.pxshl.yc_monitior.service.DownloadService;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;


/**
 * 下载类
 */

public class DownloadTask extends Thread {

    private Context mContext;
    private FileInfo mFileInfo;
    private Socket mSocket;
    public boolean isCancel = false;  //是否取消下载，取消之后就删除下载到一般的文件，不支持断点续传

    public DownloadTask(Context context, FileInfo fileInfo) {
        mContext = context;
        mFileInfo = fileInfo;
    }


    @Override
    public void run() {



        OutputStream os = null;
        BufferedInputStream in = null;
        // 下载是按天数分类，一天为一个文件夹，方便用户分时间段查看
        File file = new File(Data.DL_VIDEO_PATH + mFileInfo.getFileName().split("/")[0], mFileInfo.getFileName().split("/")[1]);
        RandomAccessFile raf = null;
        Intent intent = new Intent(DownloadService.UPDATE);

        try {
            mSocket = new Socket(Data.SERVER_IP, Data.SERVER_PORT1);
            mSocket.setSoTimeout(5000);
            //输出
            os = mSocket.getOutputStream();
            byte[] out_buff = (Data.DOWNLOAD + " " + Data.account + " " + Tools.pwdToMd5(Data.password) + " " + mFileInfo.getFileName() + '\n').getBytes();
            os.write(out_buff);
            os.flush();

            //输入
            in = new BufferedInputStream(mSocket.getInputStream());
            byte[] in_buff = new byte[1024 * 2];

            raf = new RandomAccessFile(file, "rwd");
            ;
            mFileInfo.setDownloading(true);
            int len;
            long finished = mFileInfo.getFinished();
            long lastTime = System.currentTimeMillis();

            while ((len = in.read(in_buff)) != -1) {
                if (isCancel) {
                    downloadFail();
                    return;
                }

                //累加已下载大小
                finished += len;
                raf.write(in_buff, 0, len);

                if (System.currentTimeMillis() - lastTime > 500) {
                    mFileInfo.setFinished(finished);
                    intent.putExtra("fileInfo", mFileInfo);     //设置频率发送广播
                    LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(intent);
                    lastTime = System.currentTimeMillis();
                }
            }

            if (finished < mFileInfo.getLength()) {  //试试用用不等于？
                throw new IOException("finished < fileSize " + " finish " + finished + " fileSize" + mFileInfo.getLength());
            }
            downloadFinish();
        } catch (IOException e) {
            downloadFail();
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (in != null) {
                    in.close();
                }
                if (mSocket != null) {
                    mSocket.close();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void downloadFail() {
        File file = new File(Data.DL_VIDEO_PATH, mFileInfo.getFileName());
        if (file.exists()) {
            file.delete();
        }

        mFileInfo.setFinish(false);
        mFileInfo.setDownloading(false);
        mFileInfo.setFinished(0);
        mFileInfo.setLength(0);
        Intent intent = new Intent(DownloadService.FAIL);
        intent.putExtra("fileInfo", mFileInfo);
        LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(intent);

    }

    private void downloadFinish() {
        mFileInfo.setFinish(true);
        mFileInfo.setDownloading(false);
        Intent intent = new Intent(DownloadService.FINISH);
        intent.putExtra("fileInfo", mFileInfo);
        LocalBroadcastManager.getInstance(MyApplication.getContext()).sendBroadcast(intent);
    }
}



