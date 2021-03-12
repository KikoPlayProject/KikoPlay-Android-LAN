package com.kikyou.kikoplay.module;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.kikyou.kikoplay.MainActivity;
import com.kikyou.kikoplay.PlayActivity;
import com.kikyou.kikoplay.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;

class DanmuComment {
    int time;
    int originTime;
    String text;
    int type;
    int color;
    int source;
}
class SourceInfo {
    String title;
    String scriptId, scriptData, scriptName;
    int id;
    int delay;
    int count;
    int duration;
    List<Pair<Integer, Integer>> timeline=new ArrayList<>();
    void setTimeline(String timelineStr) {
        timeline.clear();
        if(timelineStr.trim().length()==0) return;
        String[] tls = timelineStr.trim().split(";");
        for (String tl:tls) {
            int start = Integer.parseInt(tl.split(" ")[0]);
            int duration = Integer.parseInt(tl.split(" ")[1]);
            timeline.add(new Pair<>(start, duration));
        }
        Collections.sort(timeline, new Comparator<Pair<Integer, Integer>>() {
            @Override
            public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
                return o1.first.compareTo(o2.first);
            }
        });
    }
    void addTimelineItem(int start, int duration){
        for(Pair<Integer, Integer> pair:timeline){
            if(pair.first==start) return;
        }
        timeline.add(new Pair<>(start, duration));
        Collections.sort(timeline, new Comparator<Pair<Integer, Integer>>() {
            @Override
            public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
                return o1.first.compareTo(o2.first);
            }
        });
    }
    void removeTimeline(int pos){
        if(pos<0||pos>=timeline.size()) return;
        timeline.remove(pos);
    }
    String getTimeline(){
        StringBuilder builder=new StringBuilder();
        for(Pair<Integer, Integer> pair:timeline){
            builder.append(pair.first);
            builder.append(' ');
            builder.append(pair.second);
            builder.append(';');
        }
        return builder.toString();
    }
}
public class DanmuPool extends BaseAdapter {
    private Context context;
    private List<SourceInfo> sources;
    private HashMap<Integer, SourceInfo> sourcesHash;
    private List<DanmuComment> comments;
    private SimpleExoPlayer player;
    private DanmakuView danmakuView;
    private DanmakuContext danmakuContext;
    private BaseDanmakuParser parser;
    private String kikoPlayServer;
    private String poolId;
    private List<String> launchScriptIds;

    public DanmuPool(SimpleExoPlayer player, DanmakuView danmakuView, DanmakuContext danmakuContext, BaseDanmakuParser parser, Context context, String serverAddress){
        this.player=player;
        this.danmakuView=danmakuView;
        this.danmakuContext=danmakuContext;
        this.parser=parser;
        this.context=context;
        kikoPlayServer=serverAddress;
        sources=new ArrayList<>();
        launchScriptIds= new ArrayList<>();
        sourcesHash=new HashMap<>();
        comments=new ArrayList<>();
    }
    public int getDanmuCount(){return comments.size();}
    @Override
    public int getCount() {
        return sources.size();
    }

