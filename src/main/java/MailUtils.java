import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hanlipeng
 * @date 2018/10/16
 */
public class MailUtils {
    private static Pattern paramPattern = Pattern.compile("(#\\{)([^}:]*)(:?)([\\s\\S]*)(}$)");
    private static Map<Class, Function> dataTypeFunction;
    private static Set<Class> type;
    private static Supplier<String> defaultNullFunction;
    private static Supplier<String> nullFunction;

    static {
        dataTypeFunction = new HashMap<>();
        dataTypeFunction.put(String.class, Object::toString);
        dataTypeFunction.put(Number.class, String::valueOf);
        type = dataTypeFunction.keySet();
        defaultNullFunction = () -> "";
    }

    public static String buildMail(String mailTemplate, Object param) {
        HashMap<Object, Map<String, Object>> bean = new HashMap<>(8);
        if (param instanceof Collection) {
            StringBuilder mail = new StringBuilder();
            for (Object p : (Collection) param) {
                mail.append(buildMail(mailTemplate, p));
            }
            return mail.toString();
        }
        BiFunction paramGetter = paramGetters(param, bean);
        if (paramGetter != null) {
            List<MailReg> mailRegs = findReg(mailTemplate);
            StringBuilder builder = new StringBuilder(mailTemplate);
            for (MailReg mailReg : mailRegs) {
                int start = builder.indexOf(mailReg.reg);
                builder.replace(start, start + mailReg.regLength, buildMail(mailReg.template, paramGetter.apply(param, mailReg.key)));
            }
            return builder.toString();
        } else {
            return paramToString(param);
        }
    }

    private static List<MailReg> findReg(String mailTemplate) {
        LinkedList<MailReg> result = new LinkedList<>();
        LinkedList<Character> paramStack = new LinkedList<>();
        char[] chars = mailTemplate.toCharArray();
        int index = 0;
        boolean paramFlag = false;
        for (int i = 0; i < chars.length; i++) {
            char aChar = chars[i];
            if (aChar == '#' && (i + 1 < chars.length) && (chars[i + 1] == '{') && !paramFlag) {
                paramFlag = true;
            }
            if (aChar == '{' && paramFlag) {
                index++;
            }
            if (paramFlag) {
                paramStack.add(aChar);
            }
            if (aChar == '}' && paramFlag) {
                index--;
                if (index == 0) {
                    MailReg mailReg = new MailReg();
                    StringBuilder builder = new StringBuilder();
                    paramStack.forEach(builder::append);
                    mailReg.reg = builder.toString();
                    mailReg.regLength = paramStack.size();
                    Matcher matcher = paramPattern.matcher(mailReg.reg);
                    if (matcher.find()) {
                        mailReg.key = matcher.group(2);
                        mailReg.template = matcher.group(4);
                    }
                    Logger debug = LoggerFactory.getLogger(MailUtils.class);
                    debug.debug(String.format("getreg: reg:%s regLength:%s  key:%s template%s", mailReg.reg, mailReg.regLength, mailReg.key, mailReg.template));

                    paramStack.clear();
                    paramFlag = false;
                    result.add(mailReg);
                }
            }
        }
        if (paramFlag) {
            StringBuilder builder = new StringBuilder();
            paramStack.forEach(builder::append);
            throw new RuntimeException(String.format("数据格式有误,\n在第%s个'{'\n%s\n中的第%s个'{'无闭合", result.size() + 1, builder.toString(), index));
        }
        return result;
    }

    static class MailReg {
        String reg;
        int regLength;
        String key;
        String template;
    }

    private static BiFunction paramGetters(Object param, HashMap<Object, Map<String, Object>> bean) {
        BiFunction paramGetter;
        if (param instanceof Map) {
            paramGetter = (o, k) -> ((Map) o).get(k);
        } else if (type.stream().anyMatch(c -> c.isInstance(param))) {
            paramGetter = null;
        } else {
            paramGetter = (o, k) -> {
                Map<String, Object> map = bean.get(o);
                if (map == null) {
                    map = new HashMap<>(8);
                    try {
                        BeanInfo beanInfo = Introspector.getBeanInfo(o.getClass());
                        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                        for (PropertyDescriptor property : propertyDescriptors) {
                            String key = property.getName();
                            Method getters = property.getReadMethod();
                            map.put(key, getters.invoke(o));
                        }
                    } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
                return map.get(k);
            };
        }
        return paramGetter;
    }

    private static String paramToString(Object param) {
        return Optional.ofNullable(param).map(o -> {
             Function function = dataTypeFunction.get(param.getClass());
             if (function == null) {
                 Class type = MailUtils.type.stream().filter(t -> t.isInstance(param)).findAny().orElseThrow(() -> new RuntimeException("未找到处理方式"));
                 function = dataTypeFunction.get(type);
             }
             return function.apply(param).toString();
         }).orElse(Optional.ofNullable(nullFunction).map(Supplier::get).orElse(defaultNullFunction.get()));
    }

    public static void registeNullFuntion(Supplier<String> nullFunction) {
        MailUtils.nullFunction = nullFunction;
    }

    public static <T> void registeDataDealFuntion(Class<T> clazz, Function<T, String> function) {
        dataTypeFunction.put(clazz, function);
    }
}
