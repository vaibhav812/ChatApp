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
import android.os.AsyncTask;
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
import java.nio.charset.Charset;

import static android.os.Environment.getExternalStorageDirectory;
import static com.csulb.android.chatapp.ChatActivity.SELECT_IMAGE;

/*Todo: Bug in onItemClick on images. Only latest image opens irrespective of which image has been clicked. -- Solved.
*/
public class WiFiChatActivity extends AppCompatActivity {
    private static final String TAG = "WiFiChatActivity";
    private static final int REQUEST_RECORDING_PERMISSIONS = 9;
    EditText messageText;
    Button sendButton;
    ImageButton recordAudioButton;
    ImageView fullscreen;
    ListView messageListView;
    ChatMessageAdapter messageAdapter;
    boolean isGroupOwner = false;
    String addr = null;
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
                            Log.e(TAG, "BITMAP IS NULL");
                        }
                    } else if (msg.arg2 == 3) {
                        String filename = getFilename();
                        Log.d(TAG, "Saving to file: " + filename);
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
                            Toast.makeText(WiFiChatActivity.this, "Could not save the file", Toast.LENGTH_SHORT).show();
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
                            Log.e(TAG, "IMAGE BITMAP IS NULL");
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
    private WiFiServerThread serverThread = null;
    private WiFiClientThread clientThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_chat);

        messageText = (EditText) findViewById(R.id.wifi_chat_message_text);
        sendButton = (Button) findViewById(R.id.wifi_send_button);
        recordAudioButton = (ImageButton) findViewById(R.id.wifi_record);
        fullscreen = (ImageView) findViewById(R.id.wifi_fullscreen_image);
        messageListView = (ListView) findViewById(R.id.wifi_chat_list_view);
        messageAdapter = new ChatMessageAdapter(WiFiChatActivity.this, R.layout.chat_message);
        messageListView.setAdapter(messageAdapter);
        messageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatMessage msg = (ChatMessage) parent.getItemAtPosition(position);
                if(msg.imageBitmap != null) {
                    fullscreen.setImageBitmap(msg.imageBitmap);
                    fullscreen.setBackgroundColor(Color.BLACK);
                    sendButton.setVisibility(View.INVISIBLE);
                    fullscreen.setVisibility(View.VISIBLE);
                } else if(msg.audioFile != null) {
                    Log.d(TAG, "Playing filename: " + msg.audioFile.getName());
                    player = MediaPlayer.create(WiFiChatActivity.this, Uri.fromFile(msg.audioFile));
                    player.start();
                }
            }
        });

        isGroupOwner = getIntent().getBooleanExtra(MessageConstants.EXTRA_IS_GROUP_OWNER, false);
        addr = getIntent().getStringExtra(MessageConstants.EXTRA_OWNER_ADDR);

        if (isGroupOwner) {
            serverThread = new WiFiServerThread(handler);
            serverThread.start();
        } else {
            clientThread = new WiFiClientThread(addr, handler);
            clientThread.start();
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageText.getText().toString().trim();
                messageText.setText("");
                if (message.length() > 0) {
                    sendWriteRequest(message.getBytes(), MessageConstants.DATATYPE_TEXT);
                }
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
                            ActivityCompat.requestPermissions(WiFiChatActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
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

        if (serverThread != null) {
            serverThread.cancel();
        }

        if (clientThread != null) {
            clientThread.cancel();
        }

        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
        }
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
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
                    String encodedImage = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
                    Log.d("ENCODED STRING", encodedImage);
                    sendWriteRequest(encodedImage.getBytes(), MessageConstants.DATATYPE_IMAGE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    void startRecording() {
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

    private void stopRecording() {
        Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show();
        if (recorder != null) {
            recorder.stop();
            recorder.reset();
            recorder.release();
            recorder = null;
            if (serverThread != null || clientThread != null) {
                try {
                    File f = new File(fileName);
                    FileInputStream fis = new FileInputStream(fileName);
                    byte[] buff = new byte[(int) f.length()];
                    fis.read(buff);
                    sendWriteRequest(buff, MessageConstants.DATATYPE_FILE);
                    fis.close();
                } catch (Exception e) {
                    Log.e(TAG, "Could not open stream to save data", e);
                }
            }
        }
    }

    private boolean checkPermission() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED);
    }

    private String getFilename() {
        String filepath = getExternalStorageDirectory().getPath();
        File appFolder = new File(filepath, "ChatApp");
        if (!appFolder.exists()) {
            if (!appFolder.mkdirs()) {
                Toast.makeText(this, "Could not create App folder. Any activity requiring storage is suspended", Toast.LENGTH_LONG).show();
                return null;
            }
        }
        return appFolder.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".mp3";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_RECORDING_PERMISSIONS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startRecording();
                }
                break;
        }
    }

    void sendWriteRequest(final byte[] data, final String datatype) {
        Log.d(TAG, "Received send request for bytes - " + new String(data, Charset.defaultCharset()));
        if (isGroupOwner) {
            AsyncTask task = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] params) {
                    serverThread.write(data, datatype);
                    return null;
                }
            };
            task.execute();
        } else {
            AsyncTask task = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] params) {
                    clientThread.write(data, datatype);
                    return null;
                }
            };
            task.execute();
        }

    }
}
