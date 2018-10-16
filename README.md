# stringformater
本意是想找个邮件的模板工具.找了很多没找到.就准备自己写一个;
## 规则
字符串中以'#{xxx}'的格式为基础做字符串替换;  
```java
String template = "testId:#{userId},testName:#{userName},\n#{users:users.userName:#{userName}\n#{user:user.userName#{userName}}}";

String result = MailUtils.buildMailWithObjectParam(template, bean);
```
bean可以为Map,List或者是bean(无参构造方法,get set 方法与field对应);  
如果传入的是数据类型.则会直接返回传入的参数;
``` java
String bean = "test"
String result = MailUtils.buildMailWithObjectParam(template, bean); \\ result="test"
```
可以传嵌套参数,会对bean,map,collection类型进行不同的处理,写法为  
#{bean:xxxxx#{field}xxxx}  
#{list:xxxxx#{bean:xxxx#{field}}xxxx} \\ 每一条list会构建为

目前只对String和Number类会转化为字符串,如有需要可以在MailUtils 112行做拦截修改,在137行做不同类型的数据转化
``` java
private static BiFunction paramGetters(Object param, HashMap<Object, Map<String, Object>> bean) {
        BiFunction paramGetter;
        if (param instanceof Map) {
            paramGetter = (o, k) -> ((Map) o).get(k);
        } else if (param instanceof String || param instanceof Number || param == null) {  \\在这个地方添加放行类型
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
    public static String paramToString(Object param) {
        return Optional.ofNullable(param).map(Object::toString).orElse(""); \\ 在这个地方加转化方法
    }
   ```
