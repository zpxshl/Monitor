package com.example.pxshl.yc_monitior.util;

import android.os.Environment;

import java.io.File;

/**
 * Created by pxshl on 17-7-26.
 */

//全局变量
public class Data {

    public static boolean isLogin;
    public static String account = "";
    public static String password = "";
    public static String account_msg = account + " " + Tools.pwdToMd5(password);
    public volatile static int alarm_sensitivity = -1; //报警灵敏度 取自范围0-100， 0表示没开启报警功能

    public final static String SERVER_IP = "119.23.240.131";

    public final static int SERVER_PORT1 = 8890; //默认用该端口和服务器通信（服务器要求的）
    public final static int SERVER_PORT2 = 8891;  //发送验证码时需要用到的端口
    public final static String MONITOR_WIFI_IP = "172.24.1.1";
    public final static int MONITOR_PORT = 8888;   //监控器端口，用于发送广播和wifi帐号密码时需要

    public  static int UDP_PORT = 20000; //初始化为20000，会根据实际情况更改

    private static final String DL_PATH =   Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +"monitor" +
            File.separator;
    public static final String DL_VIDEO_PATH = DL_PATH + "video" + File.separator;
    public static final String DL_PHOTO_PATH = DL_PATH + "photo" + File.separator;


    //和服务器约定好的命令，用char表示
    public final static char LOGIN = 'L';
    public final static char ASK_ALARM_SENSITIVITY = 'A';
    public final static char SET_ALARM_SENSITIVITY = 'B';
    public final static char CHANGE_PASSWORD = 'C';
    public final static char START_PLAY  = 'P';
    public final static char STOP_PLAY  = 's';
    public final static char LIST_FILE  = 'l';
    public final static char FILE_SIZE  = 'S';
    public final static char DOWNLOAD  = 'D';
    public final static char PHOTO = 'p'; //图片 jpg
    public final static char ALARM = 'a';
    public final static char PHOTO_DATE = 'b';
    public final static char SEND_PHONE = 'x';
    public final static char CAPTCHA = 'X';



}
