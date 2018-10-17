import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StopWatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author hanlipeng
 * @date 2018/10/16
 */
public class MailTest {
    @Before
    public void setUp() {
        MailUtils.registeNullFuntion(() -> "null");
        MailUtils.registeDataDealFuntion(Long.class, o -> "Long " + o);
        MailUtils.registeDataDealFuntion(Integer.class, o -> "Integer " + o);
    }

    /**
     * 正常的通过测试结果会打印在控制台,多次运行只有第一次会很慢.(应该是数据初始化的问题,猜测,没找到实际原因).
     */
    @Test
    public void BeanUtilNormalTest() {
        TestBean testBean = new TestBean();
        testBean.setUserId("testId1");
        testBean.setUserName("testName1");
        LinkedList<UserBean> users = new LinkedList<>();
        UserBean userBean = new UserBean();
        userBean.setUserName("userName1");
        users.add(userBean);
        UserBean userBean1 = new UserBean();
        userBean1.setUserName("userName2");
        users.add(userBean1);
        UserBean userBean2 = new UserBean();
        userBean2.setUserName("userName3");
        users.add(userBean2);
        testBean.setUsers(users);
        testBean.setUser(userBean1);
        String template = "testId:#{userId},testName:#{userName},\n#{users:users.userName:#{userName}\n#{user:user.userName#{userName}}}";
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("1");
        String s = MailUtils.buildMail(template, testBean);
        stopWatch.stop();
        System.out.println(stopWatch.prettyPrint());
        System.out.println(s);
    }

    /**
     * list基础数据测试,会直接吧list中的结果输出出来,不建议list中存放基础类型
     */
    @Test
    public void listTest() {
        String template = "list:#{value}"; //结果是直接拼接,不建议这样使用
        List<String> list = Arrays.asList("a,", "a,", "a,", "a,", "a");
        String result = MailUtils.buildMail(template, list); //a,a,a,a,a
        System.out.println(result);
    }

    @Test
    public void mapTest() {
        HashMap<String, Object> param = new HashMap<>();
        param.put("key1", "key1所对应的值");
        param.put("key2", "key2所对应的值");
        HashMap<String, Object> listParam1 = new HashMap<>();
        listParam1.put("key", "listKey1Value");
        HashMap<String, Object> listParam2 = new HashMap<>();
        listParam2.put("key", "listKey2Value");
        param.put("list1", Arrays.asList(listParam1, listParam2));
        String template = "key1:#{key1}\nkey2:#{key2}\nlist:\n#{list1:listKey:#{key}\n}";
        System.out.println(MailUtils.buildMail(template, param));
    }

    /**
     * 对于long类型已经在setup方法中注册到方法集中;
     */
    @Test
    public void SpecialTest() {
        HashMap<String, Object> param = new HashMap<>();
        param.put("long", 3L);
        param.put("integer", 3);
        System.out.println(MailUtils.buildMail("Long:#{long}\nInteger:#{integer}", param));
    }
}
