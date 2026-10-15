package com.keytech.xuehua.service.impl;

import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaPart;
import com.keytech.xuehua.mapper.XuehuaPartMapper;
import com.keytech.xuehua.service.IXuehuaPartService;
import org.springframework.stereotype.Service;

/**
 * @Description: 备件主数据 服务实现类
 */
@Service
public class XuehuaPartServiceImpl extends BaseServiceImpl<XuehuaPartMapper, XuehuaPart> implements IXuehuaPartService {

    /**
     * EMIS 主数据同步 upsert：同 partCode 重复推送=更新（名称/型号/类别/单位等主数据字段），
     * 本地字段 safe_stock/default_zone 不被 EMIS 推送冲掉（安全库存是仓库侧口径）。
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public XuehuaPart receiveFromEmis(XuehuaPart body, String externalNo) {
        body.setSource("EMIS");
        body.setExternalNo(externalNo);
        XuehuaPart exist = this.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<XuehuaPart>()
                .eq("part_code", body.getPartCode()).last("limit 1"));
        if (exist != null) {
            exist.setPartName(body.getPartName())
                    .setModel(body.getModel())
                    .setCategory(body.getCategory())
                    .setUnit(body.getUnit())
                    .setExternalNo(externalNo)
                    .setSource("EMIS")
                    .setUpdateTime(new java.util.Date());
            this.updateById(exist);
            return exist;
        }
        body.setStatus(1);
        body.setCreateTime(new java.util.Date());
        this.save(body);
        return body;
    }
}
