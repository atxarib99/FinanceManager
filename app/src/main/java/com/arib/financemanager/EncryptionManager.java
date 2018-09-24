package com.arib.financemanager;

import android.util.Log;

/**
 * Created by aribdhuka on 9/22/18.
 */

class EncryptionManager {

    private static String LOG_TAG = "EncryptionManager";

    static String readEncryptedString(String key) {
        String identifier = key.substring(0, 4);

        Log.d(LOG_TAG, identifier);

        if(!identifier.equals("!#@$"))
            return "NODATAFOUND";

        String data = key.substring(4);
        String decryptedData = "";

        for(int i = 0; i < data.length(); i++) {
            decryptedData += (char)(data.charAt(i) - 2);
        }

        return decryptedData;

    }

    static String createEncryptedString(String data) {
        String encryptedKey = "";
        for(int i = 0; i < data.length(); i++) {
            encryptedKey += (char)(data.charAt(i) + 2);
        }

        return encryptedKey;
    }
}
