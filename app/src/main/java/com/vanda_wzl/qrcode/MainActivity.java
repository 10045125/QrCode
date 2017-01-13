/*
 * Copyright (C) 2005-2017 UCWeb Inc. All rights reserved.
 *  Description :MainActivity.java
 *
 *  Creation    : 2017-01-13
 *  Author      : zhonglian.wzl@alibaba-inc.com
 */

package com.vanda_wzl.qrcode;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.zxing.Result;
import com.vanda.qrcode.QrCodeView;

public class MainActivity extends AppCompatActivity implements QrCodeView.ResultCallback {

    private QrCodeView mQrCodeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mQrCodeView = new QrCodeView(this, null);
        mQrCodeView.registerResultCallback(this);
        mQrCodeView.setText(getString(R.string.qrcode_desc));

        setContentView(mQrCodeView);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mQrCodeView != null) {
            mQrCodeView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mQrCodeView != null) {
            mQrCodeView.onResume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mQrCodeView != null) {
            mQrCodeView.onDestroy();
            mQrCodeView = null;
        }
    }

    @Override
    public void onResultCallback(Result result) {
        if (result != null) {
            if (mQrCodeView != null) {
                mQrCodeView.onPause();
            }
            new android.support.v7.app.AlertDialog.Builder(this).setTitle(result.getText())
                    .setCancelable(true)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if (mQrCodeView != null) {
                                mQrCodeView.onResume();
                            }
                        }
                    }).create().show();
        }
    }
}
