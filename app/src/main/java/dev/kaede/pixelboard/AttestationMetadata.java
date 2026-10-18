package dev.kaede.pixelboard;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Parse-only diagnostics. Never retains/logs certificates, challenges, IDs or tokens. */
public final class AttestationMetadata {
    private static final byte[] PLACEHOLDER =
            "PLACEHOLDER_ATTESTATION_FOR_LOCAL_DEBUG".getBytes(StandardCharsets.UTF_8);
    private static final String OID = "1.3.6.1.4.1.11129.2.1.17";
    private AttestationMetadata() {}

    public static String describe(byte[][] chain) {
        if (chain == null) return "entries=0 x509=0 placeholder=false parse_only=true";
        if (chain.length > 8) return "entries=" + chain.length + " inspection=bounded parse_only=true";
        int parsed = 0;
        boolean placeholder = false;
        String extension = "key_description=absent";
        // Android chain order is leaf -> root. Prefer the extension nearest the root.
        boolean foundExtension = false;
        for (int i = chain.length - 1; i >= 0; i--) {
            byte[] bytes = chain[i];
            if (Arrays.equals(PLACEHOLDER, bytes)) placeholder = true;
            if (bytes == null || bytes.length == 0 || bytes.length > 65536) continue;
            try {
                ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                X509Certificate certificate = (X509Certificate)
                        CertificateFactory.getInstance("X.509").generateCertificate(stream);
                if (stream.available() != 0) continue;
                parsed++;
                byte[] encoded = certificate.getExtensionValue(OID);
                if (!foundExtension && encoded != null) {
                    foundExtension = true;
                    try { extension = keyDescription(encoded); }
                    catch (IllegalArgumentException e) { extension = "key_description=malformed"; }
                }
            } catch (java.security.cert.CertificateException | RuntimeException ignored) { }
        }
        return "entries=" + chain.length + " x509=" + parsed + " placeholder=" + placeholder
                + " " + extension + " parse_only=true";
    }

    static String keyDescription(byte[] encoded) {
        Node octets = root(encoded, 4);
        Node sequence = read(encoded, octets.start, octets.end);
        if (sequence.kind != 0 || sequence.tag != 16 || sequence.end != octets.end)
            throw new IllegalArgumentException("Not a key description sequence");
        List<Node> fields = sequence.children();
        if (fields.size() != 8) throw new IllegalArgumentException("Key description arity");
        String result = "key_description=present security=" + security(fields.get(1).number(10))
                + " keymaster=" + security(fields.get(3).number(10));
        Node tee = fields.get(7);
        if (tee.kind != 0 || tee.tag != 16) throw new IllegalArgumentException("Authorization list");
        for (Node field : tee.children()) {
            if (field.kind != 2 || field.tag != 704) continue;
            Node trust = read(encoded, field.start, field.end);
            if (trust.kind != 0 || trust.tag != 16 || trust.end != field.end)
                throw new IllegalArgumentException("Root of trust");
            List<Node> items = trust.children();
            if (items.size() < 3) throw new IllegalArgumentException("Root of trust arity");
            Node locked = items.get(1);
            if (locked.kind != 0 || locked.tag != 1 || locked.end - locked.start != 1)
                throw new IllegalArgumentException("Invalid Boolean");
            int value = encoded[locked.start] & 255;
            if (value != 0 && value != 255) throw new IllegalArgumentException("Non-DER Boolean");
            return result + " deviceLocked=" + (value != 0)
                    + " boot=" + boot(items.get(2).number(10));
        }
        return result + " root_of_trust=absent";
    }

    private static String security(int value) {
        return value == 0 ? "SOFTWARE" : value == 1 ? "TEE" : value == 2 ? "STRONGBOX" : "UNKNOWN";
    }
    private static String boot(int value) {
        return value == 0 ? "VERIFIED" : value == 1 ? "SELF_SIGNED"
                : value == 2 ? "UNVERIFIED" : value == 3 ? "FAILED" : "UNKNOWN";
    }
    private static Node root(byte[] data, int tag) {
        if (data == null || data.length > 65536) throw new IllegalArgumentException("Size");
        Node result = read(data, 0, data.length);
        if (result.kind != 0 || result.tag != tag || result.end != data.length)
            throw new IllegalArgumentException("Unexpected root");
        return result;
    }
    private static Node read(byte[] data, int offset, int limit) {
        if (offset < 0 || offset >= limit || limit > data.length) throw new IllegalArgumentException("Truncated tag");
        int first = data[offset++] & 255;
        int tag = first & 31;
        if (tag == 31) {
            tag = 0;
            int count = 0, part;
            do {
                if (offset >= limit || count++ >= 4) throw new IllegalArgumentException("Tag overflow");
                part = data[offset++] & 255;
                tag = (tag << 7) | (part & 127);
            } while ((part & 128) != 0);
        }
        if (offset >= limit) throw new IllegalArgumentException("Missing length");
        int size = data[offset++] & 255;
        if ((size & 128) != 0) {
            int bytes = size & 127;
            if (bytes == 0 || bytes > 3 || bytes > limit - offset)
                throw new IllegalArgumentException("Invalid length");
            size = 0;
            for (int i = 0; i < bytes; i++) size = (size << 8) | (data[offset++] & 255);
        }
        if (size > limit - offset) throw new IllegalArgumentException("Truncated value");
        return new Node(data, first >>> 6, tag, offset, offset + size);
    }
    private static final class Node {
        final byte[] data;
        final int kind, tag, start, end;
        Node(byte[] data, int kind, int tag, int start, int end) {
            this.data = data; this.kind = kind; this.tag = tag; this.start = start; this.end = end;
        }
        List<Node> children() {
            List<Node> result = new ArrayList<>();
            for (int next = start; next < end;) {
                if (result.size() >= 256) throw new IllegalArgumentException("Too many nodes");
                Node item = read(data, next, end);
                result.add(item);
                next = item.end;
            }
            return result;
        }
        int number(int expectedTag) {
            if (kind != 0 || tag != expectedTag || end - start < 1 || end - start > 4
                    || (data[start] & 128) != 0) throw new IllegalArgumentException("Invalid number");
            int value = 0;
            for (int i = start; i < end; i++) value = (value << 8) | (data[i] & 255);
            return value;
        }
    }
}
