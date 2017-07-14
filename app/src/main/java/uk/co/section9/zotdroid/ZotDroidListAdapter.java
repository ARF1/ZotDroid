package uk.co.section9.zotdroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by oni on 14/07/2017.
 * http://android-coding.blogspot.co.uk/2014/02/expandablelistview-example.html
 */

public class ZotDroidListAdapter extends BaseExpandableListAdapter {

    Context _context;
    private ArrayList<String> _list_group;
    private HashMap<String, ArrayList<String>> _list_child;

    public ZotDroidListAdapter(Context context, ArrayList<String> groups, HashMap<String, ArrayList<String>> children ) {
        super();
        _list_group = groups;
        _list_child = children;
        _context = context;
    }

    @Override
    public int getGroupCount() {
        return _list_group.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return _list_child.get(_list_group.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return _list_group.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return _list_child.get(_list_group.get(groupPosition)).get(childPosition);
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
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater)_context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.main_list_group, null);
        }

        String textGroup = (String)getGroup(groupPosition);

        TextView textViewGroup = (TextView)convertView.findViewById(R.id.group);
        textViewGroup.setText(textGroup);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater infalInflater =
                    (LayoutInflater)_context
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.main_list_item, null);
        }

        TextView textViewItem =
                (TextView)convertView.findViewById(R.id.item);

        String text = (String)getChild(groupPosition, childPosition);

        textViewItem.setText(text);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
