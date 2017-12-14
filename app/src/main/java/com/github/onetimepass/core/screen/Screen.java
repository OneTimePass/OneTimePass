package com.github.onetimepass.core.screen;
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

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.Storage;
import com.github.onetimepass.core.Utility;
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.Controller;
import com.rustamg.filedialogs.FileDialog;

import java.io.File;

/**
 * The type Screen.
 */
abstract public class Screen implements ScreenInterface {

    /*************************************************************************
     * MEMBER VARIABLES
     */

    private boolean mAlive = false;
    private Configuration mConfig;
    private final Controller mController;
    private ScreenFragment mScreenFragment;
    private Storage mStorage;
    private Uri mInboundUri;
    private String[] mInboundData;

    /*************************************************************************
     * SCREEN LIFECYCLE
     * @param controller the controller
     */
    public Screen(Controller controller) {
        mController = controller;
        mStorage = Storage.getInstance(controller);
    }

    /*************************************************************************
     * FINAL PUBLIC METHODS
     * @return the conf
     */
    final public Configuration getConf() {
        return mConfig;
    }

    /**
     * Gets screen fragment.
     *
     * @return the screen fragment
     */
    final public ScreenFragment getScreenFragment() {
        Notify.Debug(getConf().tag);
        FragmentManager fm = mController.getSupportFragmentManager();
        // check the cache first
        mScreenFragment = (ScreenFragment) fm.findFragmentByTag(mConfig.tag);
        if (mScreenFragment == null) {
            // get a new instance tied to this screen
            mScreenFragment = ScreenFragment.newInstance(mConfig,this);
            // and add it to the cache
            fm.beginTransaction().add(mScreenFragment, mConfig.tag).commit();
        }
        return mScreenFragment;
    }

    /**
     * Remove screen fragment.
     */
    final public void removeScreenFragment() {
        Notify.Debug(getConf().tag);
        getController().popBackStack();
        FragmentManager fm = mController.getSupportFragmentManager();
        ScreenFragment fragment = getScreenFragment();
        if (fragment != null && getController().isAlive()) {
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.remove(mScreenFragment);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commit();
            mScreenFragment = null;
        }
    }

    /**
     * Gets storage.
     *
     * @return the storage
     */
    final public Storage getStorage() {
        return mStorage;
    }

    /**
     * Is alive boolean.
     *
     * @return the boolean
     */
    final public boolean isAlive() {
        return mAlive;
    }

    /**
     * Sets inboud data.
     *
     * @param data the data
     */
    final public void setInboudData(String[] data) {
        Notify.Debug(getConf().tag);
        if (data.length > 0) {
            Notify.Debug("received["+getConf().tag+"]: " + data[0]);
            mInboundData = data;
        } else {
            Notify.Debug("received["+getConf().tag+"]: null");
            mInboundData = null;
        }
    }

    /**
     * Get inbound data string [ ].
     *
     * @return the string [ ]
     */
    final public String[] getInboundData() {
        return mInboundData;
    }

    /*************************************************************************
     * OVERRIDE-ABLE METHODS
     * @param in the in
     */
    public void setInboundUri(Uri in) {
        Notify.Debug(getConf().tag);
        mInboundUri = in;
    }

    /**
     * Gets inbound uri.
     *
     * @return the inbound uri
     */
    public Uri getInboundUri() {
        return mInboundUri;
    }

    /**
     * Process inbound uri boolean.
     *
     * @param uri the uri
     * @return the boolean
     */
    public boolean processInboundUri(Uri uri) {
        return false;
    }

    /*************************************************************************
     * CONVENIENCE METHODS
     * @param config the config
     */
    final protected void setConfiguration(Configuration config) {
        mConfig = config;
        mController.registerScreen(this);
        Notify.Debug(getConf().tag);
    }

    /**
     * Gets controller.
     *
     * @return the controller
     */
    final protected Controller getController() {
        return mController;
    }

    /**
     * Show keyboard.
     *
     * @param view the view
     */
    final protected void ShowKeyboard(View view) {
        Utility.ShowAndFocusKeyboard(mController,view);
    }

    /**
     * Hide keyboard.
     */
    final protected void HideKeyboard() {
        Utility.HideKeyboard(mController);
    }

    private Resources getResources() {
        return mController.getResources();
    }

    /**
     * Gets color.
     *
     * @param color the color
     * @return the color
     */
    final protected int getColor(int color) {
        return getResources().getColor(color);
    }

    /*************************************************************************
     * Default Implementations
     * @param requestCode the request code
     */
    public void onFilePermissionGranted(int requestCode) {
        // unimplemented
    }

    /**
     * On file permission denied.
     *
     * @param requestCode the request code
     */
    public void onFilePermissionDenied(int requestCode) {
        // unimplemented
    }

    /**
     * On file selected.
     *
     * @param dialog the dialog
     * @param file   the file
     */
    public void onFileSelected(FileDialog dialog, File file) {
        // unimplemented
    }

    /**
     * On back pressed boolean.
     *
     * @return the boolean
     */
    public boolean onBackPressed() {
        return false; // not handled
    }

    /**
     * On detach.
     */
    public void onDetach() {
        // unimplemented
    }

    /**
     * On attach.
     */
    public void onAttach() {
        // unimplemented
    }

    /**
     * On create.
     *
     * @param savedInstanceState the saved instance state
     */
    public void onCreate(Bundle savedInstanceState) {
        // unimplemented
    }

    /**
     * On create view view.
     *
     * @param inflater           the inflater
     * @param container          the container
     * @param savedInstanceState the saved instance state
     * @param layout             the layout
     * @return the view
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, View layout) {
        // unimplemented
        return null;
    }

    /**
     * On resume.
     */
    public void onResume() {
        mAlive = true;
    }

    /**
     * On pause.
     */
    public void onPause() {
        mAlive = false;
    }

    /**
     * On prepare options menu.
     *
     * @param menu the menu
     */
    public void onPrepareOptionsMenu(Menu menu) {
        // unimplemented
    }

    /**
     * On create options menu.
     *
     * @param menu     the menu
     * @param inflater the inflater
     */
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // unimplemented
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // unimplemented
        return false;
    }
}
