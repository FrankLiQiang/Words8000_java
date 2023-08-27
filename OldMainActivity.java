package com.frank.words8000;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.frank.words8000.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int TIME_DIFF = 2000;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> launcher;
    public static Uri _uri;
    private String fileName, folderName;
    private long _exitTime = 0;
    private static MusicService.MusicControl musicControl;
    private MyServiceConn conn;
    private static boolean isPlay = true;
    private ArrayList<Integer> listTime = new ArrayList<>();
    private int minTime, maxTime;

    private static SeekBar sb_time;
    private Timer thisTimer;
    private TimerTask thisTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

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

        findViewById(R.id.btn_play).setOnClickListener(this);
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_end).setOnClickListener(this);
        findViewById(R.id.btn_reset).setOnClickListener(this);
        findViewById(R.id.ll_pause).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbind(false);
    }

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - _exitTime) > TIME_DIFF) {
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

        return super.onOptionsItemSelected(item);
    }

    private void addFlag(int currentTime) {
        if (listTime.isEmpty()) {
            listTime.add(currentTime);
            minTime = currentTime;
            maxTime = currentTime;
        } else {
            if (currentTime < minTime - TIME_DIFF) {
                minTime = currentTime;
                listTime.add(0, currentTime);
            } else if (currentTime > maxTime + TIME_DIFF) {
                maxTime = currentTime;
                listTime.add(currentTime);
            } else {
                for (int i = 0; i < listTime.size() - 1; i++) {
                    if (listTime.get(i) + TIME_DIFF < currentTime
                        && currentTime < listTime.get(i + 1) - TIME_DIFF) {
                        listTime.add(i, currentTime);
                        break;
                    }
                }
            }
        }
    }

    private int getNextTime(boolean isNext, int currentTime) {
        if (listTime.isEmpty()) {
            return -1;
        }

        if (!isPlay && listTime.size() > 1) {
            maxTime = listTime.get(listTime.size() - 2);
            listTime.remove(listTime.size() - 1);
        }
        if (currentTime < minTime) {
            if (isNext) {
                return listTime.get(0);
            }
            return 0;
        }
        if (currentTime > maxTime) {
            return listTime.get(listTime.size() - 1);
        }

        currentTime = isNext ? currentTime + TIME_DIFF : currentTime - TIME_DIFF;
//        Log.d("AAA", "currentTime + " + currentTime);
        for (int i = 0; i < listTime.size() - 1; i++) {
            if (listTime.get(i) < currentTime && currentTime < listTime.get(i + 1)) {
                if (isNext) {
                    return listTime.get(i + 1);
                } else {
//                    for (int j = 0; j < listTime.size(); j++) {
//                        Log.d("AAA", "listTime + (" + j + ") = " + listTime.get(j));
//                    }
                    return listTime.get(i);
                }
            }
        }
        return -1;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_play) {
            if (isPlay) {
                musicControl.pausePlay();
                ((ImageView) findViewById(R.id.iv_play)).setImageResource(R.drawable.end_play_new);
                ((TextView) findViewById(R.id.tv_play)).setText(R.string.play_img);
                addFlag(musicControl.getMediaPlayer().getCurrentPosition());
            } else {
                musicControl.continuePlay();
                ((ImageView) findViewById(R.id.iv_play)).setImageResource(R.drawable.start_play_new);
                ((TextView) findViewById(R.id.tv_play)).setText(R.string.pause_img);
            }
            isPlay = !isPlay;
        } else if (v.getId() == R.id.btn_start) {
            int time = getNextTime(false, musicControl.getMediaPlayer().getCurrentPosition());
            if (time > -1) {
                musicControl.seekTo(time);
            }
            if (!isPlay) {
                findViewById(R.id.btn_play).performClick();
            }
        } else if (v.getId() == R.id.btn_end) {
            int time = getNextTime(true, musicControl.getMediaPlayer().getCurrentPosition());
            if (time > -1) {
                musicControl.seekTo(time);
            }
            if (!isPlay) {
                findViewById(R.id.btn_play).performClick();
            }
        } else if (v.getId() == R.id.btn_reset) {
            listTime.clear();
            musicControl.seekTo(0);
            if (!isPlay) {
                findViewById(R.id.btn_play).performClick();
            }
        } else if (v.getId() == R.id.ll_pause) {
            findViewById(R.id.btn_play).performClick();
        }
    }

    private void reset() {
        listTime.clear();
        String pathAndName = _uri.getPath();
        String tmp;
        int start = pathAndName.lastIndexOf("/");
        int end = pathAndName.lastIndexOf(".");
        if (start != -1 && end != -1) {
            fileName = pathAndName.substring(start + 1, end);
            tmp = pathAndName.substring(0, start);
            end = start;
            start = tmp.lastIndexOf("/");
            folderName = pathAndName.substring(start + 1, end);
            //getSupportActionBar().setDisplayShowTitleEnabled(true);  // この行を追加
            getSupportActionBar().setTitle(fileName);
        }

        musicControl.play(_uri);
        minTime = 0;
        maxTime = musicControl.getMediaPlayer().getDuration();
        sb_time.setMax(maxTime);
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
            if (msg.what == 0) {
                sb_time.setProgress(musicControl.getMediaPlayer().getCurrentPosition());
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