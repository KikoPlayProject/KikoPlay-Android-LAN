package com.kikyou.kikoplay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;
import com.kikyou.kikoplay.module.AsyncCallBack;
import com.kikyou.kikoplay.module.DanmuPool;
import com.kikyou.kikoplay.module.HttpUtil;
import com.kikyou.kikoplay.module.PlayListAdapter;
import com.kikyou.kikoplay.module.PlayListItem;
import com.kikyou.kikoplay.module.ResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;

public class PlayActivity extends AppCompatActivity implements PlaylistFragment.OnFragmentInteractionListener,DanmuFragment.OnFragmentInteractionListener {
    String kikoPlayServer;
    PlayListAdapter curAdapter;
    List<PlayListItem> curList;
    PlayListItem selectedItem;
    DanmuPool pool;
    AsyncCallBack loadDanmuCallBack;

    TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
    TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
    SimpleExoPlayer player;
    AudioManager mAudioManager;

    FrameLayout surfaceFrame;
    DanmakuContext danmuContext;
    SurfaceView videoSurface;
    SubtitleView subtitleView;
    AspectRatioFrameLayout surfaceAspect;
    DanmakuView danmuView;
    View playControl;
    ImageButton playPause, fullScreen, danmuSetting, danmuVisible, danmuLaunch, back,
            captureImage, captureSnippet;
    SeekBar videoSeekBar;
    TextView timeText,seekTipText,titleText;
    ConstraintLayout danmuSettingView,danmuSettingLayout;
    TextView blockTopView,blockRollView,blockBottomView;
    SeekBar danmuOpacitySeek,danmuSizeSeek,danmuSpeedSeek,danmuRegionSeek;
    TextView danmuOpacityVal,danmuSizeVal,danmuSpeedVal,danmuRegionVal;

    PlaylistFragment playlistFragment;
    DanmuFragment danmuFragment;
    TabLayout tabLayout;
    ViewPager viewPager;

    boolean isDoubleClick = false;
    boolean isFullScreen = false;
    boolean seekToLastPlayTime = false;
    boolean isSeeking = false;
    boolean isChangingVolume = false;
    boolean isChangingBrightness = false;
    boolean pauseOnStop = false;
    int maxVolume,curVolume;
    float curBrightness=0.f;
    float downX=0.f,downY=0.f;
    int downTime=0;
    boolean isBlockTop = false, isBlockBottom = false, isBlockRoll = false;
    float textScale=1.f;
    int snippetStartPos = -1;


