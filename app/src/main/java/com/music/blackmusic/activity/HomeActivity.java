package com.music.blackmusic.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.music.blackmusic.R;
import com.music.blackmusic.adapter.HomeListViewAdapter;
import com.music.blackmusic.database.DBManager;
import com.music.blackmusic.entity.PlayListInfo;
import com.music.blackmusic.service.MusicPlayerService;
import com.music.blackmusic.util.Constant;
import com.music.blackmusic.util.MyApplication;
import com.music.blackmusic.util.MyMusicUtil;

import java.io.IOException;
import java.util.List;

public class HomeActivity extends PlayBarBaseActivity {
    private static final String TAG = HomeActivity.class.getName();
    private DBManager dbManager;
    private DrawerLayout mDrawerLayout;
    private NavigationView navView;
    private ImageView navHeadIv;
    private LinearLayout localMusicLl;
    private LinearLayout lastPlayLl;
    private LinearLayout myLoveLl;
    private LinearLayout myListTitleLl;
    private Toolbar toolbar;
    private TextView localMusicCountTv;
    private TextView lastPlayCountTv;
    private TextView myLoveCountTv;
    private TextView myPLCountTv;
    private ImageView myPLArrowIv;
    private ImageView myPLAddIv;
    private ListView listView;
    private HomeListViewAdapter adapter;
    private List<PlayListInfo> playListInfos;
    private int count;
    private boolean isOpenMyPL = false; //标识我的歌单列表打开状态
    private long exitTime = 0;
    private boolean isStartTheme = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        dbManager = DBManager.getInstance(HomeActivity.this);
        //上方栏toolbar
        toolbar = (Toolbar)findViewById(R.id.home_activity_toolbar);
        setSupportActionBar(toolbar);
        //获得首页整体id
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        //获取左边栏id
        navView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navView.getHeaderView(0);
        navHeadIv = (ImageView)headerView.findViewById(R.id.nav_head_bg_iv);
        ActionBar actionBar = getSupportActionBar();
        //先得到actionbar
        if (actionBar != null) {
            // 该类固定写法，规定了导航图标
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.drawer_menu);
        }
        //刷新日间夜间模式
        refreshNightModeTitle();

        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            //选中导航中的按钮时执行下面操作
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                mDrawerLayout.closeDrawers();
                switch (item.getItemId()){
                    //主题商城
                    case R.id.nav_theme:
                        isStartTheme = true;
                        Intent intentTheme = new Intent(HomeActivity.this,ThemeActivity.class);
                        startActivity(intentTheme);
                        break;
                        //日夜
                    case R.id.nav_night_mode:
                        int preTheme = 0;
                        if(MyMusicUtil.getNightMode(HomeActivity.this)){
                            //当前为夜间模式，则恢复之前的主题
                            MyMusicUtil.setNightMode(HomeActivity.this,false);
                            preTheme = MyMusicUtil.getPreTheme(HomeActivity.this);
                            MyMusicUtil.setTheme(HomeActivity.this,preTheme);
                        }else {
                            //当前为白天模式，则切换到夜间模式
                            MyMusicUtil.setNightMode(HomeActivity.this,true);
                            MyMusicUtil.setTheme(HomeActivity.this,ThemeActivity.THEME_SIZE-1);
                        }
                        recreate();
                        refreshNightModeTitle();
                        break;
                        //关于
                    case R.id.nav_about_me:
                        Intent aboutTheme = new Intent(HomeActivity.this,AboutActivity.class);
                        startActivity(aboutTheme);
                        break;
                        //退出同时停止播放
                    case R.id.nav_logout:
                        finish();
                        Intent intentBroadcast = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                        //用来传值的函数
                        intentBroadcast.putExtra(Constant.COMMAND, Constant.COMMAND_RELEASE);
                        //向service发送广播
                        sendBroadcast(intentBroadcast);
                        //跳转
                        Intent stopIntent = new Intent(HomeActivity.this,MusicPlayerService.class);
                        stopService(stopIntent);
                        break;
                }
                return true;
            }
        });
        init();
        Intent startIntent = new Intent(HomeActivity.this,MusicPlayerService.class);
        startService(startIntent);
    }

     //更新按钮模式（日间/夜间）
    private void refreshNightModeTitle(){
        if (MyMusicUtil.getNightMode(HomeActivity.this)){
            navView.getMenu().findItem(R.id.nav_night_mode).setTitle("日间模式");
        }else {
            navView.getMenu().findItem(R.id.nav_night_mode).setTitle("夜间模式");
        }
    }
   //暂停保存状态（被其他覆盖时），记录下显示的每首歌曲数量
    @Override
    protected void onResume() {
        super.onResume();
        count = dbManager.getMusicCount(Constant.LIST_ALLMUSIC);
        localMusicCountTv.setText(count + "");
        count = dbManager.getMusicCount(Constant.LIST_LASTPLAY);
        lastPlayCountTv.setText(count + "");
        count = dbManager.getMusicCount(Constant.LIST_MYLOVE);
        myLoveCountTv.setText(count + "");
        count = dbManager.getMusicCount(Constant.LIST_MYPLAY);
        myPLCountTv.setText("(" + count + ")");
        adapter.updateDataList();
    }

    private void init(){
        //匹配所有id
        localMusicLl = (LinearLayout) findViewById(R.id.home_local_music_ll);
        lastPlayLl = (LinearLayout) findViewById(R.id.home_recently_music_ll);
        myLoveLl = (LinearLayout) findViewById(R.id.home_my_love_music_ll);
        myListTitleLl = (LinearLayout) findViewById(R.id.home_my_list_title_ll);
        listView = (ListView)findViewById(R.id.home_my_list_lv);
        localMusicCountTv = (TextView) findViewById(R.id.home_local_music_count_tv);
        lastPlayCountTv = (TextView) findViewById(R.id.home_recently_music_count_tv);
        myLoveCountTv = (TextView) findViewById(R.id.home_my_love_music_count_tv);
        myPLCountTv = (TextView) findViewById(R.id.home_my_list_count_tv);
        myPLArrowIv = (ImageView) findViewById(R.id.home_my_pl_arror_iv);
        myPLAddIv = (ImageView) findViewById(R.id.home_my_pl_add_iv);
      //跳转到本地音乐
        localMusicLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this,LocalMusicActivity.class);
                startActivity(intent);
            }
        });
      //跳转到最近音乐
        lastPlayLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this,LastMyloveActivity.class);
                intent.putExtra(Constant.LABEL,Constant.LABEL_LAST);
                startActivity(intent);
            }
        });
     //跳转到我的喜爱
        myLoveLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this,LastMyloveActivity.class);
                intent.putExtra(Constant.LABEL,Constant.LABEL_MYLOVE);
                startActivity(intent);
            }
        });
        //得到歌单
        playListInfos = dbManager.getMyPlayList();
        adapter = new HomeListViewAdapter(playListInfos,this,dbManager);
        listView.setAdapter(adapter);
        myPLAddIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //添加歌单
                final AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                View view = LayoutInflater.from(HomeActivity.this).inflate(R.layout.dialog_create_playlist,null);
                final EditText playlistEt = (EditText)view.findViewById(R.id.dialog_playlist_name_et);
                builder.setView(view);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = playlistEt.getText().toString();
                        if (TextUtils.isEmpty(name)) {
                            Toast.makeText(HomeActivity.this,"请输入歌单名",Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dbManager.createPlaylist(name);
                        dialog.dismiss();
                        adapter.updateDataList();
                    }
                });

                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();//配置好后再builder show
            }
        });
        myListTitleLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //展现我的歌单
                if (isOpenMyPL){
                    isOpenMyPL = false;
                    myPLArrowIv.setImageResource(R.drawable.arrow_right);
                    listView.setVisibility(View.GONE);
                }else {
                    isOpenMyPL = true;
                    myPLArrowIv.setImageResource(R.drawable.arrow_down);
                    listView.setVisibility(View.VISIBLE);
                    playListInfos = dbManager.getMyPlayList();
                    adapter = new HomeListViewAdapter(playListInfos,HomeActivity.this,dbManager);
                    listView.setAdapter(adapter);
                }
            }
        });
    }
       //更新播放列表歌曲数量
    public void updatePlaylistCount(){
        count = dbManager.getMusicCount(Constant.LIST_MYPLAY);
        myPLCountTv.setText("(" + count + ")");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");

    }
    //改变主题时更新主题
    @Override
    protected void onPause() {
        super.onPause();
        if (isStartTheme){
            HomeActivity.this.finish();
        }
        isStartTheme = false;
    }
    //手机回退键回退到桌面功能
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
            if((System.currentTimeMillis()-exitTime) > 2000){
                Toast.makeText(getApplicationContext(), "再按一次切换到桌面", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                moveTaskToBack(true);
            }
            return true;
        }
        finish();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}