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
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.ControlAction;
import com.github.onetimepass.core.control.Controller;
import com.github.onetimepass.core.screen.Screen;


/**
 * Manage the "unlock" screen. This is the entry point into an already setup
 * account data set. Prompts for password.
 */
final public class UnLockScreen extends Screen {

    private EditText mPassPhrase;

    /**
     * Setup a new instance of the UnLock Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public UnLockScreen(final Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "unlock",
                        R.string.unlock,
                        R.string.app_name,
                        R.layout.fragment_unlock,
                        R.menu.options_unlock,
                        R.id.optgrp_unlock,
                        false,
                        false,
                        true
                )
        );

        /**
         * "lock" - trigger the storage engine to close and transition to the unlock screen
         */
        controller.registerControlAction(
                new ControlAction() {
                    @Override
                    public String getTag() {
                        return "lock";
                    }

                    @Override
                    public boolean needsToBeAlive() { return false; }

                    @Override
                    public void performAction(final Context context, final String[] data) {
                        Storage.PerformOperation(
                                controller,
                                "close",
                                new Storage.Operation() {
                                    @Override
                                    public void onStorageSuccess(Context context, Storage instance) {
                                        Notify.Debug();
                                        controller.clearAuthCache();
                                        if (controller.isAlive())
                                            controller.transitionToScreen("unlock");
                                    }
                                    @Override
                                    public void onStorageFailure() {
                                        Notify.Debug();
                                        controller.clearAuthCache();
                                        if (controller.isAlive())
                                            controller.transitionToScreen("unlock");
                                    }
                                }
                        );
                    }
                }
        );

        /**
         * "unlock" - attempt to unlock the storage engine and transition to the default screen
         */
        controller.registerControlAction(
                new ControlAction() {
                    @Override
                    public String getTag() {
                        return "unlock";
                    }

                    @Override
                    public boolean needsToBeAlive() { return true; }

                    @Override
                    public void performAction(final Context context, final String[] data) {
                        Storage.PerformOperation(
                                controller,
                                "open",
                                data[0],
                                new Storage.Operation() {
                                    @Override
                                    public void onStorageSuccess(Context context, Storage instance) {
                                        Notify.Debug();
                                        controller.setAuthCache(data[0]);
                                        controller.performControlAction("default");
                                    }
                                    @Override
                                    public void onStorageFailure() {
                                        Notify.Long(controller,R.string.error_bad_pass);
                                        controller.clearAuthCache();
                                        controller.transitionToScreen("unlock");
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
        mPassPhrase = layout.findViewById(R.id.unlock_passphrase);
        mPassPhrase.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mPassPhrase.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    HideKeyboard();
                    doActionUnlock();
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
        ResetState();
        ShowKeyboard( mPassPhrase );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_quit:
                getController().performControlAction("quit");
                return true;
            case R.id.action_unlock_phrase:
                doActionUnlock();
                return true;
        }
        return false;
    }


    private void ResetState() {
        Notify.Debug();
        mPassPhrase.setText("");
    }

    private void doActionUnlock() {
        String s = mPassPhrase.getText().toString();
        if (s.isEmpty()) {
            Notify.Short(getController(),R.string.error_no_pass);
            return;
        }
        Notify.Debug();
        getController().performControlAction("unlock",new String[] {s});
    }
}