    Timer refreshTimer;
    long lastTime = 0L;
    OrientationEventListener mOrientationListener;
    OrientationHandler orientationHandler;
    int startRotation;
    int videoWidth, videoHeight;
    BaseDanmakuParser parser;
    SeekBar.OnSeekBarChangeListener danmuSettingSeekListener;
    SharedPreferences.Editor configEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        SharedPreferences config = getSharedPreferences("config", MODE_PRIVATE);
        kikoPlayServer = config.getString("ServerAddress", "");
        curAdapter = new PlayListAdapter(this);
        if (curList == null) curList = ((KikoPlayApp) getApplication()).curPlayList;
        if (selectedItem == null) selectedItem = ((KikoPlayApp) getApplication()).curPlayItem;
        curAdapter.setPlayList(curList);
        configEditor = getSharedPreferences("config", MODE_PRIVATE).edit();
        initViews();
        initDanmu();
        initConfig(config);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isFullScreen=true;
            fullScreen.setImageResource(R.drawable.ic_fullscreen_exit);
        }
        try {
            curBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS)/255.f;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        playlistFragment=PlaylistFragment.newInstance();
        danmuFragment=DanmuFragment.newInstance();
        playlistFragment.setAdapter(curAdapter);
        danmuFragment.setAdapter(pool);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int i) {
                return i==0?playlistFragment:danmuFragment;
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return position==0?"列表":"弹幕";
            }
        });
        tabLayout.setupWithViewPager(viewPager);
        play(selectedItem);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            isFullScreen=true;
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            danmuSetting.setVisibility(View.VISIBLE);
        }
        else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            getWindow().getDecorView().setSystemUiVisibility(uiFlags);
            isFullScreen=false;
            danmuSetting.setVisibility(View.GONE);
            if(danmuSettingView.getVisibility()!=View.INVISIBLE) {
                danmuSettingView.setVisibility(View.INVISIBLE);
            }
        }
        resizeVideo();
        fullScreen.setImageResource(isFullScreen? R.drawable.ic_fullscreen_exit : R.drawable.ic_fullscreen);
        danmuSettingSeekListener.onProgressChanged(danmuRegionSeek,danmuRegionSeek.getProgress(),false);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(player!=null){
            if(pauseOnStop){
                player.setPlayWhenReady(true);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(player!=null){
            if(player.getPlayWhenReady()) {
                player.setPlayWhenReady(false);
                pauseOnStop = true;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(Build.VERSION.SDK_INT>=24 && isInMultiWindowMode()){
            configEditor.apply();
            updatePlayTime(curAdapter.getCurPlayItem());
            super.onBackPressed();
            return;
        }
        if(isFullScreen){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        else{
            configEditor.apply();
            updatePlayTime(curAdapter.getCurPlayItem());
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(refreshTimer!=null) refreshTimer.cancel();
        if(orientationHandler!=null) orientationHandler.removeCallbacksAndMessages(null);
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        if(danmuView!=null) danmuView.release();
    }

    void initConfig(SharedPreferences config){
        danmuSettingSeekListener=new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                switch(seekBar.getId()){
                    case R.id.danmu_opac_seek:
                        danmuOpacityVal.setText(String.format("%d%%",progress));
                        danmuContext.setDanmakuTransparency(progress/100.f);
                        if(fromUser) configEditor.putInt("DanmuOpacity",progress);
                        break;
                    case R.id.danmu_speed_seek:
                        danmuSpeedVal.setText(String.format("%d",progress));
                        danmuContext.setScrollSpeedFactor((20-progress)/10.f);
                        if(fromUser) configEditor.putInt("DanmuSpeed",progress);
                        break;
                    case R.id.danmu_size_seek:
                        danmuSizeVal.setText(String.format("%d%%",progress+50));
                        textScale=(progress+50)/100.f;
                        danmuContext.setScaleTextSize(textScale);
                        if(fromUser) configEditor.putInt("DanmuSize",progress);
                        break;
                    case R.id.danmu_region_seek:
                        danmuRegionVal.setText(getResources().getStringArray(R.array.danmu_region)[progress]);
                        surfaceFrame.post(new Runnable() {
                            @Override
                            public void run() {
                                int maxLens = (int)(surfaceFrame.getHeight()/(25 * textScale * (getResources().getDisplayMetrics().density - 0.5f)));
                                final int lens=(int)(maxLens*(progress+1)*0.25f);
                                if(progress==3 || lens<=0) danmuContext.setMaximumLines(null);
                                else danmuContext.setMaximumLines(new HashMap<Integer, Integer>() {
                                    {
                                        put(BaseDanmaku.TYPE_SCROLL_RL,lens);
                                        put(BaseDanmaku.TYPE_FIX_BOTTOM,lens);
                                        put(BaseDanmaku.TYPE_FIX_TOP,lens);
                                    }
                                });
                            }
                        });
                        if(fromUser) configEditor.putInt("DanmuRegion",progress);
                        break;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        danmuOpacitySeek.setOnSeekBarChangeListener(danmuSettingSeekListener);
        danmuSpeedSeek.setOnSeekBarChangeListener(danmuSettingSeekListener);
        danmuSizeSeek.setOnSeekBarChangeListener(danmuSettingSeekListener);
        danmuRegionSeek.setOnSeekBarChangeListener(danmuSettingSeekListener);
        danmuRegionVal.setText(getResources().getStringArray(R.array.danmu_region)[3]);

        danmuOpacitySeek.setProgress(config.getInt("DanmuOpacity",100));
        danmuSpeedSeek.setProgress(config.getInt("DanmuSpeed",5));
        danmuSizeSeek.setProgress(config.getInt("DanmuSize",50));
        danmuRegionSeek.setProgress(config.getInt("DanmuRegion",3));

        isBlockTop=config.getBoolean("BlockTop",false);
        isBlockRoll=config.getBoolean("BlockRoll",false);
        isBlockBottom=config.getBoolean("BlockBottom",false);

        danmuContext.setFTDanmakuVisibility(!isBlockTop);
        danmuContext.setR2LDanmakuVisibility(!isBlockRoll);
        danmuContext.setFBDanmakuVisibility(!isBlockBottom);

        blockTopView.setTextColor(ContextCompat.getColor(this,isBlockTop?R.color.colorAccent:R.color.controlText));
        blockRollView.setTextColor(ContextCompat.getColor(this,isBlockRoll?R.color.colorAccent:R.color.controlText));
        blockBottomView.setTextColor(ContextCompat.getColor(this,isBlockBottom?R.color.colorAccent:R.color.controlText));

        View.OnClickListener blockBtnListener=new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.danmu_top:
                        isBlockTop=!isBlockTop;
                        blockTopView.setTextColor(ContextCompat.getColor(PlayActivity.this,isBlockTop?R.color.colorAccent:R.color.controlText));
                        danmuContext.setFTDanmakuVisibility(!isBlockTop);
                        configEditor.putBoolean("BlockTop",isBlockTop);
                        break;
                    case R.id.danmu_roll:
                        isBlockRoll=!isBlockRoll;
                        blockRollView.setTextColor(ContextCompat.getColor(PlayActivity.this,isBlockRoll?R.color.colorAccent:R.color.controlText));
                        danmuContext.setR2LDanmakuVisibility(!isBlockRoll);
                        configEditor.putBoolean("BlockRoll",isBlockRoll);
                        break;
                    case R.id.danmu_bottom:
                        isBlockBottom=!isBlockBottom;
                        blockBottomView.setTextColor(ContextCompat.getColor(PlayActivity.this,isBlockBottom?R.color.colorAccent:R.color.controlText));
                        danmuContext.setFBDanmakuVisibility(!isBlockBottom);
                        configEditor.putBoolean("BlockBottom",isBlockBottom);
                        break;
                }
            }
        };
        blockTopView.setOnClickListener(blockBtnListener);
        blockRollView.setOnClickListener(blockBtnListener);
        blockBottomView.setOnClickListener(blockBtnListener);

    }

    @SuppressLint("ClickableViewAccessibility")
    void initViews() {
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        videoSurface = findViewById(R.id.videoSurface);
        subtitleView = findViewById(R.id.subtitleView);
        player.setVideoSurfaceView(videoSurface);
        surfaceFrame = findViewById(R.id.surfaceFrame);
        surfaceAspect=findViewById(R.id.surfaceAspectLayout);
        surfaceAspect.setAspectRatio(16.f/9.f);
        playControl = findViewById(R.id.playControl);
        fullScreen = findViewById(R.id.ctr_fullscreen);
        danmuVisible = findViewById(R.id.ctr_danmu);
        danmuSetting = findViewById(R.id.ctr_danmu_settings);
        danmuLaunch = findViewById(R.id.ctr_danmu_launch);
        orientationHandler = new OrientationHandler(this);
        danmuView = findViewById(R.id.danmuView);
        //controlBtnPanel = findViewById(R.id.controlBtnLayout);
        timeText = findViewById(R.id.ctr_time);
        videoSeekBar = findViewById(R.id.ctr_seek);
        playPause = findViewById(R.id.ctr_pause);
        seekTipText = findViewById(R.id.seekTipTextView);
        back=findViewById(R.id.ctr_back);
        captureImage = findViewById(R.id.ctr_capture_image);
        captureSnippet = findViewById(R.id.ctr_capture_snippet);
        titleText=findViewById(R.id.ctr_title);
        tabLayout=findViewById(R.id.tabs);
        viewPager=findViewById(R.id.play_viewPager);


        danmuSettingView =findViewById(R.id.danmuSettingView);
        danmuSettingLayout=findViewById(R.id.danmuSettingLayout);
        blockTopView=findViewById(R.id.danmu_top);
        blockBottomView=findViewById(R.id.danmu_bottom);
        blockRollView=findViewById(R.id.danmu_roll);
        danmuOpacitySeek=findViewById(R.id.danmu_opac_seek);
        danmuSpeedSeek=findViewById(R.id.danmu_speed_seek);
        danmuSizeSeek=findViewById(R.id.danmu_size_seek);
        danmuRegionSeek=findViewById(R.id.danmu_region_seek);
        danmuOpacityVal=findViewById(R.id.danmu_opac_value);
        danmuSpeedVal=findViewById(R.id.danmu_speed_value);
        danmuSizeVal=findViewById(R.id.danmu_size_value);
        danmuRegionVal=findViewById(R.id.danmu_region_value);

        refreshTimer = new Timer();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        setOrientationListener();
        fullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFullScreen = !isFullScreen;
                if (isFullScreen)setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                orientationHandler.sendEmptyMessageDelayed(0, 1000);
            }
        });

        surfaceFrame.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        long curTime = System.currentTimeMillis();
                        if(curTime - lastTime < 300){
                            isDoubleClick=true;
                            player.setPlayWhenReady(!player.getPlayWhenReady());
                        }
                        else {
                            isDoubleClick=false;
                        }
                        lastTime = curTime;
                        downX=event.getX();
                        downY=event.getY();
                        downTime=(int)(player.getCurrentPosition()/1000);
                        curVolume=mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        WindowManager.LayoutParams lp = getWindow().getAttributes();
                        if(lp.screenBrightness!=-1) curBrightness=lp.screenBrightness;
                    }
                    break;
                    case MotionEvent.ACTION_MOVE: {
                        float x=event.getX(),y=event.getY();
                        int surfaceHeight=surfaceFrame.getHeight();
                        if(y<surfaceHeight/4){
                            isSeeking=false;
                            isChangingVolume=false;
                            isChangingBrightness=false;
                            seekTipText.setVisibility(View.INVISIBLE);
                            break;
                        }
                        int seekStep=surfaceFrame.getWidth()/100;
                        int volumeStep=surfaceHeight/maxVolume;
                        int brightnessStep=surfaceHeight/16;
                        if(!isChangingVolume && !isChangingBrightness && Math.abs(x-downX) > Math.abs(y-downY) && Math.abs(x-downX) >3*seekStep &&
                            danmuSettingView.getVisibility()!=View.VISIBLE){
                            seekTipText.setVisibility(View.VISIBLE);
                            playControl.setVisibility(View.VISIBLE);
                            isSeeking=true;
                        }
                        else if(!isSeeking  && !isChangingBrightness && downX>surfaceFrame.getWidth()/3*2 && Math.abs(x-downX) < Math.abs(y-downY) &&
                                Math.abs(y-downY) >volumeStep && danmuSettingView.getVisibility()!=View.VISIBLE){
                            seekTipText.setVisibility(View.VISIBLE);
                            playControl.setVisibility(View.VISIBLE);
                            isChangingVolume=true;
                        }
                        else if(!isSeeking && !isChangingVolume && downX<surfaceFrame.getWidth()/3 && Math.abs(x-downX) < Math.abs(y-downY) &&
                                Math.abs(y-downY) >brightnessStep && danmuSettingView.getVisibility()!=View.VISIBLE){
                            seekTipText.setVisibility(View.VISIBLE);
                            playControl.setVisibility(View.VISIBLE);
                            isChangingBrightness=true;
                        }
                        if(isSeeking){
                            int total_sec = (int) player.getDuration() / 1000;
                            int min = total_sec / 60;
                            int sec = total_sec - min * 60;

                            int curPos = downTime + (int)((x-downX)/seekStep);
                            if(curPos<0) curPos=0;
                            else if(curPos>total_sec) curPos=total_sec;
                            int cmin = curPos / 60;
                            int csec = curPos - cmin * 60;
                            seekTipText.setText(String.format("%02d:%02d/%02d:%02d", cmin, csec, min, sec));
                        }
                        else if(isChangingVolume){
                            int tmpVolume=curVolume+(int)((downY-y)/volumeStep);
                            seekTipText.setText(String.format("%d/%d", tmpVolume<0?0:(tmpVolume>maxVolume?maxVolume:tmpVolume),maxVolume));
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, tmpVolume<0?0:(tmpVolume>maxVolume?maxVolume:tmpVolume), 0);
                        }
                        else if(isChangingBrightness){
                            float tmpBrightness=curBrightness+(downY-y)/brightnessStep/16;
                            if(tmpBrightness<0) tmpBrightness=0;
                            else if(tmpBrightness>1.f) tmpBrightness=1.f;
                            seekTipText.setText(String.format("%d/16", (int)(tmpBrightness*16)));
                            WindowManager.LayoutParams lp = getWindow().getAttributes();
                            lp.screenBrightness = tmpBrightness;
                            getWindow().setAttributes(lp);
                        }
                    }
                    break;
                    case MotionEvent.ACTION_UP: {
                        if(isSeeking){
                            long nTime=(downTime + (int)((event.getX()-downX)*100/surfaceFrame.getWidth()))*1000;
                            player.seekTo(nTime);
                            danmuView.seekTo(nTime);

                            seekTipText.setVisibility(View.INVISIBLE);
                            playControl.setVisibility(View.INVISIBLE);
                        }
                        else if(isChangingVolume){
                            seekTipText.setVisibility(View.INVISIBLE);
                            playControl.setVisibility(View.INVISIBLE);
                        }
                        else if(isChangingBrightness){
                            seekTipText.setVisibility(View.INVISIBLE);
                            playControl.setVisibility(View.INVISIBLE);
                        }
                        else {
                            if (!isDoubleClick && System.currentTimeMillis() - lastTime < 300) {
                                if(danmuSettingView.getVisibility()==View.VISIBLE){
                                    if(event.getX()< danmuSettingLayout.getX()){
                                        danmuSettingView.setVisibility(View.INVISIBLE);
                                    }
                                }
                                else {
                                    playControl.setVisibility(playControl.getVisibility()!=View.VISIBLE ? View.VISIBLE : View.INVISIBLE);
                                    if(isFullScreen && playControl.getVisibility()==View.INVISIBLE){
                                        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_FULLSCREEN //hide statusBar
                                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION; //hide navigationBar
                                        surfaceFrame.setSystemUiVisibility(uiFlags);
                                    }
                                }
                            }
                        }
                        isSeeking=false;
                        isChangingVolume=false;
                        isChangingBrightness=false;
                    }
                    break;
                }
                return true;

            }
        });
        videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                player.seekTo(progress * 1000);
                danmuView.seekTo((long) progress * 1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItem == null) return;
                try {


                    JSONObject obj = new JSONObject();
                    int curPos = (int)player.getCurrentPosition()/1000;
                    int cmin = curPos/ 60;
                    int csec = curPos - cmin * 60;
                    String info = String.format("%02d:%02d - %s", cmin, csec, selectedItem.getTitle());
                    obj.put("mediaId", selectedItem.getMedia());
                    obj.put("animeName", selectedItem.getAnimeName());
                    obj.put("pos", curPos);
                    obj.put("info", info);
                    HttpUtil.postAsync("http://" + kikoPlayServer + "/api/screenshot", obj.toString(), null, 5000, 5000);

                    Snackbar.make(findViewById(R.id.playLayout),
                            String.format(getString(R.string.capture_image_task_sent), String.format("%02d:%02d", cmin, csec)),
                            Snackbar.LENGTH_LONG).show();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        });
        captureSnippet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItem == null) return;
                try {
                    if(snippetStartPos==-1)
                    {
                        snippetStartPos = (int)player.getCurrentPosition()/1000;
                        captureSnippet.setImageResource(R.drawable.ic_capture_stop);
                        int cmin = snippetStartPos / 60;
                        int csec = snippetStartPos - cmin * 60;
                        Snackbar.make(findViewById(R.id.playLayout),
                                String.format(getString(R.string.capture_snipped_start_tip), String.format("%02d:%02d", cmin, csec)),
                                Snackbar.LENGTH_LONG).show();
                    }
                    else
                    {
                        final int curPos = (int)player.getCurrentPosition()/1000;
                        AlertDialog.Builder inputDialog = new AlertDialog.Builder(PlayActivity.this);
                        View view = LayoutInflater.from(PlayActivity.this).inflate(R.layout.dialog_snippet, null);
                        final CheckBox retainAudioCheck = view.findViewById(R.id.snippet_retain_audio_check);
                        TextView tipView = view.findViewById(R.id.snippet_info);
                        int start = snippetStartPos, end = curPos;
                        if(end < start) {
                            start = curPos; end = snippetStartPos;
                        }
                        if(end - start < 1) end = start + 1;
                        if(end - start > 15) end = start + 15;
                        int smin = start / 60, emin = end / 60;
                        int ssec = start - smin * 60, esec = end - emin * 60;
                        tipView.setText(String.format(getString(R.string.snippet_info),
                                String.format("%02d:%02d", smin, ssec),
                                String.format("%02d:%02d", emin, esec),
                                end - start));
                        inputDialog.setTitle(R.string.snippet_dialog_title).setView(view);
                        final int snippetStart = snippetStartPos, snippetDuraition = end - start;
                        inputDialog.setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try{
                                            JSONObject obj = new JSONObject();
                                            int cmin = snippetStart/ 60;
                                            int csec = snippetStart - cmin * 60;
                                            String info = String.format("%02d:%02d, %ds - %s", cmin, csec, snippetDuraition, selectedItem.getTitle());
                                            obj.put("mediaId", selectedItem.getMedia());
                                            obj.put("animeName", selectedItem.getAnimeName());
                                            obj.put("pos", snippetStart);
                                            obj.put("duration", snippetDuraition);
                                            obj.put("info", info);
                                            obj.put("retainAudio", retainAudioCheck.isChecked());
                                            HttpUtil.postAsync("http://" + kikoPlayServer + "/api/screenshot", obj.toString(), null, 5000, 5000);
                                            Snackbar.make(findViewById(R.id.playLayout),
                                                    getString(R.string.capture_snipped_stop_tip),
                                                    Snackbar.LENGTH_LONG).show();
                                        }
                                        catch (Exception e){
                                            e.printStackTrace();
                                        }
                                    }
                                }).setNegativeButton("Cancel",null).show();
                        snippetStartPos = -1;
                        captureSnippet.setImageResource(R.drawable.ic_capture_start);
                    }

                } catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        });
        danmuLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItem == null || !pool.canLaunch()) return;
                    final int time = (int)player.getCurrentPosition();
                    int cmin = time/1000/ 60;
                    int csec = time/1000 - cmin * 60;

                    final EditText editText = new EditText(PlayActivity.this);
                    AlertDialog.Builder inputDialog = new AlertDialog.Builder(PlayActivity.this);
                    inputDialog.setTitle(String.format(getString(R.string.launch_dialog_title), String.format("%02d:%02d", cmin, csec)))
                            .setView(editText);
                    inputDialog.setPositiveButton("Send",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    pool.launch(time, editText.getText().toString());
                                }
                    }).setNegativeButton("Cancel",null).show();
            }
        });
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.setPlayWhenReady(!player.getPlayWhenReady());
            }
        });
        danmuVisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(danmuView.isShown()){
                    danmuVisible.setImageResource(R.drawable.ic_danmu_hide);
                    danmuView.hide();
                }
                else{
                    danmuVisible.setImageResource(R.drawable.ic_danmu_show);
                    danmuView.show();
                }
            }
        });
        danmuSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                danmuSettingView.setVisibility(View.VISIBLE);
                danmuSettingView.setAnimation(AnimationUtils.loadAnimation(PlayActivity.this, R.anim.move_in_right));
                playControl.setVisibility(View.INVISIBLE);

            }
        });
        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final int total_sec = (int) player.getDuration() / 1000;
                int min = total_sec / 60;
                int sec = total_sec - min * 60;

                final int bufPos = (int) player.getBufferedPosition() / 1000;
                final int curPos = (int) player.getCurrentPosition() / 1000;
                int cmin = curPos / 60;
                int csec = curPos - cmin * 60;
                final String time = String.format("%02d:%02d/%02d:%02d", cmin, csec, min, sec);
                playControl.post(new Runnable() {
                    @Override
                    public void run() {
                        timeText.setText(time);
                        videoSeekBar.setMax(total_sec);
                        videoSeekBar.setSecondaryProgress(bufPos);
                        videoSeekBar.setProgress(curPos);
                    }
                });
            }
        }, 1000, 1000);
        player.addVideoListener(new VideoListener() {
            @Override
            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                videoWidth = width;
                videoHeight = height;
                resizeVideo();
            }
        });
        player.addTextOutput(new TextRenderer.Output() {
            @Override
            public void onCues(List<Cue> cues) {
                Log.e("cues are ",cues.toString());
                if(subtitleView!=null){
                    subtitleView.onCues(cues);
                }
            }
        });
        player.addListener(new Player.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        danmuView.pause();
                        break;
                    case Player.STATE_ENDED:
                        danmuView.pause();
                        PlayListItem next=curAdapter.getNextItem();
                        if(next!=null) play(next);
                        else updatePlayTime(curAdapter.getCurPlayItem());
                        break;
                    case Player.STATE_READY:
                        videoSurface.setKeepScreenOn(playWhenReady);
                        if (seekToLastPlayTime) {
                            player.seekTo(selectedItem.getPlayTime() * 1000);
                            danmuView.start((long) selectedItem.getPlayTime() * 1000);
                            seekToLastPlayTime=false;
                        }
                        if (playWhenReady) danmuView.resume();
                        else danmuView.pause();
                }
                playPause.setImageResource(playWhenReady ? R.drawable.ic_pause : R.drawable.ic_play);
                videoSurface.setKeepScreenOn(playWhenReady);
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Snackbar.make(findViewById(R.id.playLayout), error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    void setOrientationListener() {
        mOrientationListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int rotation) {
                if (startRotation == -2) {
                    startRotation = rotation;
                }
                int r = Math.abs(startRotation - rotation);
                r = r > 180 ? 360 - r : r;
                if (r > 30) {
                    boolean canRotate = false;
                    try {
                        canRotate = (Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (canRotate)
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                    else
                        setRequestedOrientation(isFullScreen ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    this.disable();
                }
            }
        };
        orientationHandler = new OrientationHandler(this);
    }

    void resizeVideo() {
        if (isFullScreen) {
            ViewGroup.LayoutParams p = surfaceFrame.getLayoutParams();
            p.height = ViewGroup.LayoutParams.MATCH_PARENT;
            p.width = ViewGroup.LayoutParams.MATCH_PARENT;
            surfaceFrame.setLayoutParams(p);
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            float h = dm.widthPixels * videoHeight / (float) videoWidth;
            ViewGroup.LayoutParams p = surfaceFrame.getLayoutParams();
            p.width = ViewGroup.LayoutParams.MATCH_PARENT;
            p.height = (int) h;
            surfaceFrame.setLayoutParams(p);
        }
        surfaceAspect.setAspectRatio((float)videoWidth/(float)videoHeight);
    }

    void play(final PlayListItem item) {
        if(item==curAdapter.getCurPlayItem()) return;
        updatePlayTime(curAdapter.getCurPlayItem());
        pool.clear();
        HttpUtil.getAsync("http://" + kikoPlayServer + "/api/subtitle?id=" + item.getMedia(), new ResponseHandler() {
            @Override
            public void onResponse(byte[] result) {
                if (result != null) {
                    try {
                        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(PlayActivity.this, Util.getUserAgent(PlayActivity.this, "Test"));
                        MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(Uri.parse("http://" + kikoPlayServer + "/media/" + item.getMedia()));
                        JSONObject obj=new JSONObject(new String(result));
                        String subtitleType=obj.getString("type");
                        if(subtitleType.isEmpty()) {
                            player.prepare(videoSource);
                        }
                        else
                        {
                            Format subtitleFormat = Format.createTextSampleFormat(null,
                                    subtitleType.equals("srt")?MimeTypes.APPLICATION_SUBRIP:MimeTypes.TEXT_SSA,
                                    Format.NO_VALUE,null);
                            MediaSource subtitleSource = new SingleSampleMediaSource.Factory(dataSourceFactory)
                                    .createMediaSource(Uri.parse("http://" + kikoPlayServer + "/sub/"+subtitleType+"/" + item.getMedia()), subtitleFormat, C.TIME_UNSET);
                            MergingMediaSource mergedSource = new MergingMediaSource(videoSource, subtitleSource);
                            player.prepare(mergedSource);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    selectedItem=item;
                    pool.loadDanmu(item.getPool(), loadDanmuCallBack);
                    //loadDanmu(item);
                    seekToLastPlayTime=(item.getPlayTimeState()==1);
                    player.setPlayWhenReady(true);
                    titleText.setText(item.getTitle());
                    curAdapter.setCurPlayItem(item);
                    //updateDanmuButton.setEnabled(true);
                    //updateDanmuButton.setText(getText(R.string.danmu_update));
                } else {
                    Snackbar.make(findViewById(R.id.playLayout), getText(R.string.subtitleInfoFailed), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    void initDanmu() {
        parser = new BaseDanmakuParser() {
            @Override
            protected IDanmakus parse() {
                return new Danmakus();
            }
        };
        danmuView.setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                danmuView.start();
            }

            @Override
            public void updateTimer(DanmakuTimer timer) {

            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {

            }

            @Override
            public void drawingFinished() {

            }
        });
        danmuView.enableDanmakuDrawingCache(true);
        danmuContext = DanmakuContext.create();
        danmuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN,3);//描边
        danmuContext.setDuplicateMergingEnabled(true);//重复合并
        danmuContext.setScrollSpeedFactor(1.4f);//弹幕滚动速度
        danmuContext.setDanmakuTransparency(0.6f);
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_LR, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        danmuContext.preventOverlapping(overlappingEnablePair);
        danmuView.prepare(parser,danmuContext);
        pool=new DanmuPool(player,danmuView,danmuContext,parser,this,kikoPlayServer);
        loadDanmuCallBack=new AsyncCallBack() {
            @Override
            public void onResponse(int state) {
                danmuFragment.updateCountInfo();
                if(state<0){
                    Snackbar.make(findViewById(R.id.playLayout), getText(R.string.loadDanmuFaildTip), Snackbar.LENGTH_LONG)
                            .setAction("Retry", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    pool.loadDanmu(selectedItem.getPool(), loadDanmuCallBack);
                                }
                            }).show();
                }
                if(pool.canLaunch()){
                    danmuLaunch.setVisibility(View.VISIBLE);
                } else {
                    danmuLaunch.setVisibility(View.GONE);
                }
            }
        };
    }
    void updatePlayTime(PlayListItem item){
        if(item==null) return;
        JSONObject obj=new JSONObject();
        try {
            obj.put("mediaId", item.getMedia());
            int curPos=(int)(player.getCurrentPosition()/1000);
            int duration=(int)(player.getDuration()/1000);
            obj.put("playTime", curPos);
            int state=0;
            if(curPos>15 && curPos<duration-15) state=1;
            else if(curPos>duration-15 || item.getPlayTimeState()==2) state=2;
            obj.put("playTimeState",state);
            curAdapter.setCurPlayTime(curPos,state);
            HttpUtil.postAsync("http://" + kikoPlayServer + "/api/updateTime",obj.toString(),null,5000,5000);
            setResult(1);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onPlayItemChanged(PlayListItem item) {
        play(item);
    }

    @Override
    public void showUpdateStatus(int state) {
        danmuFragment.updateCountInfo();
        if(state<0){
            Snackbar.make(findViewById(R.id.playLayout), getText(R.string.danmuUpdateFailed), Snackbar.LENGTH_LONG).show();
        }
    }
}
class OrientationHandler extends Handler {
    // WeakReference to the outer class's instance.
    private WeakReference<PlayActivity> mOuter;

    OrientationHandler(PlayActivity activity) {
        mOuter = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        PlayActivity outer = mOuter.get();
        if (outer != null) {
            outer.startRotation = -2;
            outer.mOrientationListener.enable();
        }
    }
}