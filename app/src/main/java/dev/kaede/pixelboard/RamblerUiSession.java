package dev.kaede.pixelboard;

import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;

/** Per-call key purpose and per-controller placement; no global "key placed" Boolean. */
final class RamblerUiSession {
    enum Purpose { MINIMIZE, KEYBOARD }
    static final class Scope implements AutoCloseable {
        private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
        final Object owner;
        final Purpose purpose;
        private final Scope previous;
        int closeDepth;
        boolean relabeled;
        Scope(Object owner,Purpose purpose) { this.owner=owner; this.purpose=purpose; previous=CURRENT.get(); CURRENT.set(this); }
        static Scope current(Object owner) { Scope s=CURRENT.get(); return s!=null && s.owner==owner?s:null; }
        boolean interceptFactory() { return closeDepth==0; }
        @Override public void close() { if(previous==null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    private WeakReference<Object> owner=new WeakReference<>(null);
    private int slot;
    synchronized void placed(Object controller,int slot) { owner=new WeakReference<>(controller); this.slot=slot; }
    synchronized int slot(Object controller) { return controller!=null && owner.get()==controller?slot:0; }
    synchronized int cleared(Object controller) {
        int value=slot(controller);
        if(value!=0) { owner.clear(); slot=0; }
        return value;
    }
    boolean reveal(Object controller,ViewGroup header) {
        int id=slot(controller);
        if(id==0 || header==null) return false;
        View key=header.findViewById(id);
        if(key==null) return false;
        key.animate().cancel(); key.setAlpha(1f); key.setVisibility(View.VISIBLE);
        return true;
    }
}
