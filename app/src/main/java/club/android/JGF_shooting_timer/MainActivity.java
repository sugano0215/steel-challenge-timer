package club.android.JGF_shooting_timer;

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
    private long endTime;
    private long diffTime;
    // 10 msec order
    private int period = 1;
    private int derayTime = 2000;
    private int SAMPLE_RATE = 22050;
    public static final int CALL_RESULT_CODE = 100;
    private double baseValue;
    private int Value = 55;
    private TextView timerText;
    private Button startButton, stopButton, configButton, resetButton;

    private Thread thread = null;
    private final Handler handler = new Handler();
    private volatile boolean stopRun = false;
    private int bufferSize;
    private AudioRecord audioRecord;
    private SimpleDateFormat dataFormat = new SimpleDateFormat("mm:ss.SSS");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerText = (TextView) findViewById(R.id.timer);
        timerText.setText(dataFormat.format(0));
        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);
        configButton = (Button) findViewById(R.id.config_button);
        resetButton = (Button) findViewById(R.id.reset_button);

        startButton.setOnClickListener(this);
        // @todo あとで消す
        stopButton.setOnClickListener(this);
        configButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        stopButton.setEnabled(false);
        resetButton.setEnabled(true);
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        baseValue = 12.0;
        audioRecord.stop();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.start_button) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            configButton.setEnabled(false);
            resetButton.setEnabled(false);
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
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP);
            thread.start();
            startTime = System.currentTimeMillis();
        } else if (v.getId() == R.id.stop_button) {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            configButton.setEnabled(true);
            resetButton.setEnabled(true);
            stopRun = true;
            thread = null;
        } else if (v.getId() == R.id.config_button){
            // 設定画面の呼び出し用
            stopButton.setEnabled(true);
            startButton.setEnabled(true);
            configButton.setEnabled(true);
            resetButton.setEnabled(true);
            stopRun = true;
            thread = null;
            timerText.setText(dataFormat.format(0));
            Intent intent = new Intent(this, SubActivity.class);
            intent.putExtra("value", Value);
            // startActivityで起動する
            startActivityForResult(intent, CALL_RESULT_CODE);
        } else {
            stopButton.setEnabled(true);
            startButton.setEnabled(true);
            configButton.setEnabled(true);
            resetButton.setEnabled(true);
            timerText.setText(dataFormat.format(0));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CALL_RESULT_CODE){
            if(resultCode == Activity.RESULT_OK){
                // subActivityから受け取ったtextを表示
                int value = data.getIntExtra("value", 55);
                Value = value;
            }
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
            long sum = 0;
            for(int i = 0; i < bufferSize; i++){
                sum += Math.abs(buffer[i]);
            }
            short avg = (short) (sum / bufferSize);

            final double db = 20.0 * Math.log10(avg / baseValue);
            System.out.println(db);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    endTime = System.currentTimeMillis();
                    // カウント時間 = 経過時間 - 開始時間
                    diffTime = (endTime - startTime);
                    timerText.setText(dataFormat.format(diffTime));
                }
            });
            // @todo dbの値の決め方とifの条件は調整する
            if(db > Value || diffTime > 30000){
                audioRecord.stop();
                stopRun = true;
                thread = null;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        startButton.setEnabled(false);
                        stopButton.setEnabled(false);
                        configButton.setEnabled(true);
                        resetButton.setEnabled(true);
                    }
                });
            }
        }
    }

}
