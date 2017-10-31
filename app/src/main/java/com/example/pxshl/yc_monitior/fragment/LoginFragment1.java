package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;


import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.activity.MainActivity;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import javax.net.ssl.SSLEngine;

import static android.content.Context.MODE_PRIVATE;

/**
 * 登陆界面
 */

public class LoginFragment1 extends Fragment{


    private String account; //帐号
    private String password; //密码
    private ProgressDialog pd;
    private View mView;
    private Activity mActivity;


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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        if (mView == null){
            mView = inflater.inflate(R.layout.fragment_login1,null);

            final EditText accountEt = (EditText) mView.findViewById(R.id.account_et);
            final EditText passwordEt = (EditText) mView.findViewById(R.id.password_et);
            final CheckBox rembPass = (CheckBox) mView.findViewById(R.id.remb_pass);

            final CheckBox showPass = (CheckBox) mView.findViewById(R.id.show_pass);
            showPass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (showPass.isChecked()) {
                        //设置为可见
                        passwordEt.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    } else {
                        //设置为不可见
                        passwordEt.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD |InputType.TYPE_CLASS_TEXT);
                    }
                }
            });



            Button loginBtn = (Button) mView.findViewById(R.id.login_btn);
            loginBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String s= accountEt.getText().toString();
                    if (!accountEt.getText().toString().equals("") && !passwordEt.getText().toString().equals("")) {
                        account = accountEt.getText().toString();
                        password = passwordEt.getText().toString();

                        pd = new ProgressDialog(mActivity);
                        pd.setTitle("验证用户");
                        pd.setMessage("正在验证帐号密码，请稍后......");
                        pd.show();

                        new TcpTool(Data.SERVER_IP,Data.SERVER_PORT1).connect(Data.LOGIN + " " +  account + " " + Tools.pwdToMd5(password),new RequestCallBack() {

                            String msg = "";

                            @Override
                            public void onFinish(String response) {
                                if (pd != null){
                                    pd.cancel();
                                }

                                if (response.equals("")){
                                    showMsg("服务器异常，请稍后重试");
                                } else if (response.contains("true")) {

                                    Data.isLogin = true;
                                    Data.account = account;
                                    Data.password = password;


                                    if (rembPass.isChecked()) {
                                        //储存帐号密码
                                        SharedPreferences preferences = getContext().getSharedPreferences("properties", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString("account", account);
                                        editor.putString("password", password);
                                        editor.putBoolean("isLogin", true);
                                        editor.commit();
                                    }

                                    showMsg(Data.account + " 欢迎您");

                                    Intent intent = new Intent(mActivity, MainActivity.class);
                                    startActivity(intent);
                                    mActivity.finish();
                                } else {
                                    showMsg("帐号或密码错误，请重新输入");
                                }
                            }
                            @Override
                            public void onError() {
                                showMsg("联网验证失败，请稍后重试");
                            }
                        });

                    }else {
                        Toast.makeText(getContext(),"请输入完整的帐号和密码",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }


        ((Button)(mView.findViewById(R.id.btn_forget_pwd))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_forget_pwd,null);
                final EditText account = (EditText) layout.findViewById(R.id.et_account);
                final EditText new1_pwd = (EditText) layout.findViewById(R.id.et_new1_pwd);
                final EditText new2_pwd = (EditText) layout.findViewById(R.id.et_new2_pwd);
                final EditText phone = (EditText) layout.findViewById(R.id.et_phone);
                final EditText captcha = (EditText) layout.findViewById(R.id.et_captcha);
                final Button send = (Button) layout.findViewById(R.id.btn_send_CAPTCHA);


                //倒计时控件
                final CountDownTimer timer =  new CountDownTimer(60000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        send.setText("重新发送： " + millisUntilFinished/1000 + "秒");
                    }

                    @Override
                    public void onFinish() {
                        send.setText("发送验证码");
                        send.setFocusable(true);
                    }
                };


                send.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (account.getText().toString().equals("")){
                            Toast.makeText(getContext(),"请输入帐号",Toast.LENGTH_SHORT).show();
                            return;
                        }

                       if (!new1_pwd.getText().toString().equals(new2_pwd.getText().toString())){
                           Toast.makeText(getContext(),"新密码不一致，请重新输入",Toast.LENGTH_SHORT).show();
                           return;
                       }

                       if (phone.getText().toString().length() != 11){
                           Toast.makeText(getContext(),"请输入正确的手机号码",Toast.LENGTH_SHORT).show();
                           return;
                       }

                        new TcpTool(Data.SERVER_IP,Data.SERVER_PORT2).connect(Data.SEND_PHONE + " " + account.getText().toString() + " " + phone.getText().toString(),null);
                        send.setFocusable(false);
                        timer.start();

                    }
                });

                final AlertDialog dialog_forget = new AlertDialog.Builder(getActivity()).setTitle("找回密码").setView(layout)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //不操作 由下面代码监听点击事件（为了拦截dialog本身的事件）
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                timer.cancel();
                            }
                        }).setCancelable(false).create();

                dialog_forget.show();

                if (dialog_forget.getButton(AlertDialog.BUTTON_POSITIVE) != null){
                    dialog_forget.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String captcha_number = captcha.getText().toString();
                            if (captcha.equals("")){
                                Toast.makeText(getContext(),"请输入验证码",Toast.LENGTH_SHORT).show();
                            }else {
                                new TcpTool(Data.SERVER_IP,Data.SERVER_PORT2).connect(Data.CHANGE_PWD_BY_CAPTCHA + " " + account.getText().toString() + " " + new1_pwd.getText().toString() +  " " + captcha_number, new RequestCallBack() {
                                    @Override
                                    public void onFinish(String response) {
                                        if (response.equals("")){
                                            showMsg("服务器异常，请稍后在尝试");
                                        }else if (response.contains("true")){
                                            showMsg("验证成功");

                                            timer.cancel();
                                            if (mActivity != null){
                                                mActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        dialog_forget.cancel();
                                                    }
                                                });
                                            }

                                        }else {
                                            showMsg("验证码错误，请输入正确的验证码");
                                        }



                                    }

                                    @Override
                                    public void onError() {
                                        showMsg("网络异常，请稍后在试");
                                    }
                                });
                            }
                        }
                    });
                }

            }
        });


        return mView;
    }

    private void showMsg(final String msg) {
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    if (pd != null) {
                        pd.cancel();
                    }
                }
            });

        }
    }

}
