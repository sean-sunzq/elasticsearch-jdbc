package me.yufan.elasticsearch.common.json.esjson.core.deserializer;

import me.yufan.elasticsearch.common.json.JSONException;
import me.yufan.elasticsearch.common.json.esjson.core.JSONObject;
import me.yufan.elasticsearch.common.json.esjson.core.JSONParser;
import me.yufan.elasticsearch.common.json.esjson.core.bean.FieldSetterInfo;
import me.yufan.elasticsearch.common.json.esjson.core.bean.JavaBeanSetterInfo;
import me.yufan.elasticsearch.common.json.esjson.core.bean.MethodInfo;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JavaBeanDeserializer implements Deserializer {

    private static final ConcurrentMap<Class<?>, JavaBeanSetterInfo> cache = new ConcurrentHashMap<>();

    private Class<?> clazz;

    public JavaBeanDeserializer(Class<?> clazz) {
        this.clazz = clazz;
    }

    @SuppressWarnings({"unchecked", "rawtypes", "ConstantConditions"})
    public <T> T deserialize(Object object, Type type) {

        if (object.getClass() == type) {
            return (T) object;
        }

        if (object instanceof JSONObject) {
            JSONObject json = (JSONObject) object;

            JavaBeanSetterInfo setterInfo = getSetterInfo();
            try {
                Object targetObject = setterInfo.getConstructor().newInstance();

                Collection<MethodInfo> methodInfos = setterInfo.getMethodSetterInfos();
                for (MethodInfo methodInfo : methodInfos) {
                    Class<?> parameterType = methodInfo.getMethod().getParameterTypes()[0];
                    Deserializer deserializer = JSONParser.getDeserializer(parameterType);

                    Object value = json.get(methodInfo.getFieldName());
                    Object param = null;
                    if (value != null) {
                        param = deserializer.deserialize(value, parameterType);
                    }
                    methodInfo.getMethod().invoke(targetObject, param);
                }

                Collection<FieldSetterInfo> fieldSetterInfos = setterInfo.getFieldSetterInfos();
                for (FieldSetterInfo fieldSetterInfo : fieldSetterInfos) {

                    Class<?> fieldType = fieldSetterInfo.getField().getType();
                    Deserializer deserializer = JSONParser.getDeserializer(fieldType);

                    Object value = json.get(fieldSetterInfo.getFieldName());
                    Object param = null;
                    if (value != null) {
                        deserializer.deserialize(value, fieldType);
                    }
                    fieldSetterInfo.getField().set(targetObject, param);
                }

                return (T) targetObject;

            } catch (Exception e) {
                throw new JSONException(clazz.getName() + " JavaBean inject error:" + e.getMessage(), e);
            }
        }

        throw new JSONException("illegal object class:" + object.getClass() + " type:" + type);
    }

    private JavaBeanSetterInfo getSetterInfo() {

        JavaBeanSetterInfo setterInfo = cache.get(clazz);
        if (setterInfo != null) {
            return setterInfo;
        }
        setterInfo = new JavaBeanSetterInfo(clazz);

        cache.putIfAbsent(clazz, setterInfo);

        return setterInfo;
    }
}
