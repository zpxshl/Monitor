package com.example.pxshl.yc_monitior.model;

import java.io.Serializable;

/**
 * 文件信息类
 */

public class FileInfo implements Serializable {
    //文件id
    private int id;
    //文件名
    private String fileName;

    //是否下载好
    private boolean isFinish;
    //文件大小
    private long length;
    //已下载的大小
    private long finished;
    //是否正在下载
    private boolean isDownloading;
    //文件在list显示的位置
    private int i;
    private int i1;

    /**
     * 带参数的构造
     */
    public FileInfo(int id, String fileName) {
        this.id = id;
        this.fileName = fileName;
    }

    /**
     * getter和setters
     */
    public int getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }


    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean downloading) {
        this.isDownloading = downloading;
    }

    public void setI(int i) {
        this.i = i;
    }

    public void setI1(int i1) {
        this.i1 = i1;
    }

    public int getI() {
        return i;
    }

    public int getI1() {
        return i1;
    }


    public void setId(int id) {
        this.id = id;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public long getLength() {
        return length;
    }

    public long getFinished() {
        return finished;
    }


    public void setFinish(boolean finish) {
        isFinish = finish;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public void setFinished(long finished) {
        this.finished = finished;
    }

    /**
     * 重写toString()
     */
    @Override
    public String toString() {
        return "FileInfo{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", isFinish=" + isFinish +
                ", length=" + length +
                ", finished=" + finished +
                ", isDownloading=" + isDownloading +
                ", i=" + i +
                ", i1=" + i1 +
                '}';
    }
}
