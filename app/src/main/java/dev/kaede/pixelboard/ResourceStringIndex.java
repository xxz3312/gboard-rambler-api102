package dev.kaede.pixelboard;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Minimal read-only ARSC string index. Unknown entry formats are skipped, never guessed. */
final class ResourceStringIndex {
    private final ByteBuffer data;
    private final Set<String> wanted;
    private final Map<String, Set<Integer>> result = new HashMap<>();
    private final Map<Integer, Integer> aliases = new HashMap<>();
    private List<String> strings = Collections.emptyList();

    private ResourceStringIndex(byte[] bytes, Set<String> wanted) {
        data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        this.wanted = wanted;
    }

    static Map<String, Set<Integer>> find(byte[] bytes, Set<String> wanted) {
        ResourceStringIndex index = new ResourceStringIndex(bytes, wanted);
        index.table();
        // Alias chains are bounded and only resolve to strings already found in this table.
        for (int pass = 0; pass < 8; pass++) {
            boolean changed = false;
            for (Map.Entry<Integer, Integer> alias : index.aliases.entrySet())
                for (Set<Integer> ids : index.result.values())
                    if (ids.contains(alias.getValue())) changed |= ids.add(alias.getKey());
            if (!changed) break;
        }
        return index.result;
    }

    private int u16(int p) { range(p, 2); return data.getShort(p) & 0xffff; }
    private int i32(int p) { range(p, 4); return data.getInt(p); }
    private int u8(int p) { range(p, 1); return data.get(p) & 255; }
    private void range(int p, int size) {
        if (p < 0 || size < 0 || p > data.limit() - size) throw new IllegalArgumentException("ARSC bounds");
    }
    private int end(int p, int parentEnd) {
        int size = i32(p + 4), header = u16(p + 2);
        if (header < 8 || size < header || size > parentEnd - p) throw new IllegalArgumentException("ARSC chunk");
        return p + size;
    }
    private void table() {
        if (u16(0) != 2 || u16(2) < 12) throw new IllegalArgumentException("Not an ARSC table");
        int limit = end(0, data.limit());
        for (int p = u16(2); p < limit;) {
            int stop = end(p, limit);
            if (u16(p) == 1) strings = pool(p, stop);
            if (u16(p) == 0x200) pkg(p, stop);
            p = stop;
        }
    }
    private List<String> pool(int p, int stop) {
        int count = i32(p + 8), header = u16(p + 2), start = i32(p + 20);
        if (header < 28 || count < 0 || count > (stop - p - header) / 4
                || start < header || start > stop - p) throw new IllegalArgumentException("ARSC pool");
        boolean utf8 = (i32(p + 16) & 0x100) != 0;
        List<String> values = new ArrayList<>(count);
        for (int n = 0; n < count; n++) {
            int offset = i32(p + header + n * 4);
            if (offset < 0 || offset >= stop - p - start) throw new IllegalArgumentException("ARSC string offset");
            int[] at = {p + start + offset};
            int length = length(at, utf8);
            if (utf8) length = length(at, true);
            int size = Math.multiplyExact(length, utf8 ? 1 : 2);
            if (size > stop - at[0]) throw new IllegalArgumentException("ARSC string length");
            byte[] text = new byte[size];
            ByteBuffer source=data.duplicate();
            source.position(at[0]); source.get(text);
            values.add(new String(text, utf8 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16LE));
        }
        return values;
    }
    private int length(int[] at, boolean utf8) {
        int v = utf8 ? u8(at[0]++) : u16(at[0]);
        if (!utf8) at[0] += 2;
        int high = utf8 ? 0x80 : 0x8000;
        if ((v & high) == 0) return v;
        int next = utf8 ? u8(at[0]++) : u16(at[0]);
        if (!utf8) at[0] += 2;
        return ((v & (high - 1)) << (utf8 ? 8 : 16)) | next;
    }
    private void pkg(int p, int stop) {
        int header = u16(p + 2), id = i32(p + 8);
        if (header < 284 || id < 1 || id > 255) throw new IllegalArgumentException("ARSC package");
        int typeOffset = header >= 288 ? i32(p + 284) : 0;
        for (int q = p + header; q < stop;) {
            int next = end(q, stop);
            if (u16(q) == 0x201) type(q, next, id, typeOffset);
            q = next;
        }
    }
    private void type(int p, int stop, int pkg, int typeOffset) {
        int header = u16(p + 2), count = i32(p + 12), entries = i32(p + 16);
        int type = u8(p + 8) + typeOffset, flags = u8(p + 9);
        if (header < 20 || count < 0 || count > 65536 || type < 1 || type > 255
                || entries < header || entries > stop - p || (flags & ~3) != 0)
            throw new IllegalArgumentException("ARSC type");
        int width = (flags & 2) != 0 ? 2 : 4;
        if (count > (entries - header) / width) throw new IllegalArgumentException("ARSC offsets");
        for (int n = 0; n < count; n++) {
            int index = n, offset;
            if ((flags & 1) != 0) {
                index = u16(p + header + n * 4);
                offset = u16(p + header + n * 4 + 2) * 4;
            } else if ((flags & 2) != 0) {
                offset = u16(p + header + n * 2);
                if (offset == 0xffff) continue;
                offset *= 4;
            } else {
                offset = i32(p + header + n * 4);
                if (offset == -1) continue;
            }
            if (offset < 0 || offset > stop - p - entries - 8) throw new IllegalArgumentException("ARSC entry");
            int e = p + entries + offset, size = u16(e), entryFlags = u16(e + 2);
            if ((entryFlags & (1 | 8)) != 0) continue; // complex / compact
            if (size < 8 || size > stop - e - 8) throw new IllegalArgumentException("ARSC value");
            int value = e + size, kind = u8(value + 3), payload = i32(value + 4);
            int resource = (pkg << 24) | (type << 16) | index;
            if (kind == 1) aliases.put(resource, payload);
            if (kind == 3 && payload >= 0 && payload < strings.size()) {
                String text = strings.get(payload);
                if (wanted.contains(text)) result.computeIfAbsent(text, k -> new TreeSet<>()).add(resource);
            }
        }
    }
}
