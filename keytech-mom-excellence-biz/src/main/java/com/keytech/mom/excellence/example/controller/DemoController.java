package com.keytech.mom.excellence.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例 Controller —— 卓越管理(excellence-management)脚手架的最小业务样例。
 * <p>用途:验证 启动 / 路由 / Swagger 聚合 / 前端联调 链路是否畅通。
 * 新业务开发可参照此结构新增 controller(包路径在 com.keytech 下,会被启动类 ComponentScan 扫到)。</p>
 */
@Slf4j
@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/hello")
    public Object hello() {
        Map<String, Object> r = new HashMap<>();
        r.put("module", "excellence-management");
        r.put("msg", "hello from Keytech MOM Excellence scaffold");
        return r;
    }
}
