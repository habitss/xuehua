package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaPerson;
import com.keytech.xuehua.hik.HikClient;
import com.keytech.xuehua.mapper.XuehuaPersonMapper;
import com.keytech.xuehua.service.IXuehuaPersonService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

/**
 * 人员注册 服务实现
 *
 * <p>数据流向（2026-09-18 纠正后）：人员档案由<b>海康推送</b>进来（upsertFromHik，纯记录无外呼）；
 * authorizeEntry 是唯一的外呼动作——EMIS 推单校验通过后发"进仓授权"指令，
 * 放在落库事务之前由 controller 层调用（保持"事务内不做网络调用"纪律）。
 * 授权语义（用户拍板）：授权失败抛异常拒单；hik.enabled=false 降级跳过、单据照常接收。
 */
@Slf4j
@Service
public class XuehuaPersonServiceImpl extends BaseServiceImpl<XuehuaPersonMapper, XuehuaPerson> implements IXuehuaPersonService {

    @Resource
    private HikClient hikClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public XuehuaPerson upsertFromHik(Map<String, Object> body) {
        String empNo = str(body.get("empNo"));
        String name = str(body.get("name"));
        if (StrUtil.isBlank(empNo)) {
            throw new IllegalStateException("empNo(工号)不能为空");
        }
        if (StrUtil.isBlank(name)) {
            throw new IllegalStateException("name(姓名)不能为空");
        }
        XuehuaPerson person = this.getOne(new QueryWrapper<XuehuaPerson>()
                .eq("emp_no", empNo).last("limit 1"));
        boolean exists = person != null;
        if (!exists) {
            person = new XuehuaPerson().setEmpNo(empNo).setStatus(1).setHikAuthStatus(0).setCreateTime(new Date());
        }
        // 海康推送的字段全量覆盖（档案以海康为准）；本地作业许可开关(status)保留不动
        person.setName(name)
                .setDept(str(body.get("dept")))
                .setAuthMethod(str(body.get("authMethod")))
                .setFaceStatus(parseInt(body.get("faceStatus"), person.getFaceStatus()))
                .setCardNo(str(body.get("cardNo")))
                .setZones(str(body.get("zones")))
                .setHikPersonId(str(body.get("hikPersonId")))
                .setUpdateTime(new Date());
        if (exists) {
            this.updateById(person);
        } else {
            this.save(person);
        }
        log.info("[HIK] 人员注册信息记录: empNo={} name={} hikPersonId={} {}",
                empNo, name, person.getHikPersonId(), exists ? "更新" : "新增");
        return person;
    }

    @Override
    public boolean hikRemoteOpen(Integer doorNo) {
        return hikClient.remoteOpen(doorNo == null ? 1 : doorNo);
    }

    @Override
    public void authorizeEntry(XuehuaPerson person) {
        // 通道一（2026-10-15 主路径）：门禁机 ISAPI 直连（K1T34x 一体机，ISC 平台已移除）
        // ——employeeNo=工号幂等下发人员(+卡)，成功即本地认证可开门
        if (hikClient.isDoorEnabled()) {
            boolean ok = hikClient.authorizeEntryByDoor(person.getEmpNo(), person.getName(), person.getCardNo());
            person.setHikAuthStatus(ok ? 1 : 2);
            this.updateById(person);
            if (!ok) {
                // 严格模式（用户拍板）：授权失败拒单，宁可业务中断也不能让未授权人员进仓
                throw new IllegalStateException("海康门禁授权失败（人员 " + person.getEmpNo()
                        + "），单据拒收，请检查门禁机（地址/凭据/网络）");
            }
            return;
        }
        // 通道二（保留）：Artemis/iSecure Center 平台路径——若将来恢复 ISC 配置则启用
        if (hikClient.isEnabled()) {
            boolean ok = hikClient.authorizeEntry(person.getHikPersonId(), person.getEmpNo(),
                    person.getCardNo(), person.getZones());
            person.setHikAuthStatus(ok ? 1 : 2);
            this.updateById(person);
            if (!ok) {
                throw new IllegalStateException("海康门禁授权失败（人员 " + person.getEmpNo()
                        + "），单据拒收，请检查海康平台");
            }
            return;
        }
        // 降级模式：两条通道都未接入，跳过授权调用，保持"待授权"，单据照常接收
        log.info("[HIK] 门禁通道均未启用，跳过进仓授权: {}", person.getEmpNo());
    }

    @Override
    public XuehuaPerson resolveByOperator(String nameOrEmpNo) {
        if (StrUtil.isBlank(nameOrEmpNo)) {
            return null;
        }
        XuehuaPerson person = this.getOne(new QueryWrapper<XuehuaPerson>()
                .eq("emp_no", nameOrEmpNo.trim()).eq("status", 1).last("limit 1"));
        if (person == null) {
            // 工号没匹配到再按姓名（EMIS 可能只传姓名；重名取启用中 id 最小的一条）
            person = this.getOne(new QueryWrapper<XuehuaPerson>()
                    .eq("name", nameOrEmpNo.trim()).eq("status", 1).orderByAsc("id").last("limit 1"));
        }
        return person;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Integer id, int status) {
        XuehuaPerson person = this.getById(id);
        if (person == null) {
            throw new IllegalStateException("人员不存在");
        }
        person.setStatus(status);
        person.setUpdateTime(new Date());
        this.updateById(person);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /** faceStatus 解析失败/缺省时保留原值（新人员默认 0 未采集） */
    private static Integer parseInt(Object o, Integer fallback) {
        if (o == null) {
            return fallback == null ? 0 : fallback;
        }
        try {
            return Integer.valueOf(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return fallback == null ? 0 : fallback;
        }
    }
}
