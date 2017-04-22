package com.csulb.android.chatapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

public class WiFiChatActivity extends AppCompatActivity {
    EditText messageText;
    Button sendButton;
    ImageButton recordAudioButton;
    ImageView fullscreen;
    ListView messageListView;
    ChatMessageAdapter messageAdapter;
    private static final String TAG = "WiFiChatActivity";

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
        /*messageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        });*/


        /*String mode = getIntent().getStringExtra(MessageConstants.CONNECTION_MODE);
        boolean isServer = getIntent().getBooleanExtra(MessageConstants.IS_SERVER, false);
        if(mode == null) {
            Toast.makeText(this, "Could not establish connection.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Fatal: Connection mode empty");
            finish();
        } else if(mode.equals(MessageConstants.CONNECTION_MODE_BLUETOOTH)) {
            if(isServer) {
                cThread = new ConnectedThread(BluetoothServerThread.socket, handler);
            } else {
                cThread = new ConnectedThread(BluetoothConnectActivity.socket, handler);
            }
            Log.e("ChatActivity", "STARTING Connection threads.");
            cThread.start();
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageText.getText().toString();
                messageText.setText("");
                if(cThread == null) {
                    Log.e(TAG, "Fatal: Could not start ConnectedThread");
                    return;
                }
                cThread.write(message.getBytes(), MessageConstants.DATATYPE_TEXT);
            }
        });*/

        /*recordAudioButton.setOnTouchListener(new View.OnTouchListener() {
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
        });*/
    }
}
