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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.onetimepass.R;

/**
 * The support bar is the main interface to the actionbar, a customized
 * message dialog and a spinner view for blocking user interaction while
 * long running processes are happening.
 */
public class SupportBar {
    private static SupportBar mInstance = null;

    /**
     * Singleton instance getter
     *
     * @return the instance
     */
    public static SupportBar getInstance() { return mInstance; }

    /**
     * New instance support bar. This is only called from the Controller's
     * onCreate() constructor. Everywhere else uses the getInstance() method.
     *
     * @param context     the context
     * @param support_bar the support bar
     * @return the support bar
     */
    public static SupportBar newInstance(Context context, View support_bar) {
        if (mInstance == null) {
            mInstance = new SupportBar(context, support_bar);
        }
        return mInstance;
    }

    private Context mContext;
    private View mSupportBar;

    private View mDialogFrame;

    private View mDialogSpinnerBox;
    private TextView mDialogSpinnerText;
    private ProgressBar mDialogSpinnerBar;


    private SupportBar(Context context, View support_bar) {
        mContext = context;
        mSupportBar = support_bar;
        mDialogFrame = mSupportBar.findViewById(R.id.dialog_frame);
        mDialogSpinnerBox = mSupportBar.findViewById(R.id.dialog_type_spinner);
        mDialogSpinnerText = mSupportBar.findViewById(R.id.dialog_spinner_text);
        mDialogSpinnerBar = mSupportBar.findViewById(R.id.dialog_spinner_bar);
        HideAll();
    }

    private boolean isValid() {
        return mContext != null && mSupportBar != null;
    }

    /**
     * Hide the spinner and/or message box
     */
    public void HideAll() {
        Notify.Debug();
        if (!isValid())
            return;
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Reset();
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Reset();
            }
        });
    }

    /**
     * Is the spinner visible and "spinning"?
     *
     * @return the boolean
     */
    public boolean IsSpinning() {
        Notify.Debug();
        if (mDialogFrame.getVisibility() == View.VISIBLE) {
            if (mDialogSpinnerBox.getVisibility() == View.VISIBLE) {
                return true;
            }
        }
        return false;
    }

    private void Reset() {
        Notify.Debug();
        mDialogFrame.setVisibility(View.GONE);
        mDialogSpinnerBox.setVisibility(View.GONE);
        mDialogSpinnerText.setText("");
        mDialogSpinnerText.setHint("");
        mDialogSpinnerBar.setIndeterminate(true);
        ((AppCompatActivity)mContext).invalidateOptionsMenu();
    }

    /**
     * Set the options menu to disabled while the spinner is visible.
     *
     * @param menu the menu
     */
    public void UpdateOptionsMenuState(Menu menu) {
        Notify.Debug();
        boolean enabled = !SupportBar.getInstance().IsSpinning();
        for (int i=0; i < menu.size(); i++) {
            menu.getItem(i).setEnabled(enabled);
        }
    }

    /**
     * Forcibly set actionbar configurations to ensure a consistent UX
     *
     * @param show_home the show home
     */
    public void UpdateActionBarSettings(boolean show_home) {
        Notify.Debug();
        ActionBar ab = ((AppCompatActivity)mContext).getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(show_home);
            ab.setHomeButtonEnabled(show_home);
            ab.setDisplayShowHomeEnabled(!show_home);
            ab.setDisplayUseLogoEnabled(!show_home);
            ab.setHomeAsUpIndicator(R.drawable.ic_back_home);
            ab.setIcon(R.mipmap.ic_logo_round);
            ab.setLogo(R.mipmap.ic_logo_round);
            ab.setDisplayShowTitleEnabled(true);
            ab.setDisplayShowCustomEnabled(false);
        }
    }





    private void ShowMessageBoxFG(final int title,     final int message,
                                  final int pos_label, final View.OnClickListener pos,
                                  final int neg_label, final View.OnClickListener neg,
                                  Object... message_argv) {
        if (!isValid())
            return;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(title);
        if (message_argv != null && message_argv.length > 0)
            builder.setMessage(mContext.getString(message,message_argv));
        else
            builder.setMessage(message);
        // android.R.string.yes
        builder.setPositiveButton(pos_label, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (pos != null) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            pos.onClick(null);
                        }
                    });
                }
                dialog.dismiss();
            }
        });
        // android.R.string.no
        if (neg_label > 0 && neg != null) {
            builder.setNegativeButton(neg_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (neg != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                neg.onClick(null);
                            }
                        });
                    }
                    dialog.dismiss();
                }
            });
        }
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Show message box.
     *
     * @param title        the title
     * @param message      the message
     * @param pos_label    the pos label
     * @param pos          the pos
     * @param neg_label    the neg label
     * @param neg          the neg
     * @param message_argv the message argv
     */
    public void ShowMessageBox(final int title,     final int message,
                               final int pos_label, final View.OnClickListener pos,
                               final int neg_label, final View.OnClickListener neg,
                               final Object... message_argv) {
        if (!isValid())
            return;
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            ShowMessageBoxFG(title,message,pos_label,pos,neg_label,neg,message_argv);
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                ShowMessageBoxFG(title,message,pos_label,pos,neg_label,neg,message_argv);
            }
        });
    }

    /**
     * Show message box.
     *
     * @param title   the title
     * @param message the message
     */
    public void ShowMessageBox(final int title, final int message) {
        ShowMessageBox(title, message,
                android.R.string.ok, null,
                -1, null
        );
    }

    /**
     * Show yes/no box.
     *
     * @param title        the title
     * @param message      the message
     * @param yes          the yes
     * @param no           the no
     * @param message_argv the message argv
     */
    public void ShowYesNoBox(int title, int message, View.OnClickListener yes, View.OnClickListener no, Object... message_argv) {
        if (no == null) {
            no = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            };
        }
        ShowMessageBox(title,message,android.R.string.yes,yes,android.R.string.no,no,message_argv);
    }









    private void ShowSpinnerBoxFG(final int message_id, final Object... argv) {
        if (!isValid())
            return;
        Reset();
        String message = mContext.getString(message_id, argv);
        Notify.Debug("message: "+message);
        Utility.HideKeyboard(mContext);
        mDialogFrame.invalidate();
        mDialogFrame.setVisibility(View.VISIBLE);
        mDialogSpinnerBox.setVisibility(View.VISIBLE);
        mDialogSpinnerText.setText(message);
        mDialogSpinnerBar.setIndeterminate(true);
        ((AppCompatActivity)mContext).invalidateOptionsMenu();
    }

    /**
     * Show spinner box.
     *
     * @param message the message
     * @param argv    the argv
     */
    public void ShowSpinnerBox(final int message, final Object... argv) {
        if (!isValid())
            return;
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            ShowSpinnerBoxFG(message, argv);
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                ShowSpinnerBoxFG(message, argv);
            }
        });
    }

    /**
     * Update spinner text.
     *
     * @param message_id the message id
     * @param argv       the argv
     */
    void UpdateSpinnerText(final int message_id, final Object... argv) {
        final String message = mContext.getString(message_id, argv);
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            if (!IsSpinning())
                ShowSpinnerBox(message_id,argv);
            else
                mDialogSpinnerText.setText(message);
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (!IsSpinning())
                    ShowSpinnerBox(message_id,argv);
                else
                    mDialogSpinnerText.setText(message);
            }
        });
    }

}
