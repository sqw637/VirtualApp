package com.lody.virtual.helper.compat;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.os.Environment;

import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;

import java.util.Set;

public class UriCompat {
    private static final String TAG = "UriCompat";
    public static String AUTH = "virtual.fileprovider";
    private final static String[] ACTIONS = {
            Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE,
            Intent.ACTION_SENDTO,
            "android.media.action.IMAGE_CAPTURE",
            "com.android.camera.action.CROP",
    };

    public static boolean needFake(Intent intent) {
        for (String act : ACTIONS) {
            if (act.equals(intent.getAction())) {
                return true;
            }
        }
        return false;
    }

    public static Uri fakeFileUri(Uri uri) {
        if (uri == null) return null;
        if ("file".equals(uri.getScheme())) {
            String path = uri.getEncodedPath();
            String external_path = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (path.startsWith(external_path))
            {
                String split_path = path.substring(external_path.length());
                return fakeFileUri(uri.buildUpon().scheme("content").path("/external" + split_path).build());
            }
        }

        if ("content".equals(uri.getScheme())) {
            //TODO: fake file path? sdcard/Android/data/
            //fake auth
            String auth = uri.getAuthority();
            return uri.buildUpon().authority(AUTH).appendQueryParameter("__va_auth", auth).build();
        } else {
            return null;
        }
    }

    public static Intent fakeFileUri(Intent intent) {
        if (!needFake(intent)) {
            VLog.i(TAG, "don't need fake intent");
            return intent;
        }
        Uri uri = intent.getData();
        if (uri != null) {
            Uri u = fakeFileUri(uri);
            if (u != null) {
                Log.i(TAG, "fake data uri:" + uri + "->" + u);
                intent.setData(u);
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            ClipData data = intent.getClipData();
            if (data != null) {
                int count = data.getItemCount();
                for (int i = 0; i < count; i++) {
                    ClipData.Item item = data.getItemAt(i);
                    Uri u = fakeFileUri(item.getUri());
                    if (u != null) {
                        Reflect.on(item).set("mUri", u);
                    }
                }
            }
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            boolean changed2 = false;
            Set<String> keys = extras.keySet();
            for (String key : keys) {
                Object val = extras.get(key);
                if (val instanceof Intent) {
                    Intent i = fakeFileUri((Intent) val);
                    if (i != null) {
                        changed2 = true;
                        extras.putParcelable(key, i);
                    }
                } else if (val instanceof Uri) {
                    Uri u = fakeFileUri((Uri) val);
                    if (u != null) {
                        changed2 = true;
                        extras.putParcelable(key, u);
                    }
                }
            }
            if (changed2) {
                intent.putExtras(extras);
            }
        }
        return intent;
    }
}