    @Override
    public Object getItem(int position) {
        return sources.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        final SourceInfo sourceInfo = sources.get(position);
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = View.inflate(context, R.layout.sourceitemlayout,null);
            viewHolder.title = convertView.findViewById(R.id.source_title);
            viewHolder.url = convertView.findViewById(R.id.source_url);
            viewHolder.delay = convertView.findViewById(R.id.delay_info);
            viewHolder.edit_timeline = convertView.findViewById(R.id.edit_timeline);
            viewHolder.delay.setOnClickListener(new View.OnClickListener() {
                int srcId;
                {
                    srcId=sourceInfo.id;
                }
                @Override
                public void onClick(View v) {
                    showDelayDialog(srcId);
                }
            });
            viewHolder.edit_timeline.setOnClickListener(new View.OnClickListener() {
                int srcId;
                {
                    srcId=sourceInfo.id;
                }
                @Override
                public void onClick(View v) { showTimelineDialog(srcId); }
            });
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.title.setText(String.format(context.getString(R.string.source_title_info), sourceInfo.scriptName, sourceInfo.title,sourceInfo.count));
        viewHolder.url.setText(formatTime(sourceInfo.duration*1000));
        viewHolder.delay.setText(String.format(context.getString(R.string.source_delay_info),sourceInfo.delay/1000));
        return convertView;
    }
    private void showDelayDialog(final int srcId) {
        SourceInfo sourceInfo = sourcesHash.get(srcId);
        if(sourceInfo==null) return;
        final EditText editText = new EditText(context);
        editText.setText(Integer.toString(sourceInfo.delay/1000));
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(context);
        inputDialog.setTitle(R.string.delay_dialog_title).setView(editText);
        inputDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try{
                            String delayStr=editText.getText().toString();
                            if(delayStr.isEmpty()) delayStr="0";
                            int newDelay = Integer.parseInt(delayStr);
                            setDelay(srcId, newDelay*1000);
                        }
                        catch (NumberFormatException e){
                            e.printStackTrace();
                        }

                    }
                }).setNegativeButton("Cancel",null).show();
    }
    private void showTimelineDialog(final int srcId){
        final SourceInfo sourceInfo = sourcesHash.get(srcId);
        if(sourceInfo==null) return;
        TimelineDialog timelineDialog=new TimelineDialog(sourceInfo,context);
        timelineDialog.show();
    }

    private String formatTime(int mSec){
        int cs=(mSec>=0?mSec:-mSec)/1000;
        int cmin=cs/60;
        int cls=cs-cmin*60;
        return String.format("%s%02d:%02d",(mSec<0?"-":""),cmin,cls);
    }
    private void setDelay(DanmuComment comment){
        SourceInfo srcInfo = sourcesHash.get(comment.source);
        if(srcInfo==null) {
            comment.time=comment.originTime;
            return;
        }
        int delay=0;
        for(Pair<Integer, Integer> t: srcInfo.timeline){
            if(comment.originTime>t.first) delay+=t.second;
        }
        delay+=srcInfo.delay;
        comment.time=comment.originTime+delay<0?comment.originTime:comment.originTime+delay;
    }
    private void updateDanmakuView() {
        danmakuView.removeAllDanmakus(true);
        for(DanmuComment comment:comments){
            BaseDanmaku danmu = danmakuContext.mDanmakuFactory.createDanmaku(comment.type, danmakuContext);
            danmu.text = comment.text;
            danmu.setTime(comment.time);
            danmu.textColor = comment.color;
            int cv = (danmu.textColor & 0xff) + ((danmu.textColor >> 8) & 0xff) + ((danmu.textColor >> 16) & 0xff);
            danmu.textShadowColor = cv / 3 <= 128 ? Color.WHITE : Color.BLACK;
            danmu.textSize = 25 * (parser.getDisplayer().getDensity() - 0.5f);
            danmakuView.addDanmaku(danmu);
        }
        danmakuView.seekTo(player.getCurrentPosition());
        if(!player.getPlayWhenReady()) {
            danmakuView.pause();
        }
    }
    private void setPool(JSONObject danmuObj) throws JSONException {
        JSONArray sourceArray = danmuObj.getJSONArray("source");
        JSONArray danmuArray = danmuObj.getJSONArray("comment");
        sources.clear();
        sourcesHash.clear();
        comments.clear();
        for(int i=0;i<sourceArray.length();++i){
            JSONObject sourceObj = (JSONObject)sourceArray.get(i);
            SourceInfo srcInfo=new SourceInfo();
            srcInfo.title=sourceObj.getString("name");
            if(sourceObj.has("scriptName"))
                srcInfo.scriptName=sourceObj.getString("scriptName");
            if(sourceObj.has("scriptData"))
                srcInfo.scriptData=sourceObj.getString("scriptData");
            if(sourceObj.has("scriptId"))
                srcInfo.scriptId=sourceObj.getString("scriptId");
            srcInfo.id=sourceObj.getInt("id");
            if(sourceObj.has("delay"))
                srcInfo.delay=sourceObj.getInt("delay");
            if(sourceObj.has("timeline"))
                srcInfo.setTimeline(sourceObj.getString("timeline"));
            if(sourceObj.has("duration"))
                srcInfo.duration=sourceObj.getInt("duration");
            sources.add(srcInfo);
            sourcesHash.put(srcInfo.id, srcInfo);
        }
        launchScriptIds.clear();
        if(danmuObj.has("launchScripts")) {
            JSONArray scripts = danmuObj.getJSONArray("launchScripts");
            for(int i=0;i<scripts.length();++i) {
                launchScriptIds.add(scripts.get(i).toString());
            }
        }
        addDanmu(danmuArray, false);
        notifyDataSetChanged();
        updateDanmakuView();
    }
    public void clear(){
        poolId="";
        comments.clear();
        sources.clear();
        sourcesHash.clear();
    }
    private void addDanmu(JSONArray danmuArray, boolean addToView) throws JSONException{
        int[] type = {1, 5, 4};
        for (int i = 0; i < danmuArray.length(); i++) {
            JSONArray dm = (JSONArray) danmuArray.get(i);
            DanmuComment comment=new DanmuComment();
            comment.originTime=(int)(dm.getDouble(0)*1000);
            comment.type=type[dm.getInt(1)];
            comment.source=dm.getInt(3);
            comment.color=dm.getInt(2);
            comment.text=dm.getString(4);
            setDelay(comment);
            if(sourcesHash.containsKey(comment.source)){
                sourcesHash.get(comment.source).count++;
            }
            comments.add(comment);
            if(addToView){
                BaseDanmaku danmu = danmakuContext.mDanmakuFactory.createDanmaku(comment.type, danmakuContext);
                danmu.text = comment.text;
                danmu.setTime(comment.time);
                danmu.textColor = comment.color;
                int cv = (danmu.textColor & 0xff) + ((danmu.textColor >> 8) & 0xff) + ((danmu.textColor >> 16) & 0xff);
                danmu.textShadowColor = cv / 3 <= 128 ? Color.WHITE : Color.BLACK;
                danmu.textSize = 25 * (parser.getDisplayer().getDensity() - 0.5f);
                danmakuView.addDanmaku(danmu);
            }
        }
    }
    private void setTimeline(SourceInfo srcInfo){
        for(DanmuComment comment:comments){
            if(comment.source==srcInfo.id)
                setDelay(comment);
        }
        updateDanmakuView();
        notifyDataSetChanged();

        JSONObject obj=new JSONObject();
        try {
            obj.put("danmuPool", poolId);
            obj.put("timeline", srcInfo.getTimeline());
            obj.put("source", srcInfo.id);
            HttpUtil.postAsync("http://" + kikoPlayServer + "/api/updateTimeline",obj.toString(),null,5000,5000);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    private void setDelay(int srcId, int newDelay){
        SourceInfo srcInfo = sourcesHash.get(srcId);
        if(srcInfo==null) return;
        if(srcInfo.delay==newDelay) return;
        srcInfo.delay=newDelay; //ms
        for(DanmuComment comment:comments){
            if(comment.source==srcId)
                setDelay(comment);
        }
        updateDanmakuView();
        notifyDataSetChanged();

        JSONObject obj=new JSONObject();
        try {
            obj.put("danmuPool", poolId);
            obj.put("delay", newDelay);
            obj.put("source", srcId);
            HttpUtil.postAsync("http://" + kikoPlayServer + "/api/updateDelay",obj.toString(),null,5000,5000);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public void loadDanmu(final String pid, final AsyncCallBack callBack){
        if(pid.isEmpty()){
            callBack.onResponse(-2);
            return;
        }
        HttpUtil.getAsync("http://" + kikoPlayServer + "/api/danmu/full/?id=" + pid, new ResponseHandler() {
            @Override
            public void onResponse(byte[] result) {
                if (result != null) {
                    try {
                        JSONObject responseObj=new JSONObject(new String(result));
                        poolId=pid;
                        setPool(responseObj);
                        callBack.onResponse(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                        callBack.onResponse(-1);
                    }
                } else {
                    callBack.onResponse(-1);
                }
            }
        },5000,15*1000);
    }
    public void updateDanmu(final AsyncCallBack callBack){
        if(poolId.isEmpty()){
            callBack.onResponse(-2);
            return;
        }
        HttpUtil.getAsync("http://" + kikoPlayServer + "/api/danmu/full/?id=" + poolId + "&update=true", new ResponseHandler() {
            String pid;
            {
                pid=poolId;
            }
            @Override
            public void onResponse(byte[] result) {
                if(!poolId.equals(pid)) {
                    callBack.onResponse(-1);
                    return;
                }
                if (result != null) {
                    try {
                        JSONObject obj=new JSONObject(new String(result));
                        if(obj.getBoolean("update")) {
                            JSONArray danmuArray = obj.getJSONArray("comment");
                            addDanmu(danmuArray, true);
                            callBack.onResponse(0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callBack.onResponse(-1);
                    }
                } else {
                    callBack.onResponse(-1);
                }
            }
        },5000,60*1000);
    }
    public boolean canLaunch(){
        return launchScriptIds.size()>0;
    }
    public void launch(int time, String text){
        if(text.isEmpty() || poolId.isEmpty()) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("danmuPool", poolId);
            obj.put("time", time);
            obj.put("launchScripts", new JSONArray(launchScriptIds));
            obj.put("text", text);
            HttpUtil.postAsync("http://" + kikoPlayServer + "/api/danmu/launch", obj.toString(), null, 5000, 5000);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    class ViewHolder{
        TextView title;
        TextView url;
        TextView delay;
        ImageButton edit_timeline;
    }
    class TimelineDialog extends Dialog implements View.OnClickListener {
        EditText timeline_start;
        EditText timeline_duration;
        Button timeline_add;
        ListView timeline_view;
        SourceInfo srcInfo;
        ArrayAdapter<String> timeline_adapter;
        boolean sourceChanged=false;


        public TimelineDialog(SourceInfo srcInfo, Context context) {
            super(context);
            this.srcInfo=srcInfo;
        }

        @Override
        public void dismiss() {
            super.dismiss();
            if(sourceChanged) setTimeline(srcInfo);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_timeline);

            timeline_start=findViewById(R.id.timeline_start);
            timeline_duration=findViewById(R.id.timeline_duration);
            timeline_add=findViewById(R.id.timeline_add);
            timeline_view=findViewById(R.id.timeline_list);
            timeline_adapter=new ArrayAdapter<>(context,android.R.layout.simple_expandable_list_item_1);
            timeline_add.setOnClickListener(this);
            timeline_start.setText(formatTime((int)player.getCurrentPosition()));
            timeline_view.setAdapter(timeline_adapter);
            refreshAdapter();
            registerForContextMenu(timeline_view);
            timeline_view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    menu.add(0,0,0,"删除").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if(item.getItemId()==0){
                                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
                                srcInfo.removeTimeline(info.position);
                                refreshAdapter();
                                sourceChanged=true;
                            }
                            return true;
                        }
                    });
                }
            });
        }

        private void refreshAdapter(){
            timeline_adapter.clear();
            for(Pair<Integer,Integer> p:srcInfo.timeline){
                timeline_adapter.add(String.format("开始：%s 时长：%s",formatTime(p.first),formatTime(p.second)));
            }
        }
        @Override
        public void onClick(View v) {
            if(v==timeline_add){
                try{
                    String start_txt=timeline_start.getText().toString();
                    Pattern r=Pattern.compile("(\\d+):(\\d+)");
                    Matcher m=r.matcher(start_txt);
                    if(!m.matches()) return;
                    if(m.end()-m.start()!=start_txt.length()) return;
                    int start=Integer.parseInt(m.group(1))*60+Integer.parseInt(m.group(2));
                    start*=1000;
                    int duration=Integer.parseInt(timeline_duration.getText().toString())*1000;
                    if(duration==0) return;
                    srcInfo.addTimelineItem(start,duration);
                    refreshAdapter();
                    sourceChanged=true;
                }
                catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }
}
