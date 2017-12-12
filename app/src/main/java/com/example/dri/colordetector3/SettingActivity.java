package com.example.dri.colordetector3;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

/**
 * Created by takuyamorimatsu on 2017/09/27.
 */

public class SettingActivity extends AppCompatActivity {

    private EditText filename_input, border_input, interval_input, format_input;
    private Switch printRGB_input;
    private int resultCode = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        //filename_input = (EditText) findViewById(R.id.filename_input);
        border_input = (EditText) findViewById(R.id.border_input);
        interval_input = (EditText) findViewById(R.id.interval_input);
        format_input = (EditText) findViewById(R.id.format_input);

        printRGB_input = (Switch) findViewById(R.id.print_rgb_input);


        final SharedPreferences preferences = getSharedPreferences("setting", MODE_PRIVATE);
        //filename_input.setText(preferences.getString("filename_input", "filename.txt"));
        border_input.setText(String.valueOf(preferences.getInt("border_input", 100)));
        interval_input.setText(String.valueOf(preferences.getInt("interval_input", 1000)));
        format_input.setText(preferences.getString("format_input", "yyyy/MM/dd HH:mm:ss.SSS"));

        printRGB_input.setChecked(preferences.getBoolean("printRGB_input", false));
        if(printRGB_input.isChecked()){
            printRGB_input.setText("する");
        }else{
            printRGB_input.setText("しない");
        }

        printRGB_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(printRGB_input.isChecked()){
                    printRGB_input.setText("する");
                }else{
                    printRGB_input.setText("しない");
                }
            }
        });

        Button done_btn = (Button) findViewById(R.id.done_btn);
        done_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = preferences.edit();
                //editor.putString("filename_input", filename_input.getText().toString());
                editor.putInt("border_input", Integer.parseInt(border_input.getText().toString()));
                editor.putInt("interval_input", Integer.parseInt(interval_input.getText().toString()));
                editor.putString("format_input", format_input.getText().toString());

                editor.putBoolean("printRGB_input", printRGB_input.isChecked());

                editor.commit();

                resultCode = 1;
                setResult(resultCode);
                finish();
            }
        });
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK){
            return true;
        }else{
            return super.onKeyDown(keyCode, event);
        }
    }
}
