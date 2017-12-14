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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import com.github.onetimepass.R;
import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.SupportBar;
import com.github.onetimepass.core.Utility;
import com.github.onetimepass.core.account.AccountEntry;
import com.github.onetimepass.core.account.AccountInterface;
import com.github.onetimepass.core.account.AccountList;
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.ControlAction;
import com.github.onetimepass.core.control.Controller;
import com.github.onetimepass.core.screen.Screen;

import java.util.List;

/**
 * Manages "account list" screen. The primary (or "default") screen when the
 * application is unlocked. Presents a listing of all known accounts and has
 * the main navigation menu entries to access the rest of the application
 * features (such as "scan qr" and "change passphrase" for example).
 */
public class AccountListScreen extends Screen
        implements
        AccountInterface.OnDragListener,
        AccountInterface.OnListInteraction,
        SearchView.OnQueryTextListener {

    private ViewSwitcher mAccountListFrame;
    private View mAccountListView;
    private AccountList mAccountList;
    private ItemTouchHelper mItemTouchHelper;

    /**
     * Setup a new instance of the Account List Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public AccountListScreen(final Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "account_list",
                        R.string.accounts,
                        R.string.app_name,
                        R.layout.fragment_account_list,
                        R.menu.options_account_list,
                        R.id.optgrp_account_list,
                        false,
                        true,
                        true
                )
        );

        /**
         * "otpauth" - control command to handle incoming otpath:// type URIs
         * given to the SplashActivity/MainController by external means.
         */
        controller.registerControlAction(
                new ControlAction() {
                    @Override
                    public String getTag() {
                        return "otpauth";
                    }

                    @Override
                    public boolean needsToBeAlive() { return false; }

                    @Override
                    public void performAction(Context context, String[] data) {
                        Notify.Debug();
                        if (data != null && data.length > 0) {
                            try {
                                Uri uri = Uri.parse(data[0]);
                                controller.processInboundUri(uri);
                            } catch (Exception e) {
                                Notify.Error(e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, View layout) {
        Notify.Debug();
        mAccountListView = layout.findViewById(R.id.account_list);
        mAccountListFrame = layout.findViewById(R.id.account_list_frame);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug();
        getController().setTitle(R.string.app_name);
        RefreshListing();
        Uri uri = getInboundUri();
        if (uri != null) {
            setInboundUri(null);
            handleInboundUri(uri);
        }
    }

    @Override
    public void onDragBegin(AccountList.ViewHolder holder) {
        Notify.Debug();
        mItemTouchHelper.startDrag(holder);
    }

    @Override
    public void onListInteraction(AccountEntry entry) {
        Notify.Debug();
        getController().transitionToScreen(
                "account_info",
                new String[] {entry.toUri().toString()}
        );
    }

    @Override
    public void onListInteraction(List<AccountEntry> entries) {
        Notify.Debug();
        if (!getStorage().ReplaceAccountListAndSave(entries)) {
            Notify.Debug("Failed to replace the account list and save.");
        }
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (mAccountList != null) {
            mAccountList.SearchFilter(query);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mAccountList != null) {
            Notify.Debug("query="+query);
            mAccountList.SearchFilter(query);
        }
        return true;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Notify.Debug();
        menu.setGroupVisible(R.id.optgrp_account_list,true);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        HideKeyboard();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_quit:
                Notify.Debug("action_quit");
                getController().performControlAction("quit",null);
                return true;
            case R.id.action_change_lock:
                Notify.Debug("action_change_lock");
                getController().performControlAction("changelock",null);
                return true;
            case R.id.action_add_account:
                Notify.Debug("action_add_account");
                getController().transitionToScreen("account_edit");
                return true;
            case R.id.action_scan_qr:
                Notify.Debug("action_scan_qr");
                getController().performControlAction("scan_qr");
                return true;
            case R.id.action_about:
                Notify.Debug("action_about");
                getController().transitionToScreen("about_changes");
                return true;
            case R.id.action_import_data:
                Notify.Debug("action_import");
                getController().transitionToScreen("import");
                return true;
            case R.id.action_export_data:
                Notify.Debug("action_export");
                getController().transitionToScreen("export");
                return true;
            default:
                Notify.Debug("unknown menu item");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onBackPressed() {
        Notify.Debug();
        getController().performControlAction("quit");
        return true;
    }

    @Override
    public boolean processInboundUri(final Uri in) {
        Notify.Debug();
        if (Utility.verifyOtpauthUri(in)) {
            super.setInboundUri(in);
            return true;
        }
        return false;
    }

    private void handleInboundUri(final Uri in) {
        Notify.Debug();
        if (Utility.verifyOtpauthUri(in)) {
            SupportBar.getInstance().ShowYesNoBox(
                    R.string.account_inbound_uri,
                    R.string.account_inbound_uri_otpauth,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Notify.Debug("sending to account_edit: "+in.toString());
                            getController().transitionToScreen(
                                    "account_edit",
                                    new String[]{in.toString()}
                            );
                        }
                    },
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getController().performControlAction("default");
                        }
                    },
                    in.toString()
            );
        } else {
            Notify.Debug("received invalid applink uri: "+in.toString());
        }
    }

    private void RefreshListing() {
        Notify.Debug();
        if (mAccountListView instanceof RecyclerView) {
            Context context = mAccountListView.getContext();
            RecyclerView recyclerView = (RecyclerView) mAccountListView;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            AccountList adapter = new AccountList(
                    getStorage().GetAccounts(),
                    this,
                    this);
            recyclerView.setAdapter(adapter);
            mAccountList = adapter;
            mItemTouchHelper = adapter.AttachTouchCallback(recyclerView,adapter);
            updateAccountListFrame();
        }
    }

    private void updateAccountListFrame() {
        if (mAccountListFrame != null) {
            mAccountListFrame.reset();
            if (mAccountList.getFullItemCount() > 0) {
                Notify.Debug("hiding list-empty-msg");
                mAccountListFrame.showPrevious();
            } else {
                Notify.Debug("showing list-empty-msg");
                mAccountListFrame.showNext();
                mAccountListFrame.showNext();
            }
        }
    }
}
