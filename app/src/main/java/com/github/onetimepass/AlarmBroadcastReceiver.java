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

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import com.github.onetimepass.core.Notify;

import static android.content.Context.CLIPBOARD_SERVICE;
import static com.github.onetimepass.core.Constants.ACTION_ALARM;

/**
 * Very simple Alarm receiver for the explicit purpose of clearing the
 * clipboard from the background. This behavior is a defensive security
 * measure that provides a mitigation for leaking secrets via the
 * clipboard which recent versions of Android enable users to block
 * background access (which is great). This feature is for users who
 * have disabled that feature or are running older versions of Android
 * where this is not an option anyways.
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Notify.Debug();
        // did we received our trigger to handle clearing out the clipboard?
        if (ACTION_ALARM.equals(intent.getAction())) {
            ClipboardManager clipboard = (ClipboardManager)context.getSystemService(CLIPBOARD_SERVICE);
            ClipData blank_clip = ClipData.newPlainText("", "");
            // check if primary clipboard even has anything in it
            boolean hasPrimaryClip = false;
            try { hasPrimaryClip = clipboard.hasPrimaryClip(); }
            catch (NullPointerException ignore) {}
            if (hasPrimaryClip) {
                Notify.Debug("has primary clip");
                ClipData old_clip = clipboard.getPrimaryClip();
                if (old_clip != null) {
                    String old_data = old_clip.getItemAt(0).getText().toString();
                    // we don't want to kill the clipboard unless the contents match the 6-digit
                    // string pattern as used with the secret codes
                    if (old_data.matches("^\\d\\d\\d\\d\\d\\d$")) {
                        clipboard.setPrimaryClip(blank_clip);
                        Notify.Short(context, R.string.clipboard_cleared);
                    }
                }
            } else {
                // even though there's no primary clipboard, let's clear it anyways because
                // we may have been denied read-access to the primary clipboard and we like
                // being as safe as possible with preventing other background processes from
                // siphoning secrets
                try {
                    Notify.Debug("no primary clip");
                    clipboard.setPrimaryClip(blank_clip);
                    Notify.Short(context, R.string.clipboard_expired);
                } catch (NullPointerException ignore) {}
            }
        }
    }
}
