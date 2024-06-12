package io.esper.android.imin;

import android.annotation.SuppressLint;
import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;

import io.esper.android.files.R;
import io.esper.android.files.util.GeneralUtils;


@SuppressLint("NewApi")
public class DifferentDisplay extends Presentation {

    public DifferentDisplay(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (GeneralUtils.isIminUsingVideos()) {
            setContentView(R.layout.imin_app_layout_video);
        } else {
            setContentView(R.layout.imin_app_layout_photo);
        }
    }
}
