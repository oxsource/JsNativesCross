package pizzk.android.process.jscross.permission;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pizzk.android.process.jscross.R;

public class PermissionHelper {
    public static final int CHECK_REQUEST = 1081;
    private static final Map<String, ResultCallback> callbacks = new HashMap<>();

    public static boolean check(@NonNull Activity activity, @NonNull String[] ps) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) return true;
        List<String> needs = new ArrayList<>(ps.length);
        for (String s : ps) {
            int v = ActivityCompat.checkSelfPermission(activity, s);
            if (v == PackageManager.PERMISSION_GRANTED) continue;
            needs.add(s);
        }
        if (needs.isEmpty()) return true;
        ps = new String[needs.size()];
        needs.toArray(ps);
        ActivityCompat.requestPermissions(activity, ps, CHECK_REQUEST);
        return true;
    }

    /**
     * 检查应用首次使用必须权限
     */
    public static boolean checkBase(@NonNull Activity activity) {
        return check(activity, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
        });
    }

    /**
     * 检查定位权限
     */
    public static boolean checkLocation(@NonNull Activity activity) {
        return check(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
    }

    /**
     * 读取通讯录权限
     */
    public static boolean checkReadContract(@NonNull Activity activity) {
        return check(activity, new String[]{Manifest.permission.READ_CONTACTS});
    }

    /**
     * 检查权限回调
     */
    public static boolean onRequestResult(Activity activity, String[] ps, int[] grants) {
        if (null == activity) return false;
        if (null == ps || ps.length == 0) return false;
        if (null == grants || grants.length == 0) return false;
        if (ps.length != grants.length) return false;
        for (int i = 0; i < ps.length; i++) {
            String permission = ps[i];
            boolean granted = PackageManager.PERMISSION_GRANTED == grants[i];
            for (ResultCallback e : callbacks.values()) {
                e.invoke(permission, granted);
            }
            if (granted) continue;
            boolean rationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
            String refuseHint = activity.getString(R.string.permission_refuse);
            String realHint = "";
            switch (permission) {
                case Manifest.permission.ACCESS_FINE_LOCATION:
                case Manifest.permission.ACCESS_COARSE_LOCATION: {
                    realHint = activity.getString(R.string.open_location_permission);
                    break;
                }
                case Manifest.permission.READ_EXTERNAL_STORAGE:
                case Manifest.permission.WRITE_EXTERNAL_STORAGE: {
                    realHint = activity.getString(R.string.open_external_storage_permission);
                    break;
                }
            }
            String message = rationale ? refuseHint : realHint;
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public interface ResultCallback {
        void invoke(String permission, boolean granted);
    }

    public static void setCallback(String key, ResultCallback callback) {
        if (null == callback) {
            callbacks.remove(key);
        } else {
            callbacks.put(key, callback);
        }
    }
}
