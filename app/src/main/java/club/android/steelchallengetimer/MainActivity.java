package club.android.steelchallengetimer;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;


import java.text.SimpleDateFormat;


public class MainActivity extends Activity implements Runnable, View.OnClickListener {

    private long startTime;
    // 10 msec order
    private int period = 10;
    private int derayTime = 2000;
    private int SAMPLE_RATE = 44100;
    public static final int CALL_RESULT_CODE = 100;
    private double baseValue;
    private TextView timerText;
    private Button startButton, stopButton, configButton;

    private Thread thread = null;
    private final Handler handler = new Handler();
    private volatile boolean stopRun = false;
    private int bufferSize;
    private AudioRecord audioRecord;
    private SimpleDateFormat dataFormat = new SimpleDateFormat("mm:ss.SS");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerText = (TextView) findViewById(R.id.timer);
        timerText.setText(dataFormat.format(0));
        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);
        configButton = (Button) findViewById(R.id.config_button);

        startButton.setOnClickListener(this);
        // @todo あとで消す
        stopButton.setOnClickListener(this);
        configButton.setOnClickListener(this);
        stopButton.setEnabled(false);
        configButton.setEnabled(false);
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        baseValue = 15.0;
        audioRecord.stop();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.start_button) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            configButton.setEnabled(true);
            stopRun = false;
            audioRecord.startRecording();
            thread = new Thread(this);
            try {
                Thread.sleep(derayTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stopRun = true;
            }
            ToneGenerator toneGenerator
                    = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT);
            thread.start();
            startTime = System.currentTimeMillis();
        } else if (v.getId() == R.id.stop_button) {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            configButton.setEnabled(true);
            stopRun = true;
            thread = null;
        } else if (v.getId() == R.id.config_button){
            // 設定画面の呼び出し用
            Intent intent = new Intent(this, SubActivity.class);
            intent.putExtra("baseValue", baseValue);
            // startActivityで起動する
            startActivityForResult(intent, CALL_RESULT_CODE);
        }
    }

    @Override
    public void run() {
        // startが押された時2秒間待つ
        short[] buffer = new short[bufferSize];
        while (!stopRun) {
            // sleep: period msec
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stopRun = true;
            }
            int read = audioRecord.read(buffer, 0, bufferSize);
            if (read < 0) {
                throw new IllegalStateException();
            }

            int maxValue = 0;
            for (int i = 0; i < read; i++) {
                maxValue = Math.max(maxValue, buffer[i]);
            }

            final double db = 20.0 * Math.log10(maxValue / baseValue);
            System.out.println(db);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    long endTime = System.currentTimeMillis();
                    // カウント時間 = 経過時間 - 開始時間
                    long diffTime = (endTime - startTime);

                    timerText.setText(dataFormat.format(diffTime));
                }
            });
            // @todo dbの値の決め方とifの条件は調整する
            if(db > 68){
                audioRecord.stop();
                stopRun = true;
                thread = null;
            }
        }
    }

}
