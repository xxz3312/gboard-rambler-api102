package dev.kaede.pixelboard;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Read-only, exact 513248 request schema. Never reads keys, tokens or auxiliary string content. */
final class ManifestRequestMetadata {
    static final class Binding {
        private final Field labels,auxiliary,tier,client,group,variant,build;
        Binding(Class<?> request,Class<?> config) throws ReflectiveOperationException {
            labels=field(request,"c",config);
            auxiliary=field(request,"d",String.class);
            tier=field(config,"b",int.class);
            client=field(config,"c",int.class);
            group=field(config,"d",int.class);
            variant=field(config,"e",int.class);
            build=field(config,"f",long.class);
        }
        String describe(Object request) throws ReflectiveOperationException {
            Object config=labels.get(request);
            if(config==null) return "stage=pre_pcs_rpc labels=absent read_only=true";
            String extra=(String)auxiliary.get(request);
            return "stage=pre_pcs_rpc wire_build_id="+build.getLong(config)
                    +" wire_device_tier="+tier.getInt(config)
                    +" wire_client="+client.getInt(config)
                    +" wire_client_group="+group.getInt(config)
                    +" wire_variant="+variant.getInt(config)
                    +" auxiliaryStringPresent="+(extra!=null&&!extra.isEmpty())
                    +" read_only=true";
        }
    }
    private static Field field(Class<?> owner,String name,Class<?> type) throws ReflectiveOperationException {
        Field f=owner.getDeclaredField(name);
        if(f.getType()!=type||Modifier.isStatic(f.getModifiers()))throw new NoSuchFieldException("Request schema mismatch");
        f.setAccessible(true);
        return f;
    }
}
