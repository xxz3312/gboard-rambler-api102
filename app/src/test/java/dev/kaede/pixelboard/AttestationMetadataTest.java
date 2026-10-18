package dev.kaede.pixelboard;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AttestationMetadataTest {
    private static byte[] join(byte[]... arrays) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] a : arrays) out.write(a, 0, a.length);
        return out.toByteArray();
    }
    private static byte[] der(int tag, byte[] content) {
        if (content.length >= 128) throw new IllegalArgumentException();
        return join(new byte[] {(byte)tag, (byte)content.length}, content);
    }
    private static byte[] description(boolean locked, int boot) {
        byte[] trust = der(48, join(der(4, new byte[] {1}), der(1, new byte[] {(byte)(locked ? 255 : 0)}),
                der(10, new byte[] {(byte)boot}), der(4, new byte[] {2})));
        byte[] context704 = join(new byte[] {(byte)191, (byte)133, 64, (byte)trust.length}, trust);
        byte[] sequence = der(48, join(der(2, new byte[] {4}), der(10, new byte[] {1}),
                der(2, new byte[] {4}), der(10, new byte[] {1}),
                der(4, "private challenge".getBytes(StandardCharsets.UTF_8)), der(4, new byte[0]),
                der(48, new byte[0]), der(48, context704)));
        return der(4, sequence);
    }
    @Test public void parsesBootMetadataWithoutExposingChallenge() {
        String report = AttestationMetadata.keyDescription(description(false, 2));
        assertTrue(report.contains("security=TEE"));
        assertTrue(report.contains("deviceLocked=false boot=UNVERIFIED"));
        assertFalse(report.contains("challenge"));
        assertTrue(AttestationMetadata.keyDescription(description(true, 0)).contains("deviceLocked=true boot=VERIFIED"));
    }
    @Test public void detectsPlaceholderAndOpaqueMaterial() {
        String report = AttestationMetadata.describe(new byte[][] {
                "PLACEHOLDER_ATTESTATION_FOR_LOCAL_DEBUG".getBytes(StandardCharsets.UTF_8), new byte[32]});
        assertTrue(report.contains("entries=2 x509=0 placeholder=true"));
        assertFalse(report.contains("LOCAL_DEBUG"));
        assertTrue(AttestationMetadata.describe(new byte[][] {new byte[32]}).contains("placeholder=false"));
    }
    @Test public void distinguishesCertificatesFromOpaqueBytes() {
        byte[] certificate = java.util.Base64.getDecoder().decode(
                "MIIBjzCCATWgAwIBAgIUGVyY6HbaMR8w0f+MeACOUpHqRjAwCgYIKoZIzj0EAwIwHTEbMBkGA1UEAwwSZGlhZ25vc3RpYy1maXh0dXJlMB4XDTI2MTAxODE5NTA0MVoXDTI2MTAxOTE5NTA0MVowHTEbMBkGA1UEAwwSZGlhZ25vc3RpYy1maXh0dXJlMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEG5kzI+in1pSC9cQgY95yZ4+U6qJUoQroojj7KSfQChxjP1XXAhIconBJa49hSRuZNpNmehn8KdBZ8gGNPnUDiKNTMFEwHQYDVR0OBBYEFInJPU7Du9yO+fPYSB/0Ugg82bS2MB8GA1UdIwQYMBaAFInJPU7Du9yO+fPYSB/0Ugg82bS2MA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDSAAwRQIgcy2gnYkfpO1SC80Vs0nivN1D5+TnbZsbxYi0jkt7xvgCIQDtlRXoJZffN8C+1cXKLLaE10xwmdQMKb1YWCGiHmqJGQ==");
        String report = AttestationMetadata.describe(new byte[][] {certificate, new byte[] {1, 2, 3}});
        assertTrue(report.contains("entries=2 x509=1 placeholder=false"));
        assertTrue(report.contains("key_description=absent"));
        assertFalse(report.contains("fixture"));
    }
    @Test public void rejectsTruncatedAndIndefiniteDer() {
        assertThrows(IllegalArgumentException.class, () -> AttestationMetadata.keyDescription(new byte[] {4, (byte)128, 0, 0}));
        assertThrows(IllegalArgumentException.class, () -> AttestationMetadata.keyDescription(new byte[] {4, 12, 1}));
        byte[] good = description(true, 0);
        assertThrows(IllegalArgumentException.class, () -> AttestationMetadata.keyDescription(java.util.Arrays.copyOf(good, good.length - 1)));
    }
}
