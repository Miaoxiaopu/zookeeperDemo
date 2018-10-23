package com.dlw.demo;

import com.dlw.demo.zookeeper.ZookeeperClient;
import com.dlw.demo.zookeeper.ZookeeperClientListener;
import com.dlw.demo.zookeeper.ZookeeperConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @Description 服务启动先执行
 * @Author diaoliwei
 * @Date 2018/10/23 18:43
 */
@Component
public class ZkDemoApplicationRunner implements ApplicationRunner {

    private final static Logger log = LoggerFactory.getLogger(ZkDemoApplicationRunner.class);

    @Autowired
    private ZookeeperClientListener zkClientListener;

    @Autowired
    private ZookeeperConfig zookeeperConfig;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        log.info("====================>>>>>>>>启动执行zk>>>>>>>>==================");
        log.error("===>>>>>>>>zookeeper: addr:{}, sleepTime:{}, max:{}, connectionTime:{}=====", zookeeperConfig.getAddr(), zookeeperConfig.getSleepTime(), zookeeperConfig.getMax(), zookeeperConfig.getConnectionTime());
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(zookeeperConfig.getAddr())
                .retryPolicy(new ExponentialBackoffRetry(zookeeperConfig.getSleepTime(), zookeeperConfig.getMax()))
                .connectionTimeoutMs(zookeeperConfig.getConnectionTime()).build();
        LeaderLatch leaderLatch = new LeaderLatch(client, "/leaderLatch", "client1", LeaderLatch.CloseMode.NOTIFY_LEADER);
        if (zkClientListener == null) {
            log.error("==================>>>>>>>>>>>>>>>>zkClientListener is null=====>>>>>>>>>>>");
        }
        leaderLatch.addListener(zkClientListener);
        ZookeeperClient zkClient = new ZookeeperClient(leaderLatch, client);
        try {
            zkClient.startZKClient();
        } catch (Exception e) {
            log.error("======>>>>>>zk客户端连接失败<<<<<=====error:{}===", e);
            return;
        }
        boolean isZkCuratorStarted = client.isStarted();
        if (!isZkCuratorStarted) {
            log.error("zk客户端已关闭");
            return;
        }
        log.info("======>>>>>zk客户端连接成功<<<<<<=======");
    }
}
