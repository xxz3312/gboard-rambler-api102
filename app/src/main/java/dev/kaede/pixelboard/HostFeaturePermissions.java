package dev.kaede.pixelboard;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Arrays;

/** Read-only host permission status. Module permissions cannot grant access to Gboard. */
public final class HostFeaturePermissions {
    private static final String HOST = "com.google.android.inputmethod.latin";
    private HostFeaturePermissions() {}
    static String status(Context context, String permission) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(HOST, PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions == null || !Arrays.asList(info.requestedPermissions).contains(permission))
                return "安装包未声明";
            return context.getPackageManager().checkPermission(permission, HOST) == PackageManager.PERMISSION_GRANTED
                    ? "已授权" : "未授权";
        } catch (PackageManager.NameNotFoundException error) { return "未安装 Gboard"; }
    }
    public static String summary(Context context) {
        return "相机："+status(context,"android.permission.CAMERA")+"；图片："
                +status(context,Build.VERSION.SDK_INT >= 33 ? "android.permission.READ_MEDIA_IMAGES"
                : "android.permission.READ_EXTERNAL_STORAGE");
    }
}
