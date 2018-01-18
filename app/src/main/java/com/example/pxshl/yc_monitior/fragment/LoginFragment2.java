package com.example.pxshl.yc_monitior.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.text.StaticLayout;
import android.util.Log;
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
import com.example.pxshl.yc_monitior.application.MyApplication;
import com.example.pxshl.yc_monitior.inyerface.RequestCallBack;
import com.example.pxshl.yc_monitior.net.tcp.TcpTool;
import com.example.pxshl.yc_monitior.util.Data;
import com.example.pxshl.yc_monitior.util.Tools;

import static android.content.Context.MODE_PRIVATE;


/**
 * 登陆界面2
 */

public class LoginFragment2 extends Fragment {

    private Activity mActivity;
    private String phone_number;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login2, null);

        ((TextView) (view.findViewById(R.id.account_text))).setText(Data.account + " 欢迎您");
        //登出按钮 监听
        ((Button) (view.findViewById(R.id.login_off_btn))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login_off();
            }
        });

        //修改密码按钮 监听
        ((Button) (view.findViewById(R.id.change_pwd_btn))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                change_pwd();
            }
        });

        final Button bind_phone = (Button) view.findViewById(R.id.bind_phone);
        //如果已绑定手机号码
        if (Data.phone_num != null && !Data.phone_num.equals("")) {
            bind_phone.setText("已绑定手机： " + Data.phone_num);
        }

        bind_phone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bind_phone();
                }
            });



        return view;
    }

    private void showMsg(final String msg) {
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void login_off() {
        SharedPreferences preferences = getActivity().getSharedPreferences("properties", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();

        Data.isLogin = false;
        Data.account = "";
        Data.password = "";
        Data.phone_num = "";

        Toast.makeText(MyApplication.getContext(), "登出成功", Toast.LENGTH_SHORT).show();
        ((LoginActivity) getActivity()).changeFragment();
    }

    private void change_pwd() {
        View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_chang_pwd, null);
        final EditText old_pwd_et = (EditText) layout.findViewById(R.id.password_old_et);
        final EditText new_pwd1_et = (EditText) layout.findViewById(R.id.password_new1_et);
        final EditText new_pwd2_et = (EditText) layout.findViewById(R.id.password_new2_et);
        old_pwd_et.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        new_pwd1_et.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        new_pwd2_et.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);

        final AlertDialog dialog_change_pwd = new AlertDialog.Builder(getActivity()).setTitle("修改密码").setView(layout)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //不操作 由下面代码监听点击事件（为了拦截dialog本身的事件）
                        //效果是，点确定不会关闭对话框
                    }
                })
                .setNegativeButton("取消", null).setCancelable(false).create();

        dialog_change_pwd.show();

        if (dialog_change_pwd.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog_change_pwd.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String old_pwd = old_pwd_et.getText().toString();
                    final String new_pwd1 = new_pwd1_et.getText().toString();
                    String new_pwd2 = new_pwd2_et.getText().toString();

                    if (old_pwd.equals("") || new_pwd1.equals("") || new_pwd2.equals("")) {
                        showMsg("请输入三行完整的密码");
                        return;
                    } else {
                        if (old_pwd.equals(Data.password)) {
                            if (new_pwd1.equals(new_pwd2)) {
                                //向服务器发送修改密码命令

                                new TcpTool(Data.SERVER_IP, Data.SERVER_PORT1).connect(Data.CHANGE_PASSWORD + " " + Data.account + " " + Tools.pwdToMd5(Data.password) + " " + new_pwd1, new RequestCallBack() {
                                    @Override
                                    public void onFinish(String response) {
                                        dialog_change_pwd.cancel();
                                        if (response.equals("ok")) {
                                            showMsg("修改密码成功,请重新登陆");

                                            //清除原本储存的数据
                                            SharedPreferences preferences = MyApplication.getContext().getSharedPreferences("properties", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = preferences.edit();
                                            editor.clear();
                                            editor.commit();

                                            Data.account = "";
                                            Data.password = "";
                                            Data.isLogin = false;
                                            Data.phone_num = "";

                                            if (mActivity != null) {
                                                mActivity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ((LoginActivity) mActivity).changeFragment();
                                                    }
                                                });

                                            }

                                        } else {
                                            showMsg("很抱歉，服务器故障，修改密码失败，请稍后重试");
                                        }
                                    }

                                    @Override
                                    public void onError() {
                                        dialog_change_pwd.cancel();
                                        showMsg("很抱歉，修改密码错误，请稍后重试");
                                    }
                                });

                            } else {
                                showMsg("两次输入的新密码不一致，请核对后重新输入");
                            }
                        } else {
                            showMsg("原密码输入错误，请重新输入");
                        }
                    }
                }
            });
        }
    }


    private void bind_phone() {
        View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_bind_phone, null);
        final EditText captcha = (EditText) layout.findViewById(R.id.et_captcha);
        final EditText phone = (EditText) layout.findViewById(R.id.et_phone);
        final Button send = (Button) layout.findViewById(R.id.btn_send_CAPTCHA);

        //倒计时控件
        final CountDownTimer timer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                send.setText("重新发送： " + millisUntilFinished / 1000 + "秒");
            }

            @Override
            public void onFinish() {
                send.setText("发送验证码");
                send.setEnabled(true);
            }
        };

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phone_number = phone.getText().toString();
                if (phone_number.length() != 11) {
                    Toast.makeText(MyApplication.getContext(), "请输入正确的手机号码", Toast.LENGTH_SHORT).show();
                } else {

                    new TcpTool(Data.SERVER_IP, Data.SERVER_PORT2).connect(Data.SEND_PHONE + " " + Data.account + " " + Tools.pwdToMd5(Data.password) + " " + phone_number, null);
                    send.setEnabled(false);
                    timer.start();
                }
            }
        });

        final AlertDialog dialog_bind = new AlertDialog.Builder(getActivity()).setTitle("绑定手机号码").setView(layout)
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

        dialog_bind.show();

        if (dialog_bind.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog_bind.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String captcha_number = captcha.getText().toString();
                    if (captcha.equals("")) {
                        Toast.makeText(MyApplication.getContext(), "请输入验证码", Toast.LENGTH_SHORT).show();
                    } else {
                        new TcpTool(Data.SERVER_IP, Data.SERVER_PORT2).connect(Data.CAPTCHA + " " + Data.account + " " + Tools.pwdToMd5(Data.password) + " " + phone_number + " " + captcha_number, new RequestCallBack() {
                            @Override
                            public void onFinish(String response) {
                                if (response.equals("")) {
                                    showMsg("服务器异常，请稍后在尝试");
                                } else if (response.contains("true")) {
                                    showMsg("验证成功");

                                    timer.cancel();
                                    Data.phone_num = phone_number;
                                    //储存手机号码到本地
                                    SharedPreferences preferences = MyApplication.getContext().getSharedPreferences("properties", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString("phone_num", phone_number);
                                    editor.commit();

                                    if (mActivity != null) {
                                        mActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                dialog_bind.cancel();
                                            }
                                        });
                                    }

                                } else {
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

}
