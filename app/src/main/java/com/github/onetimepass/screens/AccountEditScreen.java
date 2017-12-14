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

import android.net.Uri;
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
import com.github.onetimepass.core.account.AccountEntry;
import com.github.onetimepass.core.SupportBar;
import com.github.onetimepass.core.Utility;
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.Controller;
import com.github.onetimepass.core.screen.Screen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Manages the "account edit" screen which handles both "adding" and "editing"
 * operations of account data. In the "add" context the user can input the
 * actual secret code (in Base32 format) however in "edit" context only the
 * label and issuer are editable.
 */
public class AccountEditScreen extends Screen {

    private EditText mLabel;
    private EditText mIssuer;
    private EditText mSecret;
    private TextView mSecretLabel;

    private AccountEntry mOriginal = null;

    /**
     * Setup a new instance of the Account Edit Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public AccountEditScreen(final Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "account_edit",
                        R.string.edit,
                        R.string.edit,
                        R.layout.fragment_account_edit,
                        R.menu.options_account_edit,
                        R.id.optgrp_account_edit,
                        true,
                        false,
                        false
                )
        );
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notify.Debug();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, View layout) {
        Notify.Debug();
        mLabel = layout.findViewById(R.id.edit_label);
        mLabel.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mIssuer = layout.findViewById(R.id.edit_issuer);
        mIssuer.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        mSecret = layout.findViewById(R.id.edit_secret);
        mSecret.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mSecretLabel = layout.findViewById(R.id.edit_secret_view);

        mLabel.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    Utility.ShowAndFocusKeyboard(getController(),mIssuer);
                    return true;
                }
                return false;
            }
        });

        mIssuer.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    Utility.ShowAndFocusKeyboard(getController(),mSecret);
                    return true;
                }
                else
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doActionSave();
                    return true;
                }
                return false;
            }
        });

        mSecret.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doActionSave();
                    return true;
                }
                return false;
            }
        });

        processInboundData();
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug();
        if (mOriginal != null) {
            mIssuer.setImeOptions(EditorInfo.IME_ACTION_DONE);
            getController().setTitle(R.string.edit);
            getController().findViewById(R.id.edit_instructions).setVisibility(View.GONE);
        } else {
            mIssuer.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            getController().setTitle(R.string.add);
            getController().findViewById(R.id.edit_instructions).setVisibility(View.VISIBLE);
        }
        SupportBar.getInstance().HideAll();
        ShowKeyboard(mLabel);
        processInboundData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel_edit:
                getController().performControlAction("default");
                return true;
            case R.id.action_save_edit:
                doActionSave();
                return true;
            default:
                Notify.Debug("unknown menu item");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void processInboundData() {
        Notify.Debug();
        String[] argv = getInboundData();
        if (argv != null && argv.length > 0) {
            Uri uri = Uri.parse(argv[0]);
            mOriginal = getStorage().FindAccount(uri);
            if (mOriginal == null) {
                mLabel.setText(uri.getPath().substring(1)); // substr the '/' off the path
                mIssuer.setText(uri.getQueryParameter("issuer"));
                mSecret.setText(uri.getQueryParameter("secret"));
            }
            Notify.Debug("inbound data was a URI: "+uri.toString());
        } else {
            Notify.Debug("no inbound data found: "+getConf().tag);
        }

        if (mOriginal != null) {
            mLabel.setText(mOriginal.getLabel());
            mIssuer.setText(mOriginal.getIssuer());
            mSecret.setText(mOriginal.getSecret());
            mSecret.setVisibility(View.GONE);
            mSecretLabel.setVisibility(View.GONE);
        }

    }

    private void SaveNewAccount() {
        Notify.Debug();
        AccountEntry e = AccountEntry.Create(
                mLabel.getText().toString(),
                mIssuer.getText().toString(),
                mSecret.getText().toString().toUpperCase()
        );
        SupportBar.getInstance().ShowSpinnerBox(R.string.account_adding,e.toStringTitle());
        if (mOriginal != null) {
            getStorage().ReplaceAccount(mOriginal,e);
        } else {
            if (!getStorage().AddAccount(e)) {
                Notify.Debug("Failed to AddAccount?!");
            }
        }
        getStorage().Save();
    }


    private boolean isValidAccountInfo() {
        String label = mLabel.getText().toString();
        if (!label.isEmpty()) {
            String secret = mSecret.getText().toString();
            if (!secret.isEmpty()) {
                Pattern p = Pattern.compile("^[=A-Z0-9]+$");
                Matcher m = p.matcher(secret.toUpperCase());
                if (m.matches())
                    return true;
            }
        }
        return false;
    }

    private void doActionSave() {
        Notify.Debug();
        if (isValidAccountInfo()) {
            SaveNewAccount();
            getController().performControlAction("default");
        } else {
            Notify.Short(getController(),R.string.error_edit_req_fields);
        }
    }

}
