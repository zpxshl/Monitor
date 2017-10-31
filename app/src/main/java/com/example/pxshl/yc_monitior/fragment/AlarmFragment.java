package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.adapter.AlarmELVAdapter;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.model.AlarmInfo;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 该碎片会展示报警的图片
 */

public class AlarmFragment extends Fragment {


    private Activity mActivity;
    private TextView mTextView;
    private SwipeRefreshLayout mSRL;
    private View mView;

    private AlarmELVAdapter mAdapter;    //适配器
    private List<String> groupsList = new ArrayList<>(); //一级列表
    private Map<Integer, List<AlarmInfo>> childMap = new HashMap<>(); //二级列表
    private boolean[] hasLoad;   //储存是否已经加载了图片的布尔数组，避免多次加载图片

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override   //兼容低版本安卓系统
    public void onAttach(Activity activity){
        super.onAttach(activity);
        mActivity = getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        if (mView == null){
            mView = inflater.inflate(R.layout.fragment_alarm,null);
            init();
        }

        return mView;
    }

    private void init(){

        mTextView = (TextView) mView.findViewById(R.id.alarmFrag_empty_tv);
        mSRL = (SwipeRefreshLayout) mView.findViewById(R.id.alarm_frag_srl);
        final ExpandableListView listView = (ExpandableListView) mView.findViewById(R.id.frag_alarm_listView);
        mAdapter = new AlarmELVAdapter( getContext(),groupsList,childMap);
        listView.setAdapter(mAdapter);
        listView.setEmptyView(mTextView);
        loadGroupsInfo();



        listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                loadChildsInfo(groupPosition);
            }
        });

        listView.setOnTouchListener(new View.OnTouchListener() {    //避免refreshLayout错误消费了ListView的滑动事件 当设置了EmptyView时会出现这问题
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if ( event.getAction() == MotionEvent.ACTION_MOVE){
                    if (listView.getFirstVisiblePosition() == 0 && listView.getChildAt(0).getTop() >= listView.getListPaddingTop()){
                        mSRL.setEnabled(true);
                    }else {
                        mSRL.setEnabled(false);
                    }
                }
                return false;
            }
        });

        mSRL.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                groupsList.clear();         //刷新，先清除现有数据
                childMap.clear();
                loadGroupsInfo();
            }
        });

    }

    private void loadGroupsInfo() {

        new TcpTool(Data.SERVER_IP,Data.SERVER_PORT1).connect(Data.ALARM + " " + Data.account + " " + Tools.pwdToMd5(Data.password), new RequestCallBack() {
            @Override
            public void onFinish(String response) {


                if (response.equals("")){
                    showMsg("出错啦，请稍后再刷新试下～");
                }else if(response.contains("no open alarm")){
                    showMsg("您未开启监控报警～");
                } else if (response.contains("offline")){
                    showMsg("监控器不在线~");
                }else if(response.contains("no photo")){
                    showMsg("恭喜您，没有异常的报警信息~");
                }else {
                    String[] dates = response.split(" ");

                    for (int i = 0 ;i < dates.length ;i++) {
                        groupsList.add(dates[i]);
                        childMap.put(i,new ArrayList<AlarmInfo>());
                    }

                    hasLoad = new boolean[dates.length];

                    if (mActivity != null){
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mSRL.setRefreshing(false);
                                mAdapter.notifyDataSetChanged();
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

    //加载图片
    private void loadChildsInfo(final int groupPosition){
        final String date = groupsList.get(groupPosition);

        if (hasLoad[groupPosition]){  //已经加载过了
            return;
        }

        new TcpTool(Data.SERVER_IP,Data.SERVER_PORT1).connect(Data.PHOTO_DATE + " " + Data.account + " " + Tools.pwdToMd5(Data.password) + " " + date, new RequestCallBack() {
            @Override
            public void onFinish(String response) {


                    String[] dates = response.split(" ");
                    List<AlarmInfo> list = childMap.get(groupPosition);
                    for (String time : dates) {
                        Socket socket = null;
                        try {
                            OutputStream os;
                            socket = new Socket(Data.SERVER_IP,
                                    Data.SERVER_PORT1);
                            os = socket.getOutputStream();
                            byte[] buffer = (Data.PHOTO + " " + Data.account
                                    + " " + Tools.pwdToMd5(Data.password) + " " + date + "/"+ time +  '\n').getBytes();
                            os.write(buffer);
                            os.flush();

                            Bitmap bmp = BitmapFactory.decodeStream(socket.getInputStream());

                            if (bmp == null){
                                continue;
                            }

                            list.add(new AlarmInfo(bmp,time.replace('_',':')));
                            if (mActivity != null){
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mAdapter.notifyDataSetChanged();
                                    }
                                });
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }finally {
                            try {
                                socket.close();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }

                    hasLoad[groupPosition] = true;
                }

            @Override
            public void onError() {
                showMsg("连接服务器失败，请检查网络连接~");
            }
        });

    }

    private void showMsg(final String msg){
        if (mActivity != null){
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(),msg,Toast.LENGTH_SHORT).show();
                    mSRL.setRefreshing(false);
                    mTextView.setText(msg);
                }
            });
        }
    }


}
