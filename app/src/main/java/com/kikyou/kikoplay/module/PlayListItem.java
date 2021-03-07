package com.kikyou.kikoplay.module;

import com.kikyou.kikoplay.R;

import java.util.ArrayList;
import java.util.List;

public class PlayListItem {
    private String title;
    private String animeName;
    private String media;
    private String pool;
    private int playTimeState;
    private int playTime;
    public enum Marker
    {
        M_RED, M_BLUE, M_GREEN, M_ORANGE, M_PINK, M_YELLOW, M_NONE
    }
    public static int MarkerIcon[] = {
            R.drawable.ic_mark_red, R.drawable.ic_mark_blue, R.drawable.ic_mark_green,
            R.drawable.ic_mark_orange, R.drawable.ic_mark_pink, R.drawable.ic_mark_yellow
    };
    private Marker marker;
    private PlayListItem parent;
    private List<PlayListItem> children;

    public PlayListItem(PlayListItem parent, String title, String media, String pool, String animeName){
        this.parent=parent;
        this.title=title;
        this.animeName=animeName;
        this.media=media;
        this.pool=pool;
        this.marker = Marker.M_NONE;
        if(media==null) {
            children=new ArrayList<>();
        }
        if(parent!=null){
            parent.addChild(this);
        }
    }

    private void addChild(PlayListItem child){
        children.add(child);
    }

    public PlayListItem getParent() {
        return parent;
    }

    public void setChildren(List<PlayListItem> children) {
        this.children = children;
    }

    public List<PlayListItem> getChildren() {
        return children;
    }
    public String getTitle() {
        return title;
    }

    public String getMedia() {
        return media;
    }

    public String getPool() {
        return pool;
    }

    public String getAnimeName() {return animeName;}

    public int getPlayTimeState() {
        return playTimeState;
    }

    public void setPlayTimeState(int playTimeState) {
        this.playTimeState = playTimeState;
    }

    public int getPlayTime() {
        return playTime;
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
    }

    public Marker getMarker() {return marker;}

    public void setMarker(Marker m) {this.marker = m;}
}
