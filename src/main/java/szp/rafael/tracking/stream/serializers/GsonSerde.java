package szp.rafael.tracking.stream.serializers;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.common.serialization.Serde;

import java.util.Map;

public class GsonSerde<T> implements Serde<T> {
    private final Gson gson = build();
    private final Class<T> clazz;

    public GsonSerde(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public org.apache.kafka.common.serialization.Serializer<T> serializer() {
        return (topic, data) -> {
            if (data == null) return null;
            return gson.toJson(data).getBytes();
        };
    }

    @Override
    public org.apache.kafka.common.serialization.Deserializer<T> deserializer() {
        return (topic, bytes) -> {
            if (bytes == null) return null;
            return gson.fromJson(new String(bytes), clazz);
        };
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

    public static Gson build() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(new TreeSetTypeAdapterFactory())
                .create();
    }
}