package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pxshl.yc_monitior.BuildConfig;
import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.adapter.DownLoadELVAdapter;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.model.FileInfo;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.service.DownloadService;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 下载
 */

public class DownLoadFragment extends Fragment {

    private ExpandableListView mListView ;   //显示数据的控件
    private DownLoadELVAdapter mAdapter;    //适配器
    private List<String> groupsList;    //一级列表
    private Map<Integer, List<FileInfo>> childMap; //二级列表
    private MyReceiver mReceiver;
    private SwipeRefreshLayout mSRL;
    private TextView mEmptyTv;

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
            init();
        }

        return mView;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity(); //保存引用
    }

    @Override   //兼容低版本安卓系统
    public void onAttach(Activity activity){
        super.onAttach(activity);
        mActivity = getActivity();
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


    public void init() {

        mEmptyTv = (TextView) mView.findViewById(R.id.downFrag_empty_tv);
        mListView = (ExpandableListView) mView.findViewById(R.id.show_data_list_view);
        groupsList = new ArrayList<String>();
        childMap = new HashMap<Integer, List<FileInfo>>();
        mAdapter = new DownLoadELVAdapter(getContext(), groupsList, childMap);
        mListView.setAdapter(mAdapter);
        mListView.setEmptyView(mEmptyTv);

       mListView.setOnTouchListener(new View.OnTouchListener() {    //避免refreshLayout错误消费了ListView的滑动事件 当设置了EmptyView时会出现这问题
           @Override
           public boolean onTouch(View v, MotionEvent event) {
               if ( event.getAction() == MotionEvent.ACTION_MOVE){
                   if (mListView.getFirstVisiblePosition() == 0 && mListView.getChildAt(0).getTop() >= mListView.getListPaddingTop()){
                       mSRL.setEnabled(true);
                   }else {
                       mSRL.setEnabled(false);
                   }
               }
               return false;
           }
       });

        onChildOnClickListener();
        loadFileList();
        mSRL = (SwipeRefreshLayout) mView.findViewById(R.id.downFrag_srl);
        mSRL.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                groupsList.clear();         //刷新，先清除现有数据
                childMap.clear();
                loadFileList();
            }
        });

    }

    //加载下载列表
    private void loadFileList() {
        new TcpTool(Data.SERVER_IP,Data.SERVER_PORT1).connect(Data.LIST_FILE + " " + Data.account + " " + Tools.pwdToMd5(Data.password),new RequestCallBack() {
            @Override
            public void onFinish(String response) {

                if (response.equals("")){
                    showMsg("出错啦，请稍后再刷新试下～");
                }
                else if (response.contains("offline")){
                    showMsg("监控器不在线~");
                }else if(response.contains("no video")){
                    showMsg("没有可下载的录像~");
                } else{

                    int file_id = 0;
                    int child_id = 0;
                    String[] dates = response.substring(1).split("[+]"); //去掉开头的+号  （服务端无法处理成想要的数据格式，只能自己搞）

                    List<String> fileList = Tools.getFileList(Data.DL_VIDEO_PATH); //扫描下载列表

                    for (String date : dates){
                        List<FileInfo> fileInfos = new ArrayList<FileInfo>();
                        String[] fileName = date.split(" ");

                        if (fileName.length <= 0){
                            return;
                        }

                        groupsList.add(fileName[0]);

                        for (int j = 1; j < fileName.length ;j++){
                            //fileName[0]  + "/"+ fileName[j]  和服务器约定好的文件名格式
                            FileInfo fileInfo = new FileInfo(file_id,fileName[0]  + '/' + fileName[j]);

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
                                mSRL.setRefreshing(false);
                            }
                        });
                    }
                }
            }

            @Override
            public void onError() {
                showMsg("连接服务器失败，请检查网络连接~");
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
                    Intent mp4Intent = new Intent("android.intent.action.VIEW");
                    Uri uri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){ //安卓N或以上
                        mp4Intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        uri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".fileProvider", new File(Data.DL_VIDEO_PATH + fileInfo.getFileName().split("/")[0] + File.separator + fileInfo.getFileName().split("/")[1] ));

                    }else {
                        mp4Intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        uri = Uri.fromFile(new File(Data.DL_VIDEO_PATH + fileInfo.getFileName().split("/")[0] + File.separator + fileInfo.getFileName().split("/")[1] ));
                    }


                    mp4Intent.putExtra("oneshot", 0);
                    mp4Intent.putExtra("configchange", 0);
                    mp4Intent.setDataAndType(uri, "video/*");
                    startActivity(mp4Intent);

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

            if (action != null){
                if (action.equals(DownloadService.FAIL)){
                    Toast.makeText(getContext(),fileInfo.getFileName() + " 下载失败，请重试",Toast.LENGTH_SHORT).show();
                }else if (action.equals(DownloadService.FINISH)){
                    Toast.makeText(getContext(),fileInfo.getFileName() + " 下载成功",Toast.LENGTH_SHORT).show();
                }
            }


        }
    }


    private void showMsg(final String msg) {
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    mEmptyTv.setText(msg);
                    mSRL.setRefreshing(false);
                }
            });

        }
    }

}
