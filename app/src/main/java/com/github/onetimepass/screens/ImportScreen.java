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
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
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
import com.rustamg.filedialogs.OpenFileDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


/**
 * Manage the "import" screen. This is where the user can provide the password
 * and an previously exported .otpd file for importing. Can either merge or
 * replace the entire account listing.
 */
final public class ImportScreen extends Screen {

    private EditText mPassPhrase;
    private TextView mExportPathView;

    private File mChosenFilePath;
    private Uri mChosenFileUri;

    /**
     * Setup a new instance of the Import Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public ImportScreen(final Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "import",
                        R.string.import_data,
                        R.string.import_data,
                        R.layout.fragment_import,
                        R.menu.options_import,
                        R.id.optgrp_import,
                        true,
                        true,
                        false
                )
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, View layout) {
        Notify.Debug(".");
        mPassPhrase = layout.findViewById(R.id.newphrase_text);
        mPassPhrase.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mPassPhrase.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    HideKeyboard();
                    doActionImport();
                    return true;
                }
                return false;
            }
        });

        mExportPathView = layout.findViewById(R.id.import_path_view);
        Button mChoosePathBttn = layout.findViewById(R.id.choose_path_button);
        mChoosePathBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                                R.string.import_data,
                                R.string.rw_file_perm_reason
                        );
                    } else {
                        Notify.Debug("onClick - requested");
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
                Notify.Debug("onClick - granted ("+check_write+")");
                onFilePermissionGranted(Constants.REQ_PERM_RWFILE);
            }
        });
        return layout;
    }

    @Override
    public void onFilePermissionGranted(int requestCode) {
        Notify.Debug();
        HideKeyboard();
        File dl_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        Bundle args = new Bundle();
        args.putString(FileDialog.EXTENSION,".otpd");
        args.putSerializable(FileDialog.START_DIRECTORY,dl_dir);
        FileDialog dialog = new OpenFileDialog();
        dialog.setArguments(args);
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.OneTimePassTheme);
        getController().setWaitingForFileSelection(this);
        dialog.show(getController().getSupportFragmentManager(),"import_otpd_file");
    }

    @Override
    public void onFilePermissionDenied(int requestCode) {
        Notify.Debug();
        SupportBar.getInstance().ShowMessageBox(
                R.string.import_data,
                R.string.rw_file_perm_reason
        );
        getController().popBackStack();
        mPassPhrase.setText("");
        mPassPhrase.setEnabled(false);
    }

    @Override
    public void onFileSelected(FileDialog dialog, File file) {
        Notify.Debug();
        HideKeyboard();
        mChosenFilePath = file;
        mExportPathView.setText(file.toString());
        mPassPhrase.setEnabled(true);
        ShowKeyboard( mPassPhrase );
        if (dialog != null)
            dialog.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug();
        SupportBar.getInstance().HideAll();
        ResetState();
        if (mPassPhrase.isEnabled())
            ShowKeyboard( mPassPhrase );
        else
            HideKeyboard();
        Uri uri = getInboundUri();
        if (uri != null) {
            setInboundUri(null);
            handleInboundUri(uri);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel_import:
                getController().performControlAction("default");
                return true;
            case R.id.action_import_data:
                doActionImport();
                return true;
        }
        return false;
    }

    private void ResetState() {
        Notify.Debug();
        mPassPhrase.setText("");
        mPassPhrase.setEnabled(false);
        mExportPathView.setText("");
        mChosenFilePath = null;
    }

    @Override
    public boolean processInboundUri(final Uri in) {
        Notify.Debug();
        switch (in.getScheme()) {
            case "file":
            case "content":
                // maybe check for .otpd?
                super.setInboundUri(in);
                return true;
        }
        return false;
    }

    private void handleInboundUri(final Uri in) {
        Notify.Debug();
        InputStream inputStream;
        switch (in.getScheme()) {
            case "file":
                String path = in.getEncodedPath();
                File file = new File(path);
                try {
                    inputStream = new FileInputStream(file);
                } catch (Exception e) {
                    Notify.Debug(e.getMessage());
                    e.printStackTrace();
                    Notify.Long(getController(),R.string.storage_404);
                    return;
                }
                break;
            case "content":
                try {
                    inputStream = getController().getContentResolver().openInputStream(in);
                } catch (Exception e) {
                    Notify.Debug(e.getMessage());
                    e.printStackTrace();
                    Notify.Long(getController(),R.string.storage_404);
                    return;
                }
                break;
            default:
                Notify.Debug("Not a file or content URI");
                return;
        }
        if (inputStream != null) {
            SupportBar.getInstance().ShowYesNoBox(
                    R.string.account_inbound_uri,
                    R.string.account_inbound_uri_content,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Notify.Debug();
                            mChosenFileUri = in;
                            mExportPathView.setText(in.toString());
                            mPassPhrase.setText("");
                            mPassPhrase.setEnabled(true);
                            Utility.ShowAndFocusKeyboard(getController(),mPassPhrase);
                            Notify.Long(getController(), R.string.import_uri_reminder);
                        }
                    },
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Notify.Debug();
                            getController().performControlAction("default");
                        }
                    },
                    in.toString()
            );
        } else {
            Notify.Debug("received invalid applink uri: "+in.toString());
        }
    }

    private void doActionImport() {
        Notify.Debug();
        SupportBar.getInstance().ShowMessageBox(
                R.string.import_data,
                R.string.storage_import_merge_vs_replace,
                R.string.storage_merge,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doActionImport(true);
                    }
                },
                R.string.storage_replace,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doActionImport(false);
                    }
                }
        );
    }

    private void doActionImport(boolean merge) {
        Notify.Debug();
        HideKeyboard();
        String pass = mPassPhrase.getText().toString();
        if (pass.isEmpty()) {
            Notify.Short(getController(),R.string.error_no_pass);
            return;
        }
        String source = null;
        if (mChosenFilePath != null) {
            source = mChosenFilePath.toURI().toString();
        } else if (mChosenFileUri != null) {
            source = mChosenFileUri.toString();
        }
        if (source == null) {
            Notify.Short(getController(), R.string.storage_404);
            return;
        }
        Storage.PerformOperation(
                getController(),
                merge ? "import-merge" : "import-replace",
                pass,
                source,
                new Storage.Operation() {
                    @Override
                    public void onStorageSuccess(Context context, Storage instance) {
                        Notify.Debug();
                        SupportBar.getInstance().ShowMessageBox(
                                R.string.import_data,
                                R.string.storage_imported
                        );
                        getController().performControlAction("default");
                    }

                    @Override
                    public void onStorageFailure() {
                        SupportBar.getInstance().ShowMessageBox(
                                R.string.import_data,
                                R.string.error_storage_import
                        );
                    }
                }
        );
    }
}
