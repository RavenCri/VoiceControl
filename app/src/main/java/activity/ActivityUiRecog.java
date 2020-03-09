package activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import java.util.Map;
import params.CommonRecogParams;
import params.OnlineRecogParams;
import raven.speak.R;
import recog.IStatus;

/**
 * 识别的基类Activity。 ActivityCommon定义了通用的UI部分
 * 封装了识别的大部分逻辑，包括MyRecognizer的初始化，资源释放
 * <p>
 * <p>
 * 集成流程代码，只需要一句： myRecognizer.start(params);具体示例代码参见startRough()
 * =》.实例化 myRecognizer   new MyRecognizer(this, listener);
 * =》 实例化 listener  new MessageStatusRecogListener(null);
 * </p>
 * 集成文档： http://ai.baidu.com/docs#/ASR-Android-SDK/top 集成指南一节
 * demo目录下doc_integration_DOCUMENT
 * ASR-INTEGRATION-helloworld  ASR集成指南-集成到helloworld中 对应 ActivityMiniRecog
 * ASR-INTEGRATION-TTS-DEMO ASR集成指南-集成到合成DEMO中 对应 ActivityUiRecog
 * <p>
 * 大致流程为
 * 1. 实例化MyRecognizer ,调用release方法前不可以实例化第二个。参数中需要开发者自行填写语音识别事件的回调类，实现开发者自身的业务逻辑
 * 2. 如果使用离线命令词功能，需要调用loadOfflineEngine。在线功能不需要。
 * 3. 根据识别的参数文档，或者demo中测试出的参数，组成json格式的字符串。调用 start 方法
 * 4. 在合适的时候，调用release释放资源。
 * <p>
 */

public abstract class ActivityUiRecog extends ChatViewUIActivity implements IStatus {

    /*
     * Api的参数类，仅仅用于生成调用START的json字符串，本身与SDK的调用无关
     */
    private final CommonRecogParams apiParams;
    /**
     * 控制UI按钮的状态
     */
    protected int status;
    /**
     * 日志使用
     */
    private static final String TAG = "ActivityUiRecog";
    /**
     * 开始录音，点击“开始”按钮后调用。
     */
    protected abstract void start();
    /**
     * 开始录音后，手动停止录音。SDK会识别在此过程中的录音。点击“停止”按钮后调用。
     */
    protected abstract void stop();
    /**
     * 开始录音后，取消这次录音。SDK会取消本次识别，回到原始状态。点击“取消”按钮后调用。
     */
    protected abstract void cancel();
    // 默认非运行状态
    protected boolean running = false;

    public ActivityUiRecog(int textId) {
        super(textId);
        String className = getClass().getSimpleName();
        System.out.println("className:"+className);
        if (className.equals("ActivityOnlineRecog") || className.equals("ActivityUiDialog")) {
            apiParams = new OnlineRecogParams();
        }else {
            throw new RuntimeException("PLEASE DO NOT RENAME DEMO ACTIVITY, current name:" + className);
        }
    }

    protected Map<String, Object> fetchParams() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        //  上面的获取是为了生成下面的Map， 自己集成时可以忽略
        Map<String, Object> params = apiParams.fetch(sp);
        //  集成时不需要上面的代码，只需要params参数。
        return params;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 调用父类方法
        super.onCreate(savedInstanceState);
        apiParams.initSamplePath(this);
    }

    @Override
    protected void initChatViewUI() {
        super.initChatViewUI();
        status = STATUS_NONE;
        btn.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    //更改为按下时的背景图片
                    v.setBackgroundResource(R.drawable.recite_record_pressed);
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    //改为抬起时的图片
                    v.setBackgroundResource(R.drawable.recite_record_normal);
                }
                return false;
            }
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status) {
                    case STATUS_NONE: // 初始状态
                        start();
                        status = STATUS_WAITING_READY;
                        updateBtnTextByStatus();
                        break;
                    case STATUS_WAITING_READY: // 调用本类的start方法后，即输入START事件后，等待引擎准备完毕。
                    case STATUS_READY: // 引擎准备完毕。
                    case STATUS_SPEAKING: // 用户开始讲话
                    case STATUS_FINISHED: // 一句话识别语音结束
                    case STATUS_RECOGNITION: // 识别中
                        stop();
                        status = STATUS_STOPPED; // 引擎识别中
                        updateBtnTextByStatus();
                        break;
                    case STATUS_LONG_SPEECH_FINISHED: // 长语音识别结束
                    case STATUS_STOPPED: // 引擎识别中
                        cancel();
                        status = STATUS_NONE; // 识别结束，回到初始状态
                        updateBtnTextByStatus();
                        break;
                    default:
                        break;
                }

            }
        });

    }

    protected void handleMsg(Message msg) {
        super.handleMsg(msg);

        switch (msg.what) { // 处理MessageStatusRecogListener中的状态回调
            case STATUS_FINISHED:
                if (msg.arg2 == 1) {
                    //txtResult 识别结束，结果是”台灯”；说话结束到识别结束耗时【142ms】1573561712482
                   // txtResult.setText(msg.obj.toString());
                    System.out.println(msg.obj.toString());
                }
                status = msg.what;
                updateBtnTextByStatus();
                break;
            case STATUS_NONE:
            case STATUS_READY:
            case STATUS_SPEAKING:
            case STATUS_RECOGNITION:
                status = msg.what;
                updateBtnTextByStatus();
                break;
            default:
                break;

        }
    }

    private void updateBtnTextByStatus() {
        switch (status) {
            case STATUS_NONE:
                //btn.setText("呼叫曼拉");
                btn.setEnabled(true);

                break;
            case STATUS_WAITING_READY:
            case STATUS_READY:
            case STATUS_SPEAKING:
            case STATUS_RECOGNITION:
                //btn.setText("停止录音");
                btn.setEnabled(true);

                break;
            case STATUS_LONG_SPEECH_FINISHED:
            case STATUS_STOPPED:
               // btn.setText("取消整个识别过程");
                btn.setEnabled(true);

                break;
            default:
                break;
        }
    }
}
