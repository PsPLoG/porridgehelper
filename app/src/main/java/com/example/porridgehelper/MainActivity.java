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
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<Double> dataList = new ArrayList<>();
    ArrayList<Double> dataList_p = new ArrayList<>();
    MediaRecorder recorder;
    AudioRecord audioRecord;
    String filename;
    MediaPlayer player;
    static boolean flag = true;
    int position = 0; // 다시 시작 기능을 위한 현재 재생 위치 확인 변수
    int time = 0;
    int bufferSize = 0;

    int count = 0;
    int THRE = 5; // 1-2단계
    int THRE2 = 10; // 3단계
    boolean data_type = true;

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


        findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = true;
                startNoiseLevel();

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

    private double zcr(short[] x, double buffersize) {
        double z = 0;
        for (int i = 0; i < buffersize - 1; i++) {
            if (x[i] * x[i + 1] < 0)
                z++;
        }
        Log.e("ww", "z:" + z + "buffer" + buffersize);
        return z / buffersize;
    }

    private double power(short[] x, double buffersize) {
        double z = 0;
        for (int i = 0; i < buffersize - 1; i++) {
            x[i] *= x[i];
        }
        double avg = 0;
        for (int i = 0; i < buffersize - 1; i++) {
            avg += x[i];
        }
        avg = avg / buffersize;

        return Math.log(avg);
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
        else if (audioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING) {
            return;
        }
        data_type = true;
        audioRecord.startRecording();

        final Handler mHandler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //------------------------------------------------------
                // 죽상태에 따른 처리부분
                //------------------------------------------------------
                int state = getNoiseLevel(data_type); // true : power, false : zcr

                if (state == 0) { //음수
                    count--;
                } else if (state == 1) { //양수
                    count++;
                }
                if (data_type) {
                    if (Math.abs(count) >= THRE) {
                        if (count < 0) { // 양수 --> 음수
                            Toast.makeText(getApplicationContext(), "2단계", Toast.LENGTH_SHORT).show();
                            data_type = false;
                            count = 0;
                        } else if (count > 0) { // 음수 --> 양수
                            Toast.makeText(getApplicationContext(), "1단계", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                else{
                    if (Math.abs(count) >= THRE2){
                        Toast.makeText(getApplicationContext(), "3단계", Toast.LENGTH_SHORT).show();
                    }
                }
                Log.d("sound ", "state" + state);
                //------------------------------------------------------

                if (flag)
                    mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.post(runnable);

    }

    public int getNoiseLevel(boolean type) {
        short data[] = new short[bufferSize];
        double average = 0.0;
        //recording data;
        audioRecord.read(data, 0, bufferSize);


        dataList.add(zcr(data, bufferSize));
        dataList_p.add(power(data, bufferSize));
        int avg = data[0];
        for (int i = 1; i < data.length; i++) {
            if (i % 100 == 0) {
                //dataList.add(avg/100);
                avg = 0;
            }
            avg += data[i];
        }
        String zcr = "";
        String power = "";
        for (int i = dataList.size() - 10; i < dataList.size(); i++) {
            if (i < 0)
                continue;
            zcr += dataList.get(i) + " ";
            power += dataList_p.get(i) + " ";

        }
        Log.d("asd", dataList.size() + "");
        Log.d("sound_zcr__", zcr);
        Log.d("sound_power", power);

        //----------------------------------------------------------
        // 죽상태를 반환해주는 코드부분
        //----------------------------------------------------------
        double S1 = 0;
        double B1 = 0;

        if (type) {
            if (dataList_p.size() < 20)
                return 0;
            for (int i = dataList_p.size() - 20; i < dataList_p.size() - 10; i++) {
                B1 += dataList_p.get(i);           //i번째 시간에서 power값
                S1 += dataList_p.get(i + 10);
            }

            double R = S1 - B1;
            if (R < 0)
                return 0; // ('---------- PHASE 1 ----------')
            else if (R > 0) {
                return 1; // ('---------- PHASE 2 ----------')
            }
        } else { // zcr값 사용
            if (dataList.size() < 20)
                return 0;
            for (int i = dataList.size() - 20; i < dataList.size() - 10; i++) {
                B1 += dataList.get(i);           //i번째 시간에서 power값
                S1 += dataList.get(i + 10);
            }

            double R = S1 - B1;
            if (R < 0)
                return 0; // ('---------- PHASE 1 ----------')
            else if (R > 0) {
                return 1; // ('---------- PHASE 2 ----------')
            }
        }
        //---------------------------------------------------------=
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