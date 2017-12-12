package com.example.dri.colordetector3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {


    //TODO:パレットを分割
    //パレットの色がどの辺りを取得しているか示す
    //

    private final String TAG = "MainActivity";
    private final String ERROR_INSTANCE = "e";
    /*

        //各設定値を指定


    //白黒の境界値を指定(0~255)
    //RGB全てが100以下であるなら黒と判別
    private final int border = 100;

    //シャッター間隔を指定(ミリ秒)
    //1秒
    private final int interval = 1000;

    //時間のフォーマットを指定
    private final String format = "yyyy/MM/dd HH:mm:ss.SSS";

    */

    private String filename;
    private int border;
    private int interval;
    //use5points:5点での色判断を行う場合にtrue
    //printRGB:RGBの値をログに出力する場合にtrue
    private boolean printRGB;
    private String format;
    private String current_rgb_log = "";

    //preferenceの設定値を取得
    public void setValues(){
        SharedPreferences preferences = getSharedPreferences("setting", MODE_PRIVATE);
        filename = Util.getTimeStamp("yyyy:MM:dd_HH:mm") + ".txt";
        border = preferences.getInt("border_input", 100);
        interval = preferences.getInt("interval_input", 1000);
        format = preferences.getString("format_input", "yyyy/MM/dd HH:mm:ss.SSS");

        printRGB = preferences.getBoolean("printRGB_input", false);
    }





    // カメラインスタンス
    private Camera mCam = null;
    private TextView recent_data, recent_time;
    private TextView palette0, palette1, palette2, palette3, palette4;

    private Handler mHandler;
    private boolean isRunning = false;
    private Button setting_btn, error_btn;
    private SurfaceView preview;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //スリープにならない処理
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //widgetを取得
        palette0 = (TextView) findViewById(R.id.palette0);
        palette1 = (TextView) findViewById(R.id.palette1);
        palette2 = (TextView) findViewById(R.id.palette2);
        palette3 = (TextView) findViewById(R.id.palette3);
        palette4 = (TextView) findViewById(R.id.palette4);
        recent_data = (TextView) findViewById(R.id.recent_data);
        recent_time = (TextView) findViewById(R.id.recent_time);
        preview = (SurfaceView) findViewById(R.id.preview);


        //予想しないExceptionを処理
        MyUncaughtExceptionHandler myUncaughtExceptionHandler = new MyUncaughtExceptionHandler(this);
        Thread.setDefaultUncaughtExceptionHandler(myUncaughtExceptionHandler);

        //preferenceの設定値を取得
        setValues();

        //スタートボタンを押した時の処理
        final Button btn = (Button) findViewById(R.id.start_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isRunning){
                    //止める
                    //TODO:止めたら、終了する

                    //スリープにならないフラッグを削除
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    //予約していた処理を削除
                    if(mHandler != null){
                        mHandler.removeCallbacksAndMessages(null);
                    }

                    //アプリを終了するボタンのみを表示
                    Log.d(TAG, "fin");
                    MainActivity.this.finish();
                    MainActivity.this.moveTaskToBack(true);


                    /*
                    isRunning = false;
                    btn.setText("start");

                    //止まってる時のもの
                    recent_data.setVisibility(View.GONE);
                    preview.setVisibility(View.GONE);
                    */
                }else{
                    //動かす

                    /*//スリープにならない処理を追加
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);*/

                    preview.setVisibility(View.VISIBLE);

                    //SurfaceHolderを取得
                    SurfaceHolder holder = preview.getHolder();
                    //SurfaceHolderにコールバックをセット
                    holder.addCallback(surfaceHolderCallback);
                    //これはよくわからない
                    holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

                    //プレビュー画像を取得(非同期で処理)
                    mHandler = new Handler();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCam.setOneShotPreviewCallback(previewCallback);
                            mHandler.postDelayed(this, interval);
                        }
                    });
                    isRunning = true;
                    btn.setText("stop");
                    //動いている時のもの
                    recent_data.setVisibility(View.VISIBLE);
                    recent_time.setVisibility(View.VISIBLE);
                    setting_btn.setVisibility(View.GONE);
                    error_btn.setVisibility(View.GONE);
                }
            }
        });

        //設定ボタンを押した時の処理
        setting_btn = (Button) findViewById(R.id.setting_btn);
        setting_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        //エラー処理
        error_btn = (Button) findViewById(R.id.error_btn);
        final SharedPreferences pref = getSharedPreferences("error", MODE_PRIVATE);
        final String errormsg = pref.getString("error", ERROR_INSTANCE);
        String day = Util.getTimeStamp("yyyy:MM:dd_HH:mm");
        final String m = day + ".txt";
        //エラーが発生してた時の出力の処理
        if(!errormsg.equals(ERROR_INSTANCE)){
            error_btn.setVisibility(View.VISIBLE);
            error_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Util.writeErrorMsg(MainActivity.this, errormsg, m/*"error_msg.txt"*/);
                    pref.edit().putString("error", ERROR_INSTANCE).commit();
                    error_btn.setVisibility(View.GONE);
                }
            });
        }
    }

    //カメラのコールバック
    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            int w = camera.getParameters().getPreviewSize().width;//640
            int h = camera.getParameters().getPreviewSize().height;//480

            //ここではYUV形式で取得になる
            Bitmap bmp = Util.getBitmapImageFromYUV(data,w,h);

            String tmp_data = "";
            String tmp_time = "";
            int selected_color = 0;
            int[] color_palette = new int[5];
            String[] bw_palette = new String[5];
            current_rgb_log = "";


            int[] points_width = {10, 320, 360, 10, 320};
            int[] points_height = {240, 240, 360, 470, 470};


            int black_counter = 0;
            for (int i = 0; i < 5; i++) {
                int[] rgb = Util.getPixelRGB(bmp, points_width[i], points_height[i]);
                if (i == 4) {
                    current_rgb_log += "(" + String.valueOf(rgb[0]) + ":" + String.valueOf(rgb[1]) + ":" + String.valueOf(rgb[2]) + ")";//RGBの巡
                } else {
                    current_rgb_log += "(" + String.valueOf(rgb[0]) + ":" + String.valueOf(rgb[1]) + ":" + String.valueOf(rgb[2]) + "),";//RGBの巡
                }

                if (Util.colorChecker(rgb[0], rgb[1], rgb[2], border) == 0) {
                    black_counter++;
                    bw_palette[i] = "b";
                }else{
                    bw_palette[i] = "w";
                }
                selected_color = rgb[3];
                color_palette[i] = rgb[3];
            }

            if (black_counter >= 2) {
                tmp_data = "0";
                tmp_time = Util.getTimeStamp(format);
            } else {
                tmp_data = "255";
                tmp_time = Util.getTimeStamp(format);
            }

            bmp = null;


            //nullになりうる？

            //データと時間のtextViewを分割して、いつも同じ位置に表示されるようにする
            recent_data.setText(tmp_data);
            recent_time.setText(tmp_time);

            if(printRGB){
                //RGBの値をログに出力
                Util.writeMsg(MainActivity.this, tmp_data + "," + tmp_time + "," + current_rgb_log + "\n", filename);
            }else{
                //普通のログを出力
                Util.writeMsg(MainActivity.this, tmp_data + "," + tmp_time + "\n", filename);
            }

            //パレットに取得した色をセット
            palette0.setBackgroundColor(color_palette[0]);
            palette0.setText(bw_palette[0]);
            if(bw_palette[0].trim().equals("b")){
                palette0.setTextColor(Color.WHITE);
            }else{
                palette0.setTextColor(Color.BLACK);
            }
            palette1.setBackgroundColor(color_palette[1]);
            palette1.setText(bw_palette[1]);
            if(bw_palette[1].trim().equals("b")){
                palette1.setTextColor(Color.WHITE);
            }else{
                palette1.setTextColor(Color.BLACK);
            }
            palette2.setBackgroundColor(color_palette[2]);
            palette2.setText(bw_palette[2]);
            if(bw_palette[2].trim().equals("b")){
                palette2.setTextColor(Color.WHITE);
            }else{
                palette2.setTextColor(Color.BLACK);
            }
            palette3.setBackgroundColor(color_palette[3]);
            palette3.setText(bw_palette[3]);
            if(bw_palette[3].trim().equals("b")){
                palette3.setTextColor(Color.WHITE);
            }else{
                palette3.setTextColor(Color.BLACK);
            }
            palette4.setBackgroundColor(color_palette[4]);
            palette4.setText(bw_palette[4]);
            if(bw_palette[4].trim().equals("b")){
                palette4.setTextColor(Color.WHITE);
            }else{
                palette4.setTextColor(Color.BLACK);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        //カメラを初期化
        initializeCamera();
    }

    public void initializeCamera(){
        //カメラを初期化(カメラの設定などを指定)
        try {
            mCam = Camera.open();
            Camera.Parameters parameters = mCam.getParameters();

            //TODO:調子が悪ければオートフォーカスをonに
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);

            /*Camera.Parameters parameters = mCam.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCam.setParameters(parameters);*/

        }catch (Exception e){
            e.printStackTrace();
        }

        /*preview = (SurfaceView) findViewById(R.id.preview);
        preview.setVisibility(View.VISIBLE);
        mCamPreview = new CameraPreview(MainActivity.this, mCam);

        SurfaceHolder holder = preview.getHolder();
        holder.addCallback(surfaceHolderCallback);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);*/
    }



    @Override
    protected void onPause() {
        super.onPause();
        if(mHandler != null){
            mHandler.removeCallbacksAndMessages(null);
        }

        isRunning = false;

        //カメラ破棄インスタンスを解放
        if (mCam != null) {
            mCam.release();
            mCam = null;
            finish();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //変更した設定値を読み込み
        setValues();
    }



    SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        //SurfaceView 生成
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                // カメラインスタンスに、画像表示先を設定
                mCam.setPreviewDisplay(holder);
                //縦画面に適切になるように画面を回転
                mCam.setDisplayOrientation(90);

                // プレビュー開始
                mCam.startPreview();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "エラーが発生しています", Toast.LENGTH_SHORT).show();
            }
        }

        //SurfaceView 破棄
        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        //SurfaceHolder が変化したときのイベント
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // 画面回転に対応する場合は、ここでプレビューを停止し、
            // 回転による処理を実施、再度プレビューを開始する。
        }
    };
}

