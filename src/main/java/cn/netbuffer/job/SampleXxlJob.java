package cn.netbuffer.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Slf4j
@Component
public class SampleXxlJob {

    /**
     * 简单任务示例
     * 控制台配置 JobHandler: helloworld
     */
    @XxlJob("helloworld")
    public void helloworld() throws Exception {
        String hostIp = InetAddress.getLocalHost().getHostAddress();
        String hostName = InetAddress.getLocalHost().getHostName();
        
        log.info("XXL-JOB, Hello World. 执行器IP: {}, 主机名: {}", hostIp, hostName);
        
        XxlJobHelper.log("XXL-JOB, Hello World. 执行器IP: {}, 主机名: {}", hostIp, hostName);
        
        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("beat at: " + i);
            Thread.sleep(2000);
        }
        
        log.info("XXL-JOB, Hello World, finished.");
    }
}
