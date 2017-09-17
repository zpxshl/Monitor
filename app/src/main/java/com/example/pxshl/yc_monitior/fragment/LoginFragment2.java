package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.activity.LoginActivity;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import butterknife.ButterKnife;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by pxshl on 2017/7/28.
 */

public class LoginFragment2 extends Fragment {

    private Activity mActivity;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login2,null);
        ButterKnife.bind(this,view);

        ( (TextView)(view.findViewById(R.id.account_text))).setText(Data.account + " 欢迎您");
        //登出按钮 监听
        ( (Button)(view.findViewById(R.id.login_off_btn))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getActivity().getSharedPreferences("properties", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.commit();

                Data.isLogin = false;
                Data.account = "";
                Data.password = "";


                Toast.makeText(getContext(),"登出成功",Toast.LENGTH_SHORT).show();
                ((LoginActivity)getActivity()).changeFragment();
            }
        });

        //修改密码按钮 监听
        ( (Button)(view.findViewById(R.id.change_pwd_btn))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_chang_pwd,null);
                final EditText old_pwd_et = (EditText) layout.findViewById(R.id.password_old_et);
                final EditText new_pwd1_et = (EditText) layout.findViewById(R.id.password_new1_et);
                final EditText new_pwd2_et = (EditText) layout.findViewById(R.id.password_new2_et);
                old_pwd_et.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
                new_pwd1_et.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
                new_pwd2_et.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);

                final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle("修改密码").setView(layout)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                              //不操作 由下面代码监听点击事件（为了拦截dialog本身的事件）
                            }
                        })
                        .setNegativeButton("取消", null).setCancelable(false).create();

                dialog.show();

                if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null){
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String old_pwd = old_pwd_et.getText().toString();
                            final String new_pwd1 = new_pwd1_et.getText().toString();
                            String new_pwd2 = new_pwd2_et.getText().toString();

                            if (old_pwd.equals("") || new_pwd1.equals("") || new_pwd2.equals("")){
                                runOnUIThreadToast("请输入三行完整的密码");
                                return;
                            }else {
                                if (old_pwd.equals(Data.password)){
                                    if (new_pwd1.equals(new_pwd2)){
                                        //向服务器发送修改密码命令

                                        TcpTool.connect(Data.CHANGE_PASSWORD + " " + Data.account + " " + Tools.pwdToMd5(Data.password) + " " + new_pwd1,new RequestCallBack() {
                                            @Override
                                            public void onFinish(String response) {
                                                dialog.cancel();
                                                if (response.equals("ok")){
                                                    runOnUIThreadToast("修改密码成功,请重新登陆");

                                                    //清除原本储存的数据
                                                    SharedPreferences preferences = getContext().getSharedPreferences("properties", MODE_PRIVATE);
                                                    SharedPreferences.Editor editor = preferences.edit();
                                                    editor.clear();
                                                    editor.commit();

                                                    Data.account = "";
                                                    Data.password = "";
                                                    Data.isLogin = false;

                                                    if (mActivity!= null){
                                                        mActivity.runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                ((LoginActivity)mActivity).changeFragment();
                                                            }
                                                        });

                                                    }

                                                }else {
                                                    runOnUIThreadToast("很抱歉，服务器故障，修改密码失败，请稍后重试");
                                                }
                                            }

                                            @Override
                                            public void onError() {
                                                dialog.cancel();
                                                runOnUIThreadToast("很抱歉，修改密码错误，请稍后重试");
                                            }
                                        },true);

                                    }else {
                                        runOnUIThreadToast("两次输入的新密码不一致，请核对后重新输入");
                                    }
                                }else {
                                    runOnUIThreadToast("原密码输入错误，请重新输入");
                                }
                            }
                        }
                    });
                }
            }
        });

        return view;
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
