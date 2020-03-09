package activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;

import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.github.bassaer.chatmessageview.view.ChatView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import inputstream.InFileStream;
import raven.speak.R;
import util.MyLogger;

/**
 * Created by fujiayi on 2017/6/20.
 */

public abstract class ChatViewUIActivity extends AppCompatActivity {
    protected TextView txtLog;
    protected ImageButton btn;

    public ChatView mChatView;
    protected Handler handler;

    protected final int layout;
    private final int textId;

    protected int textViewLines = 0; // 防止textView中文本过多

    public ChatViewUIActivity(int textId) {
        this(textId, R.layout.init_chatview);
    }

    public ChatViewUIActivity(int textId, int layout) {
        super();
        this.textId = textId;
        this.layout = layout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("曼拉语音控制");
        //setStrictMode();
        InFileStream.setContext(this);
        setContentView(layout);
        // 初始化聊天面板
        initChatViewUI();
        handler = new Handler() {

            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleMsg(msg);
            }

        };
        MyLogger.setHandler(handler);
        initPermission();
    }

    protected void handleMsg(Message msg) {
        if (txtLog != null && msg.obj != null) {
            textViewLines++;
            if (textViewLines > 100) {
                textViewLines = 0;
                txtLog.setText("");
            }
        }
    }

    protected void initChatViewUI() {
        int yourId = 1;
        Bitmap yourIcon = BitmapFactory.decodeResource(getResources(), R.drawable.reboot);
        String userLeftName = "曼拉";
        mChatView = (ChatView)findViewById(R.id.message_view);
        btn = (ImageButton) findViewById(R.id.btn);
        com.github.bassaer.chatmessageview.model.ChatUser you = new com.github.bassaer.chatmessageview.model.ChatUser(yourId, userLeftName, yourIcon);
        String descText = "";
        try {
            InputStream is = getResources().openRawResource(textId);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            descText = new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        com.github.bassaer.chatmessageview.model.Message message = new com.github.bassaer.chatmessageview.model.Message.Builder()
                .setUser(you)
                .setRight(false)
                .setText(descText)
                .build();
        //聊天面板增加消息
        mChatView.send(message);
        message = new com.github.bassaer.chatmessageview.model.Message.Builder()
                .setUser(you)
                .setRight(false)
                .setText(" 左滑可以切换文本输入，右划切换语音输入哦~")
                .build();

        mChatView.send(message);

    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

    private void setStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

    }
}
