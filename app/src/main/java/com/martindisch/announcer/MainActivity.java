package com.martindisch.announcer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button mRecordButton, mPlayButton, mUploadButton;
    private boolean mRecording = false;
    private RecordWaveTask recordTask = null;
    private String mFileName = null;
    private MediaPlayer mPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecordButton = findViewById(R.id.bRecord);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRecording) {
                    mRecordButton.setText(R.string.record_stop);
                    mPlayButton.setEnabled(false);
                    launchTask();
                } else {
                    recordTask.cancel(false);
                    mRecordButton.setText(R.string.record_start);
                    mPlayButton.setEnabled(true);
                }
                mRecording = !mRecording;
            }
        });
        mPlayButton = findViewById(R.id.bPlay);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlaying();
            }
        });
        mUploadButton = findViewById(R.id.bUpload);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // Prepare the record task
        recordTask = new RecordWaveTask();

        // Record to the external cache directory for visibility
        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/message.wav";

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
    }

    private void startPlaying() {
        mRecordButton.setEnabled(false);
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mRecordButton.setEnabled(true);
                }
            });
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void launchTask() {
        switch (recordTask.getStatus()) {
            case FINISHED:
                recordTask = new RecordWaveTask();
                break;
            case PENDING:
                if (recordTask.isCancelled()) {
                    recordTask = new RecordWaveTask();
                }
        }
        File wavFile = new File(mFileName);
        recordTask.execute(wavFile);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i : grantResults) {
            if (i != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.need_permissions, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

}
