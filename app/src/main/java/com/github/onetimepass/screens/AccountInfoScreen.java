package com.github.onetimepass.screens;
/*
 This software is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; specifically
 version 2.1 of the License and not any other version.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.github.onetimepass.AlarmBroadcastReceiver;
import com.github.onetimepass.R;
import com.github.onetimepass.core.Constants;
import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.Storage;
import com.github.onetimepass.core.SupportBar;
import com.github.onetimepass.core.account.AccountEntry;
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.Controller;
import com.github.onetimepass.core.screen.Screen;

import java.util.Date;

import static android.content.Context.CLIPBOARD_SERVICE;


/**
 * Manages the "account info" screen which displays the account's TOTP secret,
 * allows for copying of the secret (by tapping the code) and viewing a QR
 * code that other TOTP applications (on another device) can use to transfer
 * the specific account. This provides a rudimentary way to export a single
 * account into any other QR-code supporting TOTP application.
 */
public class AccountInfoScreen extends Screen {

    private AccountEntry mAccountEntry;
    private TextView mSecretView;
    private TextView mLabelView;
    private TextView mIssuerView;
    private ProgressBar mProgressView;
    private ViewSwitcher mDetailsFlipper;
    private ImageView mQrCodeView;
    private Bitmap mQrCodeBitmap;

    private Handler mHandlerProgress;
    private boolean mReturnAfterEdit = false;

