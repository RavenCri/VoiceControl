package activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import config.InitConfig;
import raven.speak.ActivityUiDialog;
import raven.speak.R;
import util.HttpUtil;

public class LoginActivity extends Activity {
    public Handler handler;
    /* 用户登录信息*/
    public static JSONObject userInfo;
    public static String token;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_login);
        InitView();
    }

    private void InitView() {
        TextView username = findViewById(R.id.username);
        username.setText("test");
        TextView password = findViewById(R.id.password);
        password.setText("test");
        Button login  = findViewById(R.id.login);
        Button  regist = findViewById(R.id.regist);

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle data = msg.getData();
                JSONObject result = JSON.parseObject(data.getString("result"));
                // 如果登陆失败
                if(result.getInteger("code") != 200){
                    Toast.makeText(LoginActivity.this,
                            result.getString("data"), Toast.LENGTH_SHORT).show();
                }else{
                    userInfo = JSON.parseObject(result.getString("data"));
                    token = result.getString("token");
                    Intent mainIntent = new Intent(LoginActivity.this, ActivityUiDialog.class);
                    startActivity(mainIntent);
                    LoginActivity.this.finish();
                }


            }
        };
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Map<String,String> param = new HashMap<>();
                System.out.println(username.getText().toString());
                param.put("username",username.getText().toString());
                param.put("password",password.getText().toString());
                postFrom(param);

            }


        });
        regist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this,
                        "点击了注册", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postFrom( Map<String,String> param )   {
       new Thread(){
           @Override
           public void run() {
               JSONObject result = HttpUtil.post(InitConfig.host + "/account/login", param);
               Message msg = new Message();
               Bundle data = new Bundle();
               data.putString("result",result.toJSONString());
               msg.setData(data);
               handler.sendMessage(msg);
           }
       }.start();

    }
}
