package com.martindisch.announcer;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button mRecordButton;
    private EditText mHost, mPort;
    private boolean mRecording = false;
    private RecordWaveTask recordTask = null;
    private String mFileName = null;
    private Session mSession = null;

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
                    launchTask();
                } else {
                    recordTask.cancel(false);
                    mRecordButton.setText(R.string.record_start);
                    uploadAndPlay();
                }
                mRecording = !mRecording;
            }
        });

        mHost = findViewById(R.id.etHost);
        mPort = findViewById(R.id.etPort);
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        mHost.setText(prefs.getString("host", ""));
        mPort.setText(String.format("%d", prefs.getInt("port", 22)));

        // Prepare the record task
        recordTask = new RecordWaveTask();

        // Record to the external cache directory for visibility
        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/message.wav";

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
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

    private void uploadAndPlay() {
        // Save entered connection details to preferences
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        final String host = mHost.getText().toString();
        final int port = Integer.parseInt(mPort.getText().toString());
        editor.putString("host", host);
        editor.putInt("port", port);
        editor.apply();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Set up JSch
                    JSch.setConfig("StrictHostKeyChecking", "no");
                    JSch jsch = new JSch();
                    File privateKey = new File(Environment.getExternalStorageDirectory(), "Announcer/id_rsa");
                    jsch.addIdentity(privateKey.getAbsolutePath());
                    mSession = jsch.getSession("root", host, port);
                    mSession.connect();

                    // Start SFTP channel to upload the message
                    ChannelSftp channelsftp = (ChannelSftp) mSession.openChannel("sftp");
                    channelsftp.connect();
                    channelsftp.put(mFileName, "message.wav");
                    channelsftp.disconnect();

                    // Start SSH channel to play the message
                    ChannelExec channelssh = (ChannelExec) mSession.openChannel("exec");
                    channelssh.setCommand("aplay message.wav");
                    channelssh.connect();
                    channelssh.disconnect();
                } catch (JSchException | SftpException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        if (mSession != null) {
            mSession.disconnect();
        }
    }

}
