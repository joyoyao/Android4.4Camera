/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sprd.camera;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.camera.ui.RotateLayout;
import com.android.camera2.R;

// A layout that show the restore dialog in a popup window
public class AlertDialogPopup extends RotateLayout {
    private static final String TAG = "AlertDialogPopup";
    private TextView mRotateDialogTitle;
    private TextView mRotateDialogText;
    private TextView mRotateDialogButton1;
    private TextView mRotateDialogButton2;

    public AlertDialogPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mRotateDialogTitle = (TextView) findViewById(R.id.rotate_dialog_title);
        mRotateDialogText = (TextView) findViewById(R.id.rotate_dialog_text);
        mRotateDialogButton1 = (Button) findViewById(R.id.rotate_dialog_button1);
        mRotateDialogButton2 = (Button) findViewById(R.id.rotate_dialog_button2);
    }

    public void setAlertDialog(String title, String msg, String button1Text,
                final Runnable r1, String button2Text, final Runnable r2) {
        if (title != null) {
            mRotateDialogTitle.setText(title);
        }

        mRotateDialogText.setText(msg);

        if (button1Text != null) {
            mRotateDialogButton1.setText(button1Text);
            mRotateDialogButton1.setContentDescription(button1Text);
            mRotateDialogButton1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (r1 != null) r1.run();
                }
            });
        }
        if (button2Text != null) {
            mRotateDialogButton2.setText(button2Text);
            mRotateDialogButton2.setContentDescription(button2Text);
            mRotateDialogButton2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (r2 != null) r2.run();
                }
            });
        }
    }
}
