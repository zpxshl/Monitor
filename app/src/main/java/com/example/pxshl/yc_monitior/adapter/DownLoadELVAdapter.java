package com.example.pxshl.yc_monitior.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.example.pxshl.yc_monitior.BuildConfig;
import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.application.MyApplication;
import com.example.pxshl.yc_monitior.model.FileInfo;
import com.example.pxshl.yc_monitior.service.DownloadService;
import com.example.pxshl.yc_monitior.util.Data;

import java.io.File;
import java.util.List;
import java.util.Map;


/**
 * 下载界面对应的ListView的适配器
 */

public class DownLoadELVAdapter extends BaseExpandableListAdapter {

    Context mContext;
    //一级项目列表
    List<String> mGroupsList;
    //二级项目列表,integer对应一级项目的id
    Map<Integer, List<FileInfo>> mChildsMap;

    /**
     * 构造方法
     *
     * @param context
     * @param groupsList
     * @param childsMap
     */
    public DownLoadELVAdapter(Context context,
                              List<String> groupsList, Map<Integer, List<FileInfo>> childsMap) {
        this.mContext = context;
        this.mGroupsList = groupsList;
        this.mChildsMap = childsMap;
    }


    @Override
    public int getGroupCount() {
        return mGroupsList.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return mChildsMap.get(i).size();
    }

    @Override
    public Object getGroup(int i) {
        return mGroupsList.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return mChildsMap.get(i).get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }


    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        GroupsViewHolder groupsViewHolder = null;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.groups_download, null);
            groupsViewHolder = new GroupsViewHolder();
            groupsViewHolder.groupsTv = (TextView) view.findViewById(R.id.group_download_tv);
            view.setTag(groupsViewHolder);
        } else {
            groupsViewHolder = (GroupsViewHolder) view.getTag();
        }

        groupsViewHolder.groupsTv.setText(mGroupsList.get(i).toString());
        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        final ChildsViewHolder childsViewHolder;
        final FileInfo fileInfo = mChildsMap.get(i).get(i1);
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.childs_download, null);
            childsViewHolder = new ChildsViewHolder();
            childsViewHolder.childsTv = (TextView) view.findViewById(R.id.childs_tv);
            childsViewHolder.delBtn = (ImageButton) view.findViewById(R.id.del_btn);
            childsViewHolder.playBtn = (ImageButton)view.findViewById(R.id.play_btn);
            childsViewHolder.stopBtn = (ImageButton) view.findViewById(R.id.stop_btn);
            childsViewHolder.progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
            childsViewHolder.progressBar.setMax(100);
            view.setTag(childsViewHolder);
        } else {
            childsViewHolder = (ChildsViewHolder) view.getTag();
        }

        childsViewHolder.childsTv.setText((fileInfo.getFileName().split("/"))[1].split(".mp4")[0].replace('-',':')); //将文件名显示为对用户友好的格式
        if (fileInfo.isFinish()) {
            //如果下载完成并将progressbar设置为gone，显示可以删除的按钮，并提示用户下载完成
            childsViewHolder.progressBar.setVisibility(View.GONE);
            childsViewHolder.stopBtn.setVisibility(View.GONE);
            childsViewHolder.delBtn.setVisibility(View.VISIBLE);
            childsViewHolder.playBtn.setVisibility(View.VISIBLE);
        } else {
            if (fileInfo.getLength() != 0) {
                childsViewHolder.progressBar.setProgress((int) (fileInfo.getFinished() * 100 / fileInfo.getLength()));
                childsViewHolder.progressBar.setVisibility(View.VISIBLE);
                childsViewHolder.stopBtn.setVisibility(View.VISIBLE);
            } else {
                //长度为０，说明未下载，将pb和删除按钮隐藏起来
                childsViewHolder.progressBar.setVisibility(View.GONE);
                childsViewHolder.delBtn.setVisibility(View.GONE);
                childsViewHolder.playBtn.setVisibility(View.GONE);
                childsViewHolder.stopBtn.setVisibility(View.GONE);
            }
        }
        /**
         * 删除按钮的监听事件
         *   点击删除按钮之后，new一个对话框，提示用户是否删除文件，
         *   是的话，删除文件，并字体颜色设置回原来的颜色，将delbtn设置为gone
         */
        childsViewHolder.delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(mContext).setTitle("是否删除" + fileInfo.getFileName() + "?")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                File file = new File(Data.DL_VIDEO_PATH + fileInfo.getFileName().split("/")[0], fileInfo.getFileName().split("/")[1]);
                                if (file.exists()) {
                                    file.delete();
                                }
                                fileInfo.setFinish(false);
                                fileInfo.setDownloading(false);
                                fileInfo.setFinished(0);
                                fileInfo.setLength(0);

                                Toast.makeText(mContext, "本机视频：" + fileInfo.getFileName() + "删除成功", Toast.LENGTH_SHORT).show();
                                childsViewHolder.delBtn.setVisibility(View.GONE);
                                childsViewHolder.playBtn.setVisibility(View.GONE);
                            }
                        }).setNegativeButton("取消", null).setCancelable(false).show();
                Log.d("TAG", "delbtn clicked");
            }
        });

        childsViewHolder.playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //播放视频，启动系统的播放器进行播放
                Intent mp4Intent = new Intent("android.intent.action.VIEW");
                Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //安卓N或以上
                    mp4Intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    uri = FileProvider.getUriForFile(MyApplication.getContext(), BuildConfig.APPLICATION_ID + ".fileProvider", new File(Data.DL_VIDEO_PATH + fileInfo.getFileName().split("/")[0] + File.separator + fileInfo.getFileName().split("/")[1]));

                } else {
                    mp4Intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    uri = Uri.fromFile(new File(Data.DL_VIDEO_PATH + fileInfo.getFileName().split("/")[0] + File.separator + fileInfo.getFileName().split("/")[1]));
                }


                mp4Intent.putExtra("oneshot", 0);
                mp4Intent.putExtra("configchange", 0);
                mp4Intent.setDataAndType(uri, "video/mp4");
                mContext.startActivity(mp4Intent);
            }
        });

        childsViewHolder.stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DownloadService.class);
                intent.setAction(DownloadService.DELETE);
                intent.putExtra("fileInfo", fileInfo);
                mContext.startService(intent);
                Toast.makeText(MyApplication.getContext(), fileInfo.getFileName() + "停止下载", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }


    /**
     * 更新进度条进度
     */
    public void update(FileInfo fileInfo) {

        try {    //try语句 避免当atcivity被回收 or 刷新列表后，启动时崩溃（mChildsMap.get(fileInfo.getI()).get(fileInfo.getI1())返回null）
            FileInfo mFileInfo = mChildsMap.get(fileInfo.getI()).get(fileInfo.getI1());
            mFileInfo.setFinished(fileInfo.getFinished());
            mFileInfo.setLength(fileInfo.getLength());
            mFileInfo.setFinish(fileInfo.isFinish());
            mFileInfo.setDownloading(fileInfo.isDownloading());
            notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 一级标题的ViewHolder
     */
    static class GroupsViewHolder {
        TextView groupsTv;
    }

    /**
     * 二级标题的ViewHolder
     */
    static class ChildsViewHolder {

        TextView childsTv;
        ProgressBar progressBar;
        ImageButton delBtn;
        ImageButton playBtn;
        ImageButton stopBtn;

    }

}
