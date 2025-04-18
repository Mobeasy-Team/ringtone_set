package acr.rt.ringtone_set;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.BinaryMessenger;

/** RingtoneSetPlugin */
public class RingtoneSetPlugin implements FlutterPlugin, MethodCallHandler {
    private MethodChannel channel;
    private Context mContext;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        mContext = binding.getApplicationContext();
        setupChannel(binding.getBinaryMessenger());
    }

    private void setupChannel(BinaryMessenger messenger) {
        if (channel != null) return;

        channel = new MethodChannel(messenger, "ringtone_set");
        channel.setMethodCallHandler(this);
    }

    private boolean checkSystemWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasPermission = Settings.System.canWrite(mContext);
            if (!hasPermission) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + mContext.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            }
            return hasPermission;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setThings(String path, boolean isNotif) {
        checkSystemWritePermission();
        File mFile = new File(path);

        Uri uri = Uri.fromFile(mFile);
        ContentResolver cR = mContext.getContentResolver();
        String mime = cR.getType(uri);
        if (mime == null) mime = ".mp3";

        Log.e("setThings", "MIME: " + mime);

        if (mFile.exists()) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, mFile.getAbsolutePath());
            values.put(MediaStore.MediaColumns.TITLE, "KolpacinoRingtone");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg");
            values.put(MediaStore.MediaColumns.SIZE, mFile.length());
            values.put(MediaStore.Audio.Media.ARTIST, "Kolpaçino Sesleri");
            values.put(MediaStore.Audio.Media.IS_RINGTONE, !isNotif);
            values.put(MediaStore.Audio.Media.IS_NOTIFICATION, isNotif);
            values.put(MediaStore.Audio.Media.IS_ALARM, true);
            values.put(MediaStore.Audio.Media.IS_MUSIC, false);

            Uri newUri = cR.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

            try (OutputStream os = cR.openOutputStream(newUri)) {
                int size = (int) mFile.length();
                byte[] bytes = new byte[size];
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(mFile));
                buf.read(bytes, 0, bytes.length);
                buf.close();

                os.write(bytes);
                os.flush();
                os.close();

                RingtoneManager.setActualDefaultRingtoneUri(
                        mContext,
                        isNotif ? RingtoneManager.TYPE_NOTIFICATION : RingtoneManager.TYPE_RINGTONE,
                        newUri
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + Build.VERSION.RELEASE);
                break;
            case "setRingtone":
                setThings(call.argument("path"), false);
                result.success("success");
                break;
            case "setNotification":
                setThings(call.argument("path"), true);
                result.success("success");
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
    }
}
