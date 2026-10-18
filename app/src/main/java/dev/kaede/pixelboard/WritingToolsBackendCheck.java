package dev.kaede.pixelboard;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

/** Binding prerequisites only. Passing this check never proves inference availability. */
public final class WritingToolsBackendCheck {
    private static final String GBOARD = "com.google.android.inputmethod.latin";
    private WritingToolsBackendCheck() {}

    public static final class Result {
        public final boolean canAttempt;
        public final String code;
        public final String message;
        private Result(boolean canAttempt, String code, String message) {
            this.canAttempt = canAttempt;
            this.code = code;
            this.message = message;
        }
    }

    public static Result check(Context context, int backend) {
        if (backend != 2 && backend != 3)
            return blocked("UNKNOWN_BACKEND", "后端配置无效，保留基础云端模式。");
        PackageManager pm = context.getPackageManager();
        String packageName = backend == 2 ? "com.google.android.aicore" : "com.google.android.as.oss";
        String[] services = backend == 2 ? new String[] {
                "com.google.android.apps.aicore.service.multiuser.AiCoreMultiUserService",
                "com.google.android.apps.aicore.service.AiCoreService"
        } : new String[] {"com.google.android.apps.miphone.astrea.grpc.AstreaGrpcService"};
        try {
            if (!pm.getApplicationInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS).enabled)
                return blocked("PACKAGE_DISABLED", "后端应用已停用，保留基础云端模式。");
            boolean found = false;
            boolean active = false;
            boolean denied = false;
            boolean accessible = false;
            for (String className : services) {
                ServiceInfo service;
                try {
                    service = pm.getServiceInfo(new ComponentName(packageName, className),
                            PackageManager.MATCH_DISABLED_COMPONENTS);
                } catch (PackageManager.NameNotFoundException e) { continue; }
                found = true;
                if (!service.enabled || !service.exported) continue;
                active = true;
                if (service.permission != null
                        && pm.checkPermission(service.permission, GBOARD) != PackageManager.PERMISSION_GRANTED) {
                    denied = true;
                    continue;
                }
                accessible = true;
            }
            if (denied)
                return blocked("GBOARD_BIND_PERMISSION_DENIED",
                        "Gboard 未获授后端服务的绑定权限，V2 不启用；保留基础云端模式。");
            if (accessible)
                return new Result(true, "BINDING_PREREQUISITES_OK",
                        "绑定前置检查通过；模型、账号、RPC 与推理结果仍未验证。");
            if (found && !active)
                return blocked("SERVICE_DISABLED_OR_NOT_EXPORTED",
                        "后端服务已停用或未导出，V2 不启用；保留基础云端模式。");
            return blocked("SERVICE_NOT_VISIBLE", "后端服务未找到或不可见，保留基础云端模式。");
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return blocked("PACKAGE_NOT_VISIBLE", "无法核实后端应用与权限，保留基础云端模式。");
        }
    }

    private static Result blocked(String code, String message) {
        return new Result(false, code, message);
    }
}
