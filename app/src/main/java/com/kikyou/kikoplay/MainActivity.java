package com.kikyou.kikoplay;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.support.design.widget.Snackbar;

import com.kikyou.kikoplay.module.HttpUtil;
import com.kikyou.kikoplay.module.PlayListAdapter;
import com.kikyou.kikoplay.module.PlayListItem;
import com.kikyou.kikoplay.module.ResponseHandler;

public class MainActivity extends AppCompatActivity {

    String kikoPlayServer;
    PlayListAdapter playListAdapter;
    boolean exitConfirm=false;

    TextView emptyTextView;
    ListView playListView;
    SwipeRefreshLayout listViewSwipe;
    ImageButton connectBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.title_toolbar));

        SharedPreferences read = getSharedPreferences("config", MODE_PRIVATE);
        kikoPlayServer = read.getString("ServerAddress", "");
        playListAdapter=new PlayListAdapter(this);
        initViews();
    }
    void initViews(){
        emptyTextView=findViewById(R.id.emptyTextView);
        playListView=findViewById(R.id.playListView);
        playListView.setEmptyView(emptyTextView);
        listViewSwipe=findViewById(R.id.listSwipe);
        connectBtn=findViewById(R.id.connectBtn);
        playListView.setAdapter(playListAdapter);

        final SwipeRefreshLayout.OnRefreshListener refreshListener=new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                HttpUtil.getAsync("http://"+kikoPlayServer+"/api/playlist", new ResponseHandler() {
                    @Override
                    public void onResponse(byte[] result) {
                        if(result!=null) {
                            playListAdapter.setJsonData(new String(result));
                            if(playListAdapter.getCount()==0) emptyTextView.setText(getText(R.string.listEmpyt));
                        }
                        else {
                            emptyTextView.setText(getText(R.string.listError));
                        }
                        listViewSwipe.setRefreshing(false);
                    }
                });
            }
        };
        listViewSwipe.setOnRefreshListener(refreshListener);
        if(kikoPlayServer.isEmpty()){
            emptyTextView.setText(getText(R.string.listNotConnect));
        }
        else{
            listViewSwipe.setRefreshing(true);
            refreshListener.onRefresh();
        }
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editText = new EditText(MainActivity.this);
                editText.setText(kikoPlayServer);
                AlertDialog.Builder inputDialog = new AlertDialog.Builder(MainActivity.this);
                inputDialog.setTitle(R.string.connectDialogTitle).setView(editText);
                inputDialog.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                kikoPlayServer=editText.getText().toString();
                                SharedPreferences.Editor editor = getSharedPreferences("config", MODE_PRIVATE).edit();
                                editor.putString("ServerAddress", kikoPlayServer);
                                editor.apply();
                                listViewSwipe.setRefreshing(true);
                                refreshListener.onRefresh();
                            }
                        }).setNegativeButton("Cancel",null).show();
            }
        });
        playListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlayListItem item= (PlayListItem) playListAdapter.getItem(position);
                if(item.getMedia()==null){
                    playListAdapter.setForward(position);
                }
                else{
                    KikoPlayApp kApp=(KikoPlayApp) getApplication();
                    kApp.curPlayList=playListAdapter.getCurLevelItems();
                    kApp.curPlayItem=item;
                    startActivityForResult(new Intent(MainActivity.this,PlayActivity.class),1);
                }

            }
        });
        final GestureDetector playListSwipeDetector=new GestureDetector(this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e2.getX() - e1.getX() > playListView.getWidth()/5) {
                    playListAdapter.setBack();
                    return true;
                }
                return false;
            }
        });
        playListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return playListSwipeDetector.onTouchEvent(event);
            }
        });
    }
    @Override
    public void onBackPressed() {
        if(!playListAdapter.setBack()) {
            if(exitConfirm){
                super.onBackPressed();
            }
            else {
                Snackbar.make(findViewById(R.id.mainLayout), getText(R.string.exitConfirmTip), Snackbar.LENGTH_LONG)
                        .addCallback(new Snackbar.Callback(){
                            @Override public void onDismissed(Snackbar transientBottomBar, int event) {
                                super.onDismissed(transientBottomBar, event);
                                exitConfirm=false;
                            }
                        }).show();
                exitConfirm = true;
            }
        }
        else {
            exitConfirm = false;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==1){
            if(resultCode==1) playListAdapter.notifyDataSetChanged();
        }
    }

}
