package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.adapter.AlarmELVAdapter;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;
import android.support.v4.app.Fragment;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pxshl on 2017/9/12.
 */

public class AlarmFragment extends Fragment {


    private Activity mActivity;
    private TextView mTextView;
    private View mView;

    private AlarmELVAdapter mAdapter;    //适配器
    private List<String> groupsList = new ArrayList<>(); //一级列表
    private Map<Integer, List<Bitmap>> childMap = new HashMap<>(); //二级列表

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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

        mTextView = (TextView) mView.findViewById(R.id.frag_alarm_text);
        ExpandableListView listView = (ExpandableListView) mView.findViewById(R.id.frag_alarm_listView);
        mAdapter = new AlarmELVAdapter(getContext(),groupsList,childMap);
        listView.setAdapter(mAdapter);
        loadGroupsInfo();

        listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                loadChildsInfo(groupPosition);
            }
        });

    }

    private void loadGroupsInfo() {

        new TcpTool().connect(Data.ALARM + " " + Data.account + " " + Tools.pwdToMd5(Data.password), new RequestCallBack() {
            @Override
            public void onFinish(String response) {

                if (response.equals("no")) {
                    if (mActivity != null) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextView.setText("当前无警报图片");
                            }
                        });
                    }

                    return;
                }

                String[] dates = response.split(" ");

                for (int i = 0 ;i < dates.length ;i++) {
                    groupsList.add(dates[i]);
                    childMap.put(i,new ArrayList<Bitmap>());
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

            @Override
            public void onError() {

            }
        });
    }

    private void loadChildsInfo(final int groupPosition){
        final String date = groupsList.get(groupPosition);

        new TcpTool().connect(Data.PHOTO_DATE + " " + Data.account + " " + Tools.pwdToMd5(Data.password), new RequestCallBack() {
            @Override
            public void onFinish(String response) {
                if (response.contains("no")){

                    return;
                }

                String[] dates = response.split(" ");
                List<Bitmap> list = childMap.get(groupPosition);
                for (String time : dates) {

                    Socket socket = null;

                    try {


                        OutputStream os;
                        socket = new Socket(Data.SERVER_IP,
                                Data.SERVER_PORT);
                        os = socket.getOutputStream();
                        byte[] buffer = (Data.PHOTO + " " + Data.account
                                + " " + Tools.pwdToMd5(Data.password) + date + " "+ time +  '\n').getBytes();
                        os.write(buffer);
                        os.flush();

                        Bitmap bmp = BitmapFactory.decodeStream(socket.getInputStream());

                        if (bmp == null){
                            continue;
                        }

                        list.add(bmp);
                        mAdapter.notifyDataSetChanged();

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

            }

            @Override
            public void onError() {

            }
        });


    }

}
