package com.example.pxshl.yc_monitior.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.pxshl.yc_monitior.R;

import java.util.List;
import java.util.Map;

/**
 * Created by pxshl on 17-10-2.
 */

public class AlarmELVAdapter extends BaseExpandableListAdapter{


    Context mContext;
    //一级项目列表
    List<String> mGroupsList;
    //二级项目列表,integer对应一级项目的id
    Map<Integer, List<Bitmap>> mChildsMap;

    public AlarmELVAdapter(Context context, List<String> groupsList, Map<Integer, List<Bitmap>> childsMap) {
        mContext = context;
        mGroupsList = groupsList;
        mChildsMap = childsMap;
    }

    @Override
    public int getGroupCount() {
        return mGroupsList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildsMap.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mChildsMap.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mChildsMap.get(groupPosition).get(childPosition);
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

        if (convertView  == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.groups_alarm,null);
            viewHolder = new GroupViewHolder();
            viewHolder.groupTv = (TextView) convertView.findViewById(R.id.group_alarm_tv);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (GroupViewHolder) convertView.getTag();
        }

        viewHolder.groupTv.setText(mGroupsList.get(groupPosition));

        return convertView;

    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildViewHolder viewHolder = null;

        if (convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.childs_alarm,null);
            viewHolder = new ChildViewHolder();
            viewHolder.childIv = (ImageView) convertView.findViewById(R.id.child_alarm_iv);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ChildViewHolder) convertView.getTag();
        }

        viewHolder.childIv.setImageBitmap(mChildsMap.get(groupPosition).get(childPosition));

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
        public TextView groupTv;
    }

    static class ChildViewHolder{
        public ImageView childIv;
    }
}
