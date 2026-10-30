package dev.kaede.pixelboard;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static dev.kaede.pixelboard.RamblerUiResolver.*;

/** Native key actions and notification lifecycle adapted from PixelBoard 1.3.3 (GPL-3.0). */
final class RamblerUiHooks {
    private final Context context;
    private final RamblerUiResolver.Binding binding;
    private final RamblerUiSession session=new RamblerUiSession();
    private final AtomicBoolean armed=new AtomicBoolean(false);
    private final AtomicInteger events=new AtomicInteger();
    private RamblerUiHooks(Context context,RamblerUiResolver.Binding binding) { this.context=context; this.binding=binding; }

    static void install(XposedModule module,Context context,ClassLoader loader) {
        try {
            RamblerUiResolver.Binding binding=RamblerUiResolver.resolve(context,loader);
            new RamblerUiHooks(context,binding).install(module);
            CloudRuntimeStatus.send(context,"RAMBLER_UI","installed semantic-graph native-labels; header="
                    +binding.methods[HEADER_KEY].getDeclaringClass().getName()+" widget="
                    +binding.methods[WIDGET_KEY].getDeclaringClass().getName());
        } catch(Exception | LinkageError error) {
            CloudRuntimeStatus.send(context,"RAMBLER_UI","skipped:"+error.getClass().getSimpleName()+"; native layout retained");
            Log.w("PixelBoardLS","Rambler UI graph could not be installed; native layout retained",error);
        }
    }
    private boolean enabled() { return armed.get() && FeatureConfig.rambler() && RamblerSelection.mayExpose(context); }
    private void install(XposedModule module) {
        Method[] m=binding.methods;
        // Installed interceptors remain inert until the complete family has installed successfully.
        module.hook(m[CLOSE_KEY]).intercept(chain -> {
            RamblerUiSession.Scope scope=RamblerUiSession.Scope.current(chain.getThisObject());
            if(!armed.get() || scope==null) return chain.proceed();
            scope.closeDepth++;
            try { return chain.proceed(); } finally { scope.closeDepth--; }
        });
        module.hook(m[FACTORY]).intercept(chain -> {
            RamblerUiSession.Scope scope=RamblerUiSession.Scope.current(chain.getThisObject());
            if(!enabled() || scope==null || !scope.interceptFactory()) return chain.proceed();
            try {
                Object builder=m[BUILDER].invoke(chain.getThisObject(),chain.getArgs().get(0),chain.getArgs().get(1));
                m[LABEL].invoke(builder,scope.purpose==RamblerUiSession.Purpose.MINIMIZE?binding.minimizeLabel:binding.keyboardLabel);
                Object key=m[BUILD].invoke(builder);
                if(key==null) throw new IllegalStateException("Native builder returned null");
                scope.relabeled=true;
                return key;
            } catch(ReflectiveOperationException | RuntimeException error) {
                problem("label",error);
            }
            return chain.proceed();
        });
        module.hook(m[HEADER_KEY]).intercept(chain -> {
            Object owner=chain.getThisObject();
            if(!enabled()) { clear(owner); return chain.proceed(); }
            try(RamblerUiSession.Scope scope=new RamblerUiSession.Scope(owner,RamblerUiSession.Purpose.MINIMIZE)) {
                Object nativeKey=chain.proceed(); // Preserve host exceptions and its own branch decisions.
                if(!scope.relabeled || nativeKey==null) { clear(owner); return nativeKey; }
                try {
                    Object close=m[CLOSE_KEY].invoke(owner,m[CLOSE_ICON].invoke(owner));
                    if(close==null) return nativeKey;
                    m[ADD].invoke(null,binding.slot,nativeKey);
                    session.placed(owner,binding.slot);
                    event("placed minimize; close retained");
                    return close;
                } catch(ReflectiveOperationException | RuntimeException error) { problem("place",error); return nativeKey; }
            }
        });
        module.hook(m[WIDGET_KEY]).intercept(chain -> {
            if(!enabled()) return chain.proceed();
            try(RamblerUiSession.Scope scope=new RamblerUiSession.Scope(chain.getThisObject(),RamblerUiSession.Purpose.KEYBOARD)) {
                Object key=chain.proceed();
                if(scope.relabeled) event("keyboard return labeled");
                return key;
            }
        });
        module.hook(m[TEARDOWN]).intercept(chain -> {
            if(armed.get()) clear(chain.getThisObject());
            return chain.proceed();
        });
        module.hook(m[HIDE]).intercept(chain -> {
            Object result=chain.proceed();
            if(armed.get()) {
                try { session.reveal(chain.getThisObject(),(ViewGroup)chain.getArgs().get(0)); }
                catch(RuntimeException error) { problem("reveal",error); }
            }
            return result;
        });
        armed.set(true);
    }
    private void clear(Object owner) {
        int slot=session.slot(owner);
        if(slot==0) return;
        try {
            binding.methods[REMOVE].invoke(null,slot,"ramble_close");
            session.cleared(owner);
            event("minimize removed");
        } catch(ReflectiveOperationException | RuntimeException error) { problem("remove",error); }
    }
    private void event(String detail) {
        if(FeatureConfig.diagnostics() && events.incrementAndGet()<=16) CloudRuntimeStatus.send(context,"RAMBLER_UI_EVENT",detail);
    }
    private void problem(String stage,Exception error) {
        event(stage+" failed:"+error.getClass().getSimpleName());
        Log.w("PixelBoardLS","Rambler UI "+stage+" failed; native key retained",error);
    }
}
