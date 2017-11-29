package uk.co.section9.zotdroid;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

/**
 * Created by oni on 14/07/2017.
 * http://android-coding.blogspot.co.uk/2014/02/expandablelistview-example.html
 */

/**
 * Our main class that deals with the big list of Zotero records.
 */
public class ZotDroidListAdapter extends BaseExpandableListAdapter {

    Context _context;
    Activity _activity;
    private ArrayList<String> _list_group;
    private HashMap<String, ArrayList<String>> _list_child;
    private String _font_size;

    public ZotDroidListAdapter(Activity activity, Context context, ArrayList<String> groups, HashMap<String, ArrayList<String>> children, String fontsize ) {
        super();
        _list_group = groups;
        _list_child = children;
        _context = context;
        _font_size = fontsize;
        _activity = activity; // Probably shouldn't have this here but I dont think it matters too much.
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
        TextView textViewGroup = (TextView)convertView.findViewById(R.id.main_list_group);
        textViewGroup.setText(textGroup);

        // TODO - this duplicates stuff in main. Ideally we would have this elsewhere and with static final strings
        if (_font_size.contains("small")){ textViewGroup.setTextAppearance(_context, R.style.MainList_Title_Small); }
        else if (_font_size.contains("medium")){ textViewGroup.setTextAppearance(_context, R.style.MainList_Title_Medium);}
        else if (_font_size.contains("large")) { textViewGroup.setTextAppearance(_context, R.style.MainList_Title_Large);}
        else { textViewGroup.setTextAppearance(_context, R.style.MainList_Title_Medium);}

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

        TextView textViewItem = (TextView)convertView.findViewById(R.id.main_list_subtext);
        // TODO - this duplicates stuff in main. Ideally we would have this elsewhere and with static final strings
        if (_font_size.contains("small")){ textViewItem.setTextAppearance(_context, R.style.MainList_SubText_Small); }
        else if (_font_size.contains("medium")){ textViewItem.setTextAppearance(_context, R.style.MainList_SubText_Medium);}
        else if (_font_size.contains("large")) { textViewItem.setTextAppearance(_context, R.style.MainList_SubText_Large);}
        else { textViewItem.setTextAppearance(_context, R.style.MainList_Title_Medium);}

        String text = (String)getChild(groupPosition, childPosition);
        textViewItem.setText(text);

        if (text.contains("Attachment")) {
            if (Util.fileExists(text, _activity)) {
                ImageView imgViewChild = (ImageView) convertView.findViewById(R.id.main_list_icon_download);
                String uri = "@android:drawable/presence_online";
                int imageResource = _context.getResources().getIdentifier(uri, null, _context.getPackageName());
                imgViewChild.setImageResource(imageResource);
            }
        } else {
            // Hide icon for normal meta-data fields
            ImageView imgViewChild = (ImageView) convertView.findViewById(R.id.main_list_icon_download);
            imgViewChild.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }


    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


}
