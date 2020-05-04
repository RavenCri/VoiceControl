package activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import java.util.Timer;
import java.util.TimerTask;

import raven.speak.R;

/**
 * 启动背景 一般用于广告图
 */
public class initActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        startMainActivity();
    }

    private void startMainActivity(){
        TimerTask delayTask = new TimerTask() {
            @Override
            public void run() {
                //设置登录界面
                Intent mainIntent = new Intent(initActivity.this, LoginActivity.class);
                startActivity(mainIntent);
                initActivity.this.finish();
            }
        };
        Timer timer = new Timer();
        timer.schedule(delayTask,2000);//延时两秒执行 run 里面的操作
    }
}