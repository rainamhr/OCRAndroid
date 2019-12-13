package com.example.ocrandroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private CameraSource mCameraSource;
    private TextRecognizer mTextRecognizer;
    private SurfaceView mSurfaceView;
    private TextView mTextView;

    private static final String TAG = "MainActivity";

    private static final int RC_HANDLE_CAMERA_PERM = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.textView);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startTextRecognizer();
        } else {
            askCameraPermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraSource.stop();
    }

    private void startTextRecognizer() {
        mTextRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!mTextRecognizer.isOperational()) {
            Toast.makeText(getApplicationContext(), "Oops ! Not able to start the text recognizer ...", Toast.LENGTH_LONG).show();
        } else {
            mCameraSource = new CameraSource.Builder(getApplicationContext(), mTextRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(15.0f)
                    .setAutoFocusEnabled(true)
                    .build();

            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            mCameraSource.start(mSurfaceView.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        askCameraPermission();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            mTextRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    SparseArray<TextBlock> items = detections.getDetectedItems();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < items.size(); ++i) {
                        TextBlock item = items.valueAt(i);
                        stringBuilder.append(item.getValue());
                        stringBuilder.append("/");
                        // The following Process is used to show how to use lines & elements as well
                        for (int j = 0; j < items.size(); j++) {
                            TextBlock textBlock = items.valueAt(j);
                            stringBuilder.append(textBlock.getValue());
                            stringBuilder.append("/");
                            for (Text line : textBlock.getComponents()) {
                                //extract scanned text lines here
                                Log.v("lines", line.getValue());
                                stringBuilder.append(line.getValue());
                                stringBuilder.append("/");
                                for (Text element : line.getComponents()) {
                                    //extract scanned text words here
                                    Log.v("element", element.getValue());
                                    stringBuilder.append(element.getValue());

                                    SharedPreferences.Editor editor = getSharedPreferences("NAME", MODE_PRIVATE).edit();
                                    editor.putString("LINE", line.getValue());
                                    editor.putString("ELEMENT", element.getValue());
                                    editor.apply();
                                }
                            }
                        }
                    }

                    SharedPreferences prefs = getSharedPreferences("NAME", MODE_PRIVATE);
                    String line = prefs.getString("LINE", "");//"No name defined" is the default value.
                    String element = prefs.getString("ELEMENT", "");

                    Log.d(TAG, "receiveDetections: line" + line);
                    Log.d(TAG, "receiveDetections: element" + element);

                    final String fullText = stringBuilder.toString();
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            mTextView.setText(fullText);
                            Log.d(TAG, "runhere: fulltext" + fullText);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTextRecognizer();
            return;
        }
    }

    private void askCameraPermission() {

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }
    }
}