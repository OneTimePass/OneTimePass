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
import android.os.Bundle;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.onetimepass.R;
import com.github.onetimepass.core.Constants;
import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.Storage;
import com.github.onetimepass.core.SupportBar;
import com.github.onetimepass.core.Utility;
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.Controller;
import com.github.onetimepass.core.screen.Screen;
import com.rustamg.filedialogs.FileDialog;
import com.rustamg.filedialogs.SaveFileDialog;

import java.io.File;


/**
 * Manage the "export" screen. Prompt the user to pick (and confirm) a
 * passphrase and file path to export as. The exported file is AES
 * encrypted and not useful to any other application, so this is more
 * of a "backup" than "export".
 */
final public class ExportScreen extends Screen {

    private EditText mPassPhrase;
    private EditText mPassConfirm;
    private TextView mExportPathView;

    private File mChosenFilePath;

    /**
     * Setup a new instance of the Export Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public ExportScreen(final Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "export",
                        R.string.export_data,
                        R.string.export_data,
                        R.layout.fragment_export,
                        R.menu.options_export,
                        R.id.optgrp_export,
                        true,
                        false,
                        false
                )
        );

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, View layout) {
        Notify.Debug();
        mPassPhrase = layout.findViewById(R.id.newphrase_text);
        mPassPhrase.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mPassPhrase.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    mPassConfirm.requestFocus();
                    return true;
                }
                return false;
            }
        });
        mPassConfirm = layout.findViewById(R.id.passconfirm_text);
        mPassConfirm.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mPassConfirm.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    HideKeyboard();
                    onChoosePathButtonClick();
                    return true;
                }
                return false;
            }
        });

        TextWatcher listener = Utility.makePasswordTextWatcher(
                mPassPhrase,
                mPassConfirm
        );
        mPassPhrase.addTextChangedListener(listener);
        mPassConfirm.addTextChangedListener(listener);

        mExportPathView = layout.findViewById(R.id.export_path_view);
        Button mChoosePathBttn = layout.findViewById(R.id.choose_path_button);
        mChoosePathBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChoosePathButtonClick();
            }
        });
        return layout;
    }

    private void onChoosePathButtonClick() {
        Notify.Debug();
        int check_write = ContextCompat.checkSelfPermission(
                getController(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
        int check_read = ContextCompat.checkSelfPermission(
                getController(),
                Manifest.permission.READ_EXTERNAL_STORAGE
        );
        if ( check_write != PackageManager.PERMISSION_GRANTED || check_read != PackageManager.PERMISSION_GRANTED )
        {
            boolean need_reason = ActivityCompat.shouldShowRequestPermissionRationale(
                    getController(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
            if (need_reason) {
                SupportBar.getInstance().ShowMessageBox(
                        R.string.export_data,
                        R.string.rw_file_perm_reason
                );
            } else {
                Notify.Debug("requested");
                ActivityCompat.requestPermissions(
                        getController(),
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        Constants.REQ_PERM_RWFILE
                );
            }
            return;
        }
        Notify.Debug("granted ("+check_write+")");
        onFilePermissionGranted(Constants.REQ_PERM_RWFILE);
    }

    private boolean isPassPhraseConfirmed() {
        String p0 = mPassPhrase.getText().toString();
        String p1 = mPassConfirm.getText().toString();
        if (!p0.isEmpty() && !p1.isEmpty()) {
            if (p0.equals(p1)) {
                Notify.Debug("passphrase confirmed");
                return true;
            }
        }
        return false;
    }

    @Override
    public void onFilePermissionGranted(int requestCode) {
        Notify.Debug();
        HideKeyboard();
        File dl_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        Bundle args = new Bundle();
        args.putString(FileDialog.EXTENSION,".otpd");
        args.putSerializable(FileDialog.START_DIRECTORY,dl_dir);
        FileDialog dialog = new SaveFileDialog();
        dialog.setArguments(args);
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.OneTimePassTheme);
        getController().setWaitingForFileSelection(this);
        dialog.show(getController().getSupportFragmentManager(),"export_otpd_file");
    }

    @Override
    public void onFilePermissionDenied(int requestCode) {
        Notify.Debug();
        SupportBar.getInstance().ShowMessageBox(
                R.string.export_data,
                R.string.rw_file_perm_reason
        );
        getController().popBackStack();
    }

    @Override
    public void onFileSelected(FileDialog dialog, File file) {
        Notify.Debug();
        HideKeyboard();
        if (!file.exists()) {
            String path = file.getAbsolutePath();
            while (path.endsWith(".otpd")) {
                path = path.replace(".otpd", "");
            }
            path += ".otpd";
            mChosenFilePath = new File(path);
        } else {
            mChosenFilePath = file;
        }
        mExportPathView.setText(mChosenFilePath.toString());
        if (dialog != null)
            dialog.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug();
        SupportBar.getInstance().HideAll();
        ResetState();
        ShowKeyboard( mPassPhrase );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel_export:
                getController().performControlAction("default");
                return true;
            case R.id.action_export_data:
                doActionExport();
                return true;
        }
        return false;
    }


    private void ResetState() {
        mPassPhrase.setText("");
        mPassConfirm.setText("");
        mExportPathView.setText("");
        mChosenFilePath = null;
    }

    private void doActionExport() {
        Notify.Debug();
        HideKeyboard();
        String pass = mPassPhrase.getText().toString();
        if (pass.isEmpty()) {
            Notify.Short(getController(),R.string.error_no_pass);
            return;
        }
        if (!isPassPhraseConfirmed()) {
            Notify.Short(getController(),R.string.error_pass_no_match);
            return;
        }
        if (mChosenFilePath == null) {
            Notify.Short(getController(),R.string.storage_404);
            return;
        }
        Storage.PerformOperation(
                getController(),
                "export",
                pass,
                mChosenFilePath.toString(),
                new Storage.Operation() {
                    @Override
                    public void onStorageSuccess(Context context, Storage instance) {
                        Notify.Debug();
                        SupportBar.getInstance().ShowMessageBox(
                                R.string.export_data,
                                R.string.storage_exported
                        );
                        getController().performControlAction("default");
                    }

                    @Override
                    public void onStorageFailure() {
                        Notify.Debug();
                        SupportBar.getInstance().ShowMessageBox(
                                R.string.export_data,
                                R.string.error_storage_export
                        );
                    }
                }
        );
    }
}
