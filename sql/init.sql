CREATE TABLE IF NOT EXISTS `user`
(
    uid        BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'uid' PRIMARY KEY,
    username   VARCHAR(16)                        NOT NULL COMMENT '用户名',
    password   VARCHAR(256)                       NOT NULL COMMENT '密码',
    email      VARCHAR(32)                        NULL COMMENT '邮箱',
    phone      VARCHAR(16)                        NULL COMMENT '手机号',
    gender     TINYINT(1)                         NULL COMMENT '性别(0:男,1:女)',
    avatar     VARCHAR(256)                       NULL COMMENT '头像地址',
    status     INTEGER  DEFAULT 0 DEFAULT 0       NOT NULL COMMENT '状态',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);