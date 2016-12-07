package com.cymaybe.foucssurfaceview.fragment;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cymaybe.foucssurfaceview.R;

/**
 * Created by moubiao on 2016/12/7.
 */

public class PictureFragment extends DialogFragment {
    public static final String ORIGIN_PICTURE = "originPic";
    public static final String CROP_PICTURE = "cropPic";

    private Bitmap mOriginPicBitmap, mCropPicBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle data = getArguments();
        if (data != null) {
            mOriginPicBitmap = data.getParcelable(ORIGIN_PICTURE);
            mCropPicBitmap = data.getParcelable(CROP_PICTURE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.picture_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ImageView originImg = (ImageView) view.findViewById(R.id.origin_picture_img);
        originImg.setImageBitmap(mOriginPicBitmap);
        ImageView cropImg = (ImageView) view.findViewById(R.id.crop_picture_img);
        cropImg.setImageBitmap(mCropPicBitmap);
    }
}
