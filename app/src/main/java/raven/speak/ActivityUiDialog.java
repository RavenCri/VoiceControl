package raven.speak;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidu.speech.asr.SpeechConstant;
import com.baidu.tts.auth.AuthInfo;
import com.baidu.tts.chainofresponsibility.logger.LoggerProxy;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.github.bassaer.chatmessageview.model.ChatUser;
import com.github.bassaer.chatmessageview.view.ChatView;
import com.google.android.material.navigation.NavigationView;


import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import activity.LoginActivity;
import chat.ui.ChatUiUtil;
import config.InitConfig;
import listener.UiMessageListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import recog.ActivityAbstractRecog;
import recog.listen.ChainRecogListener;
import recog.listen.MessageStatusRecogListener;
import ua.naiksoftware.stomp.Stomp;

import ua.naiksoftware.stomp.StompClient;
import uidialog.BaiduASRDigitalDialog;
import uidialog.DigitalDialogInput;
import util.AutoCheck;
import util.HttpUtil;
import util.MyLogger;
import wakeup.MyWakeup;
import wakeup.WakeUpResult;
import wakeup.listener.IWakeupListener;
import wakeup.listener.SimpleWakeupListener;


/**
 * UI 界面调用
 * <p>
 * 本类仅仅初始化及释放MyRecognizer，具体识别逻辑在BaiduASRDialog。对话框UI在BaiduASRDigitalDialog
 * 依赖SimpleTransApplication 在两个activity中传递输入参数
 * <p>
 *
 */

public class ActivityUiDialog extends ActivityAbstractRecog {

    /**
     * 对话框界面的输入参数
     */
    private DigitalDialogInput input;
    private ChainRecogListener chainRecogListener;
    private static String TAG = "ActivityUiDialog";
    protected String appId;

    protected MyWakeup myWakeup;
    protected String appKey;

    protected String secretKey;

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
    private TtsMode ttsMode = TtsMode.ONLINE;

    // ================选择TtsMode.ONLINE  不需要设置以下参数; 选择TtsMode.MIX 需要设置下面2个离线资源文件的路径
    private static final String TEMP_DIR = "/sdcard/baiduTTS"; // 重要！请手动将assets目录下的3个dat 文件复制到该目录

    // 请确保该PATH下有这个文件
    private static final String TEXT_FILENAME = TEMP_DIR + "/" + "bd_etts_text.dat";

    // 请确保该PATH下有这个文件 ，m15是离线男声
    private static final String MODEL_FILENAME =
            TEMP_DIR + "/" + "bd_etts_common_speech_m15_mand_eng_high_am-mix_v3.0.0_20170505.dat";

    // ===============初始化参数设置完毕，更多合成参数请至getParams()方法中设置 =================
    protected Handler mainHandler;

    protected String currRobotSpeak = null;
    protected SpeechSynthesizer mSpeechSynthesizer;

    //声明SoundPool的引用
    SoundPool sp;
    //声明HashMap来存放声音文件
    HashMap<Integer, Integer> hm;
    //当前正播放的streamId
    int currStaeamId;

    public static ChatUser userRight;
    public static ChatUser userLeft;

    public static ChatView mChatView;
    //移动的终点X
    private float moveX;
    //按下时的X
    private float pressX;

    private StompClient mStompClient;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    //当前选择的设备
    private String currentChooesDevice;
    //所有设备列表
    private List<String> devices = new ArrayList<>();

    public ActivityUiDialog() {
        super(R.raw.uidialog_recog, false);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("进UiDialog");
        initUI();
        //动态权限
        initPermission();

        //声音池
        initSoundPool();
        // 语音识别初始化
        init();
        //合成初始化
        initTTs();
        // 语音唤醒初始化
        initWakeup();
        // 初始化聊天面板
        initChatView();
        //初始化点对点通信
        initStompService();
        //初始化设备列表
        initDevices();
    }

