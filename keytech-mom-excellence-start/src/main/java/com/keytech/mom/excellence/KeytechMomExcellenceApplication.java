package com.keytech.mom.excellence;

import com.xkcoding.justauth.autoconfigure.JustAuthAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * MOM-Excellence 后端启动类。
 * 扫描 org.jeecg（jeecg 原版 + ems-service 库）+ com.keytech（keytech 公共基座 + excellence 业务）。
 * ImportAutoConfiguration(JustAuthAutoConfiguration)：jeecg 原版 ThirdLoginController 依赖 AuthRequestFactory，必须显式导入。
 */
@Slf4j
@SpringBootApplication
@ComponentScan(
        basePackages = {"org.jeecg", "com.keytech"},
        excludeFilters = {
                // jeecg AI 流程编排(airag)与 system 强耦合:SysBaseApiImpl 强注入 IAiragFlowService,不可整体排除 bean。
                // 仅排除启动监听器混淆包 org.jeecg.modules.config.a(含 AiragScheduledTaskInitListener),
                // 使其 ready 事件不再回调 initCronTasksFromDB(该方法在 PG 下查询报 varchar=int 类型错)。airag 主体 bean 照常注册。
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.jeecg\\.modules\\.config\\.a\\..*")
        }
)
@MapperScan("com.keytech.**.mapper")
@ImportAutoConfiguration(JustAuthAutoConfiguration.class)
public class KeytechMomExcellenceApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(KeytechMomExcellenceApplication.class);
    }

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(KeytechMomExcellenceApplication.class, args);
        Environment env = application.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String path = oConvertUtils.getString(env.getProperty("server.servlet.context-path"));
        String version = env.getProperty("project-version.version");
        log.info("\n----------------------------------------------------------\n\t" +
                "Application Keytech-MOM-Excellence is running! Access URLs:\n\t" +
                "Local: \t\thttp://localhost:" + port + path + "/\n\t" +
                "External: \thttp://" + ip + ":" + port + path + "/\n\t" +
                "Swagger文档: \thttp://" + ip + ":" + port + path + "/doc.html\n\t" +
                "平台版本: \tV" + version + "\n" +
                "----------------------------------------------------------");
    }
}
