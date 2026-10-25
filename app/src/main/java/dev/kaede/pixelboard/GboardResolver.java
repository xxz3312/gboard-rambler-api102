package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

/** Unique semantic matches; never force a result when signatures are ambiguous. */
public final class GboardResolver {
    private static final String REV = "semantic-cloud-1";
    private static final String VOICE = RamblerBindings.VOICE;
    private static final String EXTENSION =
            "com/google/android/apps/inputmethod/libs/agenticdictation/extension/AgenticDictationExtension";
    private GboardResolver() {}
    public static final class Result {
        public final RamblerFlagBindings.Binding flags;
        public final RamblerBindings.Profile voice;
        public final boolean writing;
        public final String source;
        Result(RamblerFlagBindings.Binding flags, RamblerBindings.Profile voice, boolean writing, String source) {
            this.flags=flags; this.voice=voice; this.writing=writing; this.source=source;
        }
    }
    public static Result resolve(Context context, ClassLoader loader) {
        // Fast compatibility fallback is available even when native scanning fails.
        RamblerBindings.Profile knownVoice = RamblerBindings.select(loader);
        RamblerFlagBindings.Binding knownFlags = RamblerFlagBindings.resolveUnique(loader);
        SharedPreferences cache = context.getSharedPreferences("pixelboard_semantic_cloud", 0);
        String key = fingerprint(context);
        if (key.equals(cache.getString("key", ""))) {
            try {
                Method getter = Class.forName(cache.getString("getter_owner", ""), false, loader)
                        .getDeclaredMethod(cache.getString("getter_name", ""));
                RamblerFlagBindings.Binding flags = binding(getter, "semantic-cache");
                RamblerBindings.Profile voice = null;
                if (cache.getBoolean("voice", false)) {
                    TargetMethod[] methods = new TargetMethod[6];
                    for (int i=0;i<6;i++) {
                        String prefix="m"+i;
                        String args=cache.getString(prefix+"_args", "");
                        methods[i]=new TargetMethod(cache.getString(prefix+"_owner",""),
                                cache.getString(prefix+"_name",""),cache.getString(prefix+"_ret",""),
                                args.isEmpty()?new String[0]:args.split(","));
                        methods[i].resolve(loader);
                    }
                    voice = new RamblerBindings.Profile("semantic-cache",methods);
                }
                return new Result(flags,voice,cache.getBoolean("writing",false),"cache");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                cache.edit().clear().apply();
            }
        }
        try {
            System.loadLibrary("dexkit");
            try (DexKitBridge bridge = DexKitBridge.create(context.getApplicationInfo().sourceDir)) {
                List<Method> getters = new ArrayList<>();
                for (MethodData candidate : bridge.findMethod(FindMethod.create().matcher(
                        MethodMatcher.create().returnType("java.lang.Object").paramCount(0)
                                .usingStrings("Invalid flag: ")))) {
                    Method method = candidate.getMethodInstance(loader);
                    try { binding(method,"semantic-scan"); getters.add(method); }
                    catch (ReflectiveOperationException | IllegalArgumentException ignored) {}
                }
                Method getter = unique(getters);
                RamblerFlagBindings.Binding flags=binding(getter,"semantic-scan");
                boolean writing=hasString(bridge,"writing_tools") && hasString(bridge,"config_proofread")
                        && hasString(bridge,"writing_tools_enable_stable_entrance");
                RamblerBindings.Profile voice;
                try { voice=resolveVoice(bridge,loader); }
                catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                    voice=knownVoice;
                }
                SharedPreferences.Editor edit=cache.edit().clear().putString("key",key)
                        .putString("getter_owner",getter.getDeclaringClass().getName())
                        .putString("getter_name",getter.getName()).putBoolean("writing",writing)
                        .putBoolean("voice",voice!=null);
                if (voice!=null) {
                    TargetMethod[] methods=voice.all();
                    for (int i=0;i<methods.length;i++) {
                        TargetMethod method=methods[i]; String prefix="m"+i;
                        edit.putString(prefix+"_owner",method.owner).putString(prefix+"_name",method.name)
                                .putString(prefix+"_ret",method.returnType)
                                .putString(prefix+"_args",String.join(",",method.arguments));
                    }
                }
                edit.apply();
                return new Result(flags,voice,writing,"semantic-scan");
            }
        } catch (Exception | LinkageError failure) {
            android.util.Log.w("PixelBoardLS","Semantic resolver failed; verified signatures only",failure);
            // This is a signature fallback, not a blanket version-number allowlist.
            return new Result(knownFlags,knownVoice,knownFlags!=null && knownVoice!=null
                    && "18.4.1".equals(knownVoice.id),"verified-signature-fallback");
        }
    }
    private static String fingerprint(Context context) {
        android.content.pm.ApplicationInfo app=context.getApplicationInfo();
        StringBuilder key=new StringBuilder(REV);
        List<String> paths=new ArrayList<>(); paths.add(app.sourceDir);
        if(app.splitSourceDirs!=null) paths.addAll(Arrays.asList(app.splitSourceDirs));
        for(String path:paths) { File file=new File(path); key.append('|').append(path)
                .append(':').append(file.length()).append(':').append(file.lastModified()); }
        // Package update paths are immutable outside root; every cached descriptor is revalidated.
        return key.toString();
    }
    static RamblerFlagBindings.Binding binding(Method getter,String source) throws ReflectiveOperationException {
        int modifiers=getter.getModifiers();
        if(Modifier.isStatic(modifiers)||Modifier.isAbstract(modifiers)||Modifier.isNative(modifiers)
                ||getter.getReturnType()!=Object.class||getter.getParameterCount()!=0)
            throw new IllegalArgumentException("Invalid getter signature");
        Class<?> owner=getter.getDeclaringClass();
        owner.getDeclaredConstructor(String.class,Class.class);
        List<Field> names=new ArrayList<>(); boolean typed=false;
        for(Field field:owner.getDeclaredFields()) if(!Modifier.isStatic(field.getModifiers())) {
            if(field.getType()==String.class) names.add(field);
            if(field.getType()==Class.class) typed=true;
        }
        if(!typed) throw new IllegalArgumentException("No flag type");
        Field name=unique(names); name.setAccessible(true); getter.setAccessible(true);
        return new RamblerFlagBindings.Binding(source,owner.getName(),getter,name);
    }
    static <T> T unique(List<T> candidates) {
        if(candidates.size()!=1) throw new IllegalArgumentException("Match count="+candidates.size());
        return candidates.get(0);
    }
    private static boolean hasString(DexKitBridge bridge,String value) {
        return !bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create().usingStrings(value))).isEmpty();
    }
    private static TargetMethod target(Method method) {
        Class<?>[] params=method.getParameterTypes(); String[] args=new String[params.length];
        for(int i=0;i<params.length;i++) args[i]=params[i].getName();
        return new TargetMethod(method.getDeclaringClass().getName(),method.getName(),
                method.getReturnType().getName(),args);
    }
    private static RamblerBindings.Profile resolveVoice(DexKitBridge bridge,ClassLoader loader)
            throws ReflectiveOperationException {
        Class<?> voice=Class.forName(VOICE,false,loader);
        Class<?> selector=Class.forName(RamblerBindings.SELECTOR,false,loader);
        List<Method> layouts=new ArrayList<>(),creates=new ArrayList<>(),writes=new ArrayList<>();
        for(Method method:voice.getDeclaredMethods()) {
            int mod=method.getModifiers(); Class<?>[] args=method.getParameterTypes();
            if(!Modifier.isStatic(mod)&&Modifier.isPublic(mod)&&args.length==0&&method.getReturnType()==int.class)
                layouts.add(method);
            if(!Modifier.isStatic(mod)&&Modifier.isPublic(mod)&&method.getReturnType()==void.class
                    &&Arrays.equals(args,new Class<?>[]{android.os.Bundle.class})) creates.add(method);
            if(Modifier.isStatic(mod)&&method.getReturnType()==void.class&&args.length==4
                    &&args[0]==boolean.class&&args[2]==selector&&args[3]==selector) writes.add(method);
        }
        List<Method> setups=new ArrayList<>();
        for(MethodData data:bridge.findMethod(FindMethod.create().matcher(
                MethodMatcher.create().declaredClass(VOICE).returnType("void").paramCount(0)))) {
            Method method=data.getMethodInstance(loader);
            if(!Modifier.isPublic(method.getModifiers())||Modifier.isStatic(method.getModifiers())) continue;
            boolean superCall=false,unified=false;
            for(MethodData call:data.getInvokes()) {
                if(call.getClassName().equals(voice.getSuperclass().getName())
                        &&call.getMethodName().equals(method.getName())&&call.getParamTypeNames().isEmpty())
                    superCall=true;
                if(call.getClassName().equals(VOICE)&&call.getUsingStrings().contains("setupUnifiedLayout"))
                    unified=true;
            }
            if(superCall&&unified) setups.add(method);
        }
        List<Method> reads=new ArrayList<>();
        for(MethodData data:bridge.findMethod(FindMethod.create().matcher(
                MethodMatcher.create().returnType("boolean").paramTypes("android.content.Context")
                        .usingStrings("pref_key_switch_to_muse")))) {
            for(MethodData call:data.getInvokes()) if(call.getClassName().equals(data.getClassName())
                    &&!call.getMethodName().equals(data.getMethodName())
                    &&call.getReturnTypeName().equals("boolean")
                    &&call.getParamTypeNames().equals(Collections.singletonList("android.content.Context"))) {
                Method read=call.getMethodInstance(loader);
                if(Modifier.isStatic(read.getModifiers())&&!reads.contains(read)) reads.add(read);
            }
        }
        List<Method> defaults=new ArrayList<>();
        for(MethodData data:bridge.findMethod(FindMethod.create().matcher(
                MethodMatcher.create().returnType("void").paramCount(0)
                        .usingStrings(EXTENSION,"onCreateExtension","pref_key_switch_to_muse")))) {
            Method method=data.getMethodInstance(loader);
            if(!Modifier.isStatic(method.getModifiers())&&Modifier.isPublic(method.getModifiers())) defaults.add(method);
        }
        TargetMethod[] targets={target(unique(layouts)),target(unique(setups)),target(unique(creates)),
                target(unique(writes)),target(unique(reads)),target(unique(defaults))};
        for(TargetMethod t:targets)t.resolve(loader);
        return new RamblerBindings.Profile("semantic-scan",targets);
    }
}
