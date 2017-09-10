package com.example.pxshl.yc_monitior.util;

/**
 * Created by pxshl on 17-7-26.
 */

//全局变量
public class Data {

    public static boolean isLogin;

    public static String account = "";
    public static String password = "";

    public final static String SERVER_IP = "119.23.240.131";
  //  public final static String SERVER_IP = "119.23.240.122";
    public final static String MONITOR_WIFI_IP = "172.24.1.1";
    public  static int UDP_PORT = 20000; //初始化为20000，会根据实际情况更改

    //和服务器约定好的命令，用char表示
    public final static char LOGIN = 'L';
    public final static char ASK = 1;  //暂时没用的命令
    public final static char CHANGE_PASSWORD = 'C';

    public final static char START_PLAY  = 'P';
    public final static char STOP_PLAY  = 's';
    public final static char LIST_FILE  = 'l';
    public final static char FILE_SIZE  = 'S';
    public final static char DOWNLOAD  = 'D';
    public final static char PHOTO = 105; //图片 jpg
    public final static char alarm = 106;

}
