package dev.kaede.pixelboard;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ResourceStringIndexTest {
    /** Minimal table: one obfuscated-name package, native label, and alias to the label. */
    private byte[] table(boolean utf8,boolean sparse) {
        String label="Minimize keyboard";
        byte[] text=label.getBytes(utf8?StandardCharsets.UTF_8:StandardCharsets.UTF_16LE);
        int encoded=(utf8?2:2)+text.length+(utf8?1:2);
        int poolSize=(32+encoded+3)&~3;
        int typeSize=20+8+32, pkgSize=288+typeSize, total=12+poolSize+pkgSize;
        ByteBuffer b=ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short)2).putShort((short)12).putInt(total).putInt(1);
        int p=12;
        b.position(p); b.putShort((short)1).putShort((short)28).putInt(poolSize).putInt(1).putInt(0)
                .putInt(utf8?256:0).putInt(32).putInt(0).putInt(0);
        if(utf8) b.put((byte)label.length()).put((byte)text.length()); else b.putShort((short)label.length());
        b.put(text);
        p+=poolSize;
        b.position(p); b.putShort((short)0x200).putShort((short)288).putInt(pkgSize).putInt(127);
        b.position(p+288); b.putShort((short)0x201).putShort((short)20).putInt(typeSize).put((byte)20)
                .put((byte)(sparse?1:0)).putShort((short)0).putInt(2).putInt(28);
        if(sparse) b.putShort((short)0).putShort((short)0).putShort((short)1).putShort((short)4);
        else b.putInt(0).putInt(16);
        b.putShort((short)8).putShort((short)0).putInt(0).putShort((short)8).put((byte)0).put((byte)3).putInt(0);
        b.putShort((short)8).putShort((short)0).putInt(0).putShort((short)8).put((byte)0).put((byte)1).putInt(0x7f140000);
        return b.array();
    }
    @Test public void localizedResourceIdsAreFoundAcrossEncodingsAndSparseOffsets() {
        for(boolean utf8:new boolean[]{true,false}) for(boolean sparse:new boolean[]{true,false}) {
            Map<String,Set<Integer>> result=ResourceStringIndex.find(table(utf8,sparse),Set.of("Minimize keyboard"));
            assertEquals(Set.of(0x7f140000,0x7f140001),result.get("Minimize keyboard"));
            assertTrue(ResourceStringIndex.find(table(utf8,sparse),Set.of("Back to keyboard")).isEmpty());
        }
    }
    @Test public void truncatedAndOversizedTablesAreRejected() {
        byte[] valid=table(true,false);
        assertThrows(IllegalArgumentException.class,()->ResourceStringIndex.find(Arrays.copyOf(valid,valid.length-1),Set.of("x")));
        ByteBuffer.wrap(valid).order(ByteOrder.LITTLE_ENDIAN).putInt(4,Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class,()->ResourceStringIndex.find(valid,Set.of("x")));
    }
    @Test public void sixteenBitOffsetsResolveTheSameNativeLabels() {
        byte[] bytes=table(true,false);
        ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int type=12+b.getInt(16)+288;
        b.put(type+9,(byte)2);
        b.putShort(type+20,(short)0); b.putShort(type+22,(short)4);
        assertEquals(Set.of(0x7f140000,0x7f140001),ResourceStringIndex.find(bytes,Set.of("Minimize keyboard")).get("Minimize keyboard"));
    }
}
