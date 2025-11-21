package szp.rafael.tracking.model;

import com.google.gson.Gson;
import szp.rafael.tracking.stream.serializers.GsonSerde;

public class AbstractModel {

    private transient Gson gson = GsonSerde.build();

    public String toJSONString(){
        return gson.toJson(this);
    }
}
