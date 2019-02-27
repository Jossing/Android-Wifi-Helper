package org.jossing.wifisample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

/**
 * 输入密码界面
 *
 * @author jossing
 * @date 2019/1/2
 */
public class PasswordActivity extends AppCompatActivity {

    public static final String EXTRA_PASSWORD = "password";

    private EditText mEtPassword;
    private Button mBtnOk;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_password);
        mEtPassword = findViewById(R.id.et_password);
        mBtnOk = findViewById(R.id.btn_ok);
        mBtnOk.setEnabled(false);

        mEtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                mBtnOk.setEnabled(s.length() >= 8);
            }
        });

        mBtnOk.setOnClickListener(v -> {
            final String password = mEtPassword.getEditableText().toString();
            final Intent data = new Intent();
            data.putExtra(EXTRA_PASSWORD, password);
            setResult(RESULT_OK, data);
            finish();
        });
    }
}
