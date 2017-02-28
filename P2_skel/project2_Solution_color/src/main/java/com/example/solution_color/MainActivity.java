package com.example.solution_color;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.library.bitmap_utilities.BitMap_Helpers;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity  {

    private Toolbar toolbar;
    private Bitmap photo;
    private final int CAMERA_REQUEST = Constants.CAMERA_REQUEST_VALUE;
    private ImageView imageView;
    private int mSaturation;
    private int mPercent;
    private String text;
    private String subject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        this.imageView = (ImageView)this.findViewById(R.id.background);
        this.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        photo = drawable.getBitmap();

        ImageButton photoButton = (ImageButton) this.findViewById(R.id.camera_button);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences myPreference = PreferenceManager.getDefaultSharedPreferences(this);
        mPercent = myPreference.getInt("sketchiness", mPercent);
        mSaturation = myPreference.getInt("saturation", mSaturation);

        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        switch (item.getItemId()) {
            case R.id.action_undo:
                //imageView.setImageBitmap(photo);
                imageView.setImageResource(R.drawable.gutters);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
            case R.id.action_sketchy:
                Bitmap sketchyPhoto = BitMap_Helpers.thresholdBmp(bitmap, mPercent);
                imageView.setImageBitmap(sketchyPhoto);
                break;
            case R.id.action_colorize:
                Bitmap sketchPhoto = BitMap_Helpers.thresholdBmp(bitmap, mPercent);
                Bitmap colorPhoto = BitMap_Helpers.colorBmp(bitmap, mSaturation);
                BitMap_Helpers.merge(colorPhoto, sketchPhoto);
                imageView.setImageBitmap(colorPhoto);
                break;
            case R.id.action_share:
                shareIntentCreate();
                break;
            case R.id.action_settings:
                Intent myIntent = new Intent(this, SettingsActivity.class);
                startActivity(myIntent);
                break;
            default:
                break;
        }
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }
    }

    public void shareIntentCreate() {
        Uri uri = null;
        try {
            uri = saveToFileAndUri();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences myPreference = PreferenceManager.getDefaultSharedPreferences(this);
        text = myPreference.getString("text", text);
        subject = myPreference.getString("subject", subject);
        Intent sendIntent = new Intent();
        sendIntent.setType("image/*");
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        if (uri != null) {
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        }
        startActivity(Intent.createChooser(sendIntent, "Share Image"));
    }

    private Uri saveToFileAndUri() throws Exception{
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        long currentTime = System.currentTimeMillis();
        String fileName = "MY_APP_" + currentTime+".jpg";
        File extBaseDir = Environment.getExternalStorageDirectory();
        File file = new File(extBaseDir.getAbsoluteFile()+"/MY_DIRECTORY");
        if(!file.exists()){
            if(!file.mkdirs()){
                throw new Exception("Could not create directories, "+file.getAbsolutePath());
            }
        }
        String filePath = file.getAbsolutePath()+"/"+fileName;
        FileOutputStream out = new FileOutputStream(filePath);

        bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.JPEG_COMPRESS_VALUE, out);
        out.flush();
        out.close();

        long size = new File(filePath).length();

        ContentValues values = new ContentValues(Constants.CONTENT_VALUES);
        values.put(MediaStore.Images.Media.TITLE, fileName);

        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.DATE_ADDED, currentTime);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.ORIENTATION, Constants.ORIENTATION_VALUE);
        values.put(MediaStore.Images.Media.DATA, filePath);
        values.put(MediaStore.Images.Media.SIZE, size);

        return this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

    }

}

