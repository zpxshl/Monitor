package com.example.pxshl.yc_monitior.util;

/**
 * Created by pxshl on 17-11-28.
 * 发送心跳包的定时类
 */

public class HBTimer {

    private volatile boolean isCancel;
    private HBTask mTask;

    public HBTimer(HBTask task) {
        mTask = task;
    }

    public void schedule(final long period) {

        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {

                    if (isCancel) {
                        mTask.cancel();
                        break;
                    }

                    mTask.run();

                    try {
                        Thread.sleep(period);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start(); //子线程执行定时任务
    }

    public void cancel() {
        isCancel = true;
    }

}

