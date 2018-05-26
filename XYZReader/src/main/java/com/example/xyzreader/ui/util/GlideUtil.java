package com.example.xyzreader.ui.util;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class GlideUtil {

    public static void displayImage(@NonNull String photo, @NonNull ImageView imageView) {
        Glide.with(imageView.getContext())
                .load(photo)
                .placeholder(new ColorDrawable(Color.GRAY))
                .error(new ColorDrawable(Color.BLACK))
                .centerCrop()
                .into(imageView);
    }

}
