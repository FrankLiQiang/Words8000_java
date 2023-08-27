package com.frank.words8000;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

public class MusicService extends Service {
    private MediaPlayer player;

    public MusicService() {}

    @Override
    public  IBinder onBind(Intent intent){
        return new MusicControl();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        player = new MediaPlayer();
    }

    class MusicControl extends Binder{

        public void play(Uri uri){
            try{
                if(player != null){
                    player.setOnPreparedListener(null);
                    player.stop();
                    player.reset();
                    player.release();
                    player = null;
                }
                player = new MediaPlayer();
                player.setAudioAttributes(new AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                player.setDataSource(getApplicationContext(), uri);
                PlaybackParams params = player.getPlaybackParams();
                player.setPlaybackParams(params);
                player.prepare();
                player.setOnErrorListener(null);
                player.setOnCompletionListener(mp -> {
                    mp.seekTo(0);//循环播放
                    mp.start();
                });
                player.start();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        public void pausePlay(){
            player.pause();
        }
        public void continuePlay(){
            player.start();
        }
        public void seekTo(int progress){
            player.seekTo(progress);
        }

        public MediaPlayer getMediaPlayer(){
            return player;
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if (player != null) {
            player.stop();
            player.reset();
            player.release();
            player = null;
        }
    }
}
