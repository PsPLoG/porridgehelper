package com.example.porridgehelper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    MediaRecorder recorder;
    AudioRecord audioRecord;
    String filename;
    MediaPlayer player;
    static boolean flag = true;
    int position = 0; // 다시 시작 기능을 위한 현재 재생 위치 확인 변수
    int time = 0;
    int bufferSize = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionCheck();

        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, "recorded.mp4");
        filename = file.getAbsolutePath();
        Log.d("MainActivity", "저장할 파일 명 : " + filename);

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playAudio();
            }
        });

        findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseAudio();
            }
        });

        findViewById(R.id.restart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resumeAudio();
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAudio();
            }
        });

        final Handler mHandler = new Handler();
        findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = true;
                startNoiseLevel();
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {

                        Log.d("asd", "loading" + getNoiseLevel());
                        if (flag)
                            mHandler.postDelayed(this, 1000);
                    }
                };
                mHandler.post(runnable);
            }
        });

        findViewById(R.id.recordStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioRecord.stop();
                audioRecord.release();
                //stopRecording();
                flag = false;
            }
        });
    }

    private double zcr(short[] x, int buffersize) {
        int z = 0;
        for (int i = 0; i < buffersize - 1; i++) {
            if (x[i] * x[i + 1] < 0)
                z++;
        }
        return z / buffersize;
    }

    private void recordAudio() {
        recorder = new MediaRecorder();
        /* 그대로 저장하면 용량이 크다.
         * 프레임 : 한 순간의 음성이 들어오면, 음성을 바이트 단위로 전부 저장하는 것
         * 초당 15프레임 이라면 보통 8K(8000바이트) 정도가 한순간에 저장됨
         * 따라서 용량이 크므로, 압축할 필요가 있음 */
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 어디에서 음성 데이터를 받을 것인지
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 압축 형식 설정
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(filename);

        try {
            recorder.prepare();
            recorder.start();
            Toast.makeText(this, "녹음 시작됨.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String TAG = "test";
    public static double REFERENCE = 0.00002;

    public void startNoiseLevel() {
        Log.e(TAG, "start new recording process");
        bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.e(TAG, bufferSize + "");
        if (audioRecord == null)
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    16000, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        audioRecord.startRecording();

    }

    public double getNoiseLevel() {
        short data[] = new short[bufferSize];
        double average = 0.0;
        //recording data;
        audioRecord.read(data, 0, bufferSize);
        String tmp ="";
        for(int i=0;i<data.length;i++)
        {
            tmp+= data[i]+" ";
        }
        Log.d("asd",data.length+"");
        Log.d("asd",tmp);
//        for (short s : data) {
//            if (s > 0) {
//                average += Math.abs(s);
//            } else {
//                bufferSize--;
//            }
//        }
//        //x=max;
//        double x = average / bufferSize;
//        Log.e(TAG, "" + x);
//        double db = 0;
//        if (x == 0) {
//            Log.e(TAG, "error x=0");
//            return db;
//        }
//        // calculating the pascal pressure based on the idea that the max amplitude (between 0 and 32767) is
//        // relative to the pressure
//        double pressure = x / 51805.5336; //the value 51805.5336 can be derived from asuming that x=32767=0.6325 Pa and x=1 = 0.00002 Pa (the reference value)
//        Log.d(TAG, "x=" + pressure + " Pa");
//        db = (20 * Math.log10(pressure / REFERENCE));
//        Log.d(TAG, "db=" + db);
//        if (db > 0) {
//            Log.e(TAG, "error x=0");
//        }
        return 0;
    }


    private void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            Toast.makeText(this, "녹음 중지됨.", Toast.LENGTH_SHORT).show();
        }
    }

    private void playAudio() {
        try {
            closePlayer();

            player = new MediaPlayer();
            player.setDataSource(filename);
            player.prepare();
            player.start();

            Toast.makeText(this, "재생 시작됨.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pauseAudio() {
        if (player != null) {
            position = player.getCurrentPosition();
            player.pause();

            Toast.makeText(this, "일시정지됨.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeAudio() {
        if (player != null && !player.isPlaying()) {
            player.seekTo(position);
            player.start();

            Toast.makeText(this, "재시작됨.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudio() {
        if (player != null && player.isPlaying()) {
            player.stop();

            Toast.makeText(this, "중지됨.", Toast.LENGTH_SHORT).show();
        }
    }

    public void closePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    public void setTimeTextView(int second) {
        ((TextView) findViewById(R.id.mm)).setText("" + second / 60);
        ((TextView) findViewById(R.id.ss)).setText("" + second % 60);
    }


    public void permissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
        }
    }
}