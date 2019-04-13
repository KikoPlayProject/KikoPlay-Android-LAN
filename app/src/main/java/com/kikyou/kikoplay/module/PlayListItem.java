package com.kikyou.kikoplay.module;

import java.util.ArrayList;
import java.util.List;

public class PlayListItem {
    private String title;
    private String media;
    private String pool;
    private int playTimeState;
    private int playTime;
    private PlayListItem parent;

    private List<PlayListItem> children;

    public PlayListItem(PlayListItem parent, String title, String media, String pool){
        this.parent=parent;
        this.title=title;
        this.media=media;
        this.pool=pool;
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
}
