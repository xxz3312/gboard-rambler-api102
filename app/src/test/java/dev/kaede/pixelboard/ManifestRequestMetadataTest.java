package dev.kaede.pixelboard;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ManifestRequestMetadataTest {
    static final class Labels { int b=0,c=7,d=4,e=0; long f=20063; }
    static final class Request { Labels c=new Labels(); String d="PRIVATE_AUXILIARY_CONTENT"; }
    static final class WrongRequest { static Labels c; String d; }
    @Test public void reportsConvertedValuesWithoutMutatingOrLeakingAuxiliaryString() throws Exception {
        Request r=new Request(); Labels original=r.c;
        String result=new ManifestRequestMetadata.Binding(Request.class,Labels.class).describe(r);
        assertTrue(result.contains("wire_build_id=20063"));
        assertTrue(result.contains("wire_client_group=4"));
        assertTrue(result.contains("auxiliaryStringPresent=true"));
        assertFalse(result.contains(r.d)); assertSame(original,r.c); assertEquals(20063,r.c.f);
    }
    @Test public void rejectsStaticSchemaAndHandlesMissingLabels() throws Exception {
        try { new ManifestRequestMetadata.Binding(WrongRequest.class,Labels.class); fail(); }
        catch(NoSuchFieldException expected) {}
        Request r=new Request();r.c=null;
        assertTrue(new ManifestRequestMetadata.Binding(Request.class,Labels.class).describe(r).contains("labels=absent"));
    }
}
