-- ============================================================
-- MDM 业务字典 菜单权限 SQL (PostgreSQL / 库: smartworx_excellence / schema: excellence)
-- 表: mes4_mdm_dict_categorize / mes4_mdm_dict_group / mes4_mdm_dict
-- 挂载: 系统管理(/isystem) -> 业务字典(/mdm/dict, 组件 mdm/dict/index)
-- 前端路由组件: src/views/mdm/dict/index.vue
-- ============================================================

-- 1) 幂等清理（便于重复执行）
DELETE FROM excellence.sys_role_permission WHERE permission_id LIKE 'mdm4dict%';
DELETE FROM excellence.sys_permission      WHERE id LIKE 'mdm4dict%';

-- 2) 主菜单（menu_type=1 页面菜单，挂到"系统管理"下）
INSERT INTO excellence.sys_permission
(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES
('mdm4dict001', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '业务字典', '/mdm/dict', 'mdm/dict/index', 1, NULL, NULL, 1, NULL, '0', 5, 0, 'ant-design:appstore-twotone', 0, 0, 0, 0, '业务字典(大类/子类/配置项)', 'admin', now(), NULL, NULL, 0, 0, NULL, 0);

-- 3) 按钮（menu_type=2）
INSERT INTO excellence.sys_permission
(id, parent_id, name, url, component, is_route, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf, keep_alive, hidden, hide_tab, description, create_by, create_time, update_by, update_time, del_flag, rule_flag, status, internal_or_external)
VALUES
('mdm4dict002', 'mdm4dict001', '大类新增',     NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict_categorize:add',           '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict003', 'mdm4dict001', '大类编辑',     NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict_categorize:edit',          '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict004', 'mdm4dict001', '大类删除',     NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict_categorize:deletionBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict005', 'mdm4dict001', '子类新增',     NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict_group:add',                '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict006', 'mdm4dict001', '子类编辑',     NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict_group:edit',               '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict007', 'mdm4dict001', '子类删除',     NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict_group:deleteData',         '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict008', 'mdm4dict001', '子类批量删除', NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict_group:deleteDataBatch',    '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict009', 'mdm4dict001', '子类导出',     NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict_group:exportXls',          '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict010', 'mdm4dict001', '子类导入',     NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict_group:importExcel',        '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict011', 'mdm4dict001', '配置项新增',   NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict:add',                     '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict012', 'mdm4dict001', '配置项编辑',   NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict:edit',                    '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0),
('mdm4dict013', 'mdm4dict001', '配置项删除',   NULL, NULL, 0, NULL, NULL, 2, 'mdm:mes4_mdm_dict:deleteData',              '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', now(), NULL, NULL, 0, 0, '1', 0);

-- 4) admin 角色授权 (role_code=admin, role_id=f6817f48af4fb3af11b9e8bf182f618b)
INSERT INTO excellence.sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT md5(md5(random()::text) || t.id), 'f6817f48af4fb3af11b9e8bf182f618b', t.id, NULL, now(), '127.0.0.1'
FROM (VALUES
  ('mdm4dict001'),('mdm4dict002'),('mdm4dict003'),('mdm4dict004'),('mdm4dict005'),
  ('mdm4dict006'),('mdm4dict007'),('mdm4dict008'),('mdm4dict009'),('mdm4dict010'),
  ('mdm4dict011'),('mdm4dict012'),('mdm4dict013')
) AS t(id);
