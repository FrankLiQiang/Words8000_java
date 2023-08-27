package com.frank.words8000;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.frank.words8000.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> launcher;
    public static Uri _uri;
    private String fileName;
    private long _exitTime = 0;
    private static MusicService.MusicControl musicControl;
    private MyServiceConn conn;
    private static boolean isPlay = true;
    private static ArrayList<Integer> listTime = new ArrayList<>();

    private static SeekBar sb_time;
    private Timer thisTimer;
    private TimerTask thisTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (data == null) return;

            _uri = data.getData();
            reset();
        });
        Intent intent2 = new Intent(this, MusicService.class);
        conn = new MyServiceConn();
        bindService(intent2, conn, BIND_AUTO_CREATE);

        sb_time = findViewById(R.id.ll_seekBar);
        sb_time.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicControl.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_end).setOnClickListener(this);
        findViewById(R.id.ll_pause).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbind(false);
    }

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - _exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), R.string.once_more, Toast.LENGTH_SHORT).show();
            _exitTime = System.currentTimeMillis();
        } else {
            try {
                unbind(false);
            } catch (Exception e) {
                e.toString();
            }
            finish();
            System.exit(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setDataAndType(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*");
            launcher.launch(intent);
            return true;
        }
        if (id == R.id.action_replay) {
            listTime.clear();
            musicControl.seekTo(0);
            if (!isPlay) {
                findViewById(R.id.ll_pause).performClick();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_pause) {
            if (isPlay) {
                musicControl.pausePlay();
                ((ImageView) findViewById(R.id.iv_play)).setImageResource(R.drawable.end_play_new);
                ((TextView) findViewById(R.id.tv_play)).setText(R.string.play_img);
            } else {
                musicControl.continuePlay();
                ((ImageView) findViewById(R.id.iv_play)).setImageResource(R.drawable.start_play_new);
                ((TextView) findViewById(R.id.tv_play)).setText(R.string.pause_img);
            }
            isPlay = !isPlay;
        } else if (v.getId() == R.id.btn_start) {
            listTime.add(musicControl.getMediaPlayer().getCurrentPosition());
            if (listTime.size() == 2) {
                findViewById(R.id.btn_end).setBackground (getDrawable(R.drawable.button_bg2));
                int time = listTime.get(0);
                musicControl.seekTo(time);
            }
            if (!isPlay) {
                findViewById(R.id.ll_pause).performClick();
            }
        } else if (v.getId() == R.id.btn_end) {
            if(listTime.size() == 2) {
                findViewById(R.id.btn_end).setBackgroundColor (0xFF6200EE);
                listTime.remove(0);
                int time = listTime.get(0);
                musicControl.seekTo(time);
            }
            if (!isPlay) {
                findViewById(R.id.ll_pause).performClick();
            }
        }
    }

    private void reset() {
        listTime.clear();
        String pathAndName = _uri.getPath();
        int start = pathAndName.lastIndexOf("/");
        int end = pathAndName.lastIndexOf(".");
        if (start != -1 && end != -1) {
            fileName = pathAndName.substring(start + 1, end);
            getSupportActionBar().setTitle(fileName);
        }

        musicControl.play(_uri);
        sb_time.setMax(musicControl.getMediaPlayer().getDuration());
        addTimer();
        findViewById(R.id.ll_buttons).setVisibility(View.VISIBLE);
        findViewById(R.id.ll_pause).setVisibility(View.VISIBLE);
        sb_time.setVisibility(View.VISIBLE);
    }

    static class MyServiceConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicControl = (MusicService.MusicControl) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    private void unbind(boolean isUnbind) {
        if (!isUnbind) {
            musicControl.pausePlay();
            unbindService(conn);
        }
    }

    private static Handler handler=new Handler() {
        @Override
        public void handleMessage(Message msg) {
            sb_time.setProgress(musicControl.getMediaPlayer().getCurrentPosition());
            if(listTime.size() == 2) {
                if (musicControl.getMediaPlayer().getCurrentPosition() > listTime.get(1)) {
                    int time = listTime.get(0);
                    musicControl.seekTo(time);
                }
            }
        }
    };

    public void addTimer() {
        thisTimer = new Timer();
        thisTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    //TODO
                }
            }
        };
        thisTimer.schedule(thisTask, 0, 20);
    }
}