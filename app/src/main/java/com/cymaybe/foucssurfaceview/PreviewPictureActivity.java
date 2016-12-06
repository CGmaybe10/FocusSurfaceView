package com.cymaybe.foucssurfaceview;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import static com.cymaybe.foucssurfaceview.MainActivity.TAKE_PICTURE;

/**
 * Created by moubiao on 2016/12/6.
 */

public class PreviewPictureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview_picture_layout);

        Intent data = getIntent();
        Bitmap bitmap = data.getParcelableExtra(TAKE_PICTURE);
        ImageView previewImg = (ImageView) findViewById(R.id.preview_img);
        previewImg.setImageBitmap(bitmap);
    }
}
