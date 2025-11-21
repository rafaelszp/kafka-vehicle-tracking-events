package szp.rafael.tracking.stream.serializers;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;


public final class TreeSetTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<? super T> rawType = typeToken.getRawType();

        // Only handle java.util.TreeSet (not all NavigableSet implementations)
        if (!TreeSet.class.isAssignableFrom(rawType)) {
            return null; // let other adapters handle it
        }

        // Extract element type E from TreeSet<E>
        Type elementType = Object.class;
        Type type = typeToken.getType();
        if (type instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            if (typeArgs != null && typeArgs.length == 1) {
                elementType = typeArgs[0];
            }
        }

        TypeAdapter<?> elementAdapter = gson.getAdapter(TypeToken.get(elementType));
        TypeAdapter<TreeSet<?>> treeSetAdapter = new Adapter(elementAdapter, elementType);

        return (TypeAdapter<T>) treeSetAdapter;
    }

    private static final class Adapter<E> extends TypeAdapter<TreeSet<E>> {
        private final TypeAdapter<E> elementAdapter;
        private final Type elementType;

        Adapter(TypeAdapter<E> elementAdapter, Type elementType) {
            // elementAdapter may be null theoretically but gson.getAdapter guarantees non-null
            this.elementAdapter = elementAdapter;
            this.elementType = elementType;
        }

        @Override
        public void write(JsonWriter out, TreeSet<E> value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            out.beginArray();
            for (E element : value) {
                // delegate element serialization (handles nulls)
                elementAdapter.write(out, element);
            }
            out.endArray();
        }

        @Override
        public TreeSet<E> read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            List<E> elements = new ArrayList<>();
            in.beginArray();
            while (in.hasNext()) {
                E element = elementAdapter.read(in);
                elements.add(element);
            }
            in.endArray();

            // Create TreeSet with natural ordering. Important: if the original set used
            // a custom Comparator it will be lost. If elements are not Comparable this
            // may throw ClassCastException. Consider using a different representation
            // or reattaching the comparator externally when required.
            TreeSet<E> result = new TreeSet<>();
            result.addAll(elements);
            return result;
        }
    }
}

