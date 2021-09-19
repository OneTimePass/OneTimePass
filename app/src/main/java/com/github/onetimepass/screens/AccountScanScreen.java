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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.Result;
import com.github.onetimepass.R;
import com.github.onetimepass.core.Constants;
import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.SupportBar;
import com.github.onetimepass.core.Utility;
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.ControlAction;
import com.github.onetimepass.core.control.Controller;
import com.github.onetimepass.core.screen.Screen;

import me.dm7.barcodescanner.zxing.ZXingScannerView;


/**
 * Manages "account scanning" screen. Using the camera, begin continuous
 * scanning for QR codes. Transitions to the edit screen upon successful
 * scan or returns to account list.
 */
public class AccountScanScreen extends Screen
        implements
        ZXingScannerView.ResultHandler,
        Controller.CameraInterface {

    private ZXingScannerView mScannerView;

    /**
     * Setup a new instance of the Account Scan Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public AccountScanScreen(final Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "account_scan",
                        R.string.scan_qr,
                        R.string.scan_qr,
                        R.layout.fragment_scan_qr,
                        R.menu.options_scan_qr,
                        R.id.optgrp_scan_qr,
                        true,
                        false,
                        false
                )
        );

        /**
         * "scan_qr" - control action used internally to request camera
         * permissions and then kickoff scanning procedure. Simply viewing
         * the Scan screen isn't enough to make the camera kick in.
         */
        controller.registerControlAction(
                new ControlAction() {
                    @Override
                    public String getTag() {
                        return "scan_qr";
                    }

                    @Override
                    public boolean needsToBeAlive() { return true; }

                    @Override
                    public void performAction(Context context, String[] data) {
                        Notify.Debug();
                        requestCameraPermission();
                    }
                }
        );

        controller.setCameraScreen(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state, View layout) {
        Notify.Debug();
        mScannerView = layout.findViewById(R.id.scanner_fragment);
        mScannerView.setAutoFocus(true);
        mScannerView.setFormats(ZXingScannerView.ALL_FORMATS);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug();
        getController().setTitle(R.string.scan_qr);
        SupportBar.getInstance().UpdateActionBarSettings(true);
        // Register ourselves as a handler for scan results.
        mScannerView.setResultHandler(this);
        // Start camera on resume
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        Notify.Debug();
        // Stop camera on pause
        mScannerView.stopCamera();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel:
                getController().performControlAction("default");
                return true;
        }
        return false;
    }

    @Override
    public void handleResult(Result raw) {
        Notify.Debug("received: "+raw.toString());
        getController().popBackStack();
        final Uri uri = Uri.parse(raw.getText());
        if (Utility.verifyOtpauthUri(uri)) {
            Notify.Debug("appLinkData validated");
            getController().popBackStack(true);
            getController().performControlAction(
                    "otpauth",
                    new String[] {uri.toString()}
            );
        } else {
            Notify.Error("Invalid app link received.");
        }
    }

    private void requestCameraPermission() {
        Notify.Debug();
        int permissionCheck = ContextCompat.checkSelfPermission(
                getController(),
                Manifest.permission.CAMERA
        );
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Notify.Debug("requesting permissions");
            getController().setCameraResultWanted("account_scan");
            ActivityCompat.requestPermissions(
                    getController(),
                    new String[]{Manifest.permission.CAMERA},
                    Constants.REQ_PERM_CAMERA
            );
        } else {
            onCameraPermissionGranted();
        }
    }

    public void onCameraPermissionGranted() {
        Notify.Debug();
        getController().transitionToScreen("account_scan");
    }

    public void onCameraPermissionDenied() {
        Notify.Long(getController(),R.string.error_no_cam_perm);
    }
}
