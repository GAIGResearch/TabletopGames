package utilities;
import com.google.gson.*;

import java.lang.reflect.Type;
public class SimpleDeserializer<T> implements JsonDeserializer<T> {

    @Override
    public T deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get("name");
        String className = prim.getAsString();
        Class<T> clazz = getClassInstance(className);
        JsonElement jsonObjectData = jsonObject.get("data");
        return jsonDeserializationContext.deserialize(jsonObjectData, clazz);
    }

    @SuppressWarnings("unchecked")
    public Class<T> getClassInstance(String className) {
        try {
            return (Class<T>) Class.forName(className);
        } catch (ClassNotFoundException cnfe) {
            throw new JsonParseException(cnfe.getMessage());
        }
    }
}
