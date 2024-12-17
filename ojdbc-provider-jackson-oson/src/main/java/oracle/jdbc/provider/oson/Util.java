package oracle.jdbc.provider.oson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Util {
    public static final ArrayList<String> ignoreList = new ArrayList<>();
    static {
        ignoreList.add("java.lang");
        ignoreList.add("java.util");
        ignoreList.add("java.sql");

    }
    public static boolean isJavaSerializableType(Class<?> clazz) {
        String packageName = clazz.getPackage().getName();

        if (packageName.startsWith("java.lang")
        || packageName.startsWith("java.util")
        || packageName.startsWith("java.sql")
        || packageName.startsWith("java.time")
        || packageName.startsWith("java.math")
        || packageName.startsWith("java.security")
        || clazz.isArray()) {
            return true;
        }

        return false;
    }

    public static boolean implementsSerializable(List<JavaType> interfaces) {
        boolean result = false;
        for (JavaType javaType : interfaces) {
            if(Serializable.class.isAssignableFrom(javaType.getRawClass())){
                result = true;
            }
        }
        return result;
    }

    public static boolean isJavaWrapperSerializable(BeanPropertyWriter writer) {
        JavaType type = writer.getType();
        return isJavaWrapperSerializable(type);
    }

    public static boolean isJavaWrapperSerializable(JavaType type) {
        Class<?> rawType = type.getRawClass();
        return isJavaSerializableType(rawType);
    }
}
