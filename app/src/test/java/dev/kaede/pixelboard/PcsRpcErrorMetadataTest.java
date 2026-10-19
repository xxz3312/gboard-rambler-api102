package dev.kaede.pixelboard;

import org.junit.Test;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.*;

public class PcsRpcErrorMetadataTest {
    enum Code { INVALID_ARGUMENT, PERMISSION_DENIED, UNKNOWN, SECRET_ENUM }
    static class Trailers { @Override public String toString() { throw new AssertionError("Never read trailers"); } }
    static class Status { Code r; String s; Status(Code c,String d) {r=c;s=d;} }
    static class RpcFailure extends RuntimeException { Status a; Trailers b=new Trailers(); RpcFailure(Code c,String d) {super(d);a=new Status(c,d);} }
    PcsRpcErrorMetadata.Binding binding() throws Exception {return new PcsRpcErrorMetadata.Binding(RpcFailure.class,Status.class,Code.class,Trailers.class);}
    @Test public void recognizesWrappedGenericArgumentFailure() throws Exception {
        String s=binding().describe(new ExecutionException(new RpcFailure(Code.INVALID_ARGUMENT,"Request contains an invalid argument.")));
        assertEquals("rpc=INVALID_ARGUMENT description=GENERIC_INVALID_ARGUMENT trailersPresent=true",s);
    }
    @Test public void neverLeaksErrorDescriptionOrTrailerValues() throws Exception {
        String s=binding().describe(new RpcFailure(Code.PERMISSION_DENIED,"PRIVATE_TOKEN_abc"));
        assertFalse(s.contains("PRIVATE_TOKEN"));assertTrue(s.contains("OTHER_REDACTED"));
    }
    @Test public void handlesNonRpcExceptionAndNull() throws Exception { assertTrue(binding().describe(new IllegalArgumentException("secret")).contains("rpc=UNKNOWN"));assertEquals("rpc=UNKNOWN cause=none",binding().describe(null)); }
    @Test public void unknownStatusCodeIsRedacted() throws Exception {String s=binding().describe(new RpcFailure(Code.SECRET_ENUM,"secret"));assertTrue(s.startsWith("rpc=UNKNOWN"));assertFalse(s.contains("SECRET"));}
    @Test(expected=NoSuchFieldException.class) public void rejectsUnrelatedErrorSchema() throws Exception { new PcsRpcErrorMetadata.Binding(RuntimeException.class,Status.class,Code.class,Trailers.class); }
}
