
CREATE TABLE IF NOT EXISTS `index_volume_group` (
    `name`                  varchar(255) NOT NULL,
    `create_time_ms`        bigint(20) NOT NULL,
    `create_user`           varchar(255) NOT NULL,
    PRIMARY KEY (`name`),
    UNIQUE KEY `index_volume_group_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=621 DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `index_volume` (
    `id`                    bigint(20) NOT NULL AUTO_INCREMENT,
    `version` tinyint(4) NOT NULL,
    `create_time_ms`        bigint(20) NOT NULL,
    `create_user`          varchar(255) NOT NULL,
    `update_time_ms`        bigint(20) NOT NULL,
    `update_user`           varchar(255) NOT NULL,
    `node_name` varchar(255) NOT NULL,
    `path` varchar(255) NOT NULL,
    `bytes_limit` bigint(20) DEFAULT NULL,
    `bytes_used` bigint(20) DEFAULT NULL,
    `bytes_free` bigint(20) DEFAULT NULL,
    `bytes_total` bigint(20) DEFAULT NULL,
    `status_ms` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`ID`),
    UNIQUE KEY `node_name_path` (`node_name`,`path`)
) ENGINE=InnoDB AUTO_INCREMENT=621 DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `index_volume_group_link` (
    `fk_index_volume_group_name` varchar(255) NOT NULL,
    `fk_index_volume_id` bigint(20) NOT NULL,
    UNIQUE KEY `index_volume_group_link_unique` (`fk_index_volume_group_name`,`fk_index_volume_id`),
    CONSTRAINT `index_volume_group_link_fk_group_name` FOREIGN KEY (`fk_index_volume_group_name`) REFERENCES `index_volume_group` (`name`),
    CONSTRAINT `index_volume_group_link_fk_volume_id` FOREIGN KEY (`fk_index_volume_id`) REFERENCES `index_volume` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=621 DEFAULT CHARSET=latin1;

CREATE TABLE IF NOT EXISTS `index_shard` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `node_name` varchar(255) NOT NULL,
    `fk_volume_id` bigint(11) NOT NULL,
    `index_uuid` varchar(255) NOT NULL,
    `commit_document_count` int(11) DEFAULT NULL,
    `commit_duration_ms` bigint(20) DEFAULT NULL,
    `commit_ms` bigint(20) DEFAULT NULL,
    `document_count` int(11) DEFAULT 0,
    `file_size` bigint(20) DEFAULT 0,
    `status` tinyint(4) NOT NULL,
    `partition` varchar(255) NOT NULL,
    `index_version` varchar(255) DEFAULT NULL,
    `partition_from_ms` bigint(20) DEFAULT NULL,
    `partition_to_ms` bigint(20) DEFAULT NULL,
    PRIMARY KEY (`ID`),
    KEY `index_shard_fk_volume_id` (`fk_volume_id`),
    KEY `index_shard_index_uuid` (`index_uuid`),
    CONSTRAINT `index_shard_fk_volume_id` FOREIGN KEY (`fk_volume_id`) REFERENCES `index_volume` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Copy data into the explorer table
--
DROP PROCEDURE IF EXISTS copy_index;
DELIMITER //
CREATE PROCEDURE copy_index ()
BEGIN
-- TODO: All needs figuring out, groups need creating etc
          -- TODO update auto-increment, see V7_0_0_1__config.sql as an example

END//
DELIMITER ;
CALL copy_index();
DROP PROCEDURE copy_index;