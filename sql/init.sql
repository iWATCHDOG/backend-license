CREATE TABLE IF NOT EXISTS `user`
(
    uid        BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'uid' PRIMARY KEY,
    username   VARCHAR(16)                        NOT NULL COMMENT '用户名',
    password   VARCHAR(256)                       NOT NULL COMMENT '密码',
    email      VARCHAR(32)                        NULL COMMENT '邮箱',
    phone      VARCHAR(16)                        NULL COMMENT '手机号',
    gender     INTEGER  DEFAULT 3                 NOT NULL COMMENT '性别(1:男,2:女,3:未知)',
    avatar     VARCHAR(256)                       NULL COMMENT '头像地址',
    status     INTEGER  DEFAULT 0                 NOT NULL COMMENT '状态',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

-- 定义管理员权限为*，即所有权限

CREATE TABLE IF NOT EXISTS `permission`
(
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid        BIGINT                             NOT NULL COMMENT 'uid',
    permission VARCHAR(32)                        NOT NULL COMMENT '权限',
    expiry     BIGINT   DEFAULT 0                 NOT NULL COMMENT '过期时间',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `oauth`
(
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid        BIGINT                             NOT NULL COMMENT 'uid',
    platform   INTEGER                            NOT NULL COMMENT '平台',
    openId     VARCHAR(64)                        NOT NULL COMMENT 'openId',
    token      VARCHAR(64)                        NOT NULL COMMENT 'token',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `log`
(
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid        BIGINT                             NULL COMMENT 'uid',
    requestId  VARCHAR(36)                        NOT NULL COMMENT 'requestId',
    ip         VARCHAR(32)                        NULL COMMENT 'ip',
    headers    TEXT                               NULL COMMENT 'headers',
    url        VARCHAR(256)                       NULL COMMENT 'url',
    method     VARCHAR(16)                        NULL COMMENT 'method',
    params     TEXT                               NULL COMMENT 'params',
    result     TEXT                               NULL COMMENT 'result',
    httpCode   INTEGER                            NULL COMMENT 'httpCode',
    cost       BIGINT                             NULL COMMENT 'cost',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `security_log`
(
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid        BIGINT                             NULL COMMENT 'uid',
    avatar     TEXT                               NULL COMMENT '头像信息',
    title      VARCHAR(64)                        NOT NULL COMMENT '标题',
    types      VARCHAR(36)                        NOT NULL COMMENT '类型',
    ip         VARCHAR(32)                        NULL COMMENT 'ip',
    info       TEXT                               NULL COMMENT 'info',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `orders`
(
    outTradeNo    VARCHAR(128)                       NOT NULL COMMENT '订单号' PRIMARY KEY,
    uid           BIGINT                             NOT NULL COMMENT 'uid',
    subject       VARCHAR(128)                       NOT NULL COMMENT '订单标题',
    tradeNo       VARCHAR(128)                       NULL COMMENT '系统交易号',
    totalAmount   BIGINT                             NOT NULL COMMENT '订单金额(单位：分)',
    receiptAmount BIGINT                             NULL COMMENT '实收金额(单位：分)',
    payPlatform   INTEGER  DEFAULT 0                 NOT NULL COMMENT '支付平台',
    tradeStatus   INTEGER  DEFAULT 0                 NOT NULL COMMENT '交易状态',
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available     BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `blacklist`
(
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    ip         VARCHAR(32)                        NOT NULL COMMENT 'ip',
    log        BIGINT                             NOT NULL COMMENT '日志id',
    reason     VARCHAR(256)                       NULL COMMENT '原因',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `photo`
(
    pid        BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'pid' PRIMARY KEY,
    md5        VARCHAR(32)                        NOT NULL COMMENT 'md5',
    ext        VARCHAR(16)                        NOT NULL COMMENT '扩展名',
    size       BIGINT                             NOT NULL COMMENT '大小',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `2fa`
(
    id          BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid         BIGINT                             NOT NULL COMMENT 'uid',
    secret      VARCHAR(512)                       NOT NULL COMMENT 'secret',
    forceEnable BOOL     DEFAULT FALSE             NOT NULL COMMENT '是否强制开启',
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available   BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `invite_code`
(
    id            BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid           BIGINT                             NOT NULL COMMENT '创建人',
    code          VARCHAR(16)                        NOT NULL COMMENT '邀请码',
    expiry        BIGINT   DEFAULT 0                 NOT NULL COMMENT '过期时间',
    defaultGroups TEXT                               NULL COMMENT '默认分组',
    times         INTEGER  DEFAULT 0                 NOT NULL COMMENT '次数',
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available     BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `invite_record`
(
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid        BIGINT                             NOT NULL COMMENT '使用者uid',
    code       VARCHAR(16)                        NOT NULL COMMENT '邀请码',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);