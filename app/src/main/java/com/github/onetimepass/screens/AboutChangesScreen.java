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

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.onetimepass.R;
import com.github.onetimepass.core.Constants;
import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.SupportBar;
import com.github.onetimepass.core.Utility;
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.Controller;
import com.github.onetimepass.core.screen.Screen;

import java.util.Locale;


/**
 * Manages the "about/changes" screen which handles the "about this app"
 * details, credits for all libraries / contributions and provides a changelog
 * listing of all revisions of the application.
 */
public class AboutChangesScreen extends Screen {

    /**
     * Setup a new instance of the About/Changes Screen and register any control
     * actions relevant to unlocking or locking the application.
     *
     * @param controller the controller
     */
    public AboutChangesScreen(Controller controller) {
        super(controller);
        Notify.Debug();
        setConfiguration(
                new Configuration(
                        "about_changes",
                        R.string.details,
                        R.string.details,
                        R.layout.fragment_about_changes,
                        R.menu.options_about_changes,
                        R.id.optgrp_about_changes,
                        true,
                        false,
                        false
                )
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, View layout) {
        Notify.Debug();
        SupportBar.getInstance().HideAll();
        Utility.HideKeyboard(getController());
        Controller controller = getController();
        Resources resources = controller.getResources();
        String package_name = controller.getPackageName();

        /*
            The changelog and credits entries are automatically populated in the listing view by
            programmatically searching through the string resources looking for matching key
            patterns. While this isn't quite the "most efficient" means of populating the changes
            and credit listings; this does enable for translatable entries without the need for
            rigging a translation system around flat files or even a database of the entry content.

            change_MAJOR_MINOR_PATCH_tag = version and date string
            change_MAJOR_MINOR_PATCH_log = change log entry

            credit_NNN_label = the name of the library or contribution
            credit_NNN_reason = what the library/contribution is used for
            credit_NNN_link = web link to the library or contributor
        */

        // changes_1_0_0_{tag,log}
        LinearLayout changes_entries = layout.findViewById(R.id.changes_entries);
        String latest_version = "0.0.0";
        for (int major = 0; major <= Constants.Version.MAJOR; major++) {
            for (int minor = 0; minor <= Constants.Version.MAX_MAGIC; minor++) {
                for (int patch = 0; patch <= Constants.Version.MAX_MAGIC; patch++) {
                    String tag_key = String.format(Locale.CANADA,"changes_%1$d_%2$d_%3$d_tag",major,minor,patch);
                    int tag_val = resources.getIdentifier(tag_key,"string",package_name);
                    if (tag_val > 0) {
                        String log_key = String.format(Locale.CANADA, "changes_%1$d_%2$d_%3$d_log", major, minor, patch);
                        int log_val = resources.getIdentifier(log_key, "string", package_name);
                        if (log_val > 0) {
                            View entry = inflater.inflate(R.layout.fragment_about_changes_entry, container, false);
                            ((TextView) entry.findViewById(R.id.changes_tag)).setText(tag_val);
                            ((TextView) entry.findViewById(R.id.changes_log)).setText(log_val);
                            changes_entries.addView(entry,0);
                            Notify.Debug("found change log entry: " + tag_key);
                            latest_version = resources.getString(
                                    R.string.version_label,
                                    String.format(
                                            Locale.CANADA,
                                            "%1$d.%2$d.%3$d",
                                            major, minor, patch
                                    )
                            );
                        } else {
                            Notify.Debug("found change ("+tag_key+") but no matching log");
                        }
                    }
                }
            }
        }
        // update the version field too
        Notify.Debug("latest_version: " + latest_version);
        ((TextView)layout.findViewById(R.id.changes_version_label)).setText(latest_version);

        // credit_000_{label,reason,link}
        LinearLayout changes_credits = layout.findViewById(R.id.changes_credits);
        int counter = 0;
        while (true) {
            String label_key = String.format(Locale.CANADA,"credit_%1$03d_label",counter);
            int label_val = resources.getIdentifier(label_key,"string",package_name);
            if (label_val > 0) {
                String reason_key = String.format(Locale.CANADA, "credit_%1$03d_reason", counter);
                int reason_val = resources.getIdentifier(reason_key, "string", package_name);
                if (reason_val > 0) {
                    String link_key = String.format(Locale.CANADA, "credit_%1$03d_link", counter);
                    int link_val = resources.getIdentifier(link_key, "string", package_name);
                    if (link_val > 0) {
                        View entry = inflater.inflate(R.layout.fragment_about_changes_credit, container, false);
                        ((TextView) entry.findViewById(R.id.credit_label)).setText(label_val);
                        ((TextView) entry.findViewById(R.id.credit_reason)).setText(reason_val);
                        ((TextView) entry.findViewById(R.id.credit_link)).setText(link_val);
                        changes_credits.addView(entry);
                        Notify.Debug("found credits entry: " + label_key);
                    } else {
                        Notify.Debug("found credit label (" + label_key + ") and reason (" + reason_key + ") but no matching link");
                    }
                } else {
                    Notify.Debug("found credit label (" + label_key + ") but no matching reason (skipping link check)");
                }
            } else {
                break; // assume no more entries
            }
            counter++;
        }
        return layout;
    }


    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug();
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.action_cancel_about:
                getController().performControlAction("default");
                return true;
            default:
                Notify.Debug("unknown menu item: " + item.toString());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
