package cn.netbuffer.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * XXL-JOB 高级调度任务示例
 * 包含：
 * 1. 分片广播与大数据批处理（Sharding Broadcast + Batch Processing）
 * 2. 任务生命周期钩子（init / destroy）
 * 3. 动态入参解析与校验（JobParam）
 * 4. 任务超时与优雅中断响应（Graceful Interruption）
 * 5. 结构化日志进度与执行结果上报（Progress & Result）
 */
@Slf4j
@Component
public class AdvancedJobSample {

    /**
     * 任务初始化回调方法
     * 适合在此处初始化连接池、加载预热缓存、校验环境资源等
     */
    public void init() {
        log.info(">>> [AdvancedJobSample] 任务初始化 (init callback) 触发...");
    }

    /**
     * 任务销毁回调方法
     * 适合在任务结束或执行器停止时释放连接、刷写本地缓存、安全清理资源
     */
    public void destroy() {
        log.info(">>> [AdvancedJobSample] 任务销毁 (destroy callback) 触发...");
    }

    /**
     * 案例一：分片广播与大数据批处理任务
     * 
     * 适用场景：海量数据处理（如千万级订单超时关闭、全量账单对账、批量消息推送）。
     * 控制台配置推荐：
     * - JobHandler: shardingBatchJobHandler
     * - 路由策略: 分片广播 (SHARDING_BROADCAST)
     * - 阻塞处理策略: 单机串行 / 丢弃后续调度
     * - 任务参数: 可传如 "batchSize=20,maxItems=100"
     */
    @XxlJob(value = "shardingBatchJobHandler", init = "init", destroy = "destroy")
    public void shardingBatchJobHandler() throws Exception {
        long startTime = System.currentTimeMillis();

        // 1. 获取当前节点的分片参数（核心）
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        // 2. 获取并解析动态任务参数
        String param = XxlJobHelper.getJobParam();
        int batchSize = 10;
        int totalItems = 50;

        if (param != null && !param.trim().isEmpty()) {
            XxlJobHelper.log("接收到控制台传入参数: {}", param);
            // 简单解析 key=value 传参
            for (String pair : param.split(",")) {
                String[] kv = pair.trim().split("=");
                if (kv.length == 2) {
                    if ("batchSize".equalsIgnoreCase(kv[0].trim())) {
                        batchSize = Integer.parseInt(kv[1].trim());
                    } else if ("maxItems".equalsIgnoreCase(kv[0].trim())) {
                        totalItems = Integer.parseInt(kv[1].trim());
                    }
                }
            }
        }

        XxlJobHelper.log("================= 开始执行分片任务 =================");
        XxlJobHelper.log("当前分片序号: [{}], 总分片数: [{}], 批次大小: [{}], 总数据量: [{}]",
                shardIndex, shardTotal, batchSize, totalItems);

        // 3. 模拟海量待处理数据源（真实业务中通常为从数据库通过分页或游标查询）
        List<MockItem> allItems = mockData(totalItems);

        int processedCount = 0;
        int successCount = 0;
        int currentBatch = 0;

        // 4. 分批迭代处理
        for (int i = 0; i < allItems.size(); i += batchSize) {
            currentBatch++;
            int toIndex = Math.min(i + batchSize, allItems.size());
            List<MockItem> batch = allItems.subList(i, toIndex);

            for (MockItem item : batch) {
                // 5. 优雅停机与中断检测（极关键：响应控制台「终止任务」动作）
                if (Thread.currentThread().isInterrupted()) {
                    XxlJobHelper.log("警告: 收到中断信号，任务被主动终止！已处理: {} 条，正在安全退出...", processedCount);
                    return;
                }

                // 6. 分片取模路由规则：只有符合当前分片规则的数据才由本节点处理
                // 生产环境通常直接在 SQL 中过滤：WHERE MOD(id, #{shardTotal}) = #{shardIndex}
                if (item.getId() % shardTotal == shardIndex) {
                    processSingleItem(item);
                    successCount++;
                }
                processedCount++;
            }

            // 7. 实时上报执行进度日志到调度中心控制台
            double progress = (double) processedCount / allItems.size() * 100;
            XxlJobHelper.log("分片进度: 批次 [{}] 处理完成 (进度: {}%), 本节点命中处理: {} 条",
                    currentBatch, String.format("%.1f", progress), successCount);

            // 模拟批次间耗时（捕获中断以防在休眠间隙被终止）
            try {
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                XxlJobHelper.log("警告: 批次间休眠时收到终止信号，已处理: {} 条，安全退出！", processedCount);
                return;
            }
        }

        long costTime = System.currentTimeMillis() - startTime;
        String resultMsg = String.format("分片[%d/%d]执行完成! 扫描数据: %d 条, 本节点处理: %d 条, 耗时: %d ms",
                shardIndex, shardTotal, processedCount, successCount, costTime);

        XxlJobHelper.log("================= 任务执行结束 =================");
        XxlJobHelper.log(resultMsg);

        // 8. 自定义设置任务执行结果（显示在 Admin 界面调度结果备注中）
        XxlJobHelper.handleSuccess(resultMsg);
    }

    /**
     * 案例二：动态多步骤业务流程调度（长耗时分步推进）
     * 
     * 适用场景：复杂 ETL 管道、每日多阶段汇总报表、定时多租户数据同步
     * 控制台配置推荐：
     * - JobHandler: stepWorkflowJobHandler
     * - 路由策略: 轮询 / 随机 / 忙碌转移 (BUSY_OVER)
     * - 超时时间: 设置为 300 秒，防止偶发卡死
     */
    @XxlJob("stepWorkflowJobHandler")
    public void stepWorkflowJobHandler() throws Exception {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log(">>> [步骤1/3] 任务启动，即将开始一个 60 秒的循环任务（请在控制台点击「终止任务」进行测试）...");

        // 模拟长耗时批处理循环（总计 60 秒）
        for (int i = 1; i <= 60; i++) {
            // 1. 显式检测中断标志位（应对纯 CPU/数据库计算间隙）
            if (Thread.currentThread().isInterrupted()) {
                XxlJobHelper.log(">>> [中断感知] 检测到终止信号（isInterrupted=true），已运行 {} 秒，安全退出！", i);
                return;
            }

            try {
                // 模拟每秒处理一批数据
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                // 2. 休眠中被终止：打印业务日志后直接 return 退出，无需恢复中断位，也无需 handleFail（框架会自动标记为终止失败）
                XxlJobHelper.log(">>> [中断感知] 休眠中被管理员叫停（已运行 {} 秒），安全退出！", i);
                return;
            }

            if (i % 5 == 0) {
                XxlJobHelper.log(">>> 任务进行中... 已平稳运行 {}/60 秒", i);
            }
        }

        XxlJobHelper.log(">>> [步骤3/3] 任务全流程自然执行完毕（未被提前终止）。");
        XxlJobHelper.handleSuccess("任务自然执行完成（60秒已满）");
    }

    private void processSingleItem(MockItem item) {
        // 模拟单条数据业务处理逻辑（如更新订单状态、发送通知等）
        log.debug("处理数据项: id={}, name={}", item.getId(), item.getName());
    }

    private List<MockItem> mockData(int count) {
        List<MockItem> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(new MockItem(i, "业务数据-" + i));
        }
        return list;
    }

    public static class MockItem {
        private final int id;
        private final String name;

        public MockItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}

