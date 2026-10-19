package dev.kaede.pixelboard;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class PcsManifestMetadataTest {
    static class Bytes { int n; Bytes(int n) { this.n=n; } public int d() { return n; } @Override public String toString() { throw new AssertionError("Do not inspect payload"); } }
    static class Release { long b=974378940L; }
    static class Label { String b,c; Label(String k,String v) { b=k;c=v; } }
    static class Config { String c=PcsManifestMetadata.CLIENT; List<Label> d=new ArrayList<>(); Release e=new Release(); }
    static class Proof { Bytes b=new Bytes(123),d=new Bytes(7); int c=1; }
    static class Key { Bytes b=new Bytes(65); }
    static class Options { boolean b=false; }
    static class Request { Config c=new Config(); Key d=new Key(); Proof e=new Proof(); Options f=new Options(); }
    private PcsManifestMetadata.Binding binding() throws Exception {
        return new PcsManifestMetadata.Binding(Request.class,Config.class,Label.class,Release.class,Proof.class,Bytes.class,Key.class,Options.class);
    }
    @Test public void capturesOnlyLabelsAndLengthsWithoutChangingRequest() throws Exception {
        Request r=new Request(); r.c.d.add(new Label("build_id","22347")); r.c.d.add(new Label("client_group","third_party_experimental")); r.c.d.add(new Label("device_tier",""));
        Request before=r; Proof proof=r.e; List<Label> labels=r.c.d;
        String s=binding().describe(r);
        assertTrue(s.contains("build=22347")); assertTrue(s.contains("tier=empty")); assertTrue(s.contains("apk_version=974378940"));
        assertTrue(s.contains("att_mode=1")); assertTrue(s.contains("token_n=123")); assertTrue(s.contains("key_n=65"));
        assertSame(before,r); assertSame(proof,r.e); assertSame(labels,r.c.d); assertEquals(123,r.e.b.n);
    }
    @Test public void rejectsUnrelatedClient() throws Exception { Request r=new Request();r.c.c="other";assertNull(binding().describe(r)); }
    @Test public void redactsUnexpectedValuesAndUnknownLabelContent() throws Exception {
        Request r=new Request();for(String k:List.of("build_id","client_group","device_tier","variant","secret-label"))r.c.d.add(new Label(k,"PRIVATE_TOKEN_abc"));
        String s=binding().describe(r);assertFalse(s.contains("PRIVATE_TOKEN"));assertFalse(s.contains("secret-label"));assertTrue(s.contains("unknown_labels=1"));
    }
    @Test public void handlesNullOptionalMessages() throws Exception { Request r=new Request();r.e=null;r.d=null;r.f=null;String s=binding().describe(r);assertTrue(s.contains("att_mode=-1"));assertTrue(s.contains("key_n=-1")); }
    @Test public void acceptsOnlyKnownVariantSyntax() throws Exception { Request r=new Request();r.c.d.add(new Label("variant","VARIANT_24"));assertTrue(binding().describe(r).contains("variant=VARIANT_24")); }
    @Test public void emittedLineFitsDiagnosticProtocol() throws Exception {
        Request r=new Request();r.c.d.add(new Label("build_id","22347"));r.c.d.add(new Label("client_group","third_party_experimental"));r.c.d.add(new Label("device_tier",""));
        assertTrue(("id=16 "+binding().describe(r)).length()<=320);
    }
    @Test(expected=NoSuchFieldException.class) public void mismatchedSchemaFailsClosed() throws Exception { new PcsManifestMetadata.Binding(Object.class,Config.class,Label.class,Release.class,Proof.class,Bytes.class,Key.class,Options.class); }
}
