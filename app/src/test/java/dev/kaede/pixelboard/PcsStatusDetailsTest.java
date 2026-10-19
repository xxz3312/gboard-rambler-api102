package dev.kaede.pixelboard;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

public class PcsStatusDetailsTest {
    static byte[] var(long n){ByteArrayOutputStream b=new ByteArrayOutputStream();while((n&~127L)!=0){b.write((int)(n&127)|128);n>>>=7;}b.write((int)n);return b.toByteArray();}
    static byte[] concat(byte[]... values){ByteArrayOutputStream b=new ByteArrayOutputStream();for(byte[] v:values)b.write(v,0,v.length);return b.toByteArray();}
    static byte[] text(int f,String s){return bytes(f,s.getBytes(StandardCharsets.UTF_8));}
    static byte[] bytes(int f,byte[] b){return concat(var((f<<3)|2),var(b.length),b);}
    static byte[] number(int f,long n){return concat(var(f<<3),var(n));}
    static byte[] any(String type,byte[] value){return concat(text(1,"type.googleapis.com/google.rpc."+type),bytes(2,value));}
    static byte[] status(String type,byte[] value){return concat(number(1,3),text(2,"PRIVATE_MESSAGE"),bytes(3,any(type,value)));}
    static class Trailers {
        int e;byte[][] keys;Object[] values;int reads;
        Trailers(byte[][] k,Object[] v){e=k.length;keys=k;values=v;}
        byte[] j(int i){return keys[i];}Object d(int i){reads++;return values[i];}
    }
    @Test public void extractsBadRequestFieldWithoutDescriptions(){
        byte[] violation=concat(text(1,"client_config.labels[0].value"),text(2,"PRIVATE_TOKEN_abc"),text(3,"INVALID_LABEL"));
        String s=PcsStatusDetails.decode(status("BadRequest",bytes(1,violation)));
        assertTrue(s.contains("status_code=3"));assertTrue(s.contains("types=BAD_REQUEST"));assertTrue(s.contains("field=client_config.labels[0].value"));assertTrue(s.contains("reason=INVALID_LABEL"));
        assertFalse(s.contains("PRIVATE"));
    }
    @Test public void extractsErrorInfoReasonAndSkipsMetadataValues(){
        byte[] info=concat(text(1,"INVALID_API_KEY"),text(2,"private-domain"),bytes(3,concat(text(1,"key"),text(2,"PRIVATE_TOKEN_abc"))));
        String s=PcsStatusDetails.decode(status("ErrorInfo",info));assertTrue(s.contains("reason=INVALID_API_KEY"));assertFalse(s.contains("private"));assertFalse(s.contains("PRIVATE"));
    }
    @Test public void extractsPreconditionTypeButNotSubject(){
        String s=PcsStatusDetails.decode(status("PreconditionFailure",bytes(1,concat(text(1,"MODEL_UNAVAILABLE"),text(2,"PRIVATE_DEVICE_ID"),text(3,"PRIVATE_DESCRIPTION")))));
        assertTrue(s.contains("reason=MODEL_UNAVAILABLE"));assertFalse(s.contains("PRIVATE"));
    }
    @Test public void redactsUnexpectedFieldAndReason(){
        String s=PcsStatusDetails.decode(status("BadRequest",bytes(1,concat(text(1,"secret_parameter.PRIVATE"),text(3,"user@example.com")))));
        assertFalse(s.contains("PRIVATE"));assertFalse(s.contains("example"));assertTrue(s.contains("redacted=2"));
    }
    @Test public void ignoresUnknownAnyTypes(){String s=PcsStatusDetails.decode(status("UnknownPRIVATE",text(1,"PRIVATE")));assertTrue(s.contains("unknown=1"));assertFalse(s.contains("PRIVATE"));}
    @Test public void handlesMalformedAndOversizeInput(){assertEquals("details=MALFORMED",PcsStatusDetails.decode(new byte[]{26,(byte)255}));assertEquals("details=TOO_LARGE",PcsStatusDetails.decode(new byte[65537]));}
    @Test public void enforcesDetailLimit(){byte[][] values=new byte[18][];values[0]=number(1,3);for(int i=1;i<values.length;i++)values[i]=bytes(3,any("Unknown",new byte[0]));assertEquals("details=DETAIL_LIMIT",PcsStatusDetails.decode(concat(values)));}
    @Test public void readsOnlyAllowlistedTrailerEntry() throws Exception {
        byte[] key="grpc-status-details-bin".getBytes(StandardCharsets.US_ASCII);
        Object privateValue=new Object(){@Override public String toString(){throw new AssertionError("Private value accessed");}};
        Trailers t=new Trailers(new byte[][]{"authorization".getBytes(StandardCharsets.US_ASCII),key},new Object[]{privateValue,status("ErrorInfo",text(1,"INVALID_LABEL"))});
        String s=new PcsStatusDetails.TrailersBinding(Trailers.class).describe(t);assertTrue(s.contains("INVALID_LABEL"));assertEquals(1,t.reads);
    }
    @Test public void absentDetailsDoesNotReadOtherValues() throws Exception {
        Trailers t=new Trailers(new byte[][]{"secret".getBytes(StandardCharsets.US_ASCII)},new Object[]{"PRIVATE"});
        assertEquals("details=ABSENT",new PcsStatusDetails.TrailersBinding(Trailers.class).describe(t));assertEquals(0,t.reads);
    }
    @Test public void reportsUnsupportedEncodingAndNeverStringifiesIt() throws Exception {
        Object v=new Object(){@Override public String toString(){throw new AssertionError();}};
        Trailers t=new Trailers(new byte[][]{"grpc-status-details-bin".getBytes(StandardCharsets.US_ASCII)},new Object[]{v});
        assertEquals("details=UNSUPPORTED_ENCODING",new PcsStatusDetails.TrailersBinding(Trailers.class).describe(t));
    }
    @Test public void randomMalformedInputsRemainBounded(){Random r=new Random(28);for(int i=0;i<2000;i++){byte[] b=new byte[r.nextInt(100)];r.nextBytes(b);assertTrue(PcsStatusDetails.decode(b).length()<=320);}}
    @Test(expected=NoSuchFieldException.class) public void rejectsWrongMetadataSchema() throws Exception {new PcsStatusDetails.TrailersBinding(Object.class);}
}
