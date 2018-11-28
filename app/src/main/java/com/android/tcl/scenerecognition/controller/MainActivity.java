package com.android.tcl.scenerecognition.controller;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.tcl.scenerecognition.R;
import com.android.tcl.scenerecognition.common.Common;
import com.android.tcl.scenerecognition.common.ResultData;
import com.android.tcl.scenerecognition.utils.TfliteUtils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;

public class MainActivity extends AppCompatActivity {

    private String imagePath = "";
    public static final int REQUEST_PICK_IMAGE = 0;
    private static final String TAG = "MainActivity";
    private ImageView imageV;
    private TextView scene;
    private TextView time;
    private TextView degree;
    private ResultData result = new ResultData();
    private static final int PERMISSIONS_REQUEST_STORAGE = 1;
    private boolean isGranted = false;
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupUI();

        // Example of a call to a native method
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_STORAGE);
            isGranted = false;
        }else{
            isGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == PERMISSIONS_REQUEST_STORAGE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "没有存储权限!", Toast.LENGTH_SHORT).show();
            }else{
                isGranted = true;
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setupUI(){
        imageV = (ImageView) findViewById(R.id.imageView);
        scene = (TextView) findViewById(R.id.scene);
        degree = (TextView) findViewById(R.id.degree);
        time = (TextView) findViewById(R.id.time);
    }

    private ByteBuffer prepareImg(Bitmap bmp){
        Bitmap bitmap = Bitmap.createScaledBitmap(bmp, Common.DIM_IMG_SIZE_X, Common.DIM_IMG_SIZE_Y, false);
        ByteBuffer imgData;
        int[] colorValues;
        colorValues = new int[Common.DIM_IMG_SIZE_X * Common.DIM_IMG_SIZE_Y];   //存储像素
        byte[] floatValues = new byte[Common.DIM_IMG_SIZE_X * Common.DIM_IMG_SIZE_Y * 3]; //RGB像素*3
        imgData = ByteBuffer.wrap(floatValues, 0, Common.DIM_IMG_SIZE_X * Common.DIM_IMG_SIZE_Y * 3);
        bitmap.getPixels(colorValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();
        for (int i = 0; i < colorValues.length; i++) {
            int value = colorValues[i];
            imgData.put((byte)((value >> 16) & 0xFF));
            imgData.put((byte)((value >> 8) & 0xFF));
            imgData.put((byte)((value & 0xFF)));
        }
        Log.i("jjj", "----> " + (int)imgData.array()[0] + " " + (int)imgData.array()[1]);
        Log.i("jjj", "----> " + (int)imgData.array()[2] + " " + (int)imgData.array()[3]);

        bitmap.recycle();
        return imgData;
    }


    private void runRecognize() {
        try {
            if (TextUtils.isEmpty(imagePath)) {
                scene.setText("Scene: " + getString(R.string.detect_fail));
                return;
            }
            long start = System.currentTimeMillis();
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

            ByteBuffer fb = prepareImg(bitmap);
            if (bitmap != null) {
                int rtn = TfliteUtils.runTfliteModel(fb.array(), result);
                if(0 == rtn){
                    long end = System.currentTimeMillis();
                    degree.setText("Precision: " + result.precision);
                    time.setText("Time: " + (end - start) + "ms");
                    scene.setText("Scene: " + result.scene);
                }else{
                    scene.setText("Scene: " + getString(R.string.detect_fail));
                }
                bitmap.recycle();
            } else {
                scene.setText("Scene: " + getString(R.string.detect_fail));
            }
        } catch (Exception e) {
            scene.setText("Scene: " + getString(R.string.detect_fail));
            e.printStackTrace();
        }
    }

    public void onClick(View view){
        if(!isGranted) {
            Toast.makeText(MainActivity.this, "没有存储权限!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(view.getId() == R.id.start) {
            runRecognize();
        } else if(R.id.choice == view.getId()){
            choosePic();
        }

    }

    public void choosePic() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");// 相片类型
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri uri = data.getData();
                        imagePath = getAbsoluteImagePath(uri);
                        imageV.setImageURI(uri);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, "获取图片失败");
                    }
                } else {
                    Log.d(TAG, "获取图片失败");
                }
                break;

            default:
                break;
        }
    }

    public String getAbsoluteImagePath(Uri contentUri) {
        if (contentUri == null) {
            return null;
        }
        String filePath = "";
        String[] filePathColumn = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(contentUri, filePathColumn, null,
                null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
            cursor.close();
        }
        return filePath;
    }

}
