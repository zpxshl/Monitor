package com.example.pxshl.yc_monitior.adapter;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pxshl.yc_monitior.R;
import com.example.pxshl.yc_monitior.application.MyApplication;
import com.example.pxshl.yc_monitior.fragment.AlarmFragment;
import com.example.pxshl.yc_monitior.model.AlarmInfo;
import com.example.pxshl.yc_monitior.util.Data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 对应报警界面的listView的塞佩琦
 */

public class AlarmELVAdapter extends BaseExpandableListAdapter {


    private Context mContext;
    //一级项目列表
    private List<String> mGroupsList;
    //二级项目列表,integer对应一级项目的id
    private Map<Integer, List<AlarmInfo>> mChildsMap;
    private List<List<String>> mData;//储存所有数据（的名字），出于节约流量考虑，并不会一开始就加载所有数据
    private AlarmFragment.LoadBitmap mLoad; //调用fragment加载图片
    private List<Boolean> mIsNoData;


    public AlarmELVAdapter(Context context, AlarmFragment.LoadBitmap load, List<List<String>> data, List<String> groupsList, Map<Integer, List<AlarmInfo>> childsMap,List<Boolean> isNoData) {
        mContext = context;
        mLoad = load;
        mData = data;
        mGroupsList = groupsList;
        mChildsMap = childsMap;
        mIsNoData = isNoData;
    }

    @Override
    public int getGroupCount() {
        return mGroupsList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildsMap.get(groupPosition).size() + 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        GroupViewHolder viewHolder = null;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.groups_alarm, null);
            viewHolder = new GroupViewHolder();
            viewHolder.groupTv = (TextView) convertView.findViewById(R.id.group_alarm_tv);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (GroupViewHolder) convertView.getTag();
        }

        viewHolder.groupTv.setText(mGroupsList.get(groupPosition));

        return convertView;

    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildViewHolder viewHolder = null;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.childs_alarm, null);
            viewHolder = new ChildViewHolder();
            viewHolder.childIv = (ImageView) convertView.findViewById(R.id.child_alarm_iv);
            viewHolder.childTv = (TextView) convertView.findViewById(R.id.child_alarm_tv);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ChildViewHolder) convertView.getTag();
        }


        if (childPosition == mChildsMap.get(groupPosition).size()) {

            //设置最后信息，点击继续加载 or 到底了
            viewHolder.childIv.setVisibility(View.GONE);
            viewHolder.childTv.setVisibility(View.VISIBLE);

            if (mIsNoData.get(groupPosition)){
                viewHolder.childTv.setText("恭喜您，该日期下没有报警照片");
                return convertView;
            }


            if (mData.get(groupPosition).size() == mChildsMap.get(groupPosition).size()){
                viewHolder.childTv.setText("该日期照片已经加载完毕啦");
            }else {
                viewHolder.childTv.setText("点击加载更多照片");
            }



            viewHolder.childTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    if (mChildsMap.get(groupPosition).size() == mData.get(groupPosition).size()){
                        Toast.makeText(MyApplication.getContext(),"已经加载该日期下的所有照片啦",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> data = new ArrayList<>();
                    List<String> allData = mData.get(groupPosition);

                    int index = mChildsMap.get(groupPosition).size();
                    String date = mGroupsList.get(groupPosition);

                    for (int i = 0; i < 10 && i + index < allData.size(); i++) {
                        Log.e("index",index + " " + i);
                        data.add(date + '/' + allData.get(index + i));
                    }

                    if (mLoad != null) {
                        mLoad.load(groupPosition, data);
                    }
                }

            });
            //点击加载
        } else {

            viewHolder.childIv.setVisibility(View.VISIBLE);
            viewHolder.childTv.setVisibility(View.GONE);
            viewHolder.childIv.setImageBitmap(mChildsMap.get(groupPosition).get(childPosition).getBitmap());

            viewHolder.childIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageView iv = new ImageView(mContext);
                    iv.setImageBitmap(mChildsMap.get(groupPosition).get(childPosition).getBitmap());
                    showBigImage(iv);
                }
            });

            viewHolder.childIv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlarmInfo info = mChildsMap.get(groupPosition).get(childPosition);
                    save_photo(info.getBitmap(), Data.DL_PHOTO_PATH, info.getTime().replace('/','_'));
                    Toast.makeText(MyApplication.getContext(), "图片已保存到内置储存/monitor/photo", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }





        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }


    /**
     * 一级标题的ViewHolder
     */
    static class GroupViewHolder {
        TextView groupTv;
    }

    static class ChildViewHolder {
         ImageView childIv;
         TextView childTv;
    }


    /**
     * @param bitmap   图片对应的bitmap
     * @param dir      保存的位置
     * @param fileName 保存的文件名
     */
    private void save_photo(Bitmap bitmap, String dir, String fileName) {
        File d = new File(dir);
        if (!d.exists()) {
            d.mkdirs();
        }

        try (OutputStream out = new FileOutputStream(new File(dir + File.separator + fileName));){
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showBigImage(ImageView view){

        if (view == null){
            return;
        }

        final Dialog dialog = new Dialog(mContext,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(view);
        dialog.getWindow().setWindowAnimations(R.style.CustomDialog);
        dialog.show();

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
            }
        });
    }



    /*

                    AlarmInfo info = mChildsMap.get(groupPosition).get(childPosition);
                    save_photo(info.getBitmap(), Data.DL_PHOTO_PATH, info.getTime().replace('/','_'));
                    Toast.makeText(MyApplication.getContext(), "图片已保存到内置储存/monitor/photo", Toast.LENGTH_SHORT).show();
     */
}
