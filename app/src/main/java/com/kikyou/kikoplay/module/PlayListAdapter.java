package com.kikyou.kikoplay.module;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kikyou.kikoplay.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlayListAdapter extends BaseAdapter {
    private Context context;
    private PlayListItem root;
    private PlayListItem curCollection;
    private PlayListItem curPlayItem;

    public PlayListAdapter(Context context) {
        this.context = context;
        root=new PlayListItem(null,null,null,null);
        curCollection=root;
    }

    public void setJsonData(String json){
        root=new PlayListItem(null,null,null,null);
        try {
            JSONArray curArray = new JSONArray(json);
            buildList(curArray,root);
            curCollection=root;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        notifyDataSetChanged();
    }
    public void setPlayList(List<PlayListItem> list){
        if(list!=null) {
            root.setChildren(list);
            notifyDataSetChanged();
        }
    }
    private void buildList(JSONArray curArray, PlayListItem parent) throws JSONException {
        for(int i = 0;i < curArray.length();i++){
            JSONObject itemObject = (JSONObject) curArray.get(i);
            if(itemObject.has("nodes")) {
                PlayListItem collection=new PlayListItem(parent,itemObject.getString("text"),null,null);
                buildList(itemObject.getJSONArray("nodes"),collection);
            }
            else{
                PlayListItem item=new PlayListItem(parent,itemObject.getString("text"),
                        itemObject.getString("mediaId"),itemObject.getString("danmuPool"));
                item.setPlayTime(itemObject.getInt("playTime"));
                item.setPlayTimeState(itemObject.getInt("playTimeState"));
            }
        }
    }

    public List<PlayListItem> getCurLevelItems(){
        List<PlayListItem> list=new ArrayList<>();
        for (PlayListItem item:curCollection.getChildren()) {
            if(item.getMedia()!=null) list.add(item);
        }
        return list;
    }

    public void setForward(int pos){
        PlayListItem c=curCollection.getChildren().get(pos);
        if(c!=null && c.getChildren()!=null){
            curCollection=c;
            notifyDataSetChanged();
        }
    }
    public boolean setBack(){
        if(curCollection==root) return false;
        curCollection=curCollection.getParent();
        notifyDataSetChanged();
        return true;
    }
    public PlayListItem getCurPlayItem() {
        return curPlayItem;
    }

    public void setCurPlayItem(PlayListItem curPlayItem) {
        if(curPlayItem.getMedia()==null) return;
        this.curPlayItem = curPlayItem;
        notifyDataSetChanged();
    }
    public void setCurPlayTime(int time, int state){
        if(curPlayItem==null) return;
        curPlayItem.setPlayTime(time);
        curPlayItem.setPlayTimeState(state);
        notifyDataSetChanged();
    }
    public PlayListItem getNextItem(){
        List<PlayListItem> items;
        if(curPlayItem==null || curPlayItem.getMedia()==null){
            items=curCollection.getChildren();
            for (PlayListItem item:items) {
                if(item.getMedia()!=null) return item;
            }
        }
        else{
            items=curPlayItem.getParent().getChildren();
            int npos = items.indexOf(curPlayItem)+1;
            if(npos<items.size()) return items.get(npos);
        }
        return null;
    }

    @Override
    public int getCount() {
        return curCollection.getChildren().size();
    }

    @Override
    public Object getItem(int position) {
        return curCollection.getChildren().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = View.inflate(context, R.layout.playlistitemlayout,null);
            viewHolder.title = convertView.findViewById(R.id.title);
            viewHolder.desc = convertView.findViewById(R.id.desc);
            viewHolder.playState = convertView.findViewById(R.id.play_state);
            viewHolder.expandable = convertView.findViewById(R.id.item_state);
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }
        PlayListItem item = curCollection.getChildren().get(position);

        viewHolder.title.setText(item.getTitle());
        if(item.getMedia()!=null) {
            if(item.getPlayTimeState()==0) {
                viewHolder.desc.setText(context.getText(R.string.desc_item_n));
                viewHolder.title.setTextColor(ContextCompat.getColor(context,R.color.itemTitle_N));
            }
            else if(item.getPlayTimeState()==1) {
                int min=item.getPlayTime()/60;
                int sec=item.getPlayTime()-min*60;
                viewHolder.desc.setText(String.format(context.getString(R.string.desc_item_p), min,sec));
                viewHolder.title.setTextColor(ContextCompat.getColor(context,R.color.itemTitle_P));
            }
            else{
                viewHolder.desc.setText(context.getText(R.string.desc_item_f));
                viewHolder.title.setTextColor(ContextCompat.getColor(context,R.color.itemTitle_F));
            }
            viewHolder.expandable.setVisibility(View.GONE);
        }
        else{
            viewHolder.desc.setText(String.format(context.getString(R.string.desc_collection), item.getChildren().size()));
            viewHolder.title.setTextColor(ContextCompat.getColor(context,R.color.itemTitle_Collection));
            viewHolder.expandable.setVisibility(View.VISIBLE);
        }
        if(item==curPlayItem)viewHolder.playState.setImageResource(R.drawable.ic_play);
        else viewHolder.playState.setImageDrawable(null);
        viewHolder.playState.setVisibility(item==curPlayItem?View.VISIBLE:View.GONE);

        return convertView;
    }

    class ViewHolder{
        TextView title;
        TextView desc;
        ImageView playState;
        ImageView expandable;
    }
}
