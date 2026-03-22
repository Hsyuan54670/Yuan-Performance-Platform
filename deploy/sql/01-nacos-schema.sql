SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

USE nacos_config;

/******************************************/
/*   TableName = config_info              */
/******************************************/
CREATE TABLE IF NOT EXISTS `config_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) DEFAULT NULL COMMENT 'group_id',
    `content` longtext NOT NULL COMMENT 'content',
    `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create_time',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'modified_time',
    `src_user` text COMMENT 'source user',
    `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
    `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `c_desc` varchar(256) DEFAULT NULL COMMENT 'configuration description',
    `c_use` varchar(64) DEFAULT NULL COMMENT 'configuration usage',
    `effect` varchar(64) DEFAULT NULL COMMENT 'configuration effect',
    `type` varchar(64) DEFAULT NULL COMMENT 'configuration type',
    `c_schema` text COMMENT 'configuration schema',
    `encrypted_data_key` varchar(1024) NOT NULL DEFAULT '' COMMENT 'encrypted data key',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`, `group_id`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info';

/******************************************/
/*   TableName = config_info_gray         */
/******************************************/
CREATE TABLE IF NOT EXISTS `config_info_gray` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) NOT NULL COMMENT 'group_id',
    `content` longtext NOT NULL COMMENT 'content',
    `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
    `src_user` text COMMENT 'src_user',
    `src_ip` varchar(100) DEFAULT NULL COMMENT 'src_ip',
    `gmt_create` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmt_create',
    `gmt_modified` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'gmt_modified',
    `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `gray_name` varchar(128) NOT NULL COMMENT 'gray_name',
    `gray_rule` text NOT NULL COMMENT 'gray_rule',
    `encrypted_data_key` varchar(256) NOT NULL DEFAULT '' COMMENT 'encrypted_data_key',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfogray_datagrouptenantgray` (`data_id`, `group_id`, `tenant_id`, `gray_name`),
    KEY `idx_dataid_gmt_modified` (`data_id`, `gmt_modified`),
    KEY `idx_gmt_modified` (`gmt_modified`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='config_info_gray';

/******************************************/
/*   TableName = config_tags_relation     */
/******************************************/
CREATE TABLE IF NOT EXISTS `config_tags_relation` (
    `id` bigint(20) NOT NULL COMMENT 'id',
    `tag_name` varchar(128) NOT NULL COMMENT 'tag_name',
    `tag_type` varchar(64) DEFAULT NULL COMMENT 'tag_type',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) NOT NULL COMMENT 'group_id',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `nid` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'nid',
    PRIMARY KEY (`nid`),
    UNIQUE KEY `uk_configtagrelation_configidtag` (`id`, `tag_name`, `tag_type`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_tag_relation';

/******************************************/
/*   TableName = group_capacity           */
/******************************************/
CREATE TABLE IF NOT EXISTS `group_capacity` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `group_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'group id',
    `quota` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'quota',
    `usage` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'usage',
    `max_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'max size',
    `max_aggr_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'max aggr count',
    `max_aggr_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'max aggr size',
    `max_history_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'max history count',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'modified time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='group capacity';

/******************************************/
/*   TableName = his_config_info          */
/******************************************/
CREATE TABLE IF NOT EXISTS `his_config_info` (
    `id` bigint(20) unsigned NOT NULL COMMENT 'id',
    `nid` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'nid',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) NOT NULL COMMENT 'group_id',
    `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
    `content` longtext NOT NULL COMMENT 'content',
    `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'modified time',
    `src_user` text COMMENT 'source user',
    `src_ip` varchar(50) DEFAULT NULL COMMENT 'source ip',
    `op_type` char(10) DEFAULT NULL COMMENT 'operation type',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `encrypted_data_key` varchar(1024) NOT NULL DEFAULT '' COMMENT 'encrypted data key',
    `publish_type` varchar(50) DEFAULT 'formal' COMMENT 'publish type',
    `gray_name` varchar(50) DEFAULT NULL COMMENT 'gray name',
    `ext_info` longtext DEFAULT NULL COMMENT 'ext info',
    PRIMARY KEY (`nid`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_did` (`data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='his_config_info';

/******************************************/
/*   TableName = tenant_capacity          */
/******************************************/
CREATE TABLE IF NOT EXISTS `tenant_capacity` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `tenant_id` varchar(128) NOT NULL DEFAULT '' COMMENT 'tenant id',
    `quota` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'quota',
    `usage` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'usage',
    `max_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'max size',
    `max_aggr_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'max aggr count',
    `max_aggr_size` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'max aggr size',
    `max_history_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT 'max history count',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'modified time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='tenant capacity';

/******************************************/
/*   TableName = tenant_info              */
/******************************************/
CREATE TABLE IF NOT EXISTS `tenant_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `kp` varchar(128) NOT NULL COMMENT 'kp',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `tenant_name` varchar(128) DEFAULT '' COMMENT 'tenant_name',
    `tenant_desc` varchar(256) DEFAULT NULL COMMENT 'tenant_desc',
    `create_source` varchar(32) DEFAULT NULL COMMENT 'create_source',
    `gmt_create` bigint(20) NOT NULL COMMENT 'create time',
    `gmt_modified` bigint(20) NOT NULL COMMENT 'modified time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_info_kptenantid` (`kp`, `tenant_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='tenant_info';

/******************************************/
/*   TableName = users                    */
/******************************************/
CREATE TABLE IF NOT EXISTS `users` (
    `username` varchar(50) NOT NULL COMMENT 'username',
    `password` varchar(500) NOT NULL COMMENT 'password',
    `enabled` boolean NOT NULL COMMENT 'enabled',
    PRIMARY KEY (`username`)
);

/******************************************/
/*   TableName = roles                    */
/******************************************/
CREATE TABLE IF NOT EXISTS `roles` (
    `username` varchar(50) NOT NULL COMMENT 'username',
    `role` varchar(50) NOT NULL COMMENT 'role',
    UNIQUE KEY `idx_user_role` (`username`, `role`) USING BTREE
);

/******************************************/
/*   TableName = permissions              */
/******************************************/
CREATE TABLE IF NOT EXISTS `permissions` (
    `role` varchar(50) NOT NULL COMMENT 'role',
    `resource` varchar(128) NOT NULL COMMENT 'resource',
    `action` varchar(8) NOT NULL COMMENT 'action',
    UNIQUE KEY `uk_role_permission` (`role`, `resource`, `action`) USING BTREE
);

/******************************************/
/*   TableName = config_info_beta         */
/******************************************/
CREATE TABLE IF NOT EXISTS `config_info_beta` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) NOT NULL COMMENT 'group_id',
    `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
    `content` longtext NOT NULL COMMENT 'content',
    `beta_ips` varchar(1024) DEFAULT NULL COMMENT 'beta_ips',
    `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create_time',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'modified_time',
    `src_user` text COMMENT 'src_user',
    `src_ip` varchar(50) DEFAULT NULL COMMENT 'src_ip',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `encrypted_data_key` varchar(1024) NOT NULL DEFAULT '' COMMENT 'encrypted data key',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfobeta_datagrouptenant` (`data_id`, `group_id`, `tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info_beta';

/******************************************/
/*   TableName = config_info_tag          */
/******************************************/
CREATE TABLE IF NOT EXISTS `config_info_tag` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `data_id` varchar(255) NOT NULL COMMENT 'data_id',
    `group_id` varchar(128) NOT NULL COMMENT 'group_id',
    `tenant_id` varchar(128) DEFAULT '' COMMENT 'tenant_id',
    `tag_id` varchar(128) NOT NULL COMMENT 'tag_id',
    `app_name` varchar(128) DEFAULT NULL COMMENT 'app_name',
    `content` longtext NOT NULL COMMENT 'content',
    `md5` varchar(32) DEFAULT NULL COMMENT 'md5',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create_time',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'modified_time',
    `src_user` text COMMENT 'src_user',
    `src_ip` varchar(50) DEFAULT NULL COMMENT 'src_ip',
    `encrypted_data_key` varchar(1024) NOT NULL DEFAULT '' COMMENT 'encrypted data key',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfotag_datagrouptenanttag` (`data_id`, `group_id`, `tenant_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='config_info_tag';

/******************************************/
/*   TableName = migrate_config           */
/******************************************/
CREATE TABLE IF NOT EXISTS `migrate_config` (
    `config_key` varchar(128) NOT NULL COMMENT 'config_key',
    `config_value` varchar(1024) NOT NULL COMMENT 'config_value',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create_time',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'modified_time',
    PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin COMMENT='migrate_config';

