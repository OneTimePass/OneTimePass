package com.github.onetimepass.core;
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
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.core.widget.TextViewCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.github.onetimepass.R;
import com.github.onetimepass.core.control.Controller;


/**
 * The type Utility.
 */
public class Utility {


    /**
     * Hide keyboard.
     *
     * @param context the context
     */
    private static void _HideKeyboard(final Context context) {
        if (((Controller)context).isKeyboardVisible()) {
            final Activity activity = (Activity) context;
            final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            Notify.Debug("keyboard visible, making hidden");
            if (imm != null) {
                imm.showSoftInput(activity.findViewById(R.id.root_view), InputMethodManager.HIDE_IMPLICIT_ONLY);
                imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
            }
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else {
            Notify.Debug("keyboard hidden already");
        }
    }
    public static void HideKeyboard(final Context context) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            _HideKeyboard(context);
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                _HideKeyboard(context);
            }
        });
    }

    /**
     * Show and focus keyboard.
     *
     * @param context the context
     * @param view    the view
     */
    private static void _ShowAndFocusKeyboard(final Context context,final View view) {
        final Activity activity = (Activity) context;
        final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (view.hasFocus() || view.requestFocus()) {
            if (!((Controller) context).isKeyboardVisible()) {
                Notify.Debug("keyboard hidden, making visible");
                if (imm != null) {
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
                activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            } else {
                Notify.Debug("keyboard visible already");
            }
        }
    }
    public static void ShowAndFocusKeyboard(final Context context,final View view) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            _ShowAndFocusKeyboard(context, view);
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                _ShowAndFocusKeyboard(context, view);
            }
        });
    }

    /**
     * Verify otpauth uri boolean.
     *
     * @param in the in
     * @return the boolean
     */
    public static boolean verifyOtpauthUri(Uri in) {
        if (in.getScheme().equals("otpauth")) {
            if (in.getHost().equals("totp")) {
                String secret = in.getQueryParameter("secret");
                if (secret != null && !secret.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Make password text watcher to manage state indicators visually.
     *
     * @param password the password
     * @param confirm  the confirm
     * @return the text watcher
     */
    public static TextWatcher makePasswordTextWatcher(final EditText password, final EditText confirm) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String p0 = password.getText().toString();
                String p1 = confirm.getText().toString();
                if (!p1.isEmpty()) {
                    if (!p0.isEmpty() && p0.contentEquals(p1)) {
                        TextViewCompat.setTextAppearance(confirm, R.style.OneTimePassTheme_EditText_Matching);
                    } else {
                        TextViewCompat.setTextAppearance(confirm, R.style.OneTimePassTheme_EditText_Mismatch);
                    }
                } else {
                    TextViewCompat.setTextAppearance(confirm, R.style.OneTimePassTheme_EditText);
                }
            }
        };
    }


}
