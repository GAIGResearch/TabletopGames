package games.terraformingmars.rules;
import com.google.gson.*;
import games.terraformingmars.rules.requirements.Requirement;
import utilities.Pair;
import utilities.SimpleDeserializer;
import utilities.SimpleSerializer;

import java.lang.reflect.Type;
public class Discount extends Pair<Requirement, Integer> implements JsonSerializer<Discount>, JsonDeserializer<Discount>
{
    public Discount() {
        super(null, null);
    }

    public Discount(Requirement a, Integer b) {
        super(a, b);
    }

    @Override
    public JsonElement serialize(Discount discount, Type type, JsonSerializationContext jsonSerializationContext) {

        JsonElement req = new SimpleSerializer<Requirement>().serialize(discount.a, type, jsonSerializationContext);

//        JsonElement jElement = jsonSerializationContext.serialize(t);
        JsonObject jsonObject = new JsonObject();
//        jsonObject.addProperty("name", discount.getClass().getName());
        jsonObject.add("requirement", req);
        jsonObject.addProperty("amount", discount.b);
        return jsonObject;


    }
    @Override
    public Discount deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
//        JsonPrimitive prim = (JsonPrimitive) jsonObject.get("name");
//        String className = prim.getAsString();

        JsonElement jsonObjectReq = jsonObject.get("requirement");
        Requirement req = new SimpleDeserializer<Requirement>().deserialize(jsonObjectReq, type, jsonDeserializationContext);
        int amount = jsonObject.get("amount").getAsInt();
        return new Discount(req, amount);
    }

}
