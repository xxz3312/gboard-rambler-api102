package dev.kaede.pixelboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

/** Resolves the complete native key construction and notification graph, without symbol names. */
final class RamblerUiResolver {
    static final String BASE = "com/google/android/apps/inputmethod/libs/agenticdictation/ui/impl/AbstractRamblerUiController";
    static final String HEADER = "com/google/android/apps/inputmethod/libs/agenticdictation/ui/impl/RamblerUiVkController";
    static final int HEADER_KEY=0, WIDGET_KEY=1, FACTORY=2, BUILDER=3, LABEL=4, BUILD=5,
            CLOSE_KEY=6, CLOSE_ICON=7, TEARDOWN=8, HIDE=9, ADD=10, REMOVE=11;
    static final class Binding {
        final Method[] methods;
        final int slot, minimizeLabel, keyboardLabel;
        Binding(Method[] methods, int slot, int minimize, int keyboard) {
            if (methods.length != 12 || slot == 0 || minimize == 0 || keyboard == 0)
                throw new IllegalArgumentException("Incomplete Rambler UI binding");
            this.methods=methods; this.slot=slot; minimizeLabel=minimize; keyboardLabel=keyboard;
            validate(methods);
            for (Method method : methods) method.setAccessible(true);
        }
    }
    private RamblerUiResolver() {}

