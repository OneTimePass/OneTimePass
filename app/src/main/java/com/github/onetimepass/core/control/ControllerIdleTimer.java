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

import android.os.CountDownTimer;

import com.github.onetimepass.core.Constants;
import com.github.onetimepass.core.Notify;


/**
 * The type Controller idle timer.
 */
public class ControllerIdleTimer extends CountDownTimer {

        private boolean mExitOnFinish = false;
        private boolean mIsTicking = false;
        private IdleListener mIdleListener;
    private long mIdleTimeout = 0;

    /**
     * The interface Idle listener.
     */
    public interface IdleListener {
        /**
         * On idle timer start.
         */
        void onIdleTimerStart();

        /**
         * On idle timer stop.
         */
        void onIdleTimerStop();

        /**
         * On idle timer tick.
         *
         * @param millisUntilFinished the millis until finished
         */
        void onIdleTimerTick(long millisUntilFinished);

        /**
         * On idle timer finish.
         */
        void onIdleTimerFinish();
        }

    /**
     * Instantiates a new Controller idle timer.
     *
     * @param listener the listener
     */
    public ControllerIdleTimer(IdleListener listener) {
            super(Constants.IDLE_TIMEOUT, Constants.IDLE_INTERVAL);
            mIdleListener = listener;
        }

    /**
     * Instantiates a new Controller idle timer with the given timeout.
     *
     * @param listener the listener
     * @param timeout  the number of milliseconds until expiration
     */
    public ControllerIdleTimer(IdleListener listener, long timeout) {
        super(timeout, Constants.IDLE_INTERVAL);
        mIdleListener = listener;
        mIdleTimeout = timeout;
    }

    /**
     * Return the timeout value used during construction
     * @return timeout
     */
    public long getIdleTimeout() {
        return mIdleTimeout;
    }

    /**
     * Start timer.
     */
    public void StartTimer() {
            Notify.Debug();
            mIsTicking = true;
            start();
            mIdleListener.onIdleTimerStart();
        }

    /**
     * Stop timer.
     */
    public void StopTimer() {
            Notify.Debug();
            mIsTicking = false;
            cancel();
            mIdleListener.onIdleTimerStop();
        }

    /**
     * Restart timer.
     */
    public void RestartTimer() {
            Notify.Debug();
            if (IsTicking()) {
                StopTimer();
            }
            StartTimer();
        }

        private boolean IsTicking() {
            return mIsTicking;
        }

    /**
     * Sets exit on finish.
     *
     * @param state the state
     */
    void setExitOnFinish(boolean state) {
            mExitOnFinish = state;
        }

    /**
     * Gets exit on finish.
     *
     * @return the exit on finish
     */
    boolean getExitOnFinish() {
            return mExitOnFinish;
        }

        @Override
        public void onFinish() {
            Notify.Debug();
            mIsTicking = false;
            mIdleListener.onIdleTimerFinish();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mIsTicking = true;
            mIdleListener.onIdleTimerTick(millisUntilFinished);
        }
}
