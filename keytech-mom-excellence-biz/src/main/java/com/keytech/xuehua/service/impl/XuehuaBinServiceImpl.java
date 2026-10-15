package com.keytech.xuehua.service.impl;

import cn.hutool.core.util.StrUtil;
import com.keytech.base.service.impl.BaseServiceImpl;
import com.keytech.xuehua.entity.XuehuaBin;
import com.keytech.xuehua.mapper.XuehuaBinMapper;
import com.keytech.xuehua.service.IXuehuaBinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 库位 服务实现类
 */
@Slf4j
@Service
public class XuehuaBinServiceImpl extends BaseServiceImpl<XuehuaBinMapper, XuehuaBin> implements IXuehuaBinService {

    /** 字典无"默认安全库存"配置时的最后兜底（与 db/xuehua_dicts.sql 初始值一致） */
    private static final int FALLBACK_SAFE_STOCK = 10;

    @Override
    public List<Map<String, Object>> mapRows() {
        List<Map<String, Object>> rows = baseMapper.mapRows();
        int fallback = fallbackSafeStock();
        for (Map<String, Object> r : rows) {
            int safe = r.get("safeStock") != null ? Integer.parseInt(String.valueOf(r.get("safeStock"))) : fallback;
            r.put("safeStock", safe);
            r.put("lowStock", isLowStock(r.get("status"), r.get("partCode"), r.get("qty"), safe));
        }
        return rows;
    }

    /** 工作台低位库存预警同款口径：在库 + 有备件归属 + qty>0 且 qty<=安全库存（备件未设置用字典默认） */
    public static boolean isLowStock(Object status, Object partCode, Object qty, int safe) {
        if (!Integer.valueOf(1).equals(parseInt(status)) || StrUtil.isBlank(String.valueOf(partCode == null ? "" : partCode))) {
            return false;
        }
        Integer q = parseInt(qty);
        return q != null && q > 0 && q <= safe;
    }

    /** 安全库存兜底值：字典 xuehua_safe_stock"默认安全库存"，无配置用常量 */
    @Override
    public int fallbackSafeStock() {
        try {
            String v = baseMapper.dictItemValue("xuehua_safe_stock", "默认安全库存");
            if (StrUtil.isNotBlank(v)) {
                return Integer.parseInt(v.trim());
            }
        } catch (Exception e) {
            log.warn("读取安全库存字典配置失败，使用兜底值 {}: {}", FALLBACK_SAFE_STOCK, e.getMessage());
        }
        return FALLBACK_SAFE_STOCK;
    }

    private static Integer parseInt(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
