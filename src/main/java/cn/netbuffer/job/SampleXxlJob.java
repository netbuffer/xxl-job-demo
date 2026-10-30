package cn.netbuffer.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
public class SampleXxlJob {

    private static final Logger logger = LoggerFactory.getLogger(SampleXxlJob.class);

    /**
     * 简单任务示例
     * 控制台配置 JobHandler: helloworld
     */
    @XxlJob("helloworld")
    public void helloworld() throws Exception {
        String hostIp = InetAddress.getLocalHost().getHostAddress();
        String hostName = InetAddress.getLocalHost().getHostName();
        
        logger.info("XXL-JOB, Hello World. 执行器IP: {}, 主机名: {}", hostIp, hostName);
        
        XxlJobHelper.log("XXL-JOB, Hello World. 执行器IP: {}, 主机名: {}", hostIp, hostName);
        
        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("beat at: " + i);
            Thread.sleep(2000);
        }
        
        logger.info("XXL-JOB, Hello World, finished.");
    }
}
