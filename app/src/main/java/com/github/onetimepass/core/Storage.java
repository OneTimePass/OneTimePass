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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.text.format.Time;

import com.github.onetimepass.R;
import com.github.onetimepass.core.account.AccountEntry;
import com.github.onetimepass.core.control.Controller;
import com.tozny.crypto.android.AesCbcWithIntegrity;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * The Storage engine. Manages save/load/modification of the account data.
 */
public class Storage {
    // Class variables, settings
    private File mDefaultPath;
    private File mInstancePath;
    private InputStream mInstanceStream;
    private Context mContext;
    private ArrayList<AccountEntry> mAccounts = new ArrayList<AccountEntry>();
    private AesCbcWithIntegrity.SecretKeys mPassKeys = null;
    private String mPlainText = null;

    // Singleton implementation
    @SuppressLint("StaticFieldLeak")
    private static Storage mInstance;
    private boolean mIsMainStorageInstance = false;

    /**
     * Gets instance.
     *
     * @param context the context
     * @return the instance
     */
    public static Storage getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Storage(context);
            mInstance.mIsMainStorageInstance = true;
        }
        return mInstance;
    }

    /**
     * Is default (primary) instance?
     *
     * @return the boolean
     */
    public boolean IsDefaultInstance() {
        return mIsMainStorageInstance;
    }

    // Constructors
    private Storage() {}
    private Storage(Context context) {
        mIsMainStorageInstance = false;
        mContext = context;
        mDefaultPath = context.getDatabasePath("secrets.dat");
        mInstancePath = mDefaultPath;
        mInstanceStream = null;
        mAccounts = new ArrayList<AccountEntry>();
        mPlainText = null;
    }
    private Storage(Context context, File path) {
        mIsMainStorageInstance = false;
        mContext = context;
        mDefaultPath = context.getDatabasePath("secrets.dat");
        mInstancePath = path;
        mInstanceStream = null;
        mAccounts = new ArrayList<AccountEntry>();
        mPlainText = null;
    }
    private Storage(Context context, InputStream stream) {
        mIsMainStorageInstance = false;
        mContext = context;
        mDefaultPath = context.getDatabasePath("secrets.dat");
        mInstancePath = null;
        mInstanceStream = stream;
        mAccounts = new ArrayList<AccountEntry>();
        mPlainText = null;
    }

    private Controller getController() {
        return (Controller)mContext;
    }

    /**
     * Storage path exists?
     *
     * @return the boolean
     */
    public boolean StoragePathExists() {
        return mInstancePath != null && mInstancePath.exists();
    }

    private void ShowSpinnerBox(int message, Object... argv) {
        Notify.Debug();
        if (mIsMainStorageInstance) // only update UI on main instance
            SupportBar.getInstance().ShowSpinnerBox(message,argv);
    }
    private void UpdateSpinnerText(int message, Object... argv) {
        Notify.Debug();
        if (mIsMainStorageInstance) // only update UI on main instance
            SupportBar.getInstance().UpdateSpinnerText(message,argv);
    }
    private void HideSpinnerBox() {
        Notify.Debug();
        if (mIsMainStorageInstance) // only update UI on main instance
            SupportBar.getInstance().HideAll();
    }

    /**
     * Is open?
     *
     * @return the boolean
     */
    public boolean IsOpen() {
        return mPassKeys != null && mPlainText != null;
    }

    /**
     * Get accounts array list.
     *
     * @return the array list
     */
    public ArrayList<AccountEntry> GetAccounts() {
        Notify.Debug();
        if (mAccounts == null)
            mAccounts = new ArrayList<AccountEntry>();
        return mAccounts;
    }

    private AesCbcWithIntegrity.SecretKeys makePassKeys(String passphrase) {
        Notify.Debug();
        ContentResolver cr = mContext.getApplicationContext().getContentResolver();
        String salt = passphrase;
        if (mIsMainStorageInstance)
            salt = Settings.Secure.getString(cr, Settings.Secure.ANDROID_ID);
        return makePassKeys(
                passphrase,
                salt
        );
    }

    private AesCbcWithIntegrity.SecretKeys makePassKeys(String passphrase,String salt) {
        try {
            return AesCbcWithIntegrity.generateKeyFromPassword(passphrase, salt);
        } catch (Exception e) {
            Notify.Debug("failed to generateKeyFromPassword()",e);
        }
        return null;
    }

    /**
     * Check passphrase by trying to actually decrypt the message. This is an
     * expensive operation. Use sparingly.
     *
     * @param passphrase the passphrase
     * @return the boolean
     */
    public boolean CheckPassphrase(String passphrase) {
        if (StoragePathExists()) {
            try {
                AesCbcWithIntegrity.SecretKeys passKeys = makePassKeys(passphrase);
                decrypt(passKeys); // discard results, just testing
                return true;
            } catch (Exception e) {
                Notify.Debug("checkpassphrase failed",e);
            }
        }
        return false;
    }

    private String decrypt(AesCbcWithIntegrity.SecretKeys passKeys) throws Exception {
        Notify.Debug();
        byte[] bytes;
        if (mInstanceStream != null) {
            bytes = IOUtils.toByteArray(mInstanceStream);
        } else {
            int length = (int) mInstancePath.length();
            bytes = new byte[length];
            FileInputStream input = new FileInputStream(mInstancePath);
            try {
                int total = input.read(bytes);
                Notify.Debug("read "+total+" bytes");
            } finally {
                input.close();
            }
        }
        String encoded = new String(bytes);
        AesCbcWithIntegrity.CipherTextIvMac iv = new AesCbcWithIntegrity.CipherTextIvMac(encoded);
        return AesCbcWithIntegrity.decryptString(iv,passKeys,"utf-8");
    }

    private boolean Open(String passphrase) {
        Notify.Debug();
        boolean existed = StoragePathExists();

        if (existed && mPassKeys != null && mAccounts != null && mAccounts.size() > 0) {
            Notify.Debug("Storage already open. Close first before opening again.");
            return true;
        }

        mAccounts = null;
        mPassKeys = null;

        UpdateSpinnerText(R.string.storage_preparing);
        Notify.Debug("Generating keys from passphrase");
        AesCbcWithIntegrity.SecretKeys passKeys = makePassKeys(passphrase);

        if (!existed && mIsMainStorageInstance) {
            Notify.Debug("creating new dat file");
            try {
                File parent = mInstancePath.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    Notify.Debug("failed to mkdirs on parent: " + mInstancePath.toString());
                }
            } catch (Exception e) {
                Notify.Debug("failed to mkdirs on parent: " + mInstancePath.toString());
            }
            mAccounts = new ArrayList<AccountEntry>();
            mPassKeys = passKeys;
            if (!Save()) {
                Notify.Debug("failed to save new dat file");
                mPassKeys = null;
                mAccounts.clear();
                return false;
            }
        }

        try {
            UpdateSpinnerText(R.string.storage_unlocking);
            String plaintext = decrypt(passKeys);
            if (plaintext != null) {
                mPlainText = plaintext;
                UpdateSpinnerText(R.string.storage_reading);
                JSONArray ja = new JSONArray(plaintext);
                ArrayList<AccountEntry> accounts = new ArrayList<AccountEntry>();
                for (int i = 0; i < ja.length(); i++) {
                    UpdateSpinnerText(
                            R.string.storage_reading_of,
                            (i + 1),
                            ja.length()
                    );
                    String json = ja.getString(i);
                    AccountEntry m = AccountEntry.ParseJSON(i, json);
                    if (m != null) {
                        accounts.add(m);
                        Notify.Debug("found account: " + m.getLabel());
                    }
                }
                mAccounts = accounts;
                mPassKeys = passKeys;
                getController().getIdleTimer().RestartTimer();
                return true;
            }
        } catch (Exception e) {
            Notify.Error("Error decrypting/loading accounts",e);
            if (!existed) {
                Notify.Debug("error during initial dat file creation, cleaning up");
                mInstancePath.delete();
                mAccounts = null;
                mPassKeys = null;
                getController().getIdleTimer().StopTimer();
            }
        }
        return false;
    }

    private void ImportFromFile(File source, String passphrase, boolean merge) {
        Notify.Debug();
        if (source.exists()) {
            UpdateSpinnerText(R.string.storage_unlocking);
            Storage temp_storage = new Storage(mContext,source);
            if (temp_storage.Open(passphrase)) {
                if (merge) {
                    if (MergeAccountListAndSave(temp_storage.GetAccounts()))
                        temp_storage.Close();
                    Notify.Debug("Failed to merge and save: " + source.toString());
                } else {
                    if (ReplaceAccountListAndSave(temp_storage.GetAccounts()))
                        temp_storage.Close();
                    Notify.Debug("Failed to replace and save: " + source.toString());
                }
            } else {
                Notify.Debug("Failed to unlock with passphrase: "+source.toString());
            }
        } else {
            Notify.Debug("source does not exist: "+source.toString());
        }
    }

    private void ImportFromStream(InputStream source, String passphrase, boolean merge) {
        Notify.Debug();
        if (source != null) {
            UpdateSpinnerText(R.string.storage_unlocking);
            Storage temp_storage = new Storage(mContext,source);
            if (temp_storage.Open(passphrase)) {
                if (merge) {
                    if (MergeAccountListAndSave(temp_storage.GetAccounts()))
                        temp_storage.Close();
                    Notify.Debug("Failed to merge and save: " + source.toString());
                } else {
                    if (ReplaceAccountListAndSave(temp_storage.GetAccounts()))
                        temp_storage.Close();
                    Notify.Debug("Failed to replace and save: " + source.toString());
                }
            } else {
                Notify.Debug("Failed to unlock with passphrase: "+source.toString());
            }
        } else {
            Notify.Debug("source is null");
        }
    }

    private boolean MergeAccountListAndSave(List<AccountEntry> entries) {
        Notify.Debug();
        if (IsOpen()) {
            UpdateSpinnerText(R.string.storage_merging);
            ArrayList<AccountEntry> old = mAccounts;
            mAccounts = new ArrayList<AccountEntry>();
            mAccounts.addAll(old);
            for (AccountEntry ae:entries) {
                if (!AccountExists(ae)) {
                    mAccounts.add(ae);
                    Notify.Debug("Merged: "+ae.toStringTitle());
                    UpdateSpinnerText(R.string.storage_merged,ae.toStringTitle());
                } else {
                    Notify.Debug("Exists: "+ae.toStringTitle());
                }
            }
            if (Save())
                return true;
            // recover on fail
            mAccounts = old;
        }
        return false;
    }


    /**
     * Replace account list and save.
     *
     * @param entries the entries
     * @return the boolean
     */
    public boolean ReplaceAccountListAndSave(List<AccountEntry> entries) {
        Notify.Debug();
        if (IsOpen()) {
            UpdateSpinnerText(R.string.storage_replacing);
            ArrayList<AccountEntry> old = mAccounts;
            mAccounts = new ArrayList<AccountEntry>();
            for (AccountEntry ae:entries) {
                mAccounts.add(ae);
                UpdateSpinnerText(R.string.storage_replaced,ae.toStringTitle());
            }
            if (Save())
                return true;
            // recover on fail
            mAccounts = old;
        }
        return false;
    }

    /**
     * Add account.
     *
     * @param entry the entry
     * @return the boolean
     */
    public boolean AddAccount(AccountEntry entry) {
        Notify.Debug();
        return IsOpen() && mAccounts.add(entry);
    }

    /**
     * Remove account.
     *
     * @param entry the entry
     * @return the boolean
     */
    public boolean RemoveAccount(AccountEntry entry) {
        Notify.Debug();
        if (IsOpen()) {
            if (mAccounts.contains(entry)) {
                return mAccounts.remove(entry);
            }
        }
        return false;
    }

    /**
     * Replace account.
     *
     * @param old_entry the old entry
     * @param new_entry the new entry
     */
    public void ReplaceAccount(AccountEntry old_entry, AccountEntry new_entry) {
        Notify.Debug();
        if (IsOpen()) {
            if (mAccounts.contains(old_entry)) {
                int idx = mAccounts.indexOf(old_entry);
                mAccounts.add(idx,new_entry);
                mAccounts.remove(old_entry);
            }
        }
    }

    /**
     * Find account account.
     *
     * @param in the in
     * @return the account entry
     */
    public AccountEntry FindAccount(Uri in) {
        Notify.Debug();
        try {
            for (int i=0; i < mAccounts.size(); i++) {
                AccountEntry ae = mAccounts.get(i);
                Uri uri = ae.toUri();
                if (uri.getScheme().equals(in.getScheme())) {
                    if (uri.getHost().equals(in.getHost())) {
                        if (uri.getPath().equals(in.getPath())) {
                            String uri_secret = uri.getQueryParameter("secret").toUpperCase();
                            String in_secret = in.getQueryParameter("secret").toUpperCase();
                            if (uri_secret.equals(in_secret)) {
                                return ae;
                            }
                        }
                    }
                }
            }
        } catch (NullPointerException ignore) {}
        return null;
    }

    private boolean AccountExists(AccountEntry entry) {
        Notify.Debug();
        return mAccounts.contains(entry);
    }

    private void ChangePassphrase(String old_phrase, String new_phrase) throws Exception {
        Notify.Debug();
        SupportBar.getInstance().ShowSpinnerBox(R.string.change_passphrase);
        if (IsOpen()) {
            Close();
        }
        if (Open(old_phrase)) {
            AesCbcWithIntegrity.SecretKeys passKeys = makePassKeys(new_phrase);
            if (Save(passKeys)) {
                mPassKeys = passKeys;
                return;
            }
            throw new Exception(mContext.getString(R.string.error_change_pass_fail));
        }
        SupportBar.getInstance().HideAll();
        throw new Exception(mContext.getString(R.string.error_bad_pass));
    }

    /**
     * Save.
     *
     * @return the boolean
     */
    public boolean Save() {
        return Save(mPassKeys);
    }
    private boolean Save(final AesCbcWithIntegrity.SecretKeys passKeys) {
        return Save(passKeys,null);
    }
    private boolean Save(final AesCbcWithIntegrity.SecretKeys passKeys, final File save_to) {
        Notify.Debug();
        File target_file;
        if (save_to != null) {
            target_file = save_to;
            ShowSpinnerBox(R.string.storage_exporting,mAccounts.size());
        } else {
            target_file = mInstancePath;
            ShowSpinnerBox(R.string.storage_saving,mAccounts.size());
        }
        if (target_file == null)
            target_file = mDefaultPath;
        JSONArray ja = new JSONArray();
        for (int i=0; i<mAccounts.size(); i++) {
            ja.put(mAccounts.get(i).toString());
        }
        String out = ja.toString();
        File backup = new File(target_file.getAbsolutePath()+".backup."+(System.currentTimeMillis()/1000));
        if (target_file.exists()) {
            if (!target_file.renameTo(backup))
                Notify.Error("Failed to backup target file: "+backup.toString());
        }
        try {
            AesCbcWithIntegrity.CipherTextIvMac encrypted;
            UpdateSpinnerText(R.string.storage_locking);
            encrypted = AesCbcWithIntegrity.encrypt(out,passKeys,"utf-8");
            UpdateSpinnerText(R.string.storage_writing);
            FileOutputStream output;
            output = new FileOutputStream(target_file, false);
            output.write(encrypted.toString().getBytes());
            output.flush();
            output.close();
            if (backup.exists()) {
                if (!backup.delete()) {
                    Notify.Error("Failed to cleanup restore file: "+backup.toString());
                }
            }
            HideSpinnerBox();
            return true;
        } catch (Exception e) {
            Notify.Error("Failed to write account data",e);
            Notify.Long(getController(),R.string.error_storage_save);
            if (backup.exists()) {
                if (!backup.renameTo(target_file)) {
                    Notify.Error("Failed to recover backup file: "+backup.toString());
                }
            }
        }
        HideSpinnerBox();
        return false;
    }

    /**
     * Close.
     *
     * @return the boolean
     */
    public boolean Close() {
        if (!IsOpen()) {
            getController().getIdleTimer().StopTimer();
            return false;
        }
        if (!Save())
            Notify.Debug("failed to save on Close()");
        mAccounts = null;
        mPassKeys = null;
        getController().getIdleTimer().StopTimer();
        return true;
    }

    /**
     * Gets export file name. Not used yet because the filedialogs library
     * does not support initial file names yet.
     *
     * @return the export file name
     */
    public String getExportFileName() {
        Time now = new Time();
        now.setToNow();
        return "export-" + now.format("%Y%m%d-%H%M%S") + ".otpd";
    }

    private boolean Export(String passphrase, String filepath) {
        UpdateSpinnerText(R.string.storage_preparing);
        AesCbcWithIntegrity.SecretKeys passkey = makePassKeys(passphrase,passphrase);
        File target = new File(filepath);
        return Save(passkey, target);
    }


    /**
     * The interface for defining a "Storage Operation"
     */
    public interface Operation {
        /**
         * On storage success.
         *
         * @param context  the context
         * @param instance the instance
         */
        void onStorageSuccess(Context context,Storage instance);

        /**
         * On storage failure.
         */
        void onStorageFailure();
    }

    /**
     * Perform a "storage operation".
     *
     * @param context   the context
     * @param oper      the oper
     * @param operation the operation
     */
    public static void PerformOperation(final Context context, String oper, final Storage.Operation operation) {
        PerformOperation(context, oper, null, null, operation);
    }

    /**
     * Perform a "storage operation".
     *
     * @param context   the context
     * @param oper      the oper
     * @param data      the data
     * @param operation the operation
     */
    public static void PerformOperation(final Context context, String oper, String data, final Storage.Operation operation) {
        PerformOperation(context, oper, data, null, operation);
    }

    /**
     * Perform a "storage operation".
     *
     * @param context   the context
     * @param oper      the oper
     * @param data      the data
     * @param extra     the extra
     * @param operation the operation
     */
    public static void PerformOperation(final Context context, String oper, String data, String extra, final Storage.Operation operation) {
        Notify.Debug();
        final int label;
        switch (oper) {
            case "open":           label = R.string.storage_unlocking;      break;
            case "save":           label = R.string.storage_writing;        break;
            case "close":          label = R.string.storage_locking;        break;
            case "change":         label = R.string.change_passphrase;      break;
            case "export":         label = R.string.export_data;            break;
            case "import-merge":   label = R.string.storage_import_merge;   break;
            case "import-replace": label = R.string.storage_import_replace; break;
            default:
                Notify.Debug("Unknown operation string: "+oper);
                return;
        }

        new AsyncTask<String,Void,Storage>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                SupportBar.getInstance().ShowSpinnerBox(label);
            }

            @Override
            protected Storage doInBackground(String... strings) {
                Notify.Debug();
                Storage storage = getInstance(context);
                boolean merge = false;
                switch (strings[0]) {
                    case "open":
                        if (storage.Open(strings[1]))
                            operation.onStorageSuccess(context,storage);
                        else
                            operation.onStorageFailure();
                        break;
                    case "save":
                        if (storage.Save())
                            operation.onStorageSuccess(context,storage);
                        else
                            operation.onStorageFailure();
                        break;
                    case "change":
                        try {
                            storage.ChangePassphrase(strings[1], strings[2]);
                            operation.onStorageSuccess(context, storage);
                        } catch (Exception e) {
                            Notify.Debug(e.getMessage());
                            operation.onStorageFailure();
                        }
                        break;
                    case "close":
                        if (storage.Close())
                            operation.onStorageSuccess(context,storage);
                        else
                            operation.onStorageFailure();
                        break;
                    case "export":
                        if (strings[2] != null && storage.Export(strings[1],strings[2]))
                            operation.onStorageSuccess(context,storage);
                        else
                            operation.onStorageFailure();
                        break;
                    case "import-merge":
                        merge = true;
                        // no break; fallthrough to import-replace
                    case "import-replace":
                        if (strings[2] != null) {
                            Uri in = Uri.parse(strings[2]);
                            if (in != null) {
                                try {
                                    switch (in.getScheme()) {
                                        case "file":
                                            String path = in.getEncodedPath();
                                            File file = new File(path);
                                            try {
                                                InputStream stream = new FileInputStream(file);
                                                storage.ImportFromStream(stream, strings[1], merge);
                                                operation.onStorageSuccess(context, storage);
                                                return storage;
                                            } catch (Exception e) {
                                                Notify.Debug("Failed to ImportFromStream(file)",e);
                                                operation.onStorageFailure();
                                            }
                                            break;
                                        case "content":
                                            try {
                                                InputStream stream = context.getContentResolver().openInputStream(in);
                                                storage.ImportFromStream(stream, strings[1], merge);
                                                operation.onStorageSuccess(context, storage);
                                                return storage;
                                            } catch (Exception e) {
                                                Notify.Debug("Failed to ImportFromStream(content)",e);
                                                operation.onStorageFailure();
                                            }
                                            break;
                                        default:
                                            storage.ImportFromFile(new File(strings[2]), strings[1], merge);
                                            operation.onStorageSuccess(context, storage);
                                            return storage;
                                    }
                                } catch (Exception e) {
                                    Notify.Debug("failed to parse URI: \""+strings[2]+"\"",e);
                                }
                            }
                        }
                        operation.onStorageFailure();
                        break;
                }
                return storage;
            }

            @Override
            protected void onPostExecute(Storage storage) {
                super.onPostExecute(storage);
                SupportBar.getInstance().HideAll();
            }
        }.execute(oper,data,extra);
    }
}
