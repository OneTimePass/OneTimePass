package com.github.onetimepass;
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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.Storage;
import com.github.onetimepass.core.control.ControlAction;
import com.github.onetimepass.core.control.Controller;
import com.github.onetimepass.screens.AboutChangesScreen;
import com.github.onetimepass.screens.AccountEditScreen;
import com.github.onetimepass.screens.AccountInfoScreen;
import com.github.onetimepass.screens.AccountListScreen;
import com.github.onetimepass.screens.AccountScanScreen;
import com.github.onetimepass.screens.ChangeLockScreen;
import com.github.onetimepass.screens.ExportScreen;
import com.github.onetimepass.screens.ImportScreen;
import com.github.onetimepass.screens.SetLockScreen;
import com.github.onetimepass.screens.UnLockScreen;

/**
 * Concrete implementation of Controller class. The "C" in MVC.
 */
public class MainController extends Controller
{
    /**
     * ensure the initialization code can only ever run once
     */
    private boolean mInitScreens = false;
    private boolean mInitControlActions = false;

    /**
     * Register actions and/or otherwise initialize the system
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notify.Debug();
        initControlActions();
    }

    /**
     * When resuming the activity, initialize the screens and handle any
     * runtime inbound data / URIs
     */
    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug();
        initScreens();
        Intent appLinkIntent = getIntent();
        Uri data = appLinkIntent.getData();
        if (data != null) {
            processInboundUri(data);
        }
        proceedToDefaultFragment();
    }

    /**
     * Another way that inbound data can enter the application is via the
     * onNewIntent() method.
     *
     * @param intent
     */
    @Override
    public void onNewIntent (Intent intent) {
        super.onNewIntent(intent);
        Notify.Debug();
        Uri data = intent.getData();
        if (data != null)
            processInboundUri(data);
    }

    /**
     * Screens are essentially android fragments wrapped up such that code
     * duplication is minimized and per-screen behavior is kept concise. Here
     * we initialize the screens by simply constructing them once. The screens
     * will register themselves in a singleton-design-pattern so as to not
     * require actually storing references here.
     */
    private void initScreens() {
        if (mInitScreens) {
            Notify.Debug("already initialized");
            return;
        }
        Notify.Debug();
        mInitScreens = true;
        new ImportScreen(this);
        new ExportScreen(this);
        new AboutChangesScreen(this);
        new AccountEditScreen(this);
        new AccountInfoScreen(this);
        new AccountListScreen(this);
        new AccountScanScreen(this);
        new ChangeLockScreen(this);
        new SetLockScreen(this);
        new UnLockScreen(this);
    } // end initScreens

    /**
     * Control Actions are a built-in system for managing application behavior
     * and inter-screen operations in a generic "command" style design pattern
     * which allows for a centralized means of triggering event reactions such
     * as the following "quit" and "transition to default screen" generic
     * tasks.
     *
     * Control Actions can be defined anywhere in the application so that the
     * different screens can maintain screen-specific control actions. The
     * generic application-wide actions are found here.
     */
    private void initControlActions() {
        if (mInitControlActions) {
            Notify.Debug("already initialized");
            return; // enforce once-only per instance
        }
        mInitControlActions = true;
        Notify.Debug();

        /**
         * "quit" - enable any part of the application to safely trigger the
         * quit procedures.
         */
        registerControlAction(
                new ControlAction() {
                    @Override
                    public String getTag() {
                        return "quit";
                    }

                    @Override
                    public boolean needsToBeAlive() { return false; }

                    @Override
                    public void performAction(final Context context, final String[] data) {
                        Notify.Short(context,R.string.quit_message);
                        clearAuthCache();
                        getStorage().Close();
                        onExitCleanup();
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Notify.Debug();
                                System.exit(0);
                            }
                        },500);
                        finish();
                    }
                }
        );

        /**
         * "default" - transition to the logical-default screen (if unlocked;
         * account list; else unlock/setup)
         */
        registerControlAction(
                new ControlAction() {
                    @Override
                    public String getTag() {
                        return "default";
                    }

                    @Override
                    public boolean needsToBeAlive() { return true; }

                    @Override
                    public void performAction(Context context, String[] data) {
                        Notify.Debug();
                        proceedToDefaultFragment();
                    }
                }
        );

        /**
         * "reopen" - not currently used but provides a means of closing and
         * reopening the account data set using existing credentials. Intended
         * for recovery steps.
         */
        registerControlAction(
                new ControlAction() {
                    @Override
                    public String getTag() {
                        return "reopen";
                    }

                    @Override
                    public boolean needsToBeAlive() { return true; }

                    @Override
                    public void performAction(Context context, String[] data) {
                        getStorage().Close();
                        Storage.PerformOperation(
                                getSelf(),
                                "open",
                                data[0],
                                new Storage.Operation() {
                                    @Override
                                    public void onStorageSuccess(Context context, Storage instance) {
                                        Notify.Debug();
                                        transitionToScreen("account_list");
                                    }
                                    @Override
                                    public void onStorageFailure() {
                                        Notify.Debug();
                                        clearAuthCache();
                                        transitionToScreen("unlock");
                                    }
                                }
                        );
                    }
                }
        );
    } // end initControlActions
}
