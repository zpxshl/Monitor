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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.pxshl.yc_monitior.R;
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

    private SimpleAdapter mSimpleAdapter;
    private List<Map<String,Object>> mListItems = new ArrayList<>();
    private Activity mActivity;
    private TextView mTextView;
    private View mView;

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
        ListView listView = (ListView) mView.findViewById(R.id.frag_alarm_listView);
        mSimpleAdapter = new SimpleAdapter(mActivity,mListItems,R.layout.alarm_list_item,new String[] {"data","photo"},new int[] {R.id.alarm_date,R.id.alarm_photo});
        mSimpleAdapter.setViewBinder(new SimpleAdapter.ViewBinder() { //SimpleAdapter默认是无法加载从网络获取的bitmap对象，需要此特殊操作
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if((view instanceof ImageView) && (data instanceof Bitmap)) {
                    ImageView imageView = (ImageView) view;
                    imageView.setImageBitmap((Bitmap) data);
                    return true;
                }
                return false;
            }
        });
        listView.setAdapter(mSimpleAdapter);

        TcpTool.connect(Data.ALARM + " " + Data.account + " " + Tools.pwdToMd5(Data.password), new RequestCallBack() {
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

                String[] photoNames = response.split(" ");
                for (String photoName : photoNames) {//加载图片

                    Socket socket = null;
                    OutputStream os;

                    try {
                        socket = new Socket(Data.SERVER_IP, Data.SERVER_PORT);   //此时已经在子线程中，可直接操作网络
                        os = socket.getOutputStream();
                        byte[] buffer = (Data.PHOTO + " " + Data.account + " " + Tools.pwdToMd5(Data.password) + photoName +  '\n').getBytes();
                        os.write(buffer);
                        os.flush();

                        Bitmap bmp = BitmapFactory.decodeStream(socket.getInputStream());

                        if (bmp == null){
                            continue;
                        }

                        Map<String,Object> listItem = new HashMap<>();
                        listItem.put("data",photoName);
                        listItem.put("photo",bmp);
                        mListItems.add(listItem);

                        if (mActivity != null){
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSimpleAdapter.notifyDataSetChanged();
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
            }

            @Override
            public void onError() {

            }
        },true);

    }


}