    /**
     * Setup a new instance of the Account Info Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public AccountInfoScreen(Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "account_info",
                        R.string.details,
                        R.string.details,
                        R.layout.fragment_account_info,
                        R.menu.options_account_info,
                        R.id.optgrp_account_info,
                        true,
                        false,
                        false
                )
        );
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, View layout) {
        Notify.Debug();
        mSecretView = layout.findViewById(R.id.account_secret_view);
        mLabelView = layout.findViewById(R.id.account_label_view);
        mIssuerView = layout.findViewById(R.id.account_issuer_view);
        mProgressView = layout.findViewById(R.id.account_progress_view);
        mDetailsFlipper = layout.findViewById(R.id.account_details_flipper);
        mQrCodeView = layout.findViewById(R.id.account_qrcode_image);
        ImageView mQrCodeIcon = layout.findViewById(R.id.account_qrcode_icon);

        mHandlerProgress = new Handler(Looper.getMainLooper());

        String[] argv = getInboundData();
        if (argv != null && argv.length > 0) {
            Uri uri = Uri.parse(argv[0]);
            mAccountEntry = getStorage().FindAccount(uri);
            if (mAccountEntry != null)
                Notify.Debug("found account: "+ mAccountEntry.toString());
            else
                Notify.Debug("didn't find matching account: "+uri);
        } else {
            mAccountEntry = null;
            Notify.Debug("no account given?!");
        }

        mDetailsFlipper.reset();
        mDetailsFlipper.showNext();

        mQrCodeView.setImageResource(R.mipmap.ic_logo_round);
        if (mAccountEntry != null)
            mQrCodeIcon.setImageDrawable(mAccountEntry.MakeIconDrawable());

        mDetailsFlipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAccountEntry != null && mQrCodeBitmap == null) {
                    SupportBar.getInstance().ShowSpinnerBox(R.string.account_make_qr);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            int fg = getController().getResources().getColor(R.color.qr_code_fg);
                            int bg = getController().getResources().getColor(R.color.qr_code_bg);
                            mQrCodeBitmap = mAccountEntry.MakeQrCodeBitmap(fg, bg);
                            mQrCodeView.setImageBitmap(mQrCodeBitmap);
                            SupportBar.getInstance().HideAll();
                            mDetailsFlipper.showNext();
                        }
                    });
                } else {
                    mDetailsFlipper.showNext();
                }
            }
        });

        mSecretView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Notify.Debug();
                        final Activity activity = getController();
                        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("text", mAccountEntry.totpString());
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                        }
                        long ms = getSecretTimeDelta();
                        Notify.Short(getController(),R.string.clipboard_expires_in,(ms/1000));
                        Intent intentToFire = new Intent(activity.getApplicationContext(), AlarmBroadcastReceiver.class);
                        intentToFire.setAction(Constants.ACTION_ALARM);
                        PendingIntent alarmIntent = PendingIntent
                                .getBroadcast(
                                        activity.getApplicationContext(),
                                        0,
                                        intentToFire,
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                );
                        AlarmManager alarmManager = (AlarmManager)activity
                                .getApplicationContext().
                                        getSystemService(Context.ALARM_SERVICE);
                        int sdk_int = android.os.Build.VERSION.SDK_INT;
                        if (sdk_int >= Build.VERSION_CODES.M) {
                            if (alarmManager != null) {
                                alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                        getSecretTimeElapsedDelta(),
                                        alarmIntent
                                );
                            }
                        } else if (sdk_int >= Build.VERSION_CODES.KITKAT && sdk_int < Build.VERSION_CODES.M) {
                            if (alarmManager != null) {
                                alarmManager.setExact(
                                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                        getSecretTimeElapsedDelta(),
                                        alarmIntent
                                );
                            }
                        } else {
                            if (alarmManager != null) {
                                alarmManager.set(
                                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                        getSecretTimeElapsedDelta(),
                                        alarmIntent
                                );
                            }
                        }
                    }
                }
        );

        updateViews();
        startSecretTimer();
        SupportBar.getInstance().HideAll();

        return layout;
    }


    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug();
        if (mReturnAfterEdit) {
            getController().popBackStack();
        }
        mQrCodeBitmap = null;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        FragmentManager fm = getController().getSupportFragmentManager();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel_account:
                getController().performControlAction("default");
                return true;
            case R.id.action_edit_account:
                mReturnAfterEdit = true;
                getController().transitionToScreen(
                        "account_edit",
                        new String[] {mAccountEntry.toUri().toString()}
                );
                return true;
            case R.id.action_delete_account:
                SupportBar.getInstance()
                        .ShowYesNoBox(R.string.delete, R.string.account_delete_prompt,
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Notify.Debug();
                                        Storage storage = Storage.getInstance(getController());
                                        if (storage.RemoveAccount(mAccountEntry))
                                            storage.Save();
                                        getController().performControlAction("default");
                                    }
                                },
                                null
                        );
                return true;
            default:
                Notify.Debug("unknown menu item: "+item.toString());
                break;
        }
        return super.onOptionsItemSelected(item);
    }




    private void updateViews() {
        if (mAccountEntry != null) {
            mSecretView.setText(mAccountEntry.totpString());
            mLabelView.setText(mAccountEntry.getLabel());
            mIssuerView.setText(mAccountEntry.getIssuer());
        }
        long n = getSecretTimeDelta();
        double p = (double)n / 30000.0 * 100.0;
        mProgressView.setProgress((int)p);
    }

    private long getSecretTimeDelta() {
        long msTime = System.currentTimeMillis();
        Date curDateTime = new Date(msTime);
        int sec = curDateTime.getSeconds();
        int delay;
        if (sec < 30) {
            delay = 1000 * (30-sec);
        } else {
            delay = 1000 * (60-sec);
        }
        return delay;
    }

    private long getSecretTimeElapsedDelta() {
        long msTime = SystemClock.elapsedRealtime();
        return msTime + getSecretTimeDelta();
    }

    private void startSecretTimer() {
        Notify.Debug();
        mHandlerProgress.postDelayed(new Runnable() {
            public void run() {
                long n = getSecretTimeDelta();
                double p = (double)n / 30000.0 * 100.0;
                if ((int)p == 0)
                    updateViews();
                mProgressView.setProgress((int)p);
                mHandlerProgress.postDelayed(this, 250);
            }
        }, 250);
    }

}
