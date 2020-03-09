package raven.speak;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class LoginActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_login);
        InitView();
    }

    private void InitView() {
        View username = findViewById(R.id.username);
        View password = findViewById(R.id.password);
        Button login  = findViewById(R.id.login);
        Button  regist = findViewById(R.id.regist);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this,
                        "点击了登陆", Toast.LENGTH_SHORT).show();
                Intent mainIntent = new Intent(LoginActivity.this, ActivityUiDialog.class);
                startActivity(mainIntent);
                LoginActivity.this.finish();
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
}
