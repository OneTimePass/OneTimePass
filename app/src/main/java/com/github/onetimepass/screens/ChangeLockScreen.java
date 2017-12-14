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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.github.onetimepass.R;
import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.Storage;
import com.github.onetimepass.core.SupportBar;
import com.github.onetimepass.core.Utility;
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.ControlAction;
import com.github.onetimepass.core.control.Controller;
import com.github.onetimepass.core.screen.Screen;


/**
 * Manages the "change lock" feature. Prompts for the existing passphrase as
 * well as a new passphrase (with confirmation)
 */
public class ChangeLockScreen extends Screen {

    private EditText mOldPhrase;
    private EditText mPassPhrase;
    private EditText mPassConfirm;

    /**
     * Setup a new instance of the Change Lock Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public ChangeLockScreen(final Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "changelock",
                        R.string.change_passphrase,
                        R.string.change_passphrase,
                        R.layout.fragment_change_lock,
                        R.menu.options_change_lock,
                        R.id.optgrp_change_lock,
                        true,
                        false,
                        false
                )
        );

        /**
         * "changelock" - if unlocked; display changelock screen, else clear
         * auth credentials and prompt to unlock
         */
        controller.registerControlAction(
                new ControlAction() {
                    @Override
                    public String getTag() {
                        return "changelock";
                    }

                    @Override
                    public boolean needsToBeAlive() { return true; }

                    @Override
                    public void performAction(Context context, String[] data) {
                        Notify.Debug();
                        if (getStorage().IsOpen()) {
                            controller.transitionToScreen("changelock");
                        } else {
                            controller.clearAuthCache();
                            controller.transitionToScreen("unlock");
                        }
                    }
                }
        );

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, View layout) {
        Notify.Debug();
        mOldPhrase = layout.findViewById(R.id.oldphrase_text);
        mOldPhrase.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mPassPhrase = layout.findViewById(R.id.newphrase_text);
        mPassPhrase.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mPassConfirm = layout.findViewById(R.id.passconfirm_text);
        mPassConfirm.setImeOptions(EditorInfo.IME_ACTION_DONE);

        TextWatcher listener = Utility.makePasswordTextWatcher(
                mPassPhrase,
                mPassConfirm
        );
        mPassPhrase.addTextChangedListener(listener);
        mPassConfirm.addTextChangedListener(listener);

        mOldPhrase.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    ShowKeyboard(mPassPhrase);
                    return true;
                }
                return false;
            }
        });
        mPassPhrase.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    ShowKeyboard(mPassConfirm);
                    return true;
                }
                return false;
            }
        });
        mPassConfirm.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    HideKeyboard();
                    doActionChangeLock();
                    return true;
                }
                return false;
            }
        });

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug();
        ResetFields();
        ShowKeyboard(mOldPhrase);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel_phrase:
                getController().performControlAction("default");
                return true;
            case R.id.action_change_phrase:
                doActionChangeLock();
                return true;
        }
        return false;
    }


    private void ResetFields() {
        Notify.Debug();
        if (mOldPhrase != null)
            mOldPhrase.setText("");
        if (mPassPhrase != null)
            mPassPhrase.setText("");
        if (mPassConfirm != null)
            mPassConfirm.setText("");
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

    private void doActionChangeLock() {
        Notify.Debug();
        final String old = mOldPhrase.getText().toString();
        SupportBar.getInstance().ShowSpinnerBox(R.string.changing_passphrase);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getStorage().CheckPassphrase(old)) {
                    if (isPassPhraseConfirmed()) {
                        final String p1 = mPassConfirm.getText().toString();
                        Storage.PerformOperation(
                                getController(),
                                "change",
                                old,
                                p1,
                                new Storage.Operation() {
                                    @Override
                                    public void onStorageSuccess(Context context, Storage instance) {
                                        Notify.Debug();
                                        getController().performControlAction("lock");
                                    }

                                    @Override
                                    public void onStorageFailure() {
                                        Notify.Debug();
                                        getController().performControlAction("lock");
                                    }
                                }
                        );
                    }
                } else {
                    SupportBar.getInstance().HideAll();
                    Notify.Long(getController(),R.string.error_bad_pass);
                }
            }
        }, 100);
    }
}
