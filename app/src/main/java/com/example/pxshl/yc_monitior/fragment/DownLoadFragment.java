package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.adapter.MyExpanableListViewAdapter;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.model.FileInfo;
import com.example.pxshl.yc_monitior.net.tcp.TcpConnect;
import com.example.pxshl.yc_monitior.service.DownloadService;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by pxshl on 17-7-26.
 */

public class DownLoadFragment extends Fragment {

    private ExpandableListView mListView ;   //显示数据的控件
    private MyExpanableListViewAdapter mAdapter;    //适配器
    private List<String> groupsList;    //一级列表
    private Map<Integer, List<FileInfo>> childMap; //二级列表
    private MyReceiver mReceiver;

    private Activity mActivity;  //防止当Activity被系统回收（或暂时回收时，getActivity（）返回null）
    private View mView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); //Activity被销毁再重新生成时，该fragment不会被重新构造
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        IntentFilter intentFilter = new IntentFilter();
        mReceiver = new MyReceiver();
        intentFilter.addAction(DownloadService.UPDATE);
        intentFilter.addAction(DownloadService.FAIL);
        intentFilter.addAction(DownloadService.FINISH);
        mActivity.registerReceiver(mReceiver, intentFilter);

        if (mView == null){
            mView = inflater.inflate(R.layout.fragment_download,null);
    //        init();
        }

        return mView;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity(); //保存引用
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null; //释放引用
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mActivity.unregisterReceiver(mReceiver);
    }

    /**
     * 初始化操作
     */
    public void init() {

        mListView = (ExpandableListView) mView.findViewById(R.id.show_data_list_view);
        groupsList = new ArrayList<String>();
        childMap = new HashMap<Integer, List<FileInfo>>();
        mAdapter = new MyExpanableListViewAdapter(getContext(), groupsList, childMap);
        mListView.setAdapter(mAdapter);
        onChildOnClickListener();

        TcpConnect.send(Data.LIST_FILE + " " + Data.account + " " + Tools.pwdToMd5(Data.password));
        TcpConnect.receive(new RequestCallBack() {
            @Override
            public void onFinish(String response) {
                if (response.equals("no")){
                    runOnUIThreadToast("监控器不在线");
                    return;
                }else {



                    int file_id = 0;
                    int child_id = 0;
                    String[] dayData = response.split("[+]");

                    List<String> fileList = Tools.getFileList(DownloadService.DOWNLOAD_PATH); //扫描下载列表

                    for (String data: dayData){
                        List<FileInfo> fileInfos = new ArrayList<FileInfo>();
                        String[] fileName = data.split(" ");

                        if (fileName.length <= 0){
                            return;
                        }

                        groupsList.add(fileName[0]);

                        for (int i = 1; i < fileName.length ;i++){
                            //fileName[0]  + "/"+ fileName[i]  和服务器约定好的文件名格式
                            FileInfo fileInfo = new FileInfo(file_id,fileName[0]  + '/' + fileName[i],false,0,0,false,0,0);

                            for (String name:fileList){    //判断是否有已经下载
                                if (name.equals(fileInfo.getFileName())){
                                    fileInfo.setFinish(true);
                                    break;
                                }
                            }

                            fileInfos.add(fileInfo);
                            file_id++;
                        }

                        childMap.put(child_id,fileInfos);
                        child_id++;
                    }
                    if (mActivity != null){
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }

            @Override
            public void onError() {
                runOnUIThreadToast("连接服务器失败");
            }
        });




    }

    public void onChildOnClickListener() {
        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                FileInfo fileInfo = childMap.get(i).get(i1);
                fileInfo.setI(i);
                fileInfo.setI1(i1);
                Intent intent = new Intent(mActivity, DownloadService.class);
                if (!fileInfo.isFinish() && !fileInfo.isDownloading()) {
                    //启动下载
                    intent.setAction(DownloadService.START_DOWNLOAD);
                    intent.putExtra("fileInfo", fileInfo);
                    mActivity.startService(intent);
                    Toast.makeText(getContext(), fileInfo.getFileName() + "开始下载", Toast.LENGTH_SHORT).show();
                } else if (!fileInfo.isFinish() && fileInfo.isDownloading()) {
                    //停止下载
                    intent.setAction(DownloadService.DELETE);
                    intent.putExtra("fileInfo", fileInfo);
                    mActivity.startService(intent);
                    Toast.makeText(getContext(), fileInfo.getFileName() + "停止下载", Toast.LENGTH_SHORT).show();
                } else if (fileInfo.isFinish()){
                    //播放视频，启动系统的播放器进行播放


                }

                return true;
            }
        });
    }


    //接收广播，更新fileInfo，调用mAdapter的update方法
    class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            mAdapter.update(fileInfo);

            String action = intent.getAction();
            if (action.equals(DownloadService.FAIL)){
                Toast.makeText(getContext(),fileInfo.getFileName() + " 下载失败，请重试",Toast.LENGTH_SHORT).show();
            }else if (action.equals(DownloadService.FINISH)){
                Toast.makeText(getContext(),fileInfo.getFileName() + " 下载成功",Toast.LENGTH_SHORT).show();
            }

        }
    }


    private void runOnUIThreadToast(final String msg){
        if (mActivity == null){
            return;
        }else {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(),msg,Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


}
