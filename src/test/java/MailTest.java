import org.junit.Test;
import org.springframework.util.StopWatch;

import java.util.LinkedList;

/**
 * @author hanlipeng
 * @date 2018/10/16
 */
public class MailTest {
    @Test
    public void BeanUtilTest() {
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
        String s = MailUtils.buildMailWithObjectParam(template, testBean);
        stopWatch.stop();
        stopWatch.start("2");
        s = MailUtils.buildMailWithObjectParam(template, testBean);
        stopWatch.stop();
        stopWatch.start("3");
        s = MailUtils.buildMailWithObjectParam(template, testBean);
        stopWatch.stop();
        stopWatch.start("4");
        s = MailUtils.buildMailWithObjectParam(template, testBean);
        stopWatch.stop();
        System.out.println(stopWatch.prettyPrint());
        System.out.println(s);
    }
}
