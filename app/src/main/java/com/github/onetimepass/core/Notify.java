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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.github.onetimepass.BuildConfig;


/**
 * The fire-and-forget notifications (toast messages) and logging facilities.
 */
public class Notify {
    /**
     * The Tag.
     */
    static final String TAG = "OneTimePass";

    /**
     * Debug utility.
     *
     * @param message the message
     */
    static void DebugUtility(String message) {
        if (BuildConfig.DEBUG) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            StackTraceElement stackTraceElement = stackTraceElements[4];
            String methodName = stackTraceElement.getMethodName();
            int linum = stackTraceElement.getLineNumber();
            String fileName = stackTraceElement.getFileName();
            String prefix = "[" + fileName + ":" + String.valueOf(linum) + "/" + methodName + "]";
            if (message != null) {
                Log.d(TAG, prefix + ": " + message);
            } else {
                Log.d(TAG, prefix);
            }
        }
    }

    /**
     * Debug.
     */
    public static void Debug() {
        if (BuildConfig.DEBUG)
            DebugUtility(null);
    }

    /**
     * Debug.
     *
     * @param message the message
     */
    public static void Debug(String message) {
        if (BuildConfig.DEBUG) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            StackTraceElement stackTraceElement = stackTraceElements[3];
            String methodName = stackTraceElement.getMethodName();
            int linum = stackTraceElement.getLineNumber();
            String fileName = stackTraceElement.getFileName();
            String prefix = "[" + fileName + ":" + String.valueOf(linum) + "/" + methodName + "]";
            if (message != null) {
                Log.d(TAG, prefix + ": " + message);
            } else {
                Log.d(TAG, prefix);
            }
        }
    }

    /**
     * Debug.
     *
     * @param message the message
     * @param e       the e
     */
    public static void Debug(String message, Exception e) {
        if (BuildConfig.DEBUG) {
            DebugUtility(message);
            e.printStackTrace();
        }
    }

    /**
     * Error.
     *
     * @param message the message
     */
    public static void Error(String message) {
        Log.e(TAG,message);
    }

    /**
     * Error.
     *
     * @param message the message
     * @param e       the e
     */
    public static void Error(String message, Exception e) {
        Error(message);
        if (BuildConfig.DEBUG) {
            String em = e.getMessage();
            if (em != null)
                Error(em);
            e.printStackTrace();
        }
    }

    private static void NewToast(final Context context,
                                 final int duration, final int gravity,
                                 final int offset_x, final int offset_y,
                                 final int message, final Object... argv)
    {

        final String translated = context.getString(message,argv);
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Toast t = Toast.makeText(context, translated, duration);
            t.setGravity(gravity, offset_x, offset_y);
            t.show();
            DebugUtility("[FGT] " + translated);
            return;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast t = Toast.makeText(context, translated, duration);
                    t.setGravity(gravity, offset_x, offset_y);
                    t.show();
                    DebugUtility("[BGT] " + message);
                } catch (Exception e) {
                    Debug("Failed [BGT] " + message,e);
                }
            }
        });
    }

    /**
     * Short.
     *
     * @param context the context
     * @param message the message
     * @param argv    the argv
     */
    public static void Short(final Context context, final int message, final Object... argv) {
        NewToast(
                context, Toast.LENGTH_SHORT,
                Constants.TOAST_GRAVITY,
                Constants.TOAST_OFFSET_X,
                Constants.TOAST_OFFSET_Y,
                message, argv
        );
    }

    /**
     * Long.
     *
     * @param context the context
     * @param message the message
     * @param argv    the argv
     */
    public static void Long(final Context context, final int message, final Object... argv) {
        NewToast(
                context, Toast.LENGTH_LONG,
                Constants.TOAST_GRAVITY,
                Constants.TOAST_OFFSET_X,
                Constants.TOAST_OFFSET_Y,
                message, argv
        );
    }
}
