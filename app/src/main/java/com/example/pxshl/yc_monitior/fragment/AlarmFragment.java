package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
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
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 该碎片会展示报警的图片
 */

public class AlarmFragment extends Fragment {


    private Activity mActivity;
    private TextView mTextView;
    private SwipeRefreshLayout mSRL;
    private View mView;

    private AlarmELVAdapter mAdapter;    //适配器
    private List<String> groupsList = new LinkedList<>(); //一级列表
    private Map<Integer, List<AlarmInfo>> childMap = new HashMap<>(); //二级列表


    //通过mData判断子项信息是否加载完
    private List<List<String>> mData = new ArrayList<>();//代会再补充注释

    private List<Boolean> isNoData = new ArrayList<>(); //某一天下没有报警照片
    private AtomicBoolean[] isLoadings; //线程安全 可修改为退出应用也将其改为false
    private volatile boolean isLoadGroupMsg;
    private static BitmapFactory.Options m0ptions = new BitmapFactory.Options();
    private static LoadBitmap mLoad;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }



    @Override   //兼容低版本安卓系统
    public void onAttach(Activity activity) {
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
        m0ptions.inPreferredConfig = Bitmap.Config.RGB_565;  //压缩图片
    }



    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        if (mView == null) {
            mView = inflater.inflate(R.layout.fragment_alarm, null);
            init();
        }

        return mView;
    }

    private void init() {

        mLoad = new LoadBitmap(this);

        mTextView = (TextView) mView.findViewById(R.id.alarmFrag_empty_tv);
        mSRL = (SwipeRefreshLayout) mView.findViewById(R.id.alarm_frag_srl);
        final ExpandableListView listView = (ExpandableListView) mView.findViewById(R.id.frag_alarm_listView);
        mAdapter = new AlarmELVAdapter(getContext(),mLoad,mData,groupsList, childMap,isNoData);
        listView.setAdapter(mAdapter);
        listView.setEmptyView(mTextView);
        loadGroupsInfo();


        listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                loadChildsInfo(groupPosition);
            }
        });

        listView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                isLoadings[groupPosition].set(false);
            }
        });

        listView.setOnTouchListener(new View.OnTouchListener() {    //避免refreshLayout错误消费了ListView的滑动事件 当设置了EmptyView时会出现这问题
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {

                    if (listView.getCount() > 0) {
                        if (listView.getFirstVisiblePosition() == 0 && listView.getChildAt(0).getTop() >= listView.getListPaddingTop()) {
                            mSRL.setEnabled(true);
                        } else {
                            mSRL.setEnabled(false);
                        }
                    }

                }
                return false;
            }
        });


        mSRL.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                if (!isLoading() && isLoadGroupMsg) {

                    mData.clear();
                    groupsList.clear();         //刷新，先清除现有数据
                    childMap.clear();
                    System.gc();
                    //  mAdapter.notifyDataSetChanged();
                    loadGroupsInfo();

                } else {
                    mSRL.setRefreshing(false);
                }


            }
        });

    }

    private boolean isLoading() {

        if (isLoadings == null || 0 == isLoadings.length) {
            return false;
        }

        for (AtomicBoolean ab : isLoadings) {
            if (ab.get()) {
                return true;
            }
        }

        return false;
    }

    private void loadGroupsInfo() {

        isLoadGroupMsg = true;
        isNoData.clear();

        new TcpTool(Data.SERVER_IP, Data.SERVER_PORT1).connect(Data.ALARM + " " + Data.account + " " + Tools.pwdToMd5(Data.password), new RequestCallBack() {
            @Override
            public void onFinish(String response) {


                if (response.equals("")) {
                    showMsg("出错啦，请稍后再刷新试下～");
                } else if (response.contains("no open alarm")) {
                    showMsg("您未开启监控报警～");
                } else if (response.contains("offline")) {
                    showMsg("监控器不在线~");
                } else if (response.contains("no photo")) {
                    showMsg("恭喜您，没有异常的报警信息~");
                } else {

                    String[] dates = response.split(" ");


                    isLoadings = new AtomicBoolean[dates.length];

                    for (int i = 0; i < dates.length; i++) {
                        groupsList.add(dates[i]);
                        childMap.put(i, new ArrayList<AlarmInfo>());
                        isLoadings[i] = new AtomicBoolean(false);
                        mData.add(new ArrayList<String>());
                        isNoData.add(false);
                    }

                    isLoadGroupMsg = false;

                    if (mActivity != null) {
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
                isLoadGroupMsg = false;

            }
        });
    }

    //加载图片

    private void loadChildsInfo(final int groupPosition) {

        if (mData.get(groupPosition).size() != 0){  //说明已经加载过数据了
            return;
        }

        new TcpTool(Data.SERVER_IP, Data.SERVER_PORT1).connect(Data.PHOTO_DATE + " " + Data.account + " " + Tools.pwdToMd5(Data.password) + " " + groupsList.get(groupPosition), new RequestCallBack() {
            @Override
            public void onFinish(final String response) {

                if (response.equals("")){

                    isNoData.remove(groupPosition);
                    isNoData.add(groupPosition,true);
                    notifyUI();
                    return;
                }


                String[] datas = response.split(" ");
                for (String data : datas) {
                    mData.get(groupPosition).add(data);
                }
                notifyUI();

                //先加载10张照片
                String date = groupsList.get(groupPosition);
                List<String> data = new ArrayList<>();
                for (int i = 0; i < 10 && i < datas.length; i++) {
                    data.add(date + '/' + mData.get(groupPosition).get(i));
                }

                loadBitmaps(groupPosition,data);
            }

            @Override
            public void onError() {
                showMsg("连接服务器失败，请检查网络连接~");
            }
        });

    }

    private void showMsg(final String msg) {

        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    mSRL.setRefreshing(false);
                    mTextView.setText(msg);
                }
            });
        }
    }

    private void loadBitmaps(final int groupPosition,final List<String> data){

        if (isLoadings[groupPosition].get()){
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<AlarmInfo> list = childMap.get(groupPosition);

                isLoadings[groupPosition].set(true);

                for (String datum : data){
                    //当用户折叠所在标签时，isLoadings[groupPosition].get()为false，停止加载
                    if (!isLoadings[groupPosition].get()) {
                        break;
                    }

                    //当可用内存小于30M时，也不加载图片
                    if (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() < 1024 * 1024 * 30) {
                        break;
                    }

                    String msg = (Data.PHOTO + " " + Data.account
                            + " " + Tools.pwdToMd5(Data.password) + " " + datum + '\n');
                    Bitmap bmp = loadBitmap(msg);

                    if (bmp == null) {
                        break;
                    }

                    list.add(new AlarmInfo(bmp, datum));

                    if (mActivity != null) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }

                isLoadings[groupPosition].set(false);
            }
        }).start();


    }

    private Bitmap loadBitmap(String msg) {


        Bitmap bmp = null;

        try (Socket socket = new Socket(Data.SERVER_IP, Data.SERVER_PORT1)) {
            OutputStream os;
            socket.setSoTimeout(5000);
            os = socket.getOutputStream();
            os.write(msg.getBytes());
            os.flush();

            bmp = BitmapFactory.decodeStream(socket.getInputStream(), null, m0ptions);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return bmp;
    }

    public static class LoadBitmap{
        WeakReference<AlarmFragment> alarmFrag ;

        LoadBitmap(AlarmFragment fragment){
            alarmFrag = new WeakReference<AlarmFragment>(fragment);
        }

        public void load(int groupPosition,List<String> data){
            AlarmFragment fragment = alarmFrag.get();
            if (fragment != null){
                fragment.loadBitmaps(groupPosition,data);
            }
        }
    }

    private void notifyUI(){
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
