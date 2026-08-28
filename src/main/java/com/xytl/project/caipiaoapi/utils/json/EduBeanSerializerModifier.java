package com.xytl.project.caipiaoapi.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class EduBeanSerializerModifier extends BeanSerializerModifier {

    public static ThreadLocal<Set<Class<?>>> enumToObj = new ThreadLocal<Set<Class<?>>>();

    public static void setEnumToObject(Class<?>... klsList) {
        if (klsList == null || klsList.length == 0) {
            enumToObj.set(null);
            return;
        }
        Set<Class<?>> set = new HashSet<>();
        for (Class<?> cls : klsList) {
            set.add(cls);
        }
        enumToObj.set(set);
    }

    public static void clearEnumToObject() {
        enumToObj.set(null);
    }

    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        if (beanDesc.getBeanClass().isEnum()) {
            return new EnumJsonSerializer(serializer);
        }
        return serializer;
    }

    boolean isEnum(BeanPropertyWriter writer) {
        Class<?> clazz = writer.getType().getRawClass();
        return clazz.isEnum();
    }

    static class EnumJsonSerializer extends JsonSerializer<Object> {

        JsonSerializer<Object> beanSerializer;

        @SuppressWarnings("unchecked")
        public EnumJsonSerializer(JsonSerializer<?> beanSerializer) {
            this.beanSerializer = (JsonSerializer<Object>) beanSerializer;
        }

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value != null) {
                if (enumToObj.get() != null && enumToObj.get().contains(value.getClass())) {
                    beanSerializer.serialize(value, jgen, provider);// remove以后，就会以object的形式输出
                } else {
                    jgen.writeString(value.toString());
                }
            }
        }
    }
}
