package com.qoncrete.sdk.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.qoncrete.sdk.Qoncrete;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements OnClickListener, TestThread.Callback {

    private EditText editSourceID;
    private EditText editApiToken;
    private TextView txtTotal;
    private Switch switchSSL;
    private Switch switchDNSCache;
    private Switch switchAutoBatch;
    private EditText editBatchSize;
    private EditText editAutoSendAfter;
    private TestThread testThread;
    private long total = 0;
    private Qoncrete qoncrete;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editSourceID = (EditText) this.findViewById(R.id.edit_sourceID);
        editApiToken = (EditText) this.findViewById(R.id.edit_apiToken);
        txtTotal = (TextView) this.findViewById(R.id.txt_total);
        switchSSL = (Switch) this.findViewById(R.id.switch_ssl);
        switchDNSCache = (Switch) this.findViewById(R.id.switch_dns_cache);
        switchAutoBatch = (Switch) this.findViewById(R.id.switch_autoBatch);
        editBatchSize = (EditText) this.findViewById(R.id.edit_batchSize);
        editAutoSendAfter = (EditText) this.findViewById(R.id.edit_autoSendAfter);
        this.findViewById(R.id.btn_start).setOnClickListener(this);
        this.findViewById(R.id.btn_stop).setOnClickListener(this);
        visibilityBtn(false);
        switchAutoBatch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editBatchSize.setEnabled(b);
                editAutoSendAfter.setEnabled(b);
            }
        });
        initQoncrete();
    }

    private void initQoncrete() {
        if (qoncrete != null) {
            qoncrete.destroy();
        }
        qoncrete = new Qoncrete.Builder().sourceID(editSourceID.getText().toString()).apiToken(editApiToken.getText().toString())
                .secureTransport(switchSSL.isChecked())
                .cacheDNS(switchDNSCache.isChecked())
                .autoBatch(switchAutoBatch.isChecked())
                .batchSize(Integer.valueOf(editBatchSize.getText().toString()))
                .autoSendAfter(Integer.valueOf(editAutoSendAfter.getText().toString()))
                .build(this);
//        Qoncrete qoncrete = new Qoncrete(this, "sourceID", "apiToken");
        qoncrete.setCallback(new Qoncrete.Callback() {
            @Override
            public void onFailure() {
                System.out.println("Demo  onFailure");
            }

            @Override
            public void onResponse() {
                System.out.println("Demo  onResponse");
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                if (!checkSetting()) return;
                if (testThread != null) {
                    Toast.makeText(this, "no stop", Toast.LENGTH_SHORT).show();
                } else {
                    testThread = new TestThread(this);
                    testThread.setTime(200);
                    testThread.start();
                }
                break;
            case R.id.btn_stop:
                if (!checkSetting()) return;
                if (testThread != null) {
                    testThread.close();
                    testThread.isInterrupted();
                    testThread = null;
                } else {
                    Toast.makeText(this, "no start", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_setting:
                if (!checkSetting()) return;
                if (testThread != null) {
                    testThread.close();
                    testThread.isInterrupted();
                    testThread = null;
                }
                visibilityBtn(true);
                initQoncrete();
                total = 0;
                txtTotal.setText(String.valueOf(total));
                break;
        }
    }

    @Override
    public void run() {

        sendData();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtTotal.setText(String.valueOf(++total));
            }
        });
    }

    private void sendData() {
        JSONObject j = new JSONObject();
        try {
            j.put("title", "Hello World");
            j.put("content", "Cool");
            j.put("date", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        qoncrete.send(j.toString());
    }

    private boolean checkSetting() {
        if (TextUtils.isEmpty(editSourceID.getText().toString()) || TextUtils.isEmpty(editApiToken.getText().toString())) {
            Toast.makeText(this, "SourceID ApiToken can`t be null", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void visibilityBtn(boolean visibility) {
        this.findViewById(R.id.btn_start).setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
        this.findViewById(R.id.btn_stop).setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
    }

}

class TestThread extends Thread {
    private boolean isClose = false;
    private int time = 1000;

    private Callback callback;

    public TestThread(Callback callback) {
        this.callback = callback;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void close() {
        isClose = true;
    }

    @Override
    public void run() {
        while (!isClose) {
            callback.run();
            try {
                sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    interface Callback {
        void run();
    }
}
