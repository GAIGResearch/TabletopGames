package utilities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

/**
 * The one configured Jackson {@link ObjectMapper} for TAG. Code under {@code src/} must not create
 * its own {@code new ObjectMapper()}; a test ({@code JsonLibraryRulesTest}) enforces this.
 * <p>
 * Objects are serialised from their fields, as Gson did, not from their getters:
 * <ul>
 *     <li>All non-static, non-transient fields are written, whatever their visibility.</li>
 *     <li>Getters and is-getters are never treated as properties. With them on, Jackson would publish
 *     {@code Component.getComponentID()}, {@code getOwnerId()}, {@code GridBoard.getGridValues()} etc.,
 *     and {@code transient} would no longer keep those values out of the output.</li>
 *     <li>A class that declares two such fields with the same name, one shadowing the other, is refused, as
 *     Gson refused it; Jackson would otherwise silently write only one of them.</li>
 *     <li>Null properties and null map values are omitted.</li>
 *     <li>Map entries are written in key order where the keys are comparable. Enum keys are written by
 *     {@code name()}, so they read back; other keys by {@code toString()}, as Gson did.</li>
 *     <li>Lambdas and anonymous or local classes are not serialised, as Gson did: a property holding one is
 *     omitted, and an array element holding one is written as {@code null}. Their captured variables are
 *     implementation detail, not data.</li>
 * </ul>
 * Reading fills a {@code HashSet} or {@code HashMap} field with a {@code LinkedHashSet} or {@code LinkedHashMap},
 * so that iterating over what was read (enum keys, say) does not change from one JVM launch to the next.
 * Reading ignores unknown properties and rejects text after the first JSON value; records and
 * {@code @JsonCreator} constructors work as usual.
 * <p>
 * The returned mapper is shared: do not reconfigure it. Use {@code mapper().copy()} for a variant.
 */
public final class Json {

    private static final JsonSerializer<Object> NULL_WRITER = new JsonSerializer<>() {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeNull();
        }
    };

    private static final ObjectMapper MAPPER = build();
    private static final ObjectWriter PRETTY = MAPPER.writerWithDefaultPrettyPrinter();

    private Json() {
    }

    private static ObjectMapper build() {
        SimpleModule module = new SimpleModule("TAG conventions");
        module.setSerializers(new SimpleSerializers() {
            @Override
            public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription beanDesc) {
                return isExcluded(type.getRawClass()) ? NULL_WRITER : super.findSerializer(config, type, beanDesc);
            }
        });
        module.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                                                             List<BeanPropertyWriter> properties) {
                checkNoShadowedFields(beanDesc.getBeanClass());
                properties.replaceAll(SkipExcludedValues::new);
                return properties;
            }
        });
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc,
                                                         BeanDeserializerBuilder builder) {
                checkNoShadowedFields(beanDesc.getBeanClass());
                return builder;
            }
        });
        module.addValueInstantiator(HashSet.class, instantiator(HashSet.class, LinkedHashSet::new));
        module.addValueInstantiator(HashMap.class, instantiator(HashMap.class, LinkedHashMap::new));
        ObjectMapper mapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
                .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(SerializationFeature.FAIL_ON_ORDER_MAP_BY_INCOMPARABLE_KEY, false)
                .registerModule(module);
        return mapper;
    }

    private static ValueInstantiator instantiator(Class<?> type, Supplier<Object> create) {
        return new ValueInstantiator.Base(type) {
            @Override
            public boolean canCreateUsingDefault() {
                return true;
            }

            @Override
            public Object createUsingDefault(DeserializationContext ctxt) {
                return create.get();
            }
        };
    }

    private static void checkNoShadowedFields(Class<?> type) {
        Map<String, Class<?>> declaredIn = new HashMap<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                int m = f.getModifiers();
                if (Modifier.isStatic(m) || Modifier.isTransient(m) || f.isSynthetic() || f.isAnnotationPresent(JsonIgnore.class))
                    continue;
                Class<?> other = declaredIn.putIfAbsent(f.getName(), c);
                if (other != null)
                    throw new IllegalArgumentException(type.getName() + " declares multiple JSON fields named "
                            + f.getName() + " (in " + other.getName() + " and " + c.getName() + ")");
            }
        }
    }

    /** Classes Gson never serialised: lambdas, and anonymous or local classes other than enum constants. */
    static boolean isExcluded(Class<?> c) {
        return !Enum.class.isAssignableFrom(c) && (c.isSynthetic() || c.isAnonymousClass() || c.isLocalClass());
    }

    /** Omits a property whose value at run time is of an excluded class. */
    private static class SkipExcludedValues extends BeanPropertyWriter {
        SkipExcludedValues(BeanPropertyWriter base) {
            super(base);
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            Object value = get(bean);
            if (value != null && isExcluded(value.getClass())) return;
            super.serializeAsField(bean, gen, prov);
        }
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /** Writer that indents its output. */
    public static ObjectWriter prettyWriter() {
        return PRETTY;
    }

    /**
     * Parses JSON text into a tree.
     *
     * @throws IllegalArgumentException if the text is not a single JSON value
     */
    public static JsonNode parse(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Not valid JSON: " + e.getOriginalMessage(), e);
        }
    }

    /** The object as a JSON tree, serialised with the shared mapper's rules. */
    public static JsonNode toTree(Object obj) {
        return MAPPER.valueToTree(obj);
    }

    public static ObjectNode object() {
        return JsonNodeFactory.instance.objectNode();
    }

    public static ArrayNode array() {
        return JsonNodeFactory.instance.arrayNode();
    }
}
