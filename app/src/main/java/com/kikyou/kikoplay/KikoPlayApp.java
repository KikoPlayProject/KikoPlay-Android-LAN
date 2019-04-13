package com.kikyou.kikoplay;

import android.app.Application;

import com.kikyou.kikoplay.module.PlayListItem;

import java.util.List;

public class KikoPlayApp extends Application {
    List<PlayListItem> curPlayList;
    PlayListItem curPlayItem;
}
