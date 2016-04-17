package com.myproject18;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.myproject18.view.GooView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GooView view = (GooView) findViewById(R.id.gooview);
        view.setmStateChangeListener(new GooView.OnStateChangeListener() {
            @Override
            public void onDisappear() {
                Toast.makeText(getApplicationContext(), "消失了", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReset(boolean isOutOfRange) {
                Toast.makeText(getApplicationContext(), "恢复了", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
