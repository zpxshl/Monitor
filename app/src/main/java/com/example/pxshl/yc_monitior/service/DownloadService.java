package com.example.pxshl.yc_monitior.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;


import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.model.FileInfo;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.DownloadTask;
import com.example.pxshl.yc_monitior.util.Tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *后台下载
 * 下载是按天数分类，一天为一个文件夹，方便用户分时间段查看
 */

public class DownloadService extends Service {
    //有空改成枚举
    //开始下载命令
    public static final String START_DOWNLOAD = "START_DOWNLOAD";

    public static final String UPDATE = "UPDATE";
    public static final  String DELETE = "DELETE";
    public static final String FAIL = "FAIL";
    public static final String FINISH = "FINISH";
    //下载的文件保存路径


    //下载任务类的集合
    private Map<Integer, DownloadTask> mTasks = new LinkedHashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        FileInfo fileInfo;
        try {
            fileInfo = (FileInfo)intent.getSerializableExtra("fileInfo");    //某些情况会抛出异常，导致程序奔溃
        }catch (Exception e){
            return super.onStartCommand(intent, flags, startId);
        }


        if (intent.getAction().equals(START_DOWNLOAD)) {
            initDownLoad(fileInfo);
        } else if (intent.getAction().equals(DELETE)) {
            DownloadTask task = mTasks.get(fileInfo.getId());
            task.isCancel = true;
        }

        return super.onStartCommand(intent, flags, startId);

    }

    private void initDownLoad(final FileInfo fileInfo) {

          new TcpTool().connect(Data.FILE_SIZE + " " + Data.account + " " + Tools.pwdToMd5(Data.password) + " " +   fileInfo.getFileName(),new RequestCallBack() {
            @Override
            public void onFinish(String response) {

                long fileSize;
                try{
                    //防止服务器抽风,返回错误数据
                    fileSize = Long.parseLong(response.trim());
                }catch (NumberFormatException e){
                    downloadFail(fileInfo);
                    return ;
                }

                if (fileSize <= 0){
                    downloadFail(fileInfo);
                    return;
                }
                RandomAccessFile raf = null;
                fileInfo.setLength(fileSize);

                File dir = new File(Data.DL_VIDEO_PATH + fileInfo.getFileName().split("/")[0]);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                File file = new File(dir, fileInfo.getFileName().split("/")[1]);

                try {
                    raf = new RandomAccessFile(file, "rwd");
                    raf.setLength(fileSize);

                    DownloadTask task = new DownloadTask(DownloadService.this, fileInfo);
                    task.start();
                    mTasks.put(fileInfo.getId(), task); //内存泄漏？

                } catch (IOException e) {
                    downloadFail(fileInfo);
                    e.printStackTrace();
                }finally {
                    if (raf != null){
                        try {
                            raf.close();
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onError() {
                downloadFail(fileInfo);
            }
        });

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void downloadFail(FileInfo fileInfo){
        Intent intent = new Intent(DownloadService.FAIL);
        intent.putExtra("fileInfo", fileInfo);
        this.sendBroadcast(intent);
    }

}
