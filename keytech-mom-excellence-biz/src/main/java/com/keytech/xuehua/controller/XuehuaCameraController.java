package com.keytech.xuehua.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.keytech.base.controller.BaseController;
import com.keytech.xuehua.entity.XuehuaCamera;
import com.keytech.xuehua.service.IXuehuaCameraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * 视频点位 Controller（监控管理）：库区/货架/库位 ↔ 海康设备通道绑定，
 * ISAPI 通道发现 + RTSP 预览/回放地址生成（无 iSecure Center，设备直连）
 */
@Tag(name = "视频点位")
@RestController
@RequestMapping("/xuehua/camera")
public class XuehuaCameraController extends BaseController<IXuehuaCameraService, XuehuaCamera> {

    @Autowired
    private IXuehuaCameraService xuehuaCameraService;

    /** 分页列表（QueryGenerator 支持 cameraCode/cameraName/zoneCode/status 等筛选） */
    @Operation(summary = "视频点位-分页列表")
    @GetMapping(value = "/listData")
    public Result<IPage<XuehuaCamera>> listData(XuehuaCamera camera,
                                                @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                HttpServletRequest req) {
        QueryWrapper<XuehuaCamera> qw = QueryGenerator.initQueryWrapper(camera, req.getParameterMap());
        qw.orderByAsc("camera_code");
        return Result.OK(xuehuaCameraService.page(new Page<>(pageNo, pageSize), qw));
    }

    /** 从 AI 服务器/NVR 拉取摄像机通道（ISAPI，未接入时降级提示） */
    @Operation(summary = "视频点位-设备通道发现")
    @GetMapping(value = "/discoverChannels")
    public Result<Map<String, Object>> discoverChannels() {
        Map<String, Object> r = xuehuaCameraService.discoverChannels();
        return Boolean.TRUE.equals(r.get("success")) ? Result.OK(r) : Result.error(String.valueOf(r.get("message")));
    }

    /** RTSP 预览/回放地址生成（实时主/子码流；带时间段时附回放地址） */
    @Operation(summary = "视频点位-RTSP地址生成")
    @GetMapping(value = "/previewUrl")
    public Result<Map<String, Object>> previewUrl(@RequestParam(name = "id") Integer id,
                                                  @RequestParam(name = "startTime", required = false) String startTime,
                                                  @RequestParam(name = "endTime", required = false) String endTime) {
        Map<String, Object> r = xuehuaCameraService.previewUrls(id, startTime, endTime);
        return Boolean.TRUE.equals(r.get("success")) ? Result.OK(r) : Result.error(String.valueOf(r.get("message")));
    }

    /** Excel 导出（@Excel 注解字段） */
    @Operation(summary = "视频点位-导出Excel")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, XuehuaCamera camera) {
        return super.exportXls(request, camera, XuehuaCamera.class, "视频点位");
    }
}
