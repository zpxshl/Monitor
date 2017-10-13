package com.example.pxshl.yc_monitior.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.model.FileInfo;
import com.example.pxshl.yc_monitior.service.DownloadService;
import com.example.pxshl.yc_monitior.util.Data;

import java.io.File;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 *ExpandableListView的适配器
 */

public class DownLoadELVAdapter extends BaseExpandableListAdapter {

    Context mContext;
    //一级项目列表
    List<String> mGroupsList;
    //二级项目列表,integer对应一级项目的id
    Map<Integer, List<FileInfo>> mChildsMap;

    /**
     * 构造方法
     * @param context
     * @param groupsList
     * @param childsMap
     */
    public DownLoadELVAdapter (Context context,
                                      List<String> groupsList, Map<Integer, List<FileInfo>> childsMap) {
        this.mContext= context;
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
            groupsViewHolder = new GroupsViewHolder(view);
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
            childsViewHolder = new ChildsViewHolder(view);
            childsViewHolder.progressBar.setMax(100);
            view.setTag(childsViewHolder);
        } else {
            childsViewHolder = (ChildsViewHolder) view.getTag();
        }

        childsViewHolder.childsTv.setText((fileInfo.getFileName().split("/"))[1].split(".mp3")[0]); //将文件名显示为对用户友好的格式
        if (fileInfo.isFinish()) {
            //如果下载完成，则将子项的背景颜色设置为蓝色，并将progressbar设置为gone，显示可以删除的按钮，并提示用户下载完成
            //该代码存在极其诡异的BUG。。。找不到原因。。。
  //          childsViewHolder.childsTv.setTextColor(mContext.getResources().getColor(R.color.colorPrimaryDark));
            childsViewHolder.progressBar.setVisibility(View.GONE);
            childsViewHolder.delBtn.setVisibility(View.VISIBLE);
        } else {
            if (fileInfo.getLength() != 0) {
                childsViewHolder.progressBar.setProgress((int)(fileInfo.getFinished()*100/fileInfo.getLength()));
                childsViewHolder.progressBar.setVisibility(View.VISIBLE);
            } else {
                //长度为０，说明未下载，将pb和删除按钮隐藏起来
                childsViewHolder.progressBar.setVisibility(View.GONE);
                childsViewHolder.delBtn.setVisibility(View.GONE);
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
              new AlertDialog.Builder(mContext).setTitle("是否删除" + fileInfo.getFileName()+"?")
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

                        Toast.makeText(mContext, fileInfo.getFileName()+"删除成功", Toast.LENGTH_SHORT).show();
                        childsViewHolder.delBtn.setVisibility(View.GONE);
                    }
                }).setNegativeButton("取消",null).setCancelable(false).show();
                Log.d("TAG", "delbtn clicked");
            }
        });
        return view;
    }


    /**
     * 更新进度条进度
     */
    public void update(FileInfo fileInfo) {

        try {    //try语句 避免当atcivity被回收or 刷新列表后，启动时崩溃（mChildsMap.get(fileInfo.getI()).get(fileInfo.getI1())返回null）
            FileInfo mFileInfo =  mChildsMap.get(fileInfo.getI()).get(fileInfo.getI1());
            mFileInfo.setFinished(fileInfo.getFinished());
            mFileInfo.setLength(fileInfo.getLength());
            mFileInfo.setFinish(fileInfo.isFinish());
            mFileInfo.setDownloading(fileInfo.isDownloading());
            notifyDataSetChanged();
        }catch (Exception e){
            e.printStackTrace();
        }



    }


    /**
     * 一级标题的ViewHolder
     */
    static class GroupsViewHolder {
        @BindView(R.id.group_download_tv)
        TextView groupsTv;

        public GroupsViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
    /**
     * 二级标题的ViewHolder
     */
   static class ChildsViewHolder {
        @BindView(R.id.childs_tv)
        TextView childsTv;
        @BindView(R.id.progress_bar)
        ProgressBar progressBar;
        @BindView(R.id.del_btn)
        ImageButton delBtn;

        public ChildsViewHolder(View view) {
            ButterKnife.bind(this, view);
        }

    }

}
