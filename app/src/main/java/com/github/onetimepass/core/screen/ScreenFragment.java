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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.github.onetimepass.R;
import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.SupportBar;
import com.github.onetimepass.core.control.Configuration;
import com.github.onetimepass.core.control.Controller;

/**
 * The type Screen fragment.
 */
public class ScreenFragment extends Fragment {

    private Configuration mConfig;
    private Screen mScreen;
    private Controller mListener;
    private boolean mIsAlive = false;

    /**
     * Instantiates a new Screen fragment.
     */
    public ScreenFragment() {}

    /**
     * New instance screen fragment.
     *
     * @param in     the in
     * @param screen the screen
     * @return the screen fragment
     */
    public static ScreenFragment newInstance(Configuration in,Screen screen) {
        Notify.Debug();
        ScreenFragment sf = new ScreenFragment();
        sf.mConfig = in;
        sf.mScreen = screen;
        return sf;
    }

    /**
     * Gets screen.
     *
     * @return the screen
     */
    public Screen getScreen() {
        return mScreen;
    }

    /**
     * Is alive boolean.
     *
     * @return the boolean
     */
    public boolean isAlive() {
        return mIsAlive;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notify.Debug(getScreen().getConf().tag);
        setRetainInstance(true);
        if (mScreen != null)
            mScreen.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Notify.Debug(getScreen().getConf().tag);
        View layout = inflater.inflate(mConfig.layout, container, false);
        if (mScreen != null)
            return mScreen.onCreateView(inflater, container, savedInstanceState, layout);
        return layout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Notify.Debug(getScreen().getConf().tag);
        if (context instanceof Controller) {
            mListener = (Controller) context;
        } else {
            throw new RuntimeException(context.toString() + " must extend Screen.Controller");
        }
        if (mScreen != null)
            mScreen.onAttach();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Notify.Debug(getScreen().getConf().tag);
        mListener = null;
        if (mScreen != null)
            mScreen.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        Notify.Debug(getScreen().getConf().tag);
        mIsAlive = true;
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar_main);
        toolbar.setTitle(mConfig.title);

        int fg_colour = getResources().getColor(R.color.actionbar_title);
        toolbar.setTitleTextColor(fg_colour);
        toolbar.setSubtitleTextColor(fg_colour);

        SupportBar.getInstance().UpdateActionBarSettings(mConfig.show_home);
        SupportBar.getInstance().HideAll();
        setHasOptionsMenu(true);
        mScreen.HideKeyboard();
        if (mScreen != null)
            mScreen.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Notify.Debug(getScreen().getConf().tag);
        mIsAlive = false;
        if (mScreen != null)
            mScreen.onPause();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Notify.Debug(getScreen().getConf().tag);
        menu.setGroupVisible(mConfig.opt_group,isAlive());
        SupportBar.getInstance().UpdateOptionsMenuState(menu);
        if (mScreen != null)
            mScreen.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Notify.Debug(getScreen().getConf().tag);
        menu.setGroupVisible(mConfig.opt_group,false);
        inflater.inflate(getScreen().getConf().menu_group, menu);
        if (mScreen != null)
            mScreen.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Notify.Debug(getScreen().getConf().tag);
        getScreen().HideKeyboard();
        return mScreen != null && mScreen.onOptionsItemSelected(item);
    }

}
