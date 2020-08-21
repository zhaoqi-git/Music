package com.example.music;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private SeekBar seekBar,seekBar1;
    private Spinner spinner;
    private ListView listView;
    private MediaPlayer player;
    private AudioManager audioManager;//音频管理引用，提供对音频的控制
    int currentVolume,maxVolume,minVolume;//当前音量和最大音量
    private ImageView begin,end;
    private List<String> data_list;
    private ArrayAdapter<String> arr_adapter;
    private List<Music> listmusic;
    private boolean ispause = false;
    private boolean isstop = false;
    private String currentposition;
    private int cposition=0;
    private TextView timesum,time0,playname;
    private SimpleDateFormat t = new SimpleDateFormat("mm:ss");//设置时间格式
    Handler handler ;
    String TAG = "Yinyue";

    //获取本地音乐文件信息
    public static List<Music> getMusicList(Context context){
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,null,null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        List<Music> musicList = new ArrayList<>();
        if (cursor.moveToFirst()){
            for (int i = 0 ; i<cursor.getCount() ; i++){
                Music m = new Music();
                Long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                // 歌手
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                // 歌的时长
                long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                // 歌的大小
                long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
                // 歌的绝对路径
                String uri = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                // 专辑
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                long album_id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                int ismusic = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
                if (ismusic != 0 && duration/(500*60)>=1){
                    m.setId(id);
                    m.setTitle(title);
                    m.setArtist(artist);
                    m.setDuration(duration);
                    m.setSize(size);
                    m.setUri(uri);
                    m.setAlbum(album);
                    m.setAlbum_id(album_id);
                    musicList.add(m);
                }
                cursor.moveToNext();
            }
        }
        return musicList;
    }

    /*************按键重写**************************************/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            seekBar1.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            seekBar1.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }else if (keyCode == KeyEvent.KEYCODE_BACK){
            System.exit(0);
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        begin = findViewById(R.id.begin);
        end = findViewById(R.id.end);
        findViewById(R.id.begin).setOnClickListener(this);
        findViewById(R.id.end).setOnClickListener(this);
        findViewById(R.id.last).setOnClickListener(this);
        findViewById(R.id.next).setOnClickListener(this);
        seekBar = findViewById(R.id.seekBar);
        seekBar1 = findViewById(R.id.seekBar1);
        spinner = findViewById(R.id.spinner);
        listView = findViewById(R.id.listview);
        timesum = findViewById(R.id.timesum);
        time0= findViewById(R.id.time0);
        playname = findViewById(R.id.playname);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }//获取存储权限

        /*************获得系统音频管理服务对象***************************************/
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        minVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekBar1.setMax(maxVolume);//设置拖动条最大长度
        seekBar1.setMin(minVolume);
        seekBar1.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        /*****************************歌曲条目***************************************/
        listmusic = getMusicList(MainActivity.this);
        final List <Map<String,Object>> list = new ArrayList<>();
        for (Iterator iterator = listmusic.iterator(); iterator.hasNext();){
            Map<String,Object> item = new HashMap<>();
            Music music = (Music) iterator.next();
            item.put("musicname",music.getTitle());
            item.put("name",music.getArtist());
            list.add(item);
        }
        //设置歌曲适配器
        SimpleAdapter simpleAdapter = new SimpleAdapter(this,list,R.layout.musicshow,
                new String[]{"musicname","name"},new int[]{R.id.musicname,R.id.name});
        listView.setAdapter(simpleAdapter);//加载适配器

        player =MediaPlayer.create(getApplication(), Uri.parse(listmusic.get(cposition).getUri()));

        /*****************************倍速***************************************/
        data_list = new ArrayList<String>();
        data_list.add("x1");
        data_list.add("x0.5");
        data_list.add("x1.5");
        data_list.add("x2");
        spinner.setPrompt("x1");
        //适配器
        arr_adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,data_list);
        //设置样式
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //加载适配器
        spinner.setAdapter(arr_adapter);
        /*********ListView条目点击事件*****************************************/
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cposition = position;
                currentposition = listmusic.get(position).getUri();
                playname.setText(listmusic.get(position).getTitle());
                if (player.isPlaying()){
                    player.stop();
                    begin.setImageResource(R.drawable.pause);
                    isstop = true;
                }
                player=MediaPlayer.create(MainActivity.this, Uri.parse(currentposition));
                seekBar.setMax(player.getDuration());//音频长度
                timesum.setText(t.format(player.getDuration()));
                //Toast.makeText(getApplication(),player.getDuration(),Toast.LENGTH_SHORT).show();
                player.start();
                ispause = true;
                begin.setImageResource(R.drawable.begin);
            }
        });
        /*****************进度条更新线程**************************/
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    //线程运行内容
                    if (player.isPlaying()){
                        seekBar.setProgress(player.getCurrentPosition());
                    }
                    try{
                        Thread.sleep(100);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    handler.obtainMessage(123).sendToTarget();
                }
            }
        }).start();
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 123){
                    time0.setText(t.format(player.getCurrentPosition()));
                }
            }
        };
        /*************下拉框条目选择********************************/
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        if (player.isPlaying())
                        {
                            player.setPlaybackParams(player.getPlaybackParams().setSpeed(1));
                        }else {
                            player.setPlaybackParams(player.getPlaybackParams().setSpeed(1));
                            player.pause();

                        }break;
                    case 1:
                        if (player.isPlaying())
                        {
                            player.setPlaybackParams(player.getPlaybackParams().setSpeed((float)0.5));
                        }else {
                            player.setPlaybackParams(player.getPlaybackParams().setSpeed((float)0.5));
                            player.pause();

                        }break;
                    case 2:
                        if (player.isPlaying())
                        {
                            player.setPlaybackParams(player.getPlaybackParams().setSpeed((float)1.5));
                        }else {
                            player.setPlaybackParams(player.getPlaybackParams().setSpeed((float)1.5));
                            player.pause();

                        }break;
                    case 3:
                        if (player.isPlaying())
                        {
                            player.setPlaybackParams(player.getPlaybackParams().setSpeed(2));
                        }else {
                            player.setPlaybackParams(player.getPlaybackParams().setSpeed(2));
                            player.pause();

                        }break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //播放进度拖动条监听事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser == true){
                    player.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser == true){
                    audioManager.setStreamVolume(3,progress,0);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.begin:
                if (ispause == false) {
                    if (player != null) {
                        start();
                    }
                } else {
                    player.pause();
                    ispause = false;
                    begin.setImageResource(R.drawable.pause);
                }
                break;
            case R.id.end:
                if (isstop == false) {
                    player.stop();
                    begin.setImageResource(R.drawable.pause);
                    isstop = true;
                } else {
                    cposition = -1;
                    player.start();
                    begin.setImageResource(R.drawable.begin);
                    isstop = false;
                }
                break;
            case R.id.last:
                player.stop();
                begin.setImageResource(R.drawable.pause);
                isstop = true;
                if (cposition > 0) {
                    cposition--;
                    player = MediaPlayer.create(this, Uri.parse(listmusic.get(cposition).getUri()));
                    start();
                } else {
                    cposition = listView.getCount() - 1;
                    cposition--;
                    player = MediaPlayer.create(this, Uri.parse(listmusic.get(cposition).getUri()));
                    start();
                }
                break;
            case R.id.next:
                player.stop();
                begin.setImageResource(R.drawable.pause);
                isstop = true;
                if (cposition == listView.getCount() - 1) {
                    cposition = -1;
                    cposition++;
                    player = MediaPlayer.create(this, Uri.parse(listmusic.get(cposition).getUri()));
                    start();
                } else {
                    cposition++;
                    player = MediaPlayer.create(this, Uri.parse(listmusic.get(cposition).getUri()));
                    start();
                }
                break;
        }
    }
        //释放资源
        @Override
        protected void onDestroy() {
            if (player.isPlaying()){
                player.stop();
            }
            player.release();
            super.onDestroy();
        }

        private void play(){
            cposition++;
            if (cposition>listView.getCount()-1)
                cposition=0;
            Log.d(TAG,"%d"+cposition);
            //player.release();
            player = MediaPlayer.create(this,Uri.parse(listmusic.get(cposition).getUri()));
            start();
        }

        public void start(){
            player.start();
            begin.setImageResource(R.drawable.begin);
            seekBar.setMax(player.getDuration());//音频长度
            timesum.setText(t.format(player.getDuration()));
            ispause = true;
            playname.setText(listmusic.get(cposition).getTitle());
        }
}