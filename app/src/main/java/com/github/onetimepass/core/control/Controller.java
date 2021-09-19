package com.github.onetimepass.core.control;
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

import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.github.onetimepass.R;
import com.github.onetimepass.core.Constants;
import com.github.onetimepass.core.Notify;
import com.github.onetimepass.core.Storage;
import com.github.onetimepass.core.SupportBar;
import com.github.onetimepass.core.Utility;
import com.github.onetimepass.core.screen.Screen;
import com.rustamg.filedialogs.FileDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The Controller base class. While the messiest of all the core classes
 * this is also by far the most important next to the Storage class.
 *
 * This is the "C" of the "MVC" pattern.
 */
abstract public class Controller
        extends AppCompatActivity
        implements
        ControllerInterface,
        ControllerIdleTimer.IdleListener,
        FileDialog.OnFileSelectedListener
{

    private List<Screen> mScreens = new ArrayList<Screen>();
    private List<ControlAction> mControlActions;
    private Storage mStorage;
    private SupportBar mSupportBar;
    private ProgressBar mIdleTimerBar;
    private View mFragmentMain;
    protected ControllerIdleTimer mIdleTimer;
    private String mPassPhraseCache = null;
    private Screen mCurrentScreen = null;
    private Screen mWaitingForFileSelection = null;
    private boolean mAlive = false;
    private String mNextTransitionOverride = null;

    /*************************************************************************
     * GETTERS AND SETTERS
     * @return the fragment main
     */
    final public View getFragmentMain() {
        return mFragmentMain;
    }

    /**
     * Request focus.
     */
    final public void requestFocus() {
        mFragmentMain.requestFocus();
    }

    /**
     * Gets self.
     *
     * @return the self
     */
    final protected Controller getSelf() {
        return this;
    }

    /**
     * Gets support bar.
     *
     * @return the support bar
     */
    final protected SupportBar getSupportBar() {
        return mSupportBar;
    }

    /**
     * Gets storage.
     *
     * @return the storage
     */
    final protected Storage getStorage() {
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

    /*************************************************************************
     * SCREENS
     * @param screen the screen
     */
    final public void registerScreen(Screen screen) {
        Screen s = getScreen(screen.getConf().tag);
        if (s != null) {
            mScreens.remove(s);
        }
        mScreens.add(screen);
        Notify.Debug(screen.getConf().tag);
    }

    /**
     * Gets screen.
     *
     * @param tag the tag
     * @return the screen
     */
    final protected Screen getScreen(String tag) {
        for (int i = 0; i < mScreens.size(); i++) {
            if (mScreens.get(i).getConf().tag.equals(tag))
                return mScreens.get(i);
        }
        return null;
    }

    /**
     * Process inbound uri.
     *
     * @param uri the uri
     */
    final public void processInboundUri(Uri uri) {
        Notify.Debug();
        for (int i=0; i < mScreens.size(); i++) {
            Screen s = mScreens.get(i);
            if (s.getConf().handle_uri) {
                if (s.processInboundUri(uri)) {
                    Notify.Debug("processInboundUri found "+s.getConf().tag+" handled uri: "+uri.toString());
                    //transitionToScreen(s.getConf().tag);
                    mNextTransitionOverride = s.getConf().tag;
                    return;
                }
            }
        }
    }

    /*************************************************************************
     * CONTROLLER ACTIONS
     * @param action the action
     */
    final public void registerControlAction(ControlAction action) {
        if (mControlActions == null)
            mControlActions = new ArrayList<ControlAction>();
        ControlAction sa = getControlAction(action.getTag());
        if (sa == null) {
            mControlActions.add(action);
            Notify.Debug(action.getTag());
        } else {
            Notify.Error("Cannot add existing action: "+action.getTag());
        }
    }

    /**
     * Gets control action.
     *
     * @param tag the tag
     * @return the control action
     */
    final protected ControlAction getControlAction(String tag) {
        for (int i = 0; i < mControlActions.size(); i++) {
            if (mControlActions.get(i).getTag().equals(tag))
                return mControlActions.get(i);
        }
        return null;
    }


    /*************************************************************************
     * AUTH CREDENTIAL CACHE
     * @param passphrase the passphrase
     */
    final public void setAuthCache(String passphrase) {
        Notify.Debug();
        mPassPhraseCache = passphrase;
    }

    /**
     * Clear auth cache.
     */
    final public void clearAuthCache() {
        Notify.Debug();
        mPassPhraseCache = null;
    }


    /*************************************************************************
     * CAMERA PERMISSIONS
     */
    public interface CameraInterface {
        /**
         * On camera permission granted.
         */
        void onCameraPermissionGranted();

        /**
         * On camera permission denied.
         */
        void onCameraPermissionDenied();
    }

    private boolean mCameraResultWanted = false;
    private String mCameraResultScreen;

    /**
     * Sets camera result wanted.
     *
     * @param tag the tag
     */
    public void setCameraResultWanted(String tag) {
        mCameraResultWanted = true;
        mCameraResultScreen = tag;
    }
    private CameraInterface mCamera = null;

    /**
     * Sets camera screen.
     *
     * @param screen the screen
     */
    public void setCameraScreen(CameraInterface screen) {
        mCamera = screen;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.REQ_PERM_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mCamera != null)
                        mCamera.onCameraPermissionGranted();
                    return;
                }
                if (mCamera != null)
                    mCamera.onCameraPermissionDenied();
                return;
            case Constants.REQ_PERM_RWFILE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mCurrentScreen != null)
                        mCurrentScreen.onFilePermissionGranted(requestCode);
                    return;
                }
                if (mCurrentScreen != null)
                    mCurrentScreen.onFilePermissionDenied(requestCode);
        }
    }

    /*************************************************************************
     * IDLE TIMER
     * @return the idle timer
     */
    final public ControllerIdleTimer getIdleTimer() {
        return mIdleTimer;
    }

    public void onIdleTimerStart() {
        Notify.Debug();
        int max = (int) mIdleTimer.getIdleTimeout();
        mIdleTimerBar.setMax(max);
        mIdleTimerBar.setProgress(max);
    }

    public void onIdleTimerStop() {
        Notify.Debug();
        int max = (int) mIdleTimer.getIdleTimeout();
        mIdleTimerBar.setMax(max);
        mIdleTimerBar.setProgress(max);
    }

    public void onIdleTimerTick(long millisUntilFinished) {
        if (!mStorage.IsOpen()) {
            Notify.Debug();
            mIdleTimer.StopTimer();
            return;
        }
        mIdleTimerBar.setProgress((int)millisUntilFinished);
    }

    public void onIdleTimerFinish() {
        if (mStorage != null && mStorage.StoragePathExists()) {
            Notify.Debug();
            if (mStorage.IsOpen())
                mStorage.Close();
            mPassPhraseCache = null;
            popBackStack();
            if (mIdleTimer.getExitOnFinish()) {
                try {
                    ControlAction quit = getControlAction("quit");
                    if (quit != null)
                        quit.performAction(this, new String[]{});
                } catch (Exception ignore) {}
            } else {
                clearAuthCache();
                transitionToScreen("unlock");
            }
        }
    }

    /*************************************************************************
     * CONVENIENCE FUNCTIONS
     */
    protected void onExitCleanup() {
        Notify.Debug();
        for (int i = 0; i < mScreens.size(); i++) {
            Screen found = mScreens.get(i);
            Notify.Debug("Cleaning up screen: "+found.getConf().tag);
            try { found.removeScreenFragment(); } catch (Exception e) {}
            mScreens.remove(found);
            found = null;
        }
        finishAffinity();
    }

    /**
     * Pop back stack.
     */
    public void popBackStack() {
        popBackStack(false);
    }

    /**
     * Pop back stack.
     *
     * @param backpress the backpress
     */
    public void popBackStack(boolean backpress) {
        Notify.Debug();
        if (backpress) {
            onBackPressed();
            return;
        }
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            popBackStackFG();
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    popBackStackFG();
                }
            });
        }
    }

    private int popBackStackFG() {
        Notify.Debug("back stack count: "+getSupportFragmentManager().getBackStackEntryCount());
        try {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStackImmediate();
            }
        } catch (IllegalStateException e) {
            Notify.Debug(e.getMessage());
        }
        return getSupportFragmentManager().getBackStackEntryCount();
    }

    @Override
    public void onFileSelected(FileDialog dialog, File file) {
        if (mWaitingForFileSelection != null) {
            Notify.Debug("Selected[waited]: "+file.toString());
            mWaitingForFileSelection.onFileSelected(dialog, file);
            mWaitingForFileSelection = null;
            return;
        }
        else
        if (mCurrentScreen != null) {
            Notify.Debug("Selected[default]: "+file.toString());
            mCurrentScreen.onFileSelected(dialog, file);
            return;
        }
        Notify.Debug("Selected[nopwait]: "+file.toString());
    }

    /**
     * Sets waiting for file selection.
     *
     * @param requester the requester
     */
    public void setWaitingForFileSelection(Screen requester) {
        Notify.Debug();
        if (mWaitingForFileSelection != null)
            Notify.Debug("clobbering file selection requester: "+mWaitingForFileSelection.getConf().tag);
        mWaitingForFileSelection = requester;
    }

    /*************************************************************************
     * METHOD OVERRIDES
     */

    private boolean mKeyboardIsVisible = false;

    /**
     * Is keyboard visible boolean.
     *
     * @return the boolean
     */
    final public boolean isKeyboardVisible() {
        return mKeyboardIsVisible;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notify.Debug();
        /*getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );*/
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        mFragmentMain = findViewById(R.id.fragment_main);
        mIdleTimerBar = findViewById(R.id.idle_timer_bar);

        // toolbar setup
        Toolbar tbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(tbar);

        mSupportBar = SupportBar.newInstance(this,findViewById(R.id.dialog_frame));
        mStorage = Storage.getInstance(this);
        mIdleTimer = new ControllerIdleTimer(this);

        final View root_view = findViewById(R.id.root_view);
        if (root_view != null) {
            root_view.getViewTreeObserver()
                    .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            Rect r = new Rect();
                            root_view.getWindowVisibleDisplayFrame(r);
                            int screenHeight = root_view.getRootView().getHeight();
                            int keypadHeight = screenHeight - r.bottom;
                            mKeyboardIsVisible = keypadHeight > (screenHeight * 0.15);
                        }
                    });
        }
        Notify.Debug("super oncreate completed");
    }

    public void onResume() {
        super.onResume();
        Notify.Debug();
        mAlive = true;
        setTitle(R.string.app_name);
        mIdleTimer.cancel();
        mIdleTimer = new ControllerIdleTimer(this);
        Notify.Debug("created idle timeout timer");
        mIdleTimer.setExitOnFinish(false);
        if (mStorage.IsOpen())
            mIdleTimer.RestartTimer();
        else
            mIdleTimer.StopTimer();
    }

    public void onPause() {
        super.onPause();
        Notify.Debug();
        mIdleTimer.cancel();
        mIdleTimer = new ControllerIdleTimer(this, Constants.IDLE_EXPIRE);
        Notify.Debug("created idle expire timer");
        if (mStorage.IsOpen())
            mIdleTimer.StartTimer();
        mAlive = false;
        mIdleTimer.setExitOnFinish(true);
    }


    public void onUserInteraction() {
        super.onUserInteraction();
        mIdleTimer.RestartTimer();
    }

    public void onBackPressed() {
        Notify.Debug();
        // short circuit if back_quits
        if (mCurrentScreen.getConf().back_quits) {
            super.onBackPressed();
            performControlAction("quit");
            finish();
            return;
        }
        // only the current screen can consume back-presses
        if (mCurrentScreen.onBackPressed())
            return;

        performControlAction("default");
    }

    /*************************************************************************
     * TRANSITIONS AND IPC
     * @param tag the tag
     */
    public void onScreenInteraction(String tag) {
        onScreenInteraction(tag,new String[] {});
    }
    public void onScreenInteraction(final String tag, final String[] argv) {
        Notify.Debug();
        if (argv == null || argv.length <= 0) {
            Notify.Debug("Data is empty");
            return;
        }

        ControlAction ca = getControlAction(tag);
        if (ca != null) {
            ca.performAction(this,argv);
            return;
        }

        Screen screen = getScreen(tag);
        if (screen != null) {
            transitionToScreen(screen.getConf().tag);
            return;
        }

        Notify.Debug("Unknown input: "+tag);

    }

    /**
     * Proceed to default fragment.
     */
    protected void proceedToDefaultFragment() {
        Notify.Debug();
        if (mStorage.StoragePathExists()) {
            if (mStorage.IsOpen()) {
                if (mCameraResultWanted && mCameraResultScreen != null) {
                    String to = mCameraResultScreen;
                    mCameraResultScreen = null;
                    mCameraResultWanted = false;
                    transitionToScreen(to);
                } else if (mNextTransitionOverride != null) {
                    String to = mNextTransitionOverride;
                    mNextTransitionOverride = null;
                    transitionToScreen(to);
                } else {
                    transitionToScreen("account_list");
                }
            } else {
                transitionToScreen("unlock");
            }
        } else {
            transitionToScreen("setlock");
        }
    }

    /**
     * Perform control action.
     *
     * @param tag the tag
     */
    public void performControlAction(String tag) {
        performControlAction(tag,new String[] {});
    }

    /**
     * Perform control action.
     *
     * @param tag  the tag
     * @param data the data
     */
    public void performControlAction(String tag, String[] data) {
        Notify.Debug("received: "+tag);
        Utility.HideKeyboard(this);
        ControlAction found = getControlAction(tag);
        if (found != null) {
            if (isAlive())
                found.performAction(this, data);
            else
                Notify.Debug("not alive, not acting on: "+tag);
        } else {
            Notify.Error("ControlAction not found: "+tag);
        }
    }

    /**
     * Transition to screen.
     *
     * @param tag the tag
     */
    public void transitionToScreen(String tag) {
        transitionToScreen(tag,new String[] {});
    }

    /**
     * Transition to screen.
     *
     * @param tag  the tag
     * @param data the data
     */
    public void transitionToScreen(String tag, String[] data) {
        Notify.Debug("received: "+tag);

        popBackStack();

        if (mCurrentScreen != null)
            mCurrentScreen.removeScreenFragment();

        Screen found = getScreen(tag);
        if (found != null) {
            found.setInboudData(data);
            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_main,found.getScreenFragment());
            if (found.getConf().show_home) {
                transaction.addToBackStack(found.getConf().tag);
            }
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commit();
            mCurrentScreen = found;
        } else {
            Notify.Error("Screen not found: "+tag);
        }
    }

}
