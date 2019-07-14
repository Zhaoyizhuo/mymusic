package com.music.blackmusic.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

import com.music.blackmusic.database.DBManager;
import com.music.blackmusic.fragment.PlayBarFragment;
import com.music.blackmusic.util.Constant;
import com.music.blackmusic.util.MyMusicUtil;
import com.music.blackmusic.util.UpdateUIThread;

import java.io.File;


public class PlayerManagerReceiver extends BroadcastReceiver {

    private static final String TAG = PlayerManagerReceiver.class.getName();
    public static final String ACTION_UPDATE_UI_ADAPTER = "com.music.blackmusic.receiver.PlayerManagerReceiver:action_update_ui_adapter_broad_cast";
    private MediaPlayer mediaPlayer;
    private DBManager dbManager;
    public static int status = Constant.STATUS_STOP;
    private int playMode;
    private int threadNumber;
    private Context context;

    public PlayerManagerReceiver() {
    }

    public PlayerManagerReceiver(Context context) {
        super();
        this.context = context;
        dbManager = DBManager.getInstance(context);
        mediaPlayer = new MediaPlayer();
        Log.d(TAG, "create");
        initMediaPlayer();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int cmd = intent.getIntExtra(Constant.COMMAND,Constant.COMMAND_INIT);
        Log.d(TAG, "cmd = " + cmd);
        //识别命令类型
        switch (cmd) {
            case Constant.COMMAND_INIT:	//已经在创建的时候初始化了，可以撤销了
                Log.d(TAG, "COMMAND_INIT");
                break;
            case Constant.COMMAND_PLAY:
                Log.d(TAG, "COMMAND_PLAY");
                status = Constant.STATUS_PLAY;
                String musicPath = intent.getStringExtra(Constant.KEY_PATH);
                if (musicPath!=null) {
                    playMusic(musicPath);
                }else {
                    mediaPlayer.start();
                }
                break;
            case Constant.COMMAND_PAUSE:
                mediaPlayer.pause();
                status = Constant.STATUS_PAUSE;
                break;
            case Constant.COMMAND_STOP: //本程序停止状态都是删除当前播放音乐触发
                NumberRandom();
                status = Constant.STATUS_STOP;
                if(mediaPlayer!=null) {
                    mediaPlayer.stop();
                }
                initStopOperate();
                break;
            case Constant.COMMAND_PROGRESS://拖动进度
                int curProgress = intent.getIntExtra(Constant.KEY_CURRENT, 0);
                //异步的，可以设置完成监听来获取真正定位完成的时候
                mediaPlayer.seekTo(curProgress);
                break;
            case Constant.COMMAND_RELEASE:
                NumberRandom();
                status = Constant.STATUS_STOP;
                if(mediaPlayer!=null) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                break;
        }
        UpdateUI();
    }

    private void initStopOperate(){
        MyMusicUtil.setShared(Constant.KEY_ID,dbManager.getFirstId(Constant.LIST_ALLMUSIC));
    }
    //播放音乐
    private void playMusic(String musicPath) {
        NumberRandom();
        if (mediaPlayer!=null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "playMusic onCompletion: ");
                NumberRandom();				//切换线程
                onComplete();     //调用音乐切换模块，进行相应操作
                UpdateUI(); 				//更新界面
            }
        });

        try {
            File file = new File(musicPath);
            if(!file.exists()){
                Toast.makeText(context,"歌曲文件不存在，请重新扫描",Toast.LENGTH_SHORT).show();
                MyMusicUtil.playNextMusic(context);
                return;
            }
            mediaPlayer.setDataSource(musicPath);   //设置MediaPlayer数据源
            mediaPlayer.prepare();
            mediaPlayer.start();

            new UpdateUIThread(this, context, threadNumber).start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    //取一个（0，100）之间的不一样的随机数
    private void NumberRandom() {
        int count;
        do {
            count =(int)(Math.random()*100);
        } while (count == threadNumber);
        threadNumber = count;
    }
    //播放结束执行下一首
    private void onComplete() {
        MyMusicUtil.playNextMusic(context);
    }
    //更新界面
    private void UpdateUI() {
        Intent playBarintent = new Intent(PlayBarFragment.ACTION_UPDATE_UI_PlayBar);    //接收广播为MusicUpdateMain
        playBarintent.putExtra(Constant.STATUS, status);
        context.sendBroadcast(playBarintent);

        Intent intent = new Intent(ACTION_UPDATE_UI_ADAPTER);    //接收广播为所有歌曲列表的adapter
        context.sendBroadcast(intent);

    }


    private void initMediaPlayer() {

        NumberRandom(); // 改变线程号,使旧的播放线程停止

        int musicId = MyMusicUtil.getIntShared(Constant.KEY_ID);
        int current = MyMusicUtil.getIntShared(Constant.KEY_CURRENT);
        Log.d(TAG, "initMediaPlayer musicId = " + musicId);

        // 如果是没取到当前正在播放的音乐ID，则从数据库中获取第一首音乐的播放信息初始化
        if (musicId == -1) {
            return;
        }
        String path = dbManager.getMusicPath(musicId);
        if (path == null) {
            Log.e(TAG, "initMediaPlayer: path == null");
            return;
        }
        if (current == 0) {
            status = Constant.STATUS_STOP; // 设置播放状态为停止
        }else {
            status = Constant.STATUS_PAUSE; // 设置播放状态为暂停
        }
        Log.d(TAG, "initMediaPlayer status = " + status);
        MyMusicUtil.setShared(Constant.KEY_ID,musicId);
        MyMusicUtil.setShared(Constant.KEY_PATH,path);

        UpdateUI();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public int getThreadNumber() {
        return threadNumber;
    }
}