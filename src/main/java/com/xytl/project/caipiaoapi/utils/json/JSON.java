package com.xytl.project.caipiaoapi.utils.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import cn.hutool.core.date.DateUtil;

public class JSON {
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_DATETIME_MINI = "yyyyMMddHHmmss";
    public static final String PATTERN_DATE = "yyyy-MM-dd";
    public static final String PATTERN_TIME = "HH:mm:ss";
    static ObjectMapper keepTypeMapper = new ObjectMapper()//
            .configure(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)//
            .configure(MapperFeature.USE_ANNOTATIONS, false)// 忽略类上的注解
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)// 允许空类
            .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);// 保持类型
    static ObjectMapper normalMapper = new ObjectMapper().configure(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, false)// 解决问题:当属性是一个数组，但是json对应 的是一个字符串类型的"[]"时，把字符串直接解析为一个对象数组
            .configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, false)//
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)//去掉默认的时间戳格式
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)// 设置为true ，反序列化的时候，属性不区分大小写
            .configure(MapperFeature.USE_GETTERS_AS_SETTERS, false)// 设置为false，反序列化不使用getter做为setter!
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)//允许使用单引号
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    static {
        normalMapper.setSerializationInclusion(Include.NON_NULL);
        normalMapper.setDateFormat(new SimpleDateFormat(PATTERN_DATETIME, Locale.CHINA));//序列化时，日期的统一格式
        normalMapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
        normalMapper.addHandler(new StringToBeanDeserializationProblemHandler());
        normalMapper.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));//设置为中国上海时区
        
        // 此处注释掉的原因是为了将枚举 全部序列化为JSON形式输出 暂时这样处理  以后业务需要的时候再进行优化
        // normalMapper.setSerializerFactory(normalMapper.getSerializerFactory().withSerializerModifier(new EduBeanSerializerModifier()));
        keepTypeMapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
        keepTypeMapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
        keepTypeMapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);
        keepTypeMapper.setVisibility(PropertyAccessor.CREATOR, Visibility.NONE);
        keepTypeMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        SimpleModule serializerModule = new SimpleModule("DateSerializer", PackageVersion.VERSION);
        serializerModule.addSerializer(Long.class, ToStringSerializer.instance);// long转string ，解决js可能在大数字时可能会有问题
        serializerModule.addSerializer(Long.TYPE, ToStringSerializer.instance);//
        serializerModule.addSerializer(long.class, ToStringSerializer.instance);//
        serializerModule.addDeserializer(Date.class, new CustomDateDeSerializer());
        serializerModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(PATTERN_DATETIME)));
        serializerModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(PATTERN_DATETIME)));
        serializerModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(PATTERN_TIME)));
        serializerModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(PATTERN_DATETIME)));
        serializerModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(PATTERN_DATE)));
        serializerModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(PATTERN_TIME)));
        serializerModule.setDeserializerModifier(new BeanDeserializerModifier() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public JsonDeserializer<Enum> modifyEnumDeserializer(DeserializationConfig config, final JavaType type, BeanDescription beanDesc, final JsonDeserializer<?> deserializer) {
                return new EnumDeserializer((Class<Enum<?>>) type.getRawClass(), deserializer);
            }
            
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
                if (Object.class == beanDesc.getBeanClass() && UntypedObjectDeserializer.class == deserializer.getClass()) {
                    return new AttrDataObjectDeserializer(deserializer);
                }
                return super.modifyDeserializer(config, beanDesc, deserializer);
            }
        });
        normalMapper.registerModule(serializerModule);
    }
    
    public static ThreadLocal<JavaType> commonResultDataType = new ThreadLocal<JavaType>();

    static class AttrDataObjectDeserializer extends JsonDeserializer<Object> {
        private UntypedObjectDeserializer untypedObjectDeserializer;

        public AttrDataObjectDeserializer(JsonDeserializer<?> deserializer) {
            untypedObjectDeserializer = (UntypedObjectDeserializer) deserializer;
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            if (commonResultDataType.get() != null //
                    && jp.getParsingContext().getParent() != null) {
                if("data".equals(jp.getParsingContext().getParent().getCurrentName())//
                        && jp.getParsingContext().getParent().getCurrentValue() != null//
                        && jp.getParsingContext().getParent().getCurrentValue().getClass().getName().indexOf("CommonResult") > 0) {
                    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
                    return mapper.readValue(jp, commonResultDataType.get());
                }
                if("messageContent".equals(jp.getParsingContext().getParent().getCurrentName())//
                        && jp.getParsingContext().getParent().getCurrentValue() != null//
                        && jp.getParsingContext().getParent().getCurrentValue().getClass().getName().indexOf("MqSyncMsg") > 0) {
                    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
                    return mapper.readValue(jp, commonResultDataType.get());
                }
            }
            return untypedObjectDeserializer.deserialize(jp, ctxt);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static class EnumDeserializer extends JsonDeserializer<Enum> {
        private final Class<? extends Enum> enumType;
        JsonDeserializer oldDeserializer;
        public EnumDeserializer(Class<? extends Enum> enumType, JsonDeserializer oldDeserializer) {
            this.enumType = enumType;
            this.oldDeserializer = oldDeserializer;
        }
        @Override
        public Enum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            try {
                String value = jp.getText();
                if ("{".equals(value)) return (Enum) oldDeserializer.deserialize(jp, ctxt);
                return Enum.valueOf(enumType, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class CustomDateDeSerializer extends DateDeserializers.DateDeserializer {
        private static final long serialVersionUID = 1L;

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p != null) {
                String calendatStr = p.getText();
                if (calendatStr != null && calendatStr.indexOf("T") < 0 && calendatStr.length() == 19) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    try {
                        return sdf.parse(calendatStr);
                    } catch (ParseException e) {
                    }
                }
                if (calendatStr != null && calendatStr.indexOf("T") < 0 && calendatStr.length() == 16) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    try {
                        return sdf.parse(calendatStr);
                    } catch (ParseException e) {
                    }
                }
                if (calendatStr != null && calendatStr.length() == 10 && calendatStr.charAt(4) == '-' && calendatStr.charAt(7) == '-') {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        return sdf.parse(calendatStr);
                    } catch (ParseException e) {
                    }
                }
            }
            return super.deserialize(p, ctxt);
        }
    }

    static class StringToBeanDeserializationProblemHandler extends DeserializationProblemHandler {

        @Override
        public Object handleMissingInstantiator(DeserializationContext ctxt, Class<?> instClass, ValueInstantiator valueInsta, JsonParser p, String msg) throws IOException {
            String json = p.getText();
            if ((json.startsWith("{") && json.endsWith("}")) || (json.startsWith("[") && json.endsWith("]"))) {
                return JSON.parseObject(p.getText(), instClass);
            }
            return DeserializationProblemHandler.NOT_HANDLED;
        }

        @Override
        public Object handleUnexpectedToken(DeserializationContext ctxt, JavaType targetType, JsonToken t, JsonParser p, String failureMsg) throws IOException {
            String json = p.getText();
            if (targetType.isEnumType() && "{".equals(json)) {
                ObjectMapper mapper = (ObjectMapper) p.getCodec();
                mapper.readTree(p);// 直接读取一个节点。这样就不会忽略之后的结点解析了
                return null;
            }
            if (targetType.isCollectionLikeType() && "{".equals(json)) {
                ObjectMapper mapper = (ObjectMapper) p.getCodec();
                Object obj = mapper.readValue(p, targetType.getContentType());// 把单个结点数据，转化为一个数组对象
                List<Object> list = new ArrayList<Object>();
                list.add(obj);
                return list;
            }
            if (targetType.getRawClass().equals(String.class) && "{".equals(json)) {
                ObjectMapper mapper = (ObjectMapper) p.getCodec();
                TreeNode node = mapper.readTree(p);// 把一个对象json，转成一个字符串
                return node.toString();
            }
            if (targetType.getRawClass().equals(String.class) && "[".equals(json)) {
                ObjectMapper mapper = (ObjectMapper) p.getCodec();
                TreeNode node = mapper.readTree(p);// 把一个对象json，转成一个字符串
                return node.toString();
            }
            if ((json.startsWith("{") && json.endsWith("}")) || (json.startsWith("[") && json.endsWith("]"))) {
                return JSON.parseObject(p.getText(), targetType);
            }
            return DeserializationProblemHandler.NOT_HANDLED;
        }
    }

    public static ObjectMapper getNormalMapper() {
        return normalMapper;
    }
    public static ObjectMapper getKeepTypeMapper() {
        return keepTypeMapper;
    }

    public static Map<String, Object> parseObject(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            return normalMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String json, Class<T> class1, boolean keepType) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            return keepType ? keepTypeMapper.readValue(json, class1) : normalMapper.readValue(json, class1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String json, TypeReference<T> typeReference, boolean keepType) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            if (keepType) {
                JavaType javaType = normalMapper.getTypeFactory().constructType(typeReference.getType());
                return keepTypeMapper.readValue(json, javaType);
            }
            return parseObject(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String json, Class<T> class1) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            return normalMapper.readValue(json, class1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String json, JavaType targetType) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            return normalMapper.readValue(json, targetType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T parseObjectWithDataType(String json, JavaType targetType, JavaType dataTargetType) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            commonResultDataType.set(dataTargetType);
            T t = normalMapper.readValue(json, targetType);
            commonResultDataType.remove();
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T parseObjectWithDataType(String json, Class<T> targetType, JavaType dataTargetType) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            commonResultDataType.set(dataTargetType);
            T t = normalMapper.readValue(json, targetType);
            commonResultDataType.remove();
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObjectWithDataType(String json, Class<T> targetType, Class<T> dataTargetType) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            commonResultDataType.set(getJavaType(dataTargetType));
            T t = normalMapper.readValue(json, targetType);
            commonResultDataType.remove();
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String json, Class<T> class1, Feature[] fastjsonUnFeatures) {
        return parseObject(json, class1);
    }

    public static String toJSONString(Object obj) {
        return toJSONString(obj, false);
    }

    public static String toJSONString(Object obj, boolean keepType) {
        try {
            if (obj == null) {
                return null;
            }
            return keepType ? keepTypeMapper.writeValueAsString(obj) : normalMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            JavaType javaType = getCollectionType(ArrayList.class, clazz);
            List<T> list = normalMapper.readValue(json, javaType);
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return normalMapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }
    
    public static JavaType getJavaType(Class<?> elementClasses) {
        return normalMapper.getTypeFactory().constructType(elementClasses);
    }
    
    public static JavaType getJavaType(Type type) {
        return normalMapper.getTypeFactory().constructType(type);
    }

    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            JavaType javaType = normalMapper.getTypeFactory().constructType(typeReference.getType());
            return normalMapper.readValue(json, javaType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String json, TypeReference<T> typeReference, Feature[] fastjsonUnFeatures) {
        return parseObject(json, typeReference);
    }

    public static String toJSONStringWithDateFormat(Object obj, String defaultPattern) {
        try {
            return normalMapper.writer(new SimpleDateFormat(defaultPattern)).writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

//    public static String toJSONStringWithDateFormatAndEnum2Obj(Object obj, String defaultPattern, Class<?>... enumKlasses) {
//        EduBeanSerializerModifier.setEnumToObject(enumKlasses);
//        String json = null;
//        try {
//            json = normalMapper.writer(new SimpleDateFormat(defaultPattern)).writeValueAsString(obj);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//        EduBeanSerializerModifier.clearEnumToObject();
//        return json;
//    }

    public static String toJSON(Object obj) {
        return toJSONString(obj);
    }

    /**
     * 枚举以类的形式输出
     * 
     * @param obj
     * @param enumKlasses
     * @return
     */
//    public static String toJSONStringWithEnum2Obj(Object obj, Class<?>... enumKlasses) {
//        EduBeanSerializerModifier.setEnumToObject(enumKlasses);
//        String json = toJSONString(obj);
//        EduBeanSerializerModifier.clearEnumToObject();
//        return json;
//    }

    public static boolean isValidArray(String value) {
        return value != null && value.startsWith("[") && value.endsWith("]");
    }

}
