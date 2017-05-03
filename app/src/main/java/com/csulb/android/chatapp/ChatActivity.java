package com.csulb.android.chatapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.os.Environment.getExternalStorageDirectory;

public class ChatActivity extends AppCompatActivity {
    public static final int SELECT_IMAGE = 9;
    private static final String TAG = "ChatActivity";
    private static final int REQUEST_RECORDING_PERMISSIONS = 18;
    EditText messageText;
    Button sendButton;
    ListView messageListView;
    ImageButton recordAudioButton;
    ConnectedThread cThread;
    ChatMessageAdapter messageAdapter;
    String fileName = null;
    Bitmap imageBitmap;
    MediaPlayer player = null;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageConstants.MESSAGE_READ:
                    if (msg.arg2 == 1) {
                        byte[] readData = (byte[]) msg.obj;
                        messageAdapter.add(new ChatMessage(false, new String(readData)));
                        messageAdapter.notifyDataSetChanged();
                    } else if (msg.arg2 == 2) {
                        imageBitmap = (Bitmap) msg.obj;
                        if (imageBitmap != null) {
                            messageAdapter.add(new ChatMessage(false, imageBitmap));
                            messageAdapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Fatal: Image bitmap is null");
                        }
                    } else if (msg.arg2 == 3) {
                        String filename = getFilename();
                        FileOutputStream fos;
                        try {
                            if (filename != null) {
                                byte[] buff = (byte[]) msg.obj;
                                fos = new FileOutputStream(filename);
                                fos.write(buff);
                                fos.flush();
                                fos.close();
                                messageAdapter.add(new ChatMessage(false, new File(filename)));
                                messageAdapter.notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ChatActivity.this, "Could not save the file", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Could not save the file", e);
                        }
                    }
                    break;
                case MessageConstants.MESSAGE_WRITE:
                    if (msg.arg2 == 1) {
                        byte[] writeData = (byte[]) msg.obj;
                        messageAdapter.add(new ChatMessage(true, new String(writeData)));
                        messageAdapter.notifyDataSetChanged();
                    } else if (msg.arg2 == 2) {
                        imageBitmap = (Bitmap) msg.obj;
                        if (imageBitmap != null) {
                            messageAdapter.add(new ChatMessage(true, imageBitmap));
                            messageAdapter.notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Fatal: Image bitmap is null");
                        }
                    } else if (msg.arg2 == 3) {
                        File f = new File(fileName);
                        messageAdapter.add(new ChatMessage(true, f));
                        messageAdapter.notifyDataSetChanged();
                    }
                    break;
                case MessageConstants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
            }
        }
    };
    private MediaRecorder recorder;
    private ImageView fullscreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageText = (EditText) findViewById(R.id.chat_message_text);
        sendButton = (Button) findViewById(R.id.send_button);
        recordAudioButton = (ImageButton) findViewById(R.id.record);
        fullscreen = (ImageView) findViewById(R.id.fullscreen_image);
        messageListView = (ListView) findViewById(R.id.chat_list_view);
        messageAdapter = new ChatMessageAdapter(ChatActivity.this, R.layout.chat_message);
        messageListView.setAdapter(messageAdapter);
        messageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatMessage msg = (ChatMessage) parent.getItemAtPosition(position);
                if(msg.imageBitmap != null) {
                    fullscreen.setImageBitmap(imageBitmap);
                    fullscreen.setBackgroundColor(Color.BLACK);
                    sendButton.setVisibility(View.INVISIBLE);
                    fullscreen.setVisibility(View.VISIBLE);
                } else if(msg.audioFile != null) {
                    player = MediaPlayer.create(ChatActivity.this, Uri.fromFile(msg.audioFile));
                    player.start();
                }
            }
        });


        String mode = getIntent().getStringExtra(MessageConstants.CONNECTION_MODE);
        boolean isServer = getIntent().getBooleanExtra(MessageConstants.IS_SERVER, false);
        if(mode == null) {
            Toast.makeText(this, "Could not establish connection.", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Fatal: Connection mode empty");
            finish();
        } else if(mode.equals(MessageConstants.CONNECTION_MODE_BLUETOOTH)) {
            if(isServer) {
                cThread = new ConnectedThread(BluetoothServerThread.socket, handler);
            } else {
                cThread = new ConnectedThread(BluetoothConnectActivity.socket, handler);
            }
            Log.d(TAG, "STARTING Connection thread : " + cThread.getClass().getName());
            cThread.start();
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageText.getText().toString();
                messageText.setText("");
                if(cThread == null) {
                    Log.e(TAG, "Fatal: Could not start " + cThread.getClass().getName());
                    return;
                }
                cThread.write(message.getBytes(), MessageConstants.DATATYPE_TEXT);
            }
        });

        recordAudioButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "Start Recording");
                        if(checkPermission()) {
                            startRecording();
                        } else {
                            ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO}, REQUEST_RECORDING_PERMISSIONS);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "Stop Recording");
                        stopRecording();
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (fullscreen.getVisibility() == View.VISIBLE) {
            fullscreen.setImageDrawable(null);
            sendButton.setVisibility(View.VISIBLE);
            fullscreen.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(player != null) {
            if(player.isPlaying()) {
                player.stop();
            }
            player.release();
        }
        cThread.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_image:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"),SELECT_IMAGE);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
                    String encodedImage = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
                    Log.d(TAG, "Base64 encoded string: " + encodedImage);
                    cThread.write(encodedImage.getBytes(), MessageConstants.DATATYPE_IMAGE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    void startRecording(){
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        fileName = getFilename();
        Log.d("Start Recording File :", fileName);
        recorder.setOutputFile(fileName);
        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            Log.e(TAG, "Recording failed", e);
        }
    }

    private void stopRecording(){
        Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show();
        if(null != recorder){
            recorder.stop();
            recorder.reset();
            recorder.release();
            recorder = null;
            if(cThread != null) {
                try {
                    File f = new File(fileName);
                    FileInputStream fis = new FileInputStream(fileName);
                    byte[] buff = new byte[(int)f.length()];
                    fis.read(buff);
                    cThread.write(buff, MessageConstants.DATATYPE_FILE);
                    fis.close();
                } catch(Exception e) {
                    Log.e(TAG, "Could not open stream to save data", e);
                }
            }
        }
    }

    private boolean checkPermission(){
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED);
    }

    private String getFilename(){
        String filepath = getExternalStorageDirectory().getPath();
        File appFolder = new File(filepath, "ChatApp");
        if(!appFolder.exists()) {
            if(!appFolder.mkdirs()) {
                Toast.makeText(this, "Could not create App folder. Any activity requiring storage is suspended", Toast.LENGTH_LONG).show();
                return null;
            }
        }
        return appFolder.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".mp3";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case REQUEST_RECORDING_PERMISSIONS:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startRecording();
                }
                break;
        }
    }
}