    private void initDevices() {

        new Thread(){
            @Override
            public void run() {
                Response response = HttpUtil.get("http://"+InitConfig.host + "/device/list", null);

                if(response != null){
                    JSONObject data = null;
                    try {
                        data = JSON.parseObject(response.body().string());

                        JSONArray devicesJSON = JSONArray.parseArray(data.getString("data"));

                        for (int i = 0; i < devicesJSON.size(); i++) {
                            System.out.println(devicesJSON.get(i));
                            String deviceId = devicesJSON.getJSONObject(i).getString("deviceId");

                            if(i == 0)currentChooesDevice = deviceId;
                            devices.add(deviceId);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }.start();

    }

    private void initUI() {
        drawerLayout = findViewById(R.id.mainXml);
        navigationView = findViewById(R.id.nav);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                drawerLayout.closeDrawer(navigationView);
                if(item.getTitle().equals("主控设备")){
                    showSelect();
                }else if (item.getTitle().equals("关于我们")){
                    Toast.makeText(ActivityUiDialog.this,"科睿出品",Toast.LENGTH_SHORT).show();
                }
                //Toast.makeText(ActivityUiDialog.this,item.getTitle().toString(),Toast.LENGTH_SHORT).show();

//                drawerLayout.closeDrawer(navigationView);
                return true;
            }
        });
    }

    public void showSelect(){

        String[] items = devices.toArray(new String[devices.size()]);
        final int[] index = {0};
        AlertDialog alertDialog4 = new AlertDialog.Builder(this)
                .setTitle("选择您的主控设备：")
                .setIcon(R.mipmap.ic_launcher)
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {//添加单选框
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        index[0] = i;
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        currentChooesDevice = items[index[0]];
                        //Toast.makeText(ActivityUiDialog.this, "这是确定按钮" + "点的是：" + item[index[0]], Toast.LENGTH_SHORT).show();
                    }
                })

