package szp.rafael.tracking.helpers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class CompositeKeyUtils {

    /**
     * Cria a chave composta (key + timestamp) para o buffer store.
     * Retorna um array de bytes.
     */
    public static byte[] buildCompositeKey(final String key, final long eventTimeMillis) {
        Objects.requireNonNull(key, "key must not be null");
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        // 4 bytes para o length + keyBytes + 8 bytes para o timestamp
        ByteBuffer bb = ByteBuffer.allocate(4 + keyBytes.length + 8);
        bb.putInt(keyBytes.length);
        bb.put(keyBytes);
        bb.putLong(eventTimeMillis); // big-endian por default no ByteBuffer
        return bb.array();
    }

    public static long extractTimestamp(final byte[] compositeKey) {
        ByteBuffer bb = ByteBuffer.wrap(compositeKey);
        int len = bb.getInt();           // 4 bytes
        bb.position(4 + len);            // pula o keyBytes
        return bb.getLong();             // pega o long (big-endian)
    }

    public static String extractKey(final byte[] compositeKey) {
        ByteBuffer bb = ByteBuffer.wrap(compositeKey);
        int len = bb.getInt();
        byte[] keyBytes = new byte[len];
        bb.get(keyBytes);
        return new String(keyBytes, StandardCharsets.UTF_8);
    }

    public static byte[]  lowerBoundForKey(final String key) {
        return buildCompositeKey(key, 0L); // ou 0L se preferir
    }

    public static byte[] upperBoundForKey(final String key, final long cutoffTs) {
        return buildCompositeKey(key, cutoffTs);
    }

}
