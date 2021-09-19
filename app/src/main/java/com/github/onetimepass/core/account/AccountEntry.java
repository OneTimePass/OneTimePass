package com.github.onetimepass.core.account;
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

import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.Nullable;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.github.onetimepass.core.Notify;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.apache.commons.codec.binary.Base32;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The Account definition.
 *
 * This is the "M" in the "MVC" pattern though it does include some behaviors
 * of it's own such as generating QR codes and icon images.
 */
public class AccountEntry {
    private AccountEntry() {}

    /**
     * The Id.
     */
    public int id = -1;
    /**
     * The Visible.
     */
    public boolean visible = true;

    private String label = "";
    private String issuer = "";
    private String secret = "";

    /**
     * Gets label.
     *
     * @return the label
     */
    public String getLabel()  {return label; }

    /**
     * Gets issuer.
     *
     * @return the issuer
     */
    public String getIssuer() {return issuer;}

    /**
     * Gets secret.
     *
     * @return the secret
     */
    public String getSecret() {return secret;}

    private byte[] getDecodedSecret() {
        return new Base32().decode(getSecret());
    }


    @Override
    public boolean equals (Object o) {
        if (o != null && o instanceof AccountEntry) {
            AccountEntry other = (AccountEntry) o;
            if (other.getLabel().contentEquals(label)
                    && other.getIssuer().contentEquals(issuer)
                    && other.getSecret().contentEquals(secret))
                return true;
        }
        return false;
    }

    /**
     * Parse json account entry.
     *
     * @param idx the idx
     * @param in  the in
     * @return the account entry
     */
    @Nullable
    public static AccountEntry ParseJSON(int idx, String in) {
        Notify.Debug();
        try {
            JSONObject json = new JSONObject(in);
            AccountEntry ae = new AccountEntry();
            ae.id = idx;
            try { ae.label  = json.getString("label");  } catch (JSONException ignore) {}
            try { ae.issuer = json.getString("issuer"); } catch (JSONException ignore) {}
            try { ae.secret = json.getString("secret"); } catch (JSONException ignore) {}
            return ae;
        } catch (JSONException e) {
            Notify.Debug("Failed to create account from parsed JSON",e);
        }
        return null;
    }

    /**
     * Create account entry.
     *
     * @param label  the label
     * @param issuer the issuer
     * @param secret the secret
     * @return the account entry
     */
    public static AccountEntry Create(String label, String issuer, String secret) {
        AccountEntry ae = new AccountEntry();
        ae.label = label;
        ae.issuer = issuer;
        ae.secret = secret;
        return ae;
    }

    /**
     * Create account entry.
     *
     * @param uri the uri
     * @return the account entry
     */
    public static AccountEntry Create(Uri uri) {
        AccountEntry ae = new AccountEntry();
        ae.label = uri.getPath(); // uri decoded
        ae.issuer = uri.getQueryParameter("issuer"); // uri decoded
        ae.secret = uri.getQueryParameter("secret"); // base32 encoded
        return ae;
    }

    private JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("label", label);
            json.put("issuer", issuer);
            json.put("secret", secret);
        } catch (JSONException e) {
            Notify.Debug("AccountEntry.toString(): "+e.getMessage());
        }
        return json;
    }

    /**
     * To string title string.
     *
     * @return the string
     */
    public String toStringTitle() {
        if (issuer.length() > 0)
            return label + " " + issuer;
        return label;
    }

    public String toString() {
        return toJSON().toString();
    }

    /**
     * To uri uri.
     *
     * @return the uri
     */
    public Uri toUri() {
        try {
            String uri = "otpauth://totp/";
            if (!label.isEmpty()) {
                uri += URLEncoder.encode(label, "UTF-8");
            }
            if (!issuer.isEmpty()) {
                    uri += "?issuer=" + URLEncoder.encode(issuer, "UTF-8"); // uri encoded
            }
            if (uri.contains("?")) {
                uri += "&";
            } else {
                uri += "?";
            }
            uri += "secret=" + secret;
            return Uri.parse(uri);
        } catch (Exception e) {
            Notify.Debug("Exception: "+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Totp string string.
     *
     * @return the string
     */
    public String totpString() {
        int value = 0;
        try {
            value = generate(getDecodedSecret(), System.currentTimeMillis() / 1000);
        } catch (Exception e) {
            Notify.Debug("TOTP failure: "+e.getMessage());
            e.printStackTrace();
        }
        return String.format(Locale.CANADA,"%06d", value);
    }

    private static int generate(byte[] key, long t)
    {
        int r = 0;
        try {
            t /= 30;
            byte[] data = new byte[8];
            long value = t;
            for (int i = 8; i-- > 0; value >>>= 8) {
                data[i] = (byte) value;
            }
            SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);
            int offset = hash[20 - 1] & 0xF;
            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }
            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= Math.pow(10, 6);
            r  = (int) truncatedHash;
        } catch(Exception ignore) {
        }
        return r;
    }

    /**
     * Make icon drawable text drawable.
     *
     * @return the text drawable
     */
    public TextDrawable MakeIconDrawable() {
        Notify.Debug();
        String tag = getLabel();
        String mark = getLabel().substring(0,1);
        if (!getIssuer().isEmpty()) {
            tag = getLabel() + " " + getIssuer();
            mark = mark + getIssuer().substring(0,1);
        }
        int colour = ColorGenerator.MATERIAL.getColor(tag);
        return TextDrawable
                .builder()
                .beginConfig()
                    .bold()
                    .toUpperCase()
                .endConfig()
                .buildRound(mark, colour);
    }

    /**
     * Make qr code bitmap bitmap.
     *
     * @param fg_colour the fg colour
     * @param bg_colour the bg colour
     * @return the bitmap
     */
    public Bitmap MakeQrCodeBitmap(int fg_colour,int bg_colour) {
        Notify.Debug();
        try {
            BitMatrix bitMatrix = new MultiFormatWriter()
                    .encode(toUri().toString(),
                            BarcodeFormat.QR_CODE,
                            400,
                            400,
                            null
                    );
            int height = bitMatrix.getHeight();
            int width = bitMatrix.getWidth();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setHasAlpha(true);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? fg_colour : bg_colour);
                }
            }
            return bitmap;
        } catch (Exception e) {
            Notify.Debug("Failed to make QR Code bitmap",e);
        }
        return null;
    }
}
