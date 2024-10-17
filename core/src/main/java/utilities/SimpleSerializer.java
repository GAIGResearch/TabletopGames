package utilities;
import com.google.gson.*;

import java.lang.reflect.Type;
public class SimpleSerializer<T>  implements JsonSerializer<T> {

    @Override
    public JsonElement serialize(T t, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonElement jElement = jsonSerializationContext.serialize(t);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", t.getClass().getName());
        jsonObject.add("data", jElement);
        return jsonObject;
    }
}
