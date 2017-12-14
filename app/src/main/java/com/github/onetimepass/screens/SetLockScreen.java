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
 * Manage the "set lock" screen. This is the entry point where there is no
 * account data setup. Prompts for password and confirmation.
 */
final public class SetLockScreen extends Screen {

    private EditText mPassPhrase;
    private EditText mPassConfirm;

    /**
     * Setup a new instance of the Set Lock Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public SetLockScreen(final Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "setlock",
                        R.string.setlock,
                        R.string.app_name,
                        R.layout.fragment_setlock,
                        R.menu.options_setlock,
                        R.id.optgrp_setlock,
                        false,
                        false,
                        true
                )
        );

        controller.registerControlAction(
                new ControlAction() {
                    @Override
                    public String getTag() {
                        return "newlock";
                    }

                    @Override
                    public boolean needsToBeAlive() { return true; }

                    @Override
                    public void performAction(Context context, final String[] data) {
                        Notify.Debug();
                        SupportBar.getInstance().ShowSpinnerBox(R.string.storage_setting_up);
                        Storage.PerformOperation(
                                controller,
                                "open",
                                data[0],
                                new Storage.Operation() {
                                    @Override
                                    public void onStorageSuccess(Context context, Storage instance) {
                                        Notify.Debug();
                                        instance.Close();
                                        controller.performControlAction("default");
                                    }

                                    @Override
                                    public void onStorageFailure() {
                                        Notify.Debug();
                                        Notify.Long(controller,R.string.error_bad_pass);
                                        controller.performControlAction("default");
                                    }
                                }
                        );
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, View layout) {
        Notify.Debug();
        mPassPhrase = layout.findViewById(R.id.passphrase);
        mPassPhrase.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mPassConfirm = layout.findViewById(R.id.passconfirm);
        mPassConfirm.setImeOptions(EditorInfo.IME_ACTION_DONE);

        TextWatcher listener = Utility.makePasswordTextWatcher(
                mPassPhrase,
                mPassConfirm
        );
        mPassPhrase.addTextChangedListener(listener);
        mPassConfirm.addTextChangedListener(listener);

        mPassPhrase.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    ShowKeyboard( mPassConfirm );
                    return true;
                }
                return false;
            }
        });
        mPassConfirm.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    HideKeyboard();
                    doActionSetLock();
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
        SupportBar.getInstance().HideAll();
        ShowKeyboard( mPassPhrase );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel_phrase:
                getController().performControlAction("quit");
                return true;
            case R.id.action_setup_phrase:
                doActionSetLock();
                return true;
        }
        return false;
    }

    private void doActionSetLock() {
        String p0 = mPassPhrase.getText().toString();
        String p1 = mPassConfirm.getText().toString();
        if (!p0.isEmpty() && !p1.isEmpty() && p0.contentEquals(p1)) {
            Notify.Debug();
            getController().performControlAction("newlock", new String[] {p1});
        } else {
            Notify.Short(getController(),R.string.error_pass_no_match);
        }
    }
}