    static Binding resolve(Context context, ClassLoader loader) throws Exception {
        String fingerprint="rambler-ui-1|"+GboardResolver.fingerprint(context);
        SharedPreferences cache=context.getSharedPreferences("pixelboard_rambler_ui",0);
        int slot=context.getResources().getIdentifier("key_pos_header_end_extra_key","id",context.getPackageName());
        Configuration cfg=new Configuration(context.getResources().getConfiguration()); cfg.setLocale(Locale.US);
        Resources english=context.createConfigurationContext(cfg).getResources();
        if (fingerprint.equals(cache.getString("fingerprint",""))) {
            try {
                Method[] methods=new Method[12];
                for(int i=0;i<methods.length;i++) {
                    String[] args=cache.getString("args"+i,"").isEmpty()?new String[0]:cache.getString("args"+i,"").split(",");
                    methods[i]=new TargetMethod(cache.getString("owner"+i,""),cache.getString("name"+i,""),
                            cache.getString("return"+i,""),args).resolve(loader);
                }
                int min=cache.getInt("min",0), keyboard=cache.getInt("keyboard",0);
                require("Minimize keyboard".equals(english.getString(min)) && "Back to keyboard".equals(english.getString(keyboard)));
                return new Binding(methods,slot,min,keyboard);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) { cache.edit().clear().apply(); }
        }
        Method[] methods;
        try(HostDex dex=new HostDex(context)) { methods=scan(dex,loader); }
        Map<String,Set<Integer>> labels=ResourceStringIndex.find(resources(context.getApplicationInfo().sourceDir),
                new HashSet<>(Arrays.asList("Minimize keyboard","Back to keyboard")));
        int min=label(labels,"Minimize keyboard",english), keyboard=label(labels,"Back to keyboard",english);
        Binding binding=new Binding(methods,slot,min,keyboard);
        SharedPreferences.Editor edit=cache.edit().clear().putString("fingerprint",fingerprint).putInt("min",min).putInt("keyboard",keyboard);
        for(int i=0;i<methods.length;i++) {
            Method m=methods[i]; String[] args=Arrays.stream(m.getParameterTypes()).map(Class::getName).toArray(String[]::new);
            edit.putString("owner"+i,m.getDeclaringClass().getName()).putString("name"+i,m.getName())
                    .putString("return"+i,m.getReturnType().getName()).putString("args"+i,String.join(",",args));
        }
        edit.apply();
        return binding;
    }
    private static byte[] resources(String apk) throws IOException {
        try(ZipFile zip=new ZipFile(apk)) {
            java.util.zip.ZipEntry entry=zip.getEntry("resources.arsc");
            if(entry==null || entry.getSize()>64*1024*1024) throw new IOException("ARSC missing or too large");
            try(InputStream in=zip.getInputStream(entry); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] chunk=new byte[8192]; int n;
                while((n=in.read(chunk))!=-1) { if(out.size()>64*1024*1024-n) throw new IOException("ARSC too large"); out.write(chunk,0,n); }
                return out.toByteArray();
            }
        }
    }
    private static int label(Map<String,Set<Integer>> labels,String text,Resources english) {
        int selected=0;
        for(int id:labels.getOrDefault(text,Collections.emptySet())) {
            try {
                if ("string".equals(english.getResourceTypeName(id)) && text.equals(english.getString(id))) selected=id;
            } catch(Resources.NotFoundException ignored) {}
        }
        require(selected!=0); // Equal English labels are interchangeable; retain a native localized resource ID.
        return selected;
    }
    private static Class<?> owner(HostDex dex,ClassLoader loader,String anchor) throws Exception {
        Set<String> names=new HashSet<>();
        for(MethodData data:dex.findMethod(FindMethod.create().matcher(MethodMatcher.create().usingStrings(anchor))))
            if(data.getMethodName().equals("<clinit>")) names.add(data.getClassName());
        return Class.forName(GboardResolver.unique(new ArrayList<>(names)),false,loader);
    }
    private static List<MethodData> members(HostDex dex,Class<?> owner) {
        return dex.findMethod(FindMethod.create().matcher(MethodMatcher.create().declaredClass(owner.getName())));
    }
    private static boolean signature(MethodData data,String ret,String... args) {
        return data.getReturnTypeName().equals(ret) && data.getParamTypeNames().equals(Arrays.asList(args));
    }
    private static Method reflect(MethodData data,ClassLoader loader) throws Exception { return data.getMethodInstance(loader); }
    private static boolean calls(MethodData data,Method target,ClassLoader loader) throws Exception {
        for(MethodData call:data.getInvokes())
            if(call.getMethodName().equals(target.getName()) && call.getClassName().equals(target.getDeclaringClass().getName())
                    && reflect(call,loader).equals(target)) return true;
        return false;
    }
    private static Method unique(List<Method> values) { return GboardResolver.unique(new ArrayList<>(new LinkedHashSet<>(values))); }
    private static Method[] scan(HostDex dex,ClassLoader loader) throws Exception {
        Class<?> base=owner(dex,loader,BASE), header=owner(dex,loader,HEADER);
        require(header.getSuperclass()==base);
        List<MethodData> baseMethods=members(dex,base);
        List<Method> factories=new ArrayList<>();
        for(MethodData data:baseMethods) if(data.getParamTypeNames().equals(Arrays.asList("int","java.lang.Runnable"))) {
            for(MethodData call:data.getInvokes())
                if(call.getClassName().equals(base.getName()) && call.getParamTypeNames().equals(data.getParamTypeNames())
                        && !call.getReturnTypeName().equals(data.getReturnTypeName())) factories.add(reflect(data,loader));
        }
        Method[] m=new Method[12]; m[FACTORY]=unique(factories);
        Class<?> key=m[FACTORY].getReturnType();
        List<Method> builders=new ArrayList<>(), closeKeys=new ArrayList<>();
        for(MethodData data:baseMethods) {
            if(data.getParamTypeNames().equals(Arrays.asList("int","java.lang.Runnable")) && !data.getReturnTypeName().equals(key.getName())
                    && data.getUsingStrings().contains("ramble_close")) builders.add(reflect(data,loader));
            if(signature(data,key.getName(),"int") && calls(data,m[FACTORY],loader)) closeKeys.add(reflect(data,loader));
        }
        m[BUILDER]=unique(builders); m[CLOSE_KEY]=unique(closeKeys);
        List<Method> labels=new ArrayList<>(), builds=new ArrayList<>(), icons=new ArrayList<>(), tears=new ArrayList<>(), adds=new ArrayList<>();
        for(MethodData data:baseMethods) {
            if(reflectable(data) && data.getMethodName().equals(m[BUILDER].getName()))
                for(MethodData call:data.getInvokes()) if(call.getClassName().equals(m[BUILDER].getReturnType().getName()) && signature(call,"void","int")) labels.add(reflect(call,loader));
            if(reflectable(data) && data.getMethodName().equals(m[FACTORY].getName()))
                for(MethodData call:data.getInvokes()) if(call.getClassName().equals(m[BUILDER].getReturnType().getName()) && signature(call,key.getName())) builds.add(reflect(call,loader));
            if(signature(data,"int") && data.getUsingStrings().contains("pref_key_jetson_close_icon")) icons.add(reflect(data,loader));
            if(data.getUsingStrings().containsAll(Arrays.asList("ramble_close","ramble_retry","push_to_ramble"))
                    && data.getReturnTypeName().equals("void") && data.getParamTypeNames().size()==3) tears.add(reflect(data,loader));
            for(MethodData call:data.getInvokes()) if(signature(call,"void","int",key.getName())) {
                Method candidate=reflect(call,loader); if(Modifier.isStatic(candidate.getModifiers())) adds.add(candidate);
            }
        }
        m[LABEL]=unique(labels); m[BUILD]=unique(builds); m[CLOSE_ICON]=unique(icons); m[TEARDOWN]=unique(tears); m[ADD]=unique(adds);
        List<Method> removes=new ArrayList<>();
        for(MethodData data:baseMethods) if(data.getMethodName().equals(m[TEARDOWN].getName()))
            for(MethodData call:data.getInvokes()) if(signature(call,"void","int","java.lang.String")) {
                Method candidate=reflect(call,loader); if(Modifier.isStatic(candidate.getModifiers())) removes.add(candidate);
            }
        m[REMOVE]=unique(removes);
        List<Method> headerKeys=new ArrayList<>(), widgetKeys=new ArrayList<>(), hides=new ArrayList<>();
        for(MethodData data:dex.findMethod(FindMethod.create().matcher(MethodMatcher.create().returnType(key.getName()).paramCount(0)))) {
            Method candidate=reflect(data,loader);
            if(candidate.getDeclaringClass().getSuperclass()!=base || !calls(data,m[FACTORY],loader) || !calls(data,m[CLOSE_KEY],loader)) continue;
            if(candidate.getDeclaringClass()==header) headerKeys.add(candidate); else widgetKeys.add(candidate);
        }
        for(MethodData data:members(dex,header)) if(signature(data,"void","android.view.ViewGroup","boolean")) {
            boolean children=false, animation=false;
            for(MethodData call:data.getInvokes()) {
                children |= call.getClassName().equals("android.view.ViewGroup") && call.getMethodName().equals("getChildCount");
                animation |= call.getClassName().equals("android.view.View") && call.getMethodName().equals("animate");
            }
            if(children && animation) hides.add(reflect(data,loader));
        }
        m[HEADER_KEY]=unique(headerKeys); m[WIDGET_KEY]=unique(widgetKeys); m[HIDE]=unique(hides);
        return m;
    }
    private static boolean reflectable(MethodData data) { return !data.getMethodName().startsWith("<"); }
    static void validate(Method[] m) {
        Class<?> base=m[FACTORY].getDeclaringClass(), key=m[FACTORY].getReturnType(), builder=m[BUILDER].getReturnType();
        require(base==m[BUILDER].getDeclaringClass() && key!=builder && !key.isPrimitive() && !builder.isPrimitive());
        require(m[HEADER_KEY].getDeclaringClass().getSuperclass()==base && m[WIDGET_KEY].getDeclaringClass().getSuperclass()==base
                && m[HEADER_KEY].getDeclaringClass()!=m[WIDGET_KEY].getDeclaringClass());
        check(m[FACTORY],false,key,int.class,Runnable.class); check(m[BUILDER],false,builder,int.class,Runnable.class);
        check(m[LABEL],false,void.class,int.class); require(m[LABEL].getDeclaringClass()==builder);
        check(m[BUILD],false,key); require(m[BUILD].getDeclaringClass()==builder);
        check(m[HEADER_KEY],false,key); check(m[WIDGET_KEY],false,key);
        check(m[CLOSE_KEY],false,key,int.class); check(m[CLOSE_ICON],false,int.class);
        require(m[CLOSE_KEY].getDeclaringClass()==base && m[CLOSE_ICON].getDeclaringClass()==base && m[TEARDOWN].getDeclaringClass()==base);
        require(m[TEARDOWN].getReturnType()==void.class && !Modifier.isStatic(m[TEARDOWN].getModifiers())
                && m[TEARDOWN].getParameterCount()==3 && m[TEARDOWN].getParameterTypes()[1]==View.class && m[TEARDOWN].getParameterTypes()[2]==View.class);
        check(m[HIDE],false,void.class,ViewGroup.class,boolean.class); require(m[HIDE].getDeclaringClass()==m[HEADER_KEY].getDeclaringClass());
        check(m[ADD],true,void.class,int.class,key); check(m[REMOVE],true,void.class,int.class,String.class);
    }
    private static void check(Method m,boolean stat,Class<?> ret,Class<?>... args) {
        require(Modifier.isStatic(m.getModifiers())==stat && !Modifier.isAbstract(m.getModifiers())
                && m.getReturnType()==ret && Arrays.equals(m.getParameterTypes(),args));
    }
    private static void require(boolean condition) { if(!condition) throw new IllegalArgumentException("Rambler UI graph mismatch"); }
}
