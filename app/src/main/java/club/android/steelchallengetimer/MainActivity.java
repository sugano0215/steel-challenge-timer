package club.android.steelchallengetimer;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;

import club.android.steelchallengetimer.R;

public class MainActivity extends AppCompatActivity implements Runnable, View.OnClickListener {

    private long startTime;
    // 10 msec order
    private int period = 10;
    private int derayTime = 2000;

    private TextView timerText;
    private Button startButton, stopButton, resetButton;

    private Thread thread = null;
    private final Handler handler = new Handler();
    private volatile boolean stopRun = false;

    private SimpleDateFormat dataFormat = new SimpleDateFormat("mm:ss.SS");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerText = (TextView) findViewById(R.id.timer);
        timerText.setText(dataFormat.format(0));

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);
        resetButton = (Button) findViewById(R.id.reset_button);


        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        stopButton.setEnabled(false);
        resetButton.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        if (v == startButton) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            stopRun = false;
            thread = new Thread(this);
            thread.start();
            startTime = System.currentTimeMillis();
        } else if (v == stopButton) {
            startButton.setEnabled(false);
            stopButton.setEnabled(false);
            resetButton.setEnabled(true);
            stopRun = true;
            thread = null;
        } else {
            resetButton.setEnabled(false);
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
            timerText.setText(dataFormat.format(0));
        }
    }

    @Override
    public void run() {
        // startが押された時2秒間待つ
        try {
            Thread.sleep(derayTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
            stopRun = true;
        }
        ToneGenerator toneGenerator
                = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
        while (!stopRun) {
            // sleep: period msec
            try {
                Thread.sleep(period);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stopRun = true;
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    long endTime = System.currentTimeMillis();
                    // カウント時間 = 経過時間 - 開始時間
                    long diffTime = (endTime - startTime);

                    timerText.setText(dataFormat.format(diffTime));


                }
            });
        }
    }

    private class OnReachedVolumeListener {
    }
}