                .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Toast.makeText(ActivityUiDialog.this, "这是取消按钮", Toast.LENGTH_SHORT).show();
                    }
                })
                .create();
        alertDialog4.show();
    }

    /**
     * 创建长连接
     */
    private void initStompService() {
        HashMap<String, String> header = new HashMap() {{
            put("name", LoginActivity.userInfo.getString("username")+"@android");
        }};
        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://"+InitConfig.host+"/ws/websocket?token="+LoginActivity.token, header);
        mStompClient.connect();
        mStompClient.topic("/topic/notice").subscribe(s -> {
            Message message = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("msg",s.getPayload());
            message.setData(bundle);
            message.what = 200;
            mainHandler.sendMessage(message);

        });
        mStompClient.topic("/user/topic/reply").subscribe(s -> {
            Message message = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("msg",s.getPayload());
            message.setData(bundle);
            message.what = 200;
            mainHandler.sendMessage(message);
        });
    }
    private void initChatView() {
        //解决输入框与键盘的高度wet提
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        mChatView = (ChatView) findViewById(R.id.message_view);
        View parent = (View) findViewById(R.id.inputBox).getParent();
        //设置透明
        parent.getBackground().setAlpha(12);
        //隐藏输入框
        parent.setVisibility(View.INVISIBLE);
        //User id
        int myId = 0;
        //User icon
        Bitmap myIcon = BitmapFactory.decodeResource(getResources(), R.drawable.man);
        //User name
        String UserRightName = "你";

        int yourId = 1;
        Bitmap yourIcon = BitmapFactory.decodeResource(getResources(), R.drawable.reboot);
        String userLeftName = "曼拉";

        userRight = new com.github.bassaer.chatmessageview.model.ChatUser(myId, UserRightName, myIcon);
        userLeft = new com.github.bassaer.chatmessageview.model.ChatUser(yourId, userLeftName, yourIcon);
        // 对聊天布局的一些设置
        mChatView.setRightBubbleColor(ContextCompat.getColor(this, R.color.green500));
        mChatView.setLeftBubbleColor(ContextCompat.getColor(this, R.color.blueSky));
        //mChatView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent_15));
        mChatView.setSendButtonColor(ContextCompat.getColor(this, R.color.blueGray400));
        mChatView.setSendIcon(R.drawable.ic_action_send);
        mChatView.setRightMessageTextColor(Color.WHITE);
        mChatView.setLeftMessageTextColor(Color.WHITE);
        mChatView.setUsernameTextColor(Color.BLACK);
        mChatView.setSendTimeTextColor(Color.BLACK);
        mChatView.setDateSeparatorColor(Color.BLACK);
        mChatView.setInputTextHint("new message...");
        mChatView.setMessageMarginTop(5);
        mChatView.setMessageMarginBottom(5);
        mChatView.setOnClickSendButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mChatView.getInputText().equals("")) {
                    Toast.makeText(ActivityUiDialog.this, "你还未输入消息呢~", Toast.LENGTH_SHORT);
                    return;
                }
                replay(mChatView.getInputText());
                //Reset edit text
                mChatView.setInputText("");
            }
        });
        // 监听回车键
        TextView textView = findViewById(R.id.inputBox);
        textView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {

                if (mChatView.getInputText().equals("")) {
                    return false;
                }
                replay(mChatView.getInputText());
                //Reset edit text
                mChatView.setInputText("");
                return true;
            }
            return false;
        });
        // 增加 滑动切换输入方式
        findViewById(R.id.operation).setOnTouchListener(TouchListen());
        findViewById(R.id.message_view).setOnTouchListener(meum());

    }
    private View.OnTouchListener meum() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    //按下屏幕时
                    case MotionEvent.ACTION_DOWN:
                        pressX = event.getX();
                        break;
                    //移动
                    case MotionEvent.ACTION_MOVE:
                        moveX = event.getX();
                        break;
                    //松开屏幕时
                    case MotionEvent.ACTION_UP:

                       if (moveX - pressX > 100) {
                           drawerLayout.openDrawer(navigationView);
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        };
    }
    private View.OnTouchListener TouchListen() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    //按下屏幕时
                    case MotionEvent.ACTION_DOWN:
                        pressX = event.getX();
                        break;
                    //移动
                    case MotionEvent.ACTION_MOVE:
                        moveX = event.getX();
                        break;
                    //松开屏幕时
                    case MotionEvent.ACTION_UP:
                        if (moveX - pressX > 150) {
                            Log.i("message", "向右");
                            View parent = (View) findViewById(R.id.inputBox).getParent();
                            //显示输入框
                            parent.setVisibility(View.INVISIBLE);
                            findViewById(R.id.btn).setVisibility(View.VISIBLE);
                        } else if (moveX - pressX < 150) {
                            View parent = (View) findViewById(R.id.inputBox).getParent();
                            //隐藏按钮
                            findViewById(R.id.btn).setVisibility(View.INVISIBLE);
                            //显示 输入框
                            parent.setVisibility(View.VISIBLE);
                            Log.i("message", "向左");
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        };
    }


    /**
     *  初始化唤醒服务
     */
    private void initWakeup() {
        //唤醒成功后调用
        IWakeupListener listener = new SimpleWakeupListener(){
            @Override
            public void onSuccess(String word, WakeUpResult result) {
                super.onSuccess(word, result);
                
                start();
            }
        };
        //唤醒
        myWakeup = new MyWakeup(this, listener);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(SpeechConstant.WP_WORDS_FILE, "assets://WakeUp.bin");
        params.put(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_HIGH_SPEED_SYNTHESIZE);
        params.put("appid",appId);
        myWakeup.start(params);

    }

    /**
     *
     */
    private void init()  {
        ApplicationInfo appInfo = null;
        try {
            appInfo = this.getPackageManager()
                    .getApplicationInfo(getPackageName(),
                            PackageManager.GET_META_DATA);

            appId = String.valueOf(appInfo.metaData.getInt("com.baidu.speech.APP_ID"));
            appKey = appInfo.metaData.getString("com.baidu.speech.API_KEY");
            secretKey = appInfo.metaData.getString("com.baidu.speech.SECRET_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        /**
         * 有2个listner，一个是用户自己的业务逻辑，如MessageStatusRecogListener。另一个是UI对话框的。
         * 使用这个ChainRecogListener把两个listener和并在一起
         */
        chainRecogListener = new ChainRecogListener();
        // DigitalDialogInput 输入 ，MessageStatusRecogListener可替换为用户自己业务逻辑的listener
        chainRecogListener.addListener(new MessageStatusRecogListener(handler));
        myRecognizer.setEventListener(chainRecogListener); // 替换掉原来的listener

        mainHandler = new Handler() {
            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String showText = null;
                // 200代表 回复的消息
                if(msg.what == 200){
                    showText = msg.getData().getString("msg");
                    System.out.println(showText);
                    ChatUiUtil.showMsg("left",showText,false);

                }

                if (msg.obj != null) {
                    print("mainHandler收到："+msg.obj.toString());
                    // 如果机器人追问
                    if(msg.obj.toString().contains("播放结束回调") &&
                            (currRobotSpeak.contains("?")||currRobotSpeak.contains("请问"))){
                            start();

                    }
                }
            }

        };

    }

    /**
     * 开始录音，点击“开始”按钮后调用。
     */
    @Override
    public void start() {

        mSpeechSynthesizer.stop();
        playSound(1, 0);
        // 此处params可以打印出来，直接写到你的代码里去，最终的json一致即可。
        final Map<String, Object> params = fetchParams();
        // BaiduASRDigitalDialog的输入参数
        input = new DigitalDialogInput(myRecognizer, chainRecogListener, params);
        BaiduASRDigitalDialog.setInput(input); // 传递input信息，在BaiduASRDialog中读取,
        Intent intent = new Intent(this, BaiduASRDigitalDialog.class);
        // 修改对话框样式
        intent.putExtra(BaiduASRDigitalDialog.PARAM_DIALOG_THEME, BaiduASRDigitalDialog.THEME_GREEN_LIGHTBG);
        running = true;
        startActivityForResult(intent, 2);
    }

    /**
     * 语音识别识别成功调用
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        running = false;
        Log.i(TAG, "requestCode" + requestCode);
        if (requestCode == 2) {
            String res = "对话框的识别结果：";
            if (resultCode == RESULT_OK) {
                ArrayList results = data.getStringArrayListExtra("results");
                if (results != null && results.size() > 0) {
                    res += results.get(0);
                    res = (String) results.get(0);
                    replay(res);
                }
            } else {
                res += "没有结果";
            }
            MyLogger.info(res);
        }
    }

    /**
     *  根据识别出来的 文本 回复相应的对话 并合成发音
     * @param res
     */
    private void replay(String res) {
        com.github.bassaer.chatmessageview.model.Message msg = new com.github.bassaer.chatmessageview.model.Message.Builder()
                .setUser(userRight)
                .setRight(true)
                .setText(res)
                .build();
        mChatView.setRefreshing(true);
        mChatView.send(msg);
        //String resUrlEncode = URLEncoder.encode(res);
        new Thread(()->{

            Map<String , String> param = new HashMap<>();
            param.put("word",res);
            param.put("platForm","android");
            param.put("deviceId",currentChooesDevice);
            Response response = HttpUtil.get("http://"+InitConfig.host + "/roobot/replay", param);
            try {
                JSONObject obj = JSON.parseObject(response.body().string());
                System.out.println(obj.toJSONString());
                String data = obj.getString("msg");
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("msg",data);
                message.setData(bundle);
                message.what = 200;
                mainHandler.sendMessage(message);
                mSpeechSynthesizer.stop();
                mSpeechSynthesizer.speak(data);
                currRobotSpeak = data;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
//
//    @Override
//    protected void onPause() {
//        Log.i(TAG, "onPause");
//        super.onPause();
//        if (!running) {
//            myRecognizer.release();
//            finish();
//        }
//    }

    private void initTTs() {

        LoggerProxy.printable(true); // 日志打印在logcat中
        boolean isMix = ttsMode.equals(TtsMode.MIX);
        boolean isSuccess;
        // 日志更新在UI中，可以换成MessageListener，在logcat中查看日志
        SpeechSynthesizerListener listener = new UiMessageListener(mainHandler);
        // 1. 获取实例
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(this);
        // 2. 设置listener
        mSpeechSynthesizer.setSpeechSynthesizerListener(listener);

        // 3. 设置appId，appKey.secretKey
        int result = mSpeechSynthesizer.setAppId(appId);
        checkResult(result, "setAppId");
        result = mSpeechSynthesizer.setApiKey(appKey, secretKey);
        checkResult(result, "setAppId");

        // 4. 支持离线的话，需要设置离线模型
        if (isMix) {
            // 检查离线授权文件是否下载成功，离线授权文件联网时SDK自动下载管理，有效期3年，3年后的最后一个月自动更新。
            isSuccess = checkAuth();
            if (!isSuccess) {
                return;
            }
            // 文本模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, TEXT_FILENAME);
            // 声学模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, MODEL_FILENAME);
        }

        // 5. 以下setParam 参数选填。不填写则默认值生效
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "111");
        // 设置合成的音量，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9");
        // 设置合成的语速，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");

        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_HIGH_SPEED_SYNTHESIZE);
        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
        // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线

        // x. 额外 ： 自动so文件是否复制正确及上面设置的参数
        Map<String, String> params = new HashMap<>();
        // 复制下上面的 mSpeechSynthesizer.setParam参数
        // 上线时请删除AutoCheck的调用
        if (isMix) {
            params.put(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, TEXT_FILENAME);
            params.put(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, MODEL_FILENAME);
        }
        InitConfig initConfig =  new InitConfig(appId, appKey, secretKey, ttsMode, params, listener);
        AutoCheck.getInstance(getApplicationContext()).check(initConfig, new Handler() {
            @Override
            /**
             * 开新线程检查，成功后回调
             */
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    AutoCheck autoCheck = (AutoCheck) msg.obj;
                    synchronized (autoCheck) {
                        String message = autoCheck.obtainDebugMessage();
                        print(message); // 可以用下面一行替代，在logcat中查看代码
                        // Log.w("AutoCheckMessage", message);
                    }
                }
            }

        });

        // 6. 初始化
       mSpeechSynthesizer.initTts(ttsMode);
       checkResult(result, "initTts");
    }
    private boolean checkAuth() {
        AuthInfo authInfo = mSpeechSynthesizer.auth(ttsMode);
        if (!authInfo.isSuccess()) {
            // 离线授权需要网站上的应用填写包名。本demo的包名是com.baidu.tts.sample，定义在build.gradle中
            String errorMsg = authInfo.getTtsError().getDetailMessage();
            print("【error】鉴权失败 errorMsg=" + errorMsg);
            return false;
        } else {
            print("验证通过，离线正式授权文件存在。");
            return true;
        }
    }
    private void checkResult(int result, String method) {
        if (result != 0) {
            print("error code :" + result + " method:" + method + ", 错误码文档:http://yuyin.baidu.com/docs/tts/122 ");
        }
    }

    private void print(String message) {
        Log.i(TAG, message);
        // mShowText.append(message + "\n");
    }
    @Override
    protected void onDestroy() {
        // 基于DEMO唤醒词集成第5 退出事件管理器
        myWakeup.release();
        super.onDestroy();
    }


    private void playSound(int sound, int loop) {//获取AudioManager引用
        AudioManager am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        //获取当前音量
        float streamVolumeCurrent = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        //获取系统最大音量
        float streamVolumeMax = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //计算得到播放音量
        float volume = streamVolumeCurrent / streamVolumeMax;
        //调用SoundPool的play方法来播放声音文件
        currStaeamId = sp.play(hm.get(sound), volume, volume, 1, loop, 1.0f);
    }
    //初始化声音池
    private void initSoundPool() {
        sp = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);//创建SoundPool对象
        hm = new HashMap<Integer, Integer>();//创建HashMap对象
        //加载声音文件，并且设置为1号声音放入hm中
        hm.put(1, sp.load(this, R.raw.remind, 1));
    }

    //  下面是android 6.0以上的动态授权

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.RECORD_AUDIO
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

}