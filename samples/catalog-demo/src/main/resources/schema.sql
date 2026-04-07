-- 目录节点表
CREATE TABLE IF NOT EXISTS catalog_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '节点ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父节点ID，0表示根节点',
    name VARCHAR(100) NOT NULL COMMENT '节点名称',
    code VARCHAR(50) COMMENT '节点编码',
    path VARCHAR(500) COMMENT '从根到当前节点的路径',
    level INT COMMENT '节点层级，从1开始',
    sort INT COMMENT '同级节点排序号，从1开始',
    INDEX idx_parent (parent_id),
    INDEX idx_path (path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目录节点表';

-- 业务绑定关系表
CREATE TABLE IF NOT EXISTS catalog_rel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关系ID',
    node_id BIGINT NOT NULL COMMENT '目录节点ID',
    biz_id VARCHAR(100) NOT NULL COMMENT '业务对象ID',
    biz_type VARCHAR(50) NOT NULL COMMENT '业务类型',
    UNIQUE KEY uk_biz (biz_id, biz_type),
    INDEX idx_node_type (node_id, biz_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务绑定关系表';
