package dev.kaede.pixelboard;

import android.graphics.Paint;
import android.text.TextPaint;
import java.util.*;

/** Per-thread draw transaction. Restore the host's latest alpha even if drawing throws. */
final class CandidatePaintScope implements AutoCloseable {
    private static final ThreadLocal<CandidatePaintScope> CURRENT = new ThreadLocal<>();
    private final CandidatePaintScope previous;
    private final boolean active;
    private final IdentityHashMap<Paint, Integer> alphas = new IdentityHashMap<>();
    private boolean adjusting;
    CandidatePaintScope(boolean active) {
        this.active = active;
        previous = CURRENT.get();
        CURRENT.set(this);
    }
    static void afterPaintChange(Object receiver) {
        CandidatePaintScope scope = CURRENT.get();
        if (scope == null || !scope.active || scope.adjusting || !(receiver instanceof TextPaint)) return;
        Paint paint = (Paint) receiver;
        scope.alphas.put(paint, paint.getAlpha());
        scope.adjusting = true;
        try { paint.setAlpha(255); }
        finally { scope.adjusting = false; }
    }
    @Override public void close() {
        adjusting = true;
        try { for (Map.Entry<Paint, Integer> entry : alphas.entrySet()) entry.getKey().setAlpha(entry.getValue()); }
        finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
}
