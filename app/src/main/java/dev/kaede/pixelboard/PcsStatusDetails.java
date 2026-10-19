package dev.kaede.pixelboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Bounded google.rpc.Status metadata parser; skips messages, subjects and metadata values. */
final class PcsStatusDetails {
    private static final byte[] KEY = "grpc-status-details-bin".getBytes(StandardCharsets.US_ASCII);
    static final class TrailersBinding {
        final Field count; final Method name, value;
        TrailersBinding(Class<?> metadata) throws ReflectiveOperationException {
            count=metadata.getDeclaredField("e");
            name=metadata.getDeclaredMethod("j",int.class); value=metadata.getDeclaredMethod("d",int.class);
            if(count.getType()!=int.class || Modifier.isStatic(count.getModifiers())
                    || name.getReturnType()!=byte[].class || value.getReturnType()!=Object.class
                    || Modifier.isStatic(name.getModifiers()) || Modifier.isStatic(value.getModifiers()))
                throw new NoSuchFieldException("PCS trailers schema mismatch");
            count.setAccessible(true); name.setAccessible(true); value.setAccessible(true);
        }
        String describe(Object trailers) throws ReflectiveOperationException {
            if(trailers==null) return "details=NO_TRAILERS";
            int n=count.getInt(trailers);
            if(n<0 || n>64) return "details=TRAILER_LIMIT";
            for(int i=n-1;i>=0;i--) {
                if(Arrays.equals(KEY,(byte[])name.invoke(trailers,i))) {
                    Object bytes=value.invoke(trailers,i); // Read only this allowlisted binary entry.
                    return bytes instanceof byte[] ? decode((byte[])bytes) : "details=UNSUPPORTED_ENCODING";
                }
            }
            return "details=ABSENT";
        }
    }
    static String decode(byte[] data) {
        if(data==null) return "details=ABSENT";
        if(data.length>65536) return "details=TOO_LARGE";
        try {
            Summary s=new Summary(); Cursor c=new Cursor(data);
            while(c.more()) {
                int tag=c.tag();
                if(tag==8) { long code=c.varint(); if(code<0 || code>16) throw new IllegalArgumentException(); s.code=(int)code; }
                else if(tag==26) { if(++s.details>16) return "details=DETAIL_LIMIT"; parseAny(c.bytes(),s); }
                else c.skip(tag); // Includes Status.message: never emitted.
            }
            return s.describe();
        } catch(RuntimeException bad) { return "details=MALFORMED"; }
    }
    private static void parseAny(byte[] data,Summary s) {
        Cursor c=new Cursor(data);String type="";byte[] value=null;
        while(c.more()) {
            int tag=c.tag();
            if(tag==10) type=c.shortText(160);
            else if(tag==18) value=c.bytes();
            else c.skip(tag);
        }
        if(value==null) {s.unknown++;return;}
        if("type.googleapis.com/google.rpc.BadRequest".equals(type)) {s.types.add("BAD_REQUEST");parseBadRequest(value,s);}
        else if("type.googleapis.com/google.rpc.ErrorInfo".equals(type)) {s.types.add("ERROR_INFO");parseErrorInfo(value,s);}
        else if("type.googleapis.com/google.rpc.PreconditionFailure".equals(type)) {s.types.add("PRECONDITION_FAILURE");parsePreconditions(value,s);}
        else s.unknown++;
    }
    private static void parseBadRequest(byte[] data,Summary s) {
        Cursor c=new Cursor(data);int n=0;
        while(c.more()) {
            int tag=c.tag();if(tag!=10){c.skip(tag);continue;}
            if(++n>16) throw new IllegalArgumentException();
            Cursor v=new Cursor(c.bytes());
            while(v.more()) {
                int t=v.tag();
                if(t==10) s.field(v.shortText(128));
                else if(t==26) s.reason(v.shortText(80));
                else v.skip(t); // description and localized message are not read.
            }
        }
    }
    private static void parseErrorInfo(byte[] data,Summary s) {
        Cursor c=new Cursor(data);
        while(c.more()) {int tag=c.tag();if(tag==10)s.reason(c.shortText(80));else c.skip(tag);}
        // Domain and all metadata keys/values intentionally skipped.
    }
    private static void parsePreconditions(byte[] data,Summary s) {
        Cursor c=new Cursor(data);int n=0;
        while(c.more()) {
            int tag=c.tag();if(tag!=10){c.skip(tag);continue;}
            if(++n>16)throw new IllegalArgumentException();Cursor v=new Cursor(c.bytes());
            while(v.more()){int t=v.tag();if(t==10)s.reason(v.shortText(80));else v.skip(t);}
        }
    }
    private static final class Summary {
        int code=0,details,unknown,fieldCount,reasonCount,redacted;
        final Set<String> types=new LinkedHashSet<>(); String firstField="none",firstReason="none";
        void reason(String text) {
            reasonCount++;
            if(text.matches("[A-Z][A-Z0-9_]{0,47}")){if("none".equals(firstReason))firstReason=text;}else redacted++;
        }
        void field(String text) {
            fieldCount++;
            if(text.length()<=64 && text.matches("(?:client_config|config|labels|client|build_id|device_tier|client_group|variant|attestation|integrity|encryption|public_key|request)(?:[.][A-Za-z_][A-Za-z0-9_]*|\\[[0-9]{1,4}\\])*(?:[.][A-Za-z_][A-Za-z0-9_]*)?")) {
                if("none".equals(firstField))firstField=text;
            } else redacted++;
        }
        String describe() {return "details=PRESENT status_code="+code+" types="+(types.isEmpty()?"none":String.join(",",types))
                +" reason="+firstReason+" field="+firstField+" reasons="+reasonCount+" fields="+fieldCount+" unknown="+unknown+" redacted="+redacted;}
    }
    private static final class Cursor {
        final byte[] data;int pos,fields;
        Cursor(byte[] data){this.data=data;}
        boolean more(){return pos<data.length;}
        int tag(){if(++fields>256)throw new IllegalArgumentException();long t=varint();if(t<8||t>Integer.MAX_VALUE)throw new IllegalArgumentException();return(int)t;}
        long varint(){long n=0;for(int i=0;i<10;i++){if(pos>=data.length)throw new IllegalArgumentException();int b=data[pos++]&255;if(i==9 && b>1)throw new IllegalArgumentException();n|=(long)(b&127)<<(i*7);if((b&128)==0)return n;}throw new IllegalArgumentException();}
        int length(){long n=varint();if(n<0||n>data.length-pos)throw new IllegalArgumentException();return(int)n;}
        byte[] bytes(){int n=length();byte[] out=Arrays.copyOfRange(data,pos,pos+n);pos+=n;return out;}
        String shortText(int max){int n=length();String s=n<=max?new String(data,pos,n,StandardCharsets.UTF_8):"";pos+=n;return s;}
        void skip(int tag){switch(tag&7){case 0:varint();break;case 1:advance(8);break;case 2:advance(length());break;case 5:advance(4);break;default:throw new IllegalArgumentException();}}
        void advance(int n){if(n<0||n>data.length-pos)throw new IllegalArgumentException();pos+=n;}
    }
}
