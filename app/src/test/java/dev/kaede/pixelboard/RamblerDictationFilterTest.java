package dev.kaede.pixelboard;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class RamblerDictationFilterTest {
    public static class Action { int c; String d; Action(int intent, String text) { c=intent; d=text; } }
    public static class Span { String c="generated"; Action j; Span(int intent, String text) { j=new Action(intent,text); } }
    public static class Voice { List<Object> b=new ArrayList<>(); }
    @Test public void restoresComposeButPreservesAllEditingIntents() {
        Voice voice=new Voice();
        for(int intent=0;intent<=5;intent++)voice.b.add(new Span(intent,"spoken"));
        voice.b.add(new Span(4,""));
        voice.b.add(new Object()); voice.b.add(null);
        RamblerDictationFilter.filter(voice);
        for(int intent=0;intent<=5;intent++) {
            Span span=(Span)voice.b.get(intent);
            assertEquals(intent==4?"spoken":"generated",span.c);
            assertEquals(intent==4?0:intent,span.j.c);
        }
        assertEquals("generated",((Span)voice.b.get(6)).c);
        assertEquals(4,((Span)voice.b.get(6)).j.c);
    }
    @Test public void unexpectedSchemaIsUntouched() {
        RamblerDictationFilter.filter(null);
        RamblerDictationFilter.filter(new Object());
    }
}
