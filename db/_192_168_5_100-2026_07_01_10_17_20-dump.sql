-- MySQL dump 10.13  Distrib 8.4.10, for macos15 (arm64)
--
-- Host: 192.168.5.100    Database: erp_db
-- ------------------------------------------------------
-- Server version	8.4.6

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `approval_instance`
--

DROP TABLE IF EXISTS `approval_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_instance` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint unsigned NOT NULL COMMENT '流程ID',
  `related_id` bigint NOT NULL COMMENT '关联业务ID(如采购订单ID)',
  `related_type` varchar(50) NOT NULL COMMENT '关联业务类型(PURCHASE_ORDER等)',
  `current_node_id` bigint unsigned NOT NULL COMMENT '当前节点ID',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '审批状态(PENDING-待审批/APPROVED-已同意/REJECTED-已驳回/TRANSFERRED-已转交/ARCHIVED-已归档)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `initiator_id` bigint DEFAULT NULL COMMENT '发起人ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  KEY `idx_related` (`related_id`,`related_type`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `current_node_id` (`current_node_id`),
  CONSTRAINT `approval_instance_ibfk_1` FOREIGN KEY (`workflow_id`) REFERENCES `approval_workflow` (`id`),
  CONSTRAINT `approval_instance_ibfk_2` FOREIGN KEY (`current_node_id`) REFERENCES `approval_node` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=278 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批实例';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `approval_node`
--

DROP TABLE IF EXISTS `approval_node`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_node` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_id` bigint unsigned NOT NULL COMMENT '流程ID',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称',
  `node_order` int NOT NULL COMMENT '节点顺序',
  `role_id` bigint unsigned NOT NULL COMMENT '审批角色ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `need_bind_business` tinyint(1) DEFAULT '0' COMMENT '是否需要绑定业务',
  `bind_type` varchar(64) DEFAULT NULL COMMENT '创建绑定类型',
  `enter_node_prompt` varchar(255) DEFAULT NULL COMMENT '进入节点提示',
  `after_pass_status` varchar(64) DEFAULT NULL COMMENT '通过后业务状态',
  `reject_prompt` varchar(255) DEFAULT NULL COMMENT '驳回提示',
  `after_reject_status` varchar(50) DEFAULT NULL COMMENT '驳回后业务状态',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `reject_to_node_id` bigint DEFAULT NULL COMMENT '驳回到哪个节点（为null时驳回到第一个节点）',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  KEY `idx_workflow_id` (`workflow_id`),
  CONSTRAINT `approval_node_ibfk_1` FOREIGN KEY (`workflow_id`) REFERENCES `approval_workflow` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=210 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批节点定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `approval_record`
--

DROP TABLE IF EXISTS `approval_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_record` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` bigint unsigned NOT NULL COMMENT '审批实例ID',
  `node_id` bigint unsigned NOT NULL COMMENT '节点ID',
  `approver_id` bigint unsigned NOT NULL COMMENT '审批人ID',
  `action` varchar(20) NOT NULL COMMENT '审批动作(AGREE-同意/REJECT-驳回/TRANSFER-转交)',
  `target_node_id` bigint unsigned DEFAULT NULL COMMENT '转交目标节点ID',
  `comment` text COMMENT '审批意见',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `approved_at` datetime DEFAULT NULL COMMENT '审批时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  KEY `idx_instance_id` (`instance_id`),
  KEY `node_id` (`node_id`),
  KEY `target_node_id` (`target_node_id`),
  CONSTRAINT `approval_record_ibfk_1` FOREIGN KEY (`instance_id`) REFERENCES `approval_instance` (`id`) ON DELETE CASCADE,
  CONSTRAINT `approval_record_ibfk_2` FOREIGN KEY (`node_id`) REFERENCES `approval_node` (`id`),
  CONSTRAINT `approval_record_ibfk_3` FOREIGN KEY (`target_node_id`) REFERENCES `approval_node` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1394 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `approval_workflow`
--

DROP TABLE IF EXISTS `approval_workflow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_workflow` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `workflow_name` varchar(100) NOT NULL COMMENT '流程名称',
  `workflow_type` varchar(50) NOT NULL COMMENT '流程类型(PURCHASE_PREP-制剂室采购流程/PURCHASE_OUTSOURCING-委托加工采购流程等)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：0停用/1启用',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_workflow_type` (`workflow_type`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批流程定义';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `clean_inspection_record`
--

DROP TABLE IF EXISTS `clean_inspection_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `clean_inspection_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` int NOT NULL COMMENT '关联房间ID',
  `inspection_date` date NOT NULL COMMENT '检测日期',
  `inspection_area` varchar(100) NOT NULL COMMENT '检测区域',
  `inspection_item` varchar(100) NOT NULL COMMENT '检测项目',
  `inspection_result` varchar(20) NOT NULL COMMENT '检测结果：合格/不合格',
  `inspector` varchar(50) NOT NULL COMMENT '检测人',
  `report_file_id` bigint DEFAULT NULL COMMENT '检测报告书文件ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='洁净检测记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `department_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '部门唯一标识',
  `department_name` varchar(100) NOT NULL COMMENT '部门名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父部门ID（0为顶级）',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：0禁用/1启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`department_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `disinfection_record`
--

DROP TABLE IF EXISTS `disinfection_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disinfection_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` int NOT NULL COMMENT '关联房间ID',
  `disinfection_date` date NOT NULL COMMENT '消毒日期',
  `disinfection_method` varchar(100) NOT NULL COMMENT '消毒方式',
  `disinfection_person` varchar(50) NOT NULL COMMENT '消毒人',
  `disinfection_cycle` int NOT NULL DEFAULT '7' COMMENT '消毒周期（天）',
  `next_disinfection_date` date DEFAULT NULL COMMENT '下次消毒时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_next_date` (`next_disinfection_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消毒管理记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dosage_form`
--

DROP TABLE IF EXISTS `dosage_form`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dosage_form` (
  `dosage_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '剂型唯一标识',
  `dosage_name` varchar(50) NOT NULL COMMENT '剂型名称（法定全称）',
  `remark` text COMMENT '剂型特性备注',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：0禁用/1启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`dosage_id`),
  UNIQUE KEY `dosage_name` (`dosage_name`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='药品剂型分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `equipment`
--

DROP TABLE IF EXISTS `equipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment` (
  `equipment_id` int NOT NULL AUTO_INCREMENT COMMENT '设备ID，主键。唯一标识，自增长。',
  `equipment_name` varchar(100) NOT NULL COMMENT '设备名称。设备的具体名称，如“数控车床-1”、“激光切割机-1”。',
  `equipment_model` varchar(50) DEFAULT NULL COMMENT '设备型号。制造商提供的设备型号。',
  `equipment_type` varchar(50) DEFAULT NULL COMMENT '设备类型：生产设备/检验设备/环境设备/其他设备',
  `room_id` int DEFAULT NULL COMMENT '所在房间ID。关联room_info表的room_id字段，标识设备所在房间。',
  `production_capacity` varchar(200) DEFAULT NULL COMMENT '生产能力。描述设备的生产能力参数，如“100件/小时”、“500吨”。',
  `equipment_status` tinyint NOT NULL DEFAULT '1' COMMENT '设备状态字典：1-启用/可用，0-停用/不可用。默认启用。',
  `fixed_asset_code` varchar(50) DEFAULT NULL COMMENT '固定资产编号。公司资产管理系统中的唯一编号。',
  `manufacturer` varchar(100) DEFAULT NULL COMMENT '生产厂家。设备的生产制造商。',
  `purchase_date` date DEFAULT NULL COMMENT '购置时间。设备购买的日期。',
  `purchase_amount` decimal(15,6) DEFAULT NULL COMMENT '购置金额。设备购买的价格，保留2位小数。',
  `last_maintenance_date` date DEFAULT NULL COMMENT '上次维保时间。最近一次维护保养的日期。',
  `maintenance_cycle` int DEFAULT '6' COMMENT '维保周期（月），默认6个月',
  `reminder_days` int DEFAULT '15' COMMENT '到期提醒天数，默认15天',
  `next_maintenance_date` date DEFAULT NULL COMMENT '下次维保时间',
  `remark` text COMMENT '备注。设备的附加说明信息。',
  `creator_id` bigint unsigned DEFAULT NULL COMMENT '创建人ID。记录创建者ID，关联用户表(`user.user_id`)。',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间。记录插入时间，自动填充当前时间。',
  `updater_id` bigint unsigned DEFAULT NULL COMMENT '修改人ID。记录最后修改者ID，关联用户表(`user.user_id`)。',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间。记录更新时自动更新为当前时间。',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`equipment_id`),
  UNIQUE KEY `uk_fixed_asset_code` (`fixed_asset_code`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_equipment_name` (`equipment_name`),
  KEY `idx_equipment_status` (`equipment_status`),
  KEY `idx_manufacturer` (`manufacturer`),
  KEY `idx_purchase_date` (`purchase_date`),
  KEY `idx_room_id` (`room_id`),
  KEY `idx_updater_id` (`updater_id`),
  CONSTRAINT `fk_equipment_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_equipment_room` FOREIGN KEY (`room_id`) REFERENCES `room_info` (`room_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_equipment_updater` FOREIGN KEY (`updater_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=89 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备表，记录设备详细信息。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `equipment_maintenance`
--

DROP TABLE IF EXISTS `equipment_maintenance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment_maintenance` (
  `maintenance_id` bigint NOT NULL AUTO_INCREMENT COMMENT '维保记录唯一标识',
  `equipment_id` bigint NOT NULL COMMENT '关联设备ID',
  `maintenance_type` varchar(50) NOT NULL COMMENT '维保类型：日常保养/定期检修/故障维修',
  `maintenance_date` date NOT NULL COMMENT '维保日期',
  `next_maintenance_date` date DEFAULT NULL COMMENT '下次维保日期',
  `next_maintenance_content` text COMMENT '下次维保内容',
  `attachment` varchar(500) DEFAULT NULL COMMENT '附件路径',
  `maintenance_content` text COMMENT '维保内容',
  `maintenance_result` varchar(255) DEFAULT NULL COMMENT '维保结果',
  `cost` decimal(18,2) DEFAULT NULL COMMENT '维保费用',
  `maintainer` varchar(255) DEFAULT NULL COMMENT '维保人员',
  `maintenance_company` varchar(100) DEFAULT NULL COMMENT '维保公司',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系方式',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`maintenance_id`),
  KEY `idx_equipment_id` (`equipment_id`),
  KEY `idx_next_maintenance` (`next_maintenance_date`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备维保记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_info`
--

DROP TABLE IF EXISTS `file_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_info` (
  `file_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件唯一标识',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `stored_name` varchar(255) NOT NULL COMMENT '存储文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `content_type` varchar(100) NOT NULL COMMENT '文件类型/内容类型',
  `file_extension` varchar(20) DEFAULT NULL COMMENT '文件扩展名',
  `file_md5` varchar(32) DEFAULT NULL COMMENT '文件MD5哈希值',
  `category` varchar(50) DEFAULT NULL COMMENT '文件分类',
  `description` varchar(500) DEFAULT NULL COMMENT '文件描述',
  `file_url` varchar(500) DEFAULT NULL COMMENT '访问URL',
  `storage_type` varchar(20) DEFAULT 'LOCAL' COMMENT '存储类型：LOCAL-本地存储，CLOUD-云存储',
  `business_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `business_type` varchar(50) DEFAULT NULL COMMENT '关联业务类型',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`file_id`),
  KEY `idx_business` (`business_id`,`business_type`),
  KEY `idx_category` (`category`),
  KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB AUTO_INCREMENT=230 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `material`
--

DROP TABLE IF EXISTS `material`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `material` (
  `material_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '物料唯一标识符',
  `material_code` varchar(50) NOT NULL COMMENT '物料编码（唯一性约束）',
  `material_name` varchar(100) NOT NULL COMMENT '物料名称',
  `category_name` varchar(50) NOT NULL COMMENT '分类（如原料/辅料/包材等）',
  `unit_name` varchar(50) NOT NULL COMMENT '计量单位（直接存文本，如kg/张/瓶）',
  `spec` varchar(100) DEFAULT NULL COMMENT '规格描述',
  `material_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1启用/0禁用',
  `remark` text COMMENT '备注信息',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL COMMENT '更新人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`material_id`),
  UNIQUE KEY `material_code` (`material_code`),
  KEY `idx_material_name` (`material_name`),
  KEY `idx_category_name` (`category_name`),
  KEY `idx_material_status` (`material_status`)
) ENGINE=InnoDB AUTO_INCREMENT=2000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物料信息表（存储单位与分类名称，不用外键）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `after_material_insert` AFTER INSERT ON `material` FOR EACH ROW BEGIN
    IF NEW.material_status = 1 THEN
        INSERT INTO stock (
            prod_unit_id, item_type, item_id, item_code, item_name,
            category_name, unit_name, quantity, batch_number, production_date,
            stock_status, created_by
        )
        SELECT
            pu.prod_unit_id,
            'material',
            NEW.material_id,
            NEW.material_code,
            NEW.material_name,
            NEW.category_name,
            NEW.unit_name,
            0.000,
            CONCAT('AUTO_', DATE_FORMAT(NOW(), '%Y%m%d'), '_', NEW.material_id),
            CURDATE(),
            'normal',
            NEW.created_by
        FROM production_unit pu
        WHERE pu.prod_unit_status = 1;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `perm_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '权限唯一标识',
  `perm_key` varchar(50) NOT NULL COMMENT '权限键（唯一标识符）',
  `perm_name` varchar(100) NOT NULL COMMENT '权限名称',
  `perm_type` enum('menu','api') NOT NULL DEFAULT 'api' COMMENT '权限类型',
  `parent_id` bigint unsigned DEFAULT NULL COMMENT '父权限ID',
  `display_order` tinyint NOT NULL DEFAULT '0' COMMENT '显示顺序',
  `perm_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0禁用/1启用',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`perm_id`),
  UNIQUE KEY `uk_perm_key` (`perm_key`),
  KEY `idx_permission_parent_id` (`parent_id`),
  CONSTRAINT `fk_permission_parent` FOREIGN KEY (`parent_id`) REFERENCES `permission` (`perm_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=159 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `personnel_file`
--

DROP TABLE IF EXISTS `personnel_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `personnel_file` (
  `personnel_file_id` bigint NOT NULL AUTO_INCREMENT COMMENT '人员档案唯一标识',
  `user_id` bigint DEFAULT NULL COMMENT '关联用户ID',
  `position_id` bigint DEFAULT NULL COMMENT '岗位ID',
  `employee_no` varchar(255) DEFAULT NULL COMMENT '工号',
  `name` varchar(50) DEFAULT NULL COMMENT '姓名',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `qualification` varchar(255) DEFAULT NULL COMMENT '人员资格认定',
  `title_name` varchar(100) DEFAULT NULL COMMENT '职称名',
  `title_level` varchar(50) DEFAULT NULL COMMENT '职称等级',
  `id_card_no` varchar(255) DEFAULT NULL COMMENT '身份证号',
  `health_cert_no` varchar(255) DEFAULT NULL COMMENT '健康证编号',
  `health_cert_issue` date DEFAULT NULL COMMENT '健康证发证日期',
  `health_cert_expire` date DEFAULT NULL COMMENT '健康证到期日期',
  `last_checkup_date` date DEFAULT NULL COMMENT '上次体检时间',
  `health_file` bigint DEFAULT NULL COMMENT '健康档案（关联file_info.file_id）',
  `attachments` bigint DEFAULT NULL COMMENT '附件（关联file_info.file_id）',
  `education` varchar(255) DEFAULT NULL COMMENT '学历',
  `major` varchar(255) DEFAULT NULL COMMENT '专业',
  `entry_date` date DEFAULT NULL COMMENT '入职日期',
  `work_experience` varchar(500) DEFAULT NULL COMMENT '任职简历',
  `education_training` varchar(500) DEFAULT NULL COMMENT '教育-培训经历',
  `certificate_name` varchar(100) DEFAULT NULL COMMENT '注册证书名',
  `certificate_no` varchar(50) DEFAULT NULL COMMENT '注册证书号',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：0离职/1在职',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`personnel_file_id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_employee_no` (`employee_no`),
  KEY `idx_health_cert_expire` (`health_cert_expire`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人员档案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `plan_status_log`
--

DROP TABLE IF EXISTS `plan_status_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plan_status_log` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `plan_id` int NOT NULL COMMENT '外键，关联生产计划ID',
  `from_status` varchar(20) DEFAULT NULL COMMENT '变更前状态',
  `to_status` varchar(20) NOT NULL COMMENT '变更后状态',
  `change_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '状态变更时间',
  `operator` bigint unsigned NOT NULL COMMENT '操作人员ID',
  `remark` text COMMENT '变更原因或备注',
  PRIMARY KEY (`id`),
  KEY `idx_planid` (`plan_id`),
  KEY `idx_changetime` (`change_time`),
  KEY `idx_tostatus` (`to_status`),
  KEY `idx_operator` (`operator`),
  KEY `idx_from_status` (`from_status`),
  CONSTRAINT `fk_status_log_operator` FOREIGN KEY (`operator`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `plan_status_log_ibfk_1` FOREIGN KEY (`plan_id`) REFERENCES `production_plan` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计划状态流水表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `position`
--

DROP TABLE IF EXISTS `position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `position` (
  `position_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位唯一标识',
  `position_code` varchar(255) NOT NULL COMMENT '岗位编码（唯一约束）',
  `position_name` varchar(255) NOT NULL COMMENT '岗位名称',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
  `position_desc` varchar(255) DEFAULT NULL COMMENT '岗位描述',
  `position_level` int DEFAULT NULL COMMENT '岗位等级',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：0停用/1启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`position_id`),
  UNIQUE KEY `uk_position_code` (`position_code`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `preparation`
--

DROP TABLE IF EXISTS `preparation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `preparation` (
  `preparation_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '制剂唯一标识',
  `preparation_code` varchar(50) NOT NULL COMMENT '制剂编码（唯一性约束）',
  `preparation_name` varchar(100) NOT NULL COMMENT '制剂品名',
  `spec` varchar(100) DEFAULT NULL COMMENT '规格描述',
  `process_attr` varchar(100) DEFAULT NULL COMMENT '加工性质',
  `package_spec` varchar(100) DEFAULT NULL COMMENT '包装规格',
  `record_info` varchar(255) DEFAULT NULL COMMENT '制剂备案',
  `function_main` text COMMENT '功能主治',
  `method` text COMMENT '制法',
  `unit_name` varchar(100) DEFAULT NULL COMMENT '单位名称',
  `dosage_form` varchar(50) DEFAULT NULL COMMENT '剂型',
  `producer` varchar(100) DEFAULT NULL COMMENT '生产单位',
  `batch_qty` decimal(20,8) DEFAULT NULL COMMENT '批量',
  `invoice_price` decimal(20,8) DEFAULT NULL COMMENT '开票单价',
  `insurance_price` decimal(20,8) DEFAULT NULL COMMENT '医保单价',
  `settlement_price` decimal(20,8) DEFAULT NULL COMMENT '结算单价',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1启用/0禁用',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL COMMENT '更新人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`preparation_id`),
  UNIQUE KEY `uk_preparation_code` (`preparation_code`),
  KEY `fk_preparation_created_by` (`created_by`),
  KEY `fk_preparation_updated_by` (`updated_by`),
  KEY `idx_preparation_name` (`preparation_name`),
  KEY `idx_status` (`status`),
  KEY `idx_dosage_form` (`dosage_form`),
  CONSTRAINT `fk_preparation_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_preparation_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=363 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='制剂信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `after_preparation_insert` AFTER INSERT ON `preparation` FOR EACH ROW BEGIN
    IF NEW.status = 1 THEN
        INSERT INTO stock (
            prod_unit_id, item_type, item_id, item_code, item_name,
            category_name, unit_name, quantity, batch_number, production_date,
            stock_status, created_by
        )
        SELECT
            pu.prod_unit_id,
            'preparation',
            NEW.preparation_id,
            NEW.preparation_code,
            NEW.preparation_name,
            '制剂',
            COALESCE(NEW.unit_name, '个'),
            0.000,
            CONCAT('AUTO_', DATE_FORMAT(NOW(), '%Y%m%d'), '_', NEW.preparation_id),
            CURDATE(),
            'normal',
            NEW.created_by
        FROM production_unit pu
        WHERE pu.prod_unit_status = 1;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `preparation_document`
--

DROP TABLE IF EXISTS `preparation_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `preparation_document` (
  `doc_id` bigint NOT NULL AUTO_INCREMENT COMMENT '文档唯一标识',
  `preparation_id` bigint NOT NULL COMMENT '制剂ID',
  `doc_type` varchar(50) NOT NULL COMMENT '文档类型：工艺规程/验证方案/验证报告模板/批记录模板',
  `doc_name` varchar(255) NOT NULL COMMENT '文档名称',
  `file_id` bigint NOT NULL COMMENT '关联文件ID',
  `version_no` varchar(50) DEFAULT NULL COMMENT '版本号',
  `effective_date` date DEFAULT NULL COMMENT '生效日期',
  `expire_date` date DEFAULT NULL COMMENT '失效日期',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：0作废/1有效',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`doc_id`),
  KEY `idx_preparation_id` (`preparation_id`),
  KEY `idx_doc_type` (`doc_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='制剂文档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `preparation_formula`
--

DROP TABLE IF EXISTS `preparation_formula`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `preparation_formula` (
  `formula_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '处方明细唯一标识',
  `preparation_id` bigint unsigned NOT NULL COMMENT '制剂ID，引用preparation表',
  `preparation_code` varchar(50) NOT NULL COMMENT '制剂编码',
  `preparation_name` varchar(100) NOT NULL COMMENT '制剂品名',
  `material_id` bigint unsigned NOT NULL COMMENT '原料ID，引用material表',
  `material_code` varchar(50) NOT NULL COMMENT '原料编号',
  `material_name` varchar(100) NOT NULL COMMENT '原料名称',
  `material_category` varchar(50) NOT NULL COMMENT '原料分类（原料/辅料/包材）',
  `dosage` decimal(20,8) DEFAULT NULL COMMENT '处方量',
  `unit_id` bigint unsigned NOT NULL COMMENT '单位ID，引用unit表',
  `unit_name` varchar(50) NOT NULL COMMENT '单位名称（kg/张/个）',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL COMMENT '更新人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`formula_id`),
  UNIQUE KEY `uk_formula` (`preparation_id`,`material_id`),
  KEY `idx_preparation_id` (`preparation_id`),
  KEY `idx_material_id` (`material_id`),
  KEY `idx_unit_id` (`unit_id`),
  CONSTRAINT `fk_formula_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`material_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_formula_preparation` FOREIGN KEY (`preparation_id`) REFERENCES `preparation` (`preparation_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_formula_unit` FOREIGN KEY (`unit_id`) REFERENCES `unit` (`unit_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5461 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='制剂处方信息表（含原料及单位信息）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `preparation_process_template`
--

DROP TABLE IF EXISTS `preparation_process_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `preparation_process_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模版唯一标识',
  `preparation_id` bigint NOT NULL COMMENT '制剂ID',
  `process_type_id` bigint NOT NULL COMMENT '工序类型ID',
  `step_order` int NOT NULL COMMENT '工序顺序',
  `standard_qty` decimal(18,2) DEFAULT NULL COMMENT '标准加工数量',
  `unit_id` bigint DEFAULT NULL COMMENT '计量单位ID',
  `standard_duration` int DEFAULT NULL COMMENT '标准工时（分钟）',
  `equipment_desc` varchar(255) DEFAULT NULL COMMENT '设备要求描述',
  `room_desc` varchar(255) DEFAULT NULL COMMENT '配置室要求',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`template_id`),
  KEY `idx_preparation_id` (`preparation_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='制剂工序模版表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pressure_difference_record`
--

DROP TABLE IF EXISTS `pressure_difference_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pressure_difference_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` int NOT NULL COMMENT '关联房间ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `inspection_area` varchar(100) NOT NULL COMMENT '检测区域',
  `pressure_value` decimal(8,2) DEFAULT NULL COMMENT '压差值（Pa）',
  `recorder` varchar(50) DEFAULT NULL COMMENT '记录人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='压差记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `process_type`
--

DROP TABLE IF EXISTS `process_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `process_type` (
  `process_id` int NOT NULL AUTO_INCREMENT COMMENT '工序类型ID，主键。唯一标识，自增长。',
  `process_code` varchar(20) NOT NULL COMMENT '工序类型编码。唯一业务编码，如“ASSEMBLY”。',
  `process_name` varchar(50) NOT NULL COMMENT '工序类型名称。中文描述，如“提取”。',
  `process_description` text COMMENT '工序类型说明。详细描述，可为空。',
  `process_status` tinyint NOT NULL DEFAULT '1' COMMENT '状态。`1`-启用，`0`-未启用。默认启用。',
  `creator_id` bigint unsigned DEFAULT NULL COMMENT '创建人ID。记录创建者ID，关联用户表(`user.user_id`)。',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间。记录插入时间，自动填充。',
  `updater_id` bigint unsigned DEFAULT NULL COMMENT '修改人ID。记录最后修改者ID，关联用户表(`user.user_id`)。',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间。记录更新时自动更新。',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`process_id`),
  UNIQUE KEY `uk_process_code` (`process_code`),
  UNIQUE KEY `uk_process_name` (`process_name`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_process_status` (`process_status`),
  KEY `idx_updater_id` (`updater_id`),
  CONSTRAINT `fk_process_type_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_process_type_updater` FOREIGN KEY (`updater_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工序类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `prod_unit_invoice`
--

DROP TABLE IF EXISTS `prod_unit_invoice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_unit_invoice` (
  `prod_invoice_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '发票信息唯一标识',
  `prod_unit_id` bigint unsigned NOT NULL COMMENT '关联的生产单位ID',
  `prod_invoice_info` text NOT NULL COMMENT '发票信息内容',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`prod_invoice_id`),
  KEY `created_by` (`created_by`),
  KEY `updated_by` (`updated_by`),
  KEY `idx_invoice_prod_unit` (`prod_unit_id`),
  CONSTRAINT `prod_unit_invoice_ibfk_1` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`) ON DELETE CASCADE,
  CONSTRAINT `prod_unit_invoice_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
  CONSTRAINT `prod_unit_invoice_ibfk_3` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产单位发票信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `prod_unit_material_file`
--

DROP TABLE IF EXISTS `prod_unit_material_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prod_unit_material_file` (
  `prod_material_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '材料文件唯一标识',
  `prod_unit_id` bigint unsigned NOT NULL COMMENT '关联的生产单位ID',
  `material_type` enum('image','document','other') NOT NULL DEFAULT 'image' COMMENT '材料文件类型',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `file_md5` char(32) NOT NULL COMMENT '文件内容MD5哈希值',
  `file_size` int unsigned NOT NULL COMMENT '文件大小（字节）',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件存储路径',
  `description` text COMMENT '文件描述',
  `file_content` longtext NOT NULL COMMENT '文件内容（Base64编码）',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`prod_material_id`),
  UNIQUE KEY `uk_file_md5` (`file_md5`),
  KEY `created_by` (`created_by`),
  KEY `idx_material_prod_unit` (`prod_unit_id`),
  KEY `idx_material_type` (`material_type`),
  KEY `idx_file_md5` (`file_md5`),
  CONSTRAINT `prod_unit_material_file_ibfk_1` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`) ON DELETE CASCADE,
  CONSTRAINT `prod_unit_material_file_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产单位材料文件表（Base64存储）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `production_plan`
--

DROP TABLE IF EXISTS `production_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_plan` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '计划唯一标识',
  `plan_number` varchar(50) NOT NULL COMMENT '计划编号',
  `related_order` varchar(100) DEFAULT NULL COMMENT '关联单号（销售订单号）',
  `preparation_code` varchar(50) NOT NULL COMMENT '制剂编码',
  `preparation_name` varchar(100) NOT NULL COMMENT '制剂名称',
  `plan_quantity` decimal(20,8) NOT NULL COMMENT '计划数量（批量）',
  `plan_type` enum('self_production','contract_processing','trial_production') NOT NULL DEFAULT 'self_production' COMMENT '生产计划类型',
  `current_status` varchar(20) NOT NULL DEFAULT 'PLAN_ISSUED' COMMENT '当前状态',
  `current_status_date` datetime NOT NULL COMMENT '当前状态时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint unsigned NOT NULL COMMENT '创建人员ID',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `update_user` bigint unsigned DEFAULT NULL COMMENT '最后更新人员ID',
  `remark` text COMMENT '备注信息',
  `is_archived` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已归档（0-未归档，1-已归档）',
  `unit_name` varchar(100) DEFAULT NULL COMMENT '制剂所属单位',
  `production_unit` varchar(100) DEFAULT NULL COMMENT '生产单位',
  `unit_price` decimal(20,8) DEFAULT NULL COMMENT '单价',
  `finished_quantity` decimal(20,8) DEFAULT NULL COMMENT '成品数量',
  `production_cycle` int DEFAULT NULL COMMENT '周期（天）',
  `yield_rate` decimal(5,2) DEFAULT NULL COMMENT '得率（百分比）',
  `total_amount` decimal(20,8) DEFAULT NULL COMMENT '总金额（成品数量*单价）',
  `production_start_time` datetime DEFAULT NULL COMMENT '生产开始时间',
  `production_end_time` datetime DEFAULT NULL COMMENT '生产结束时间',
  `inspection_start_time` datetime DEFAULT NULL COMMENT '检验开始时间',
  `inspection_end_time` datetime DEFAULT NULL COMMENT '检验结束时间',
  `outbound_time` datetime DEFAULT NULL COMMENT '出库时间',
  `archive_time` datetime DEFAULT NULL COMMENT '归档时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `plan_number` (`plan_number`),
  KEY `fk_production_plan_update_user` (`update_user`),
  KEY `idx_status` (`current_status`),
  KEY `idx_createdate` (`create_time`),
  KEY `idx_plantype` (`plan_type`),
  KEY `idx_create_user` (`create_user`),
  KEY `idx_update_time` (`update_time`),
  KEY `idx_archived` (`is_archived`),
  CONSTRAINT `fk_production_plan_create_user` FOREIGN KEY (`create_user`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_production_plan_update_user` FOREIGN KEY (`update_user`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产计划主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `production_process_record`
--

DROP TABLE IF EXISTS `production_process_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_process_record` (
  `record_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '记录ID，主键。自增长，唯一标识。',
  `plan_id` int NOT NULL COMMENT '生产计划ID。外键关联生产计划表(production_plan.id)。',
  `process_name` varchar(100) DEFAULT NULL COMMENT '工序类型名称。',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名。',
  `step_order` int NOT NULL DEFAULT '1' COMMENT '工序顺序。同一生产计划内的工序执行顺序，从1开始递增。',
  `workshop` varchar(100) DEFAULT NULL COMMENT '配置室。记录工序执行的房间。',
  `processing_qty` decimal(20,4) DEFAULT NULL COMMENT '加工数量。本工序实际加工的数量。',
  `unit_name` varchar(20) DEFAULT NULL COMMENT '计量单位。如：kg、g、L等。',
  `equipment` text COMMENT '使用设备。记录工序执行过程中使用的设备信息。',
  `start_time` datetime DEFAULT NULL COMMENT '工序开始时间。工序实际开始时间。',
  `end_time` datetime DEFAULT NULL COMMENT '工序结束时间。工序实际结束时间。',
  `production_start` datetime DEFAULT NULL COMMENT '生产开始时间',
  `production_end` datetime DEFAULT NULL COMMENT '生产结束时间',
  `inspection_start` datetime DEFAULT NULL COMMENT '检验开始时间',
  `inspection_end` datetime DEFAULT NULL COMMENT '检验结束时间',
  `record_status` tinyint NOT NULL DEFAULT '1' COMMENT '记录状态。`1`-正常，`0`-作废。',
  `remark` text COMMENT '备注信息。记录工序执行过程中的特殊说明。',
  `creator_id` bigint unsigned DEFAULT NULL COMMENT '创建人ID。记录创建者ID，关联用户表(`user.user_id`)。',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间。记录插入时间，自动填充。',
  `updater_id` bigint unsigned DEFAULT NULL COMMENT '修改人ID。记录最后修改者ID，关联用户表(`user.user_id`)。',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间。记录更新时自动更新。',
  PRIMARY KEY (`record_id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_end_time` (`end_time`),
  KEY `idx_operator_name` (`operator_name`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_process_name` (`process_name`),
  KEY `idx_record_status` (`record_status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_step_order` (`step_order`),
  KEY `idx_updater_id` (`updater_id`),
  KEY `idx_workshop` (`workshop`),
  CONSTRAINT `fk_process_record_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_process_record_updater` FOREIGN KEY (`updater_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_production_process_record_plan_id` FOREIGN KEY (`plan_id`) REFERENCES `production_plan` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=96 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产工序记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `production_unit`
--

DROP TABLE IF EXISTS `production_unit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_unit` (
  `prod_unit_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '生产单位唯一标识',
  `prod_unit_code` varchar(50) NOT NULL COMMENT '生产单位编号（唯一性约束）',
  `prod_unit_name` varchar(100) NOT NULL COMMENT '生产单位名称',
  `prod_unit_address` varchar(255) DEFAULT NULL COMMENT '生产单位地址',
  `prod_unit_manager` varchar(50) NOT NULL COMMENT '负责人姓名',
  `prod_unit_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `prod_unit_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0停用/1启用',
  `prod_unit_remark` text COMMENT '备注信息',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`prod_unit_id`),
  UNIQUE KEY `uk_prod_unit_code` (`prod_unit_code`),
  KEY `fk_prod_unit_created_by` (`created_by`),
  KEY `fk_prod_unit_updated_by` (`updated_by`),
  KEY `idx_prod_unit_status` (`prod_unit_status`),
  CONSTRAINT `fk_prod_unit_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
  CONSTRAINT `fk_prod_unit_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产单位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `after_production_unit_insert` AFTER INSERT ON `production_unit` FOR EACH ROW BEGIN
    IF NEW.prod_unit_status = 1 THEN
        -- 为所有物料创建库存
        INSERT INTO stock (
            prod_unit_id, item_type, item_id, item_code, item_name,
            category_name, unit_name, quantity, batch_number, production_date,
            stock_status, created_by
        )
        SELECT
            NEW.prod_unit_id,
            'material',
            m.material_id,
            m.material_code,
            m.material_name,
            m.category_name,
            m.unit_name,
            0.000,
            CONCAT('AUTO_', DATE_FORMAT(NOW(), '%Y%m%d'), '_', m.material_id),
            CURDATE(),
            'normal',
            NEW.created_by
        FROM material m
        WHERE m.material_status = 1;

        -- 为所有制剂创建库存
        INSERT INTO stock (
            prod_unit_id, item_type, item_id, item_code, item_name,
            category_name, unit_name, quantity, batch_number, production_date,
            stock_status, created_by
        )
        SELECT
            NEW.prod_unit_id,
            'preparation',
            p.preparation_id,
            p.preparation_code,
            p.preparation_name,
            '制剂',
            COALESCE(p.unit_name, '个'),
            0.000,
            CONCAT('AUTO_', DATE_FORMAT(NOW(), '%Y%m%d'), '_', p.preparation_id),
            CURDATE(),
            'normal',
            NEW.created_by
        FROM preparation p
        WHERE p.status = 1;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `purchase_order_items`
--

DROP TABLE IF EXISTS `purchase_order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` bigint NOT NULL COMMENT '关联订单ID',
  `sequence_number` int NOT NULL COMMENT '序号',
  `material_id` bigint DEFAULT NULL COMMENT '物料ID',
  `product_name` varchar(100) NOT NULL COMMENT '制剂名称',
  `raw_material_name` varchar(100) NOT NULL COMMENT '原药材品名',
  `dose` decimal(20,8) NOT NULL COMMENT '原药材剂量',
  `unit_id` bigint DEFAULT NULL COMMENT '单位ID',
  `unit` varchar(10) NOT NULL COMMENT '单位',
  `processing_property` varchar(50) NOT NULL COMMENT '加工性质',
  `stock` decimal(20,8) NOT NULL COMMENT '库存',
  `purchase_quantity` decimal(20,8) NOT NULL COMMENT '采购数量',
  `difference` decimal(20,8) DEFAULT NULL COMMENT '差值',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  KEY `order_id` (`order_id`),
  CONSTRAINT `fk_poi_order` FOREIGN KEY (`order_id`) REFERENCES `purchase_orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1165 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购订单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `purchase_orders`
--

DROP TABLE IF EXISTS `purchase_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `purchase_number` varchar(20) NOT NULL COMMENT '采购编号',
  `supplier_id` bigint DEFAULT NULL COMMENT '供应商ID',
  `prod_unit_id` bigint DEFAULT NULL COMMENT '仓库（生产单位ID）',
  `warehouse` varchar(50) NOT NULL COMMENT '仓库',
  `processing_date` date NOT NULL COMMENT '处理日期',
  `expected_delivery_date` date NOT NULL COMMENT '预计到货日期',
  `invoice_info` varchar(100) NOT NULL COMMENT '发票信息',
  `receiving_info` varchar(100) NOT NULL COMMENT '收货信息',
  `unit` varchar(50) NOT NULL COMMENT '制剂所属单位',
  `title` varchar(100) NOT NULL COMMENT '采购单标题',
  `prescription_multiple` decimal(20,8) NOT NULL DEFAULT '1.00000000' COMMENT '处方倍数',
  `remark` text COMMENT '备注',
  `generate_production_plan` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否生成生产计划',
  `status` enum('draft','submitted','received','completed') NOT NULL DEFAULT 'draft' COMMENT '状态',
  `approval_instance_id` bigint DEFAULT NULL COMMENT '审批实例ID',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `purchase_number` (`purchase_number`)
) ENGINE=InnoDB AUTO_INCREMENT=128 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购订单主表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `trg_auto_generate_purchase_number` BEFORE INSERT ON `purchase_orders` FOR EACH ROW BEGIN
    DECLARE current_date_str VARCHAR(8);
    DECLARE max_sequence INT;
    DECLARE new_sequence INT;

    -- 生成当前日期字符串（YYYYMMDD格式）
    SET current_date_str = DATE_FORMAT(CURDATE(), '%Y%m%d');

    -- 查询当天最大序列号
    SELECT COALESCE(MAX(CAST(SUBSTRING(purchase_number, 11, 4) AS UNSIGNED)), 0)
    INTO max_sequence
    FROM purchase_orders
    WHERE purchase_number LIKE CONCAT('CG', current_date_str, '%');

    -- 新序列号 = 最大序列号 + 1
    SET new_sequence = max_sequence + 1;

    -- 生成采购编号：CG + 日期 + 4位序列号（不足补零）
    SET NEW.purchase_number = CONCAT('CG', current_date_str, LPAD(new_sequence, 4, '0'));
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `purchase_suppliers`
--

DROP TABLE IF EXISTS `purchase_suppliers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_suppliers` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `supplier_number` varchar(20) NOT NULL COMMENT '供应商编号',
  `supplier_name` varchar(100) NOT NULL COMMENT '供应商名称',
  `category` varchar(50) NOT NULL COMMENT '类别',
  `contact_person` varchar(50) DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `address` text COMMENT '地址',
  `bank_account` varchar(50) DEFAULT NULL COMMENT '银行账户',
  `bank_name` varchar(100) DEFAULT NULL COMMENT '开户行',
  `remark` text COMMENT '备注',
  `status` enum('active','inactive') NOT NULL DEFAULT 'active' COMMENT '状态',
  `material_info` varchar(255) DEFAULT NULL COMMENT '材料信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `supplier_number` (`supplier_number`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购供应商信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `role_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '角色唯一标识',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称（唯一约束）',
  `role_desc` text COMMENT '角色描述',
  `role_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1启用,0禁用',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `role_perm`
--

DROP TABLE IF EXISTS `role_perm`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_perm` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `perm_id` bigint unsigned NOT NULL COMMENT '权限ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`perm_id`),
  KEY `idx_role_perm_perm_id` (`perm_id`),
  CONSTRAINT `fk_role_perm_perm` FOREIGN KEY (`perm_id`) REFERENCES `permission` (`perm_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_perm_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8420 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `room_info`
--

DROP TABLE IF EXISTS `room_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_info` (
  `room_id` int NOT NULL AUTO_INCREMENT COMMENT '房间ID，主键。唯一标识，自增长。',
  `room_name` varchar(50) NOT NULL COMMENT '房间名。唯一标识房间的名称，如“提取一室”、“动力机房”。',
  `room_location` varchar(100) DEFAULT NULL COMMENT '房间位置。描述房间的具体位置，如“A栋3楼301”。',
  `area` decimal(10,2) DEFAULT NULL COMMENT '面积。房间的面积，单位为平方米（㎡）。',
  `remark` text COMMENT '备注。房间的附加说明信息，可为空。',
  `room_status` tinyint NOT NULL DEFAULT '1' COMMENT '房间状态。1-启用/可用，0-停用/不可用。默认启用。',
  `creator_id` bigint unsigned DEFAULT NULL COMMENT '创建人ID。记录创建者ID，关联用户表(`user.user_id`)。',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间。记录插入时间，自动填充当前时间。',
  `updater_id` bigint unsigned DEFAULT NULL COMMENT '修改人ID。记录最后修改者ID，关联用户表(`user.user_id`)。',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间。记录更新时自动更新。',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `production_type` varchar(200) DEFAULT NULL COMMENT '生产制剂类型，多选：内服/外用/眼用',
  `dust_room` tinyint(1) DEFAULT '0' COMMENT '是否为产尘操作间',
  `clean_area` tinyint(1) DEFAULT '0' COMMENT '是否为洁净区',
  `clean_grade` varchar(10) DEFAULT NULL COMMENT '洁净等级：A级/B级/C级/D级',
  `clean_procedure_file_id` bigint DEFAULT NULL COMMENT '洁净规程文件ID，关联file_info',
  PRIMARY KEY (`room_id`),
  UNIQUE KEY `uk_room_name` (`room_name`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_room_location` (`room_location`),
  KEY `idx_room_status` (`room_status`),
  KEY `idx_updater_id` (`updater_id`),
  CONSTRAINT `fk_room_info_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_room_info_updater` FOREIGN KEY (`updater_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='房间表，用于记录设备所在房间的信息。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock`
--

DROP TABLE IF EXISTS `stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock` (
  `stock_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '库存唯一标识符',
  `prod_unit_id` bigint unsigned NOT NULL COMMENT '关联的生产单位ID（库存位置）',
  `item_type` enum('material','preparation') NOT NULL COMMENT '物品类型：material物料 / preparation制剂',
  `item_id` bigint unsigned NOT NULL COMMENT '关联的物品ID（根据item_type引用对应表ID）',
  `item_code` varchar(50) NOT NULL COMMENT '物品编码（冗余存储，便于查询）',
  `item_name` varchar(100) NOT NULL COMMENT '物品名称（冗余存储，便于查询）',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称（原料/辅料/包材/制剂等）',
  `unit_name` varchar(50) NOT NULL COMMENT '计量单位',
  `quantity` decimal(20,8) NOT NULL DEFAULT '0.00000000' COMMENT '库存数量',
  `min_quantity` decimal(20,8) DEFAULT NULL COMMENT '最低库存预警数量',
  `max_quantity` decimal(20,8) DEFAULT NULL COMMENT '最高库存限制数量',
  `batch_number` varchar(100) NOT NULL COMMENT '批次号',
  `production_date` date NOT NULL COMMENT '生产日期',
  `expiry_date` date DEFAULT NULL COMMENT '有效期至',
  `storage_location` varchar(100) DEFAULT NULL COMMENT '库位/货架号',
  `stock_status` enum('normal','frozen','expired','locked') NOT NULL DEFAULT 'normal' COMMENT '库存状态',
  `remark` text COMMENT '备注信息',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `unit_price` decimal(12,2) DEFAULT NULL COMMENT '单价',
  PRIMARY KEY (`stock_id`),
  UNIQUE KEY `uk_stock_unique` (`prod_unit_id`,`item_type`,`item_id`,`batch_number`),
  KEY `created_by` (`created_by`),
  KEY `idx_stock_batch_number` (`batch_number`),
  KEY `idx_stock_category` (`category_name`),
  KEY `idx_stock_expiry_date` (`expiry_date`),
  KEY `idx_stock_item_code` (`item_code`),
  KEY `idx_stock_item_type_item_id` (`item_type`,`item_id`),
  KEY `idx_stock_prod_unit` (`prod_unit_id`),
  KEY `idx_stock_status` (`stock_status`),
  KEY `idx_stock_storage_location` (`storage_location`),
  KEY `updated_by` (`updated_by`),
  CONSTRAINT `stock_ibfk_1` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
  CONSTRAINT `stock_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
  CONSTRAINT `stock_ibfk_3` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2399 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存表（统一管理物料和制剂库存，按生产单位分配）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_in`
--

DROP TABLE IF EXISTS `stock_in`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_in` (
  `in_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '入库单唯一标识',
  `in_code` varchar(50) NOT NULL COMMENT '入库单号（唯一）',
  `in_type` enum('purchase','production','return','transfer','adjust') NOT NULL COMMENT '入库类型：采购/生产/退货/调拨/调整',
  `prod_unit_id` bigint unsigned NOT NULL COMMENT '入库仓库（生产单位ID）',
  `supplier_id` bigint unsigned DEFAULT NULL COMMENT '供应商ID（如果是采购入库）',
  `related_order` varchar(100) DEFAULT NULL COMMENT '关联单号（采购单号、生产批号等）',
  `in_date` date NOT NULL COMMENT '入库日期',
  `total_amount` decimal(20,8) DEFAULT NULL COMMENT '入库总金额',
  `in_status` enum('draft','confirmed','completed','cancelled') NOT NULL DEFAULT 'draft' COMMENT '状态：草稿/已确认/已完成/已取消',
  `remark` text COMMENT '备注',
  `approval_instance_id` bigint DEFAULT NULL COMMENT '审批实例ID',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`in_id`),
  UNIQUE KEY `uk_stock_in_code` (`in_code`),
  KEY `created_by` (`created_by`),
  KEY `prod_unit_id` (`prod_unit_id`),
  KEY `updated_by` (`updated_by`),
  CONSTRAINT `stock_in_ibfk_1` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
  CONSTRAINT `stock_in_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
  CONSTRAINT `stock_in_ibfk_3` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入库主表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `after_stock_in_cancel` AFTER UPDATE ON `stock_in` FOR EACH ROW BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_item_type VARCHAR(50);
    DECLARE v_item_id BIGINT UNSIGNED;
    DECLARE v_batch_number VARCHAR(100);
    DECLARE v_quantity DECIMAL(18,3);
    DECLARE v_stock_id BIGINT UNSIGNED;
    DECLARE v_current_quantity DECIMAL(18,3);

    DECLARE cur_detail CURSOR FOR
        SELECT item_type, item_id, batch_number, quantity
        FROM stock_in_detail
        WHERE in_id = NEW.in_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 当入库单从completed变为cancelled时
    IF OLD.in_status = 'completed' AND NEW.in_status = 'cancelled' THEN
        OPEN cur_detail;
        read_loop: LOOP
            FETCH cur_detail INTO v_item_type, v_item_id, v_batch_number, v_quantity;
            IF done THEN
                LEAVE read_loop;
            END IF;

            -- 检查库存记录是否存在（仅根据生产单位ID、物品类型和物品ID匹配）
            SELECT stock_id, quantity INTO v_stock_id, v_current_quantity
            FROM stock
            WHERE prod_unit_id = NEW.prod_unit_id
              AND item_type = v_item_type
              AND item_id = v_item_id
            LIMIT 1;

            IF v_stock_id IS NOT NULL AND v_current_quantity >= v_quantity THEN
                -- 回滚库存数量
                UPDATE stock
                SET quantity = quantity - v_quantity,
                    updated_time = NOW(),
                    updated_by = NEW.updated_by
                WHERE stock_id = v_stock_id;

                -- 记录取消交易
                INSERT INTO stock_transaction (
                    stock_id, transaction_type, transaction_date,
                    related_id, related_type, quantity_before,
                    quantity_change, quantity_after, batch_number, created_by, remark
                )
                SELECT
                    v_stock_id,
                    'adjust',
                    NOW(),
                    NEW.in_id,
                    'stock_in',
                    v_current_quantity,
                    -v_quantity,
                    v_current_quantity - v_quantity,
                    batch_number,
                    NEW.updated_by,
                    CONCAT('入库单取消回滚: ', COALESCE(NEW.in_code, ''))
                FROM stock
                WHERE stock_id = v_stock_id;
            END IF;
        END LOOP;
        CLOSE cur_detail;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `after_stock_in_confirm` AFTER UPDATE ON `stock_in` FOR EACH ROW BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_item_type VARCHAR(50);
    DECLARE v_item_id BIGINT UNSIGNED;
    DECLARE v_batch_number VARCHAR(100);
    DECLARE v_quantity DECIMAL(18,3);
    DECLARE v_stock_id BIGINT UNSIGNED;
    DECLARE v_quantity_before DECIMAL(18,3);
    DECLARE v_item_code VARCHAR(50);
    DECLARE v_item_name VARCHAR(100);
    DECLARE v_category_name VARCHAR(50);
    DECLARE v_unit_name VARCHAR(50);
    DECLARE v_production_date DATE;
    DECLARE v_expiry_date DATE;
    DECLARE v_storage_location VARCHAR(100);

    DECLARE cur_detail CURSOR FOR
        SELECT item_type, item_id, batch_number, quantity, item_code, item_name, category_name, unit_name, production_date, expiry_date, storage_location
        FROM stock_in_detail
        WHERE in_id = NEW.in_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 当入库单状态从非completed变为completed时
    IF OLD.in_status != 'completed' AND NEW.in_status = 'completed' THEN
        OPEN cur_detail;
        read_loop: LOOP
            FETCH cur_detail INTO v_item_type, v_item_id, v_batch_number, v_quantity, v_item_code, v_item_name, v_category_name, v_unit_name, v_production_date, v_expiry_date, v_storage_location;
            IF done THEN
                LEAVE read_loop;
            END IF;

            -- 检查库存记录是否存在（仅根据生产单位ID、物品类型和物品ID匹配）
            SELECT stock_id, quantity INTO v_stock_id, v_quantity_before
            FROM stock
            WHERE prod_unit_id = NEW.prod_unit_id
              AND item_type = v_item_type
              AND item_id = v_item_id
            LIMIT 1;

            -- 如果库存记录不存在，创建新的库存记录
            IF v_stock_id IS NULL THEN
                INSERT INTO stock (
                    prod_unit_id, item_type, item_id, item_code, item_name,
                    category_name, unit_name, quantity, batch_number, production_date,
                    expiry_date, storage_location, stock_status, created_by, updated_by
                )
                VALUES (
                           NEW.prod_unit_id,
                           v_item_type,
                           v_item_id,
                           v_item_code,
                           v_item_name,
                           v_category_name,
                           v_unit_name,
                           v_quantity,
                           v_batch_number,
                           v_production_date,
                           v_expiry_date,
                           v_storage_location,
                           'normal',
                           NEW.updated_by,
                           NEW.updated_by
                       );

                SET v_stock_id = LAST_INSERT_ID();
                SET v_quantity_before = 0;
            ELSE
                -- 更新现有库存记录（在原有记录上累加）
                UPDATE stock
                SET quantity = quantity + v_quantity,
                    item_code = v_item_code,
                    item_name = v_item_name,
                    category_name = v_category_name,
                    unit_name = v_unit_name,
                    production_date = v_production_date,
                    expiry_date = v_expiry_date,
                    storage_location = v_storage_location,
                    updated_by = NEW.updated_by,
                    updated_time = NOW()
                WHERE stock_id = v_stock_id;
            END IF;

            -- 记录交易记录
            INSERT INTO stock_transaction (
                stock_id, transaction_type, transaction_date,
                related_id, related_type, quantity_before,
                quantity_change, quantity_after, batch_number, created_by, remark
            )
            VALUES (
                       v_stock_id,
                       'in',
                       NOW(),
                       NEW.in_id,
                       'stock_in',
                       v_quantity_before,
                       v_quantity,
                       v_quantity_before + v_quantity,
                       v_batch_number,
                       NEW.updated_by,
                       CONCAT('入库单确认: ', COALESCE(NEW.in_code, ''))
                   );
        END LOOP;
        CLOSE cur_detail;
    END IF;

    -- 当入库单状态从completed变为其他状态时（回滚操作）
    IF OLD.in_status = 'completed' AND NEW.in_status != 'completed' THEN
        OPEN cur_detail;
        read_loop_rollback: LOOP
            FETCH cur_detail INTO v_item_type, v_item_id, v_batch_number, v_quantity, v_item_code, v_item_name, v_category_name, v_unit_name, v_production_date, v_expiry_date, v_storage_location;
            IF done THEN
                LEAVE read_loop_rollback;
            END IF;

            -- 查找对应的库存记录（仅根据生产单位ID、物品类型和物品ID匹配）
            SELECT stock_id, quantity INTO v_stock_id, v_quantity_before
            FROM stock
            WHERE prod_unit_id = NEW.prod_unit_id
              AND item_type = v_item_type
              AND item_id = v_item_id
            LIMIT 1;

            -- 如果库存记录存在，减少相应数量
            IF v_stock_id IS NOT NULL AND v_quantity_before >= v_quantity THEN
                UPDATE stock
                SET quantity = quantity - v_quantity,
                    updated_by = NEW.updated_by,
                    updated_time = NOW()
                WHERE stock_id = v_stock_id;

                -- 记录回滚交易记录
                INSERT INTO stock_transaction (
                    stock_id, transaction_type, transaction_date,
                    related_id, related_type, quantity_before,
                    quantity_change, quantity_after, batch_number, created_by, remark
                )
                VALUES (
                           v_stock_id,
                           'adjust',
                           NOW(),
                           NEW.in_id,
                           'stock_in',
                           v_quantity_before,
                           -v_quantity,
                           v_quantity_before - v_quantity,
                           v_batch_number,
                           NEW.updated_by,
                           CONCAT('入库单状态变更回滚: ', COALESCE(NEW.in_code, ''), ' 从completed到', NEW.in_status)
                       );
            END IF;
        END LOOP;
        CLOSE cur_detail;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `stock_in_detail`
--

DROP TABLE IF EXISTS `stock_in_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_in_detail` (
  `in_detail_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '入库明细唯一标识',
  `in_id` bigint unsigned NOT NULL COMMENT '关联入库单ID',
  `item_type` enum('material','preparation') NOT NULL COMMENT '物品类型',
  `item_id` bigint unsigned NOT NULL COMMENT '物品ID',
  `item_code` varchar(50) NOT NULL COMMENT '物品编码',
  `item_name` varchar(100) NOT NULL COMMENT '物品名称',
  `category_name` varchar(50) NOT NULL COMMENT '分类',
  `unit_name` varchar(50) NOT NULL COMMENT '单位',
  `batch_number` varchar(100) DEFAULT NULL COMMENT '批次号',
  `production_date` date NOT NULL COMMENT '生产日期',
  `expiry_date` date DEFAULT NULL COMMENT '有效期至',
  `quantity` decimal(20,8) NOT NULL COMMENT '入库数量',
  `unit_price` decimal(20,8) NOT NULL COMMENT '单价',
  `amount` decimal(20,8) NOT NULL COMMENT '金额',
  `storage_location` varchar(100) DEFAULT NULL COMMENT '存放位置',
  `remark` text COMMENT '备注',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`in_detail_id`),
  KEY `idx_in_detail_batch` (`batch_number`),
  KEY `idx_in_detail_item` (`item_type`,`item_id`),
  KEY `in_id` (`in_id`),
  CONSTRAINT `stock_in_detail_ibfk_1` FOREIGN KEY (`in_id`) REFERENCES `stock_in` (`in_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=293 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入库明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_in_sequence`
--

DROP TABLE IF EXISTS `stock_in_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_in_sequence` (
  `seq_date` date NOT NULL COMMENT '日期',
  `seq_number` int unsigned NOT NULL DEFAULT '0' COMMENT '序列号',
  PRIMARY KEY (`seq_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='入库单号序列表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_out`
--

DROP TABLE IF EXISTS `stock_out`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_out` (
  `out_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '出库单唯一标识',
  `out_code` varchar(50) NOT NULL COMMENT '出库单号（唯一）',
  `out_type` enum('sales','production','return','transfer','adjust') NOT NULL COMMENT '出库类型：销售/生产领用/退货/调拨/调整',
  `prod_unit_id` bigint unsigned NOT NULL COMMENT '出库仓库（生产单位ID）',
  `customer_id` bigint unsigned DEFAULT NULL COMMENT '客户ID（如果是销售出库）',
  `related_order` varchar(100) DEFAULT NULL COMMENT '关联单号（销售订单号、生产任务单等）',
  `out_date` date NOT NULL COMMENT '出库日期',
  `total_amount` decimal(20,8) DEFAULT NULL COMMENT '出库总金额',
  `out_status` enum('draft','confirmed','completed','cancelled') NOT NULL DEFAULT 'draft' COMMENT '状态：草稿/已确认/已完成/已取消',
  `remark` text COMMENT '备注',
  `approval_instance_id` bigint DEFAULT NULL COMMENT '审批实例ID',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`out_id`),
  UNIQUE KEY `uk_stock_out_code` (`out_code`),
  KEY `created_by` (`created_by`),
  KEY `prod_unit_id` (`prod_unit_id`),
  KEY `updated_by` (`updated_by`),
  CONSTRAINT `stock_out_ibfk_1` FOREIGN KEY (`prod_unit_id`) REFERENCES `production_unit` (`prod_unit_id`),
  CONSTRAINT `stock_out_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`),
  CONSTRAINT `stock_out_ibfk_3` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出库主表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `after_stock_out_cancel` AFTER UPDATE ON `stock_out` FOR EACH ROW BEGIN
    -- 当出库单从completed变为cancelled时
    IF OLD.out_status = 'completed' AND NEW.out_status = 'cancelled' THEN
        -- 回滚库存数量
        UPDATE stock s
            JOIN stock_out_detail sod ON s.stock_id = sod.stock_id
        SET s.quantity = s.quantity + sod.quantity,
            s.updated_time = NOW(),
            s.updated_by = NEW.updated_by
        WHERE sod.out_id = NEW.out_id;

        -- 记录取消交易
        INSERT INTO stock_transaction (
            stock_id, transaction_type, transaction_date,
            related_id, related_type, quantity_before,
            quantity_change, quantity_after, batch_number, created_by, remark
        )
        SELECT
            s.stock_id,
            'adjust',
            NOW(),
            NEW.out_id,
            'stock_out',
            s.quantity - sod.quantity,
            sod.quantity,
            s.quantity,
            s.batch_number,
            NEW.updated_by,
            CONCAT('出库单取消回滚: ', COALESCE(NEW.out_code, ''))
        FROM stock s
                 JOIN stock_out_detail sod ON s.stock_id = sod.stock_id
        WHERE sod.out_id = NEW.out_id;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `after_stock_out_confirm` AFTER UPDATE ON `stock_out` FOR EACH ROW BEGIN
    DECLARE v_insufficient_stock INT DEFAULT 0;

    -- 当出库单状态从非completed变为completed时
    IF OLD.out_status != 'completed' AND NEW.out_status = 'completed' THEN
        -- 检查库存是否足够
        SELECT COUNT(*) INTO v_insufficient_stock
        FROM stock_out_detail sod
                 JOIN stock s ON sod.stock_id = s.stock_id
        WHERE sod.out_id = NEW.out_id
          AND s.quantity < sod.quantity;

        IF v_insufficient_stock > 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '库存不足，无法出库';
        ELSE
            -- 更新库存数量
            UPDATE stock s
                JOIN stock_out_detail sod ON s.stock_id = sod.stock_id
            SET s.quantity = s.quantity - sod.quantity,
                s.updated_by = NEW.updated_by,
                s.updated_time = NOW()
            WHERE sod.out_id = NEW.out_id;

            -- 记录交易记录
            INSERT INTO stock_transaction (
                stock_id, transaction_type, transaction_date,
                related_id, related_type, quantity_before,
                quantity_change, quantity_after, batch_number, created_by, remark
            )
            SELECT
                s.stock_id,
                'out',
                NOW(),
                NEW.out_id,
                'stock_out',
                s.quantity + sod.quantity,
                -sod.quantity,
                s.quantity,
                s.batch_number,
                NEW.updated_by,
                CONCAT('出库单确认: ', COALESCE(NEW.out_code, ''))
            FROM stock s
                     JOIN stock_out_detail sod ON s.stock_id = sod.stock_id
            WHERE sod.out_id = NEW.out_id;
        END IF;
    END IF;

    -- 当出库单状态从completed变为其他状态时（回滚操作）
    IF OLD.out_status = 'completed' AND NEW.out_status != 'completed' THEN
        -- 回滚库存数量
        UPDATE stock s
            JOIN stock_out_detail sod ON s.stock_id = sod.stock_id
        SET s.quantity = s.quantity + sod.quantity,
            s.updated_by = NEW.updated_by,
            s.updated_time = NOW()
        WHERE sod.out_id = NEW.out_id;

        -- 记录回滚交易记录
        INSERT INTO stock_transaction (
            stock_id, transaction_type, transaction_date,
            related_id, related_type, quantity_before,
            quantity_change, quantity_after, batch_number, created_by, remark
        )
        SELECT
            s.stock_id,
            'adjust',
            NOW(),
            NEW.out_id,
            'stock_out',
            s.quantity - sod.quantity,
            sod.quantity,
            s.quantity,
            s.batch_number,
            NEW.updated_by,
            CONCAT('出库单状态变更回滚: ', COALESCE(NEW.out_code, ''), ' 从completed到', NEW.out_status)
        FROM stock s
                 JOIN stock_out_detail sod ON s.stock_id = sod.stock_id
        WHERE sod.out_id = NEW.out_id;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `stock_out_detail`
--

DROP TABLE IF EXISTS `stock_out_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_out_detail` (
  `out_detail_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '出库明细唯一标识',
  `out_id` bigint unsigned NOT NULL COMMENT '关联出库单ID',
  `stock_id` bigint unsigned NOT NULL COMMENT '关联库存记录ID',
  `item_type` enum('material','preparation') NOT NULL COMMENT '物品类型',
  `item_id` bigint unsigned NOT NULL COMMENT '物品ID',
  `item_code` varchar(50) NOT NULL COMMENT '物品编码',
  `item_name` varchar(100) NOT NULL COMMENT '物品名称',
  `category_name` varchar(50) NOT NULL COMMENT '分类',
  `unit_name` varchar(50) NOT NULL COMMENT '单位',
  `batch_number` varchar(100) NOT NULL DEFAULT '' COMMENT '批次号',
  `quantity` decimal(20,8) NOT NULL COMMENT '出库数量',
  `unit_price` decimal(20,8) NOT NULL COMMENT '单价',
  `amount` decimal(20,8) NOT NULL COMMENT '金额',
  `remark` text COMMENT '备注',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`out_detail_id`),
  KEY `idx_out_detail_batch` (`batch_number`),
  KEY `idx_out_detail_item` (`item_type`,`item_id`),
  KEY `out_id` (`out_id`),
  KEY `stock_id` (`stock_id`),
  CONSTRAINT `stock_out_detail_ibfk_1` FOREIGN KEY (`out_id`) REFERENCES `stock_out` (`out_id`) ON DELETE CASCADE,
  CONSTRAINT `stock_out_detail_ibfk_2` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`stock_id`)
) ENGINE=InnoDB AUTO_INCREMENT=277 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出库明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_out_sequence`
--

DROP TABLE IF EXISTS `stock_out_sequence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_out_sequence` (
  `seq_date` date NOT NULL COMMENT '日期',
  `seq_number` int unsigned NOT NULL DEFAULT '0' COMMENT '序列号',
  PRIMARY KEY (`seq_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出库单号序列表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stock_transaction`
--

DROP TABLE IF EXISTS `stock_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stock_transaction` (
  `transaction_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '交易记录唯一标识',
  `stock_id` bigint unsigned NOT NULL COMMENT '关联库存ID',
  `transaction_type` enum('in','out','adjust') NOT NULL COMMENT '交易类型：入库/出库/调整',
  `transaction_date` datetime NOT NULL COMMENT '交易时间',
  `related_id` bigint unsigned NOT NULL COMMENT '关联单据ID（入库单ID/出库单ID）',
  `related_type` enum('stock_in','stock_out') NOT NULL COMMENT '关联单据类型',
  `quantity_before` decimal(18,3) NOT NULL COMMENT '交易前数量',
  `quantity_change` decimal(18,3) NOT NULL COMMENT '变动数量（正数表示增加，负数表示减少）',
  `quantity_after` decimal(18,3) NOT NULL COMMENT '交易后数量',
  `batch_number` varchar(100) NOT NULL COMMENT '批次号',
  `remark` text COMMENT '备注',
  `created_by` bigint unsigned NOT NULL COMMENT '操作人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`transaction_id`),
  KEY `created_by` (`created_by`),
  KEY `idx_transaction_batch` (`batch_number`),
  KEY `idx_transaction_date` (`transaction_date`),
  KEY `idx_transaction_stock` (`stock_id`),
  CONSTRAINT `stock_transaction_ibfk_1` FOREIGN KEY (`stock_id`) REFERENCES `stock` (`stock_id`),
  CONSTRAINT `stock_transaction_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=721 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存交易记录表（用于审计和追溯）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_error_log`
--

DROP TABLE IF EXISTS `system_error_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_error_log` (
  `log_id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `error_message` text NOT NULL,
  `related_table` varchar(50) DEFAULT NULL,
  `related_id` bigint unsigned DEFAULT NULL,
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_error_time` (`created_time`),
  KEY `idx_related` (`related_table`,`related_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统错误日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `temperature_humidity_record`
--

DROP TABLE IF EXISTS `temperature_humidity_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `temperature_humidity_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` int NOT NULL COMMENT '关联房间ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `inspection_area` varchar(100) NOT NULL COMMENT '检测区域',
  `temperature` decimal(5,2) DEFAULT NULL COMMENT '温度（℃）',
  `humidity` decimal(5,2) DEFAULT NULL COMMENT '湿度（%）',
  `recorder` varchar(50) DEFAULT NULL COMMENT '记录人',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='温湿度记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `unit`
--

DROP TABLE IF EXISTS `unit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `unit` (
  `unit_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '单位唯一标识',
  `unit_name` varchar(100) NOT NULL COMMENT '单位中文名称（法定全称）',
  `symbol` varchar(20) NOT NULL COMMENT '单位符号（区分大小写）',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：0禁用/1启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`unit_id`),
  UNIQUE KEY `symbol` (`symbol`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计量单位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
  `user_account` varchar(50) NOT NULL COMMENT '用户名（唯一性约束）',
  `user_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `gender` enum('MALE','FEMALE','OTHER') DEFAULT 'OTHER' COMMENT '性别',
  `user_status` tinyint(1) DEFAULT '1' COMMENT '账户状态：0禁用/1启用',
  `user_notes` text COMMENT '用户备注信息',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_account` (`user_account`)
) ENGINE=InnoDB AUTO_INCREMENT=54 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_department`
--

DROP TABLE IF EXISTS `user_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_department` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `department_id` bigint unsigned NOT NULL COMMENT '部门ID',
  `is_primary` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否主部门',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_dept` (`user_id`,`department_id`),
  KEY `idx_user_department_dept_id` (`department_id`),
  CONSTRAINT `fk_user_department_dept` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_department_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=94 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_role`
--

DROP TABLE IF EXISTS `user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=157 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `v_category_inventory_summary`
--

DROP TABLE IF EXISTS `v_category_inventory_summary`;
/*!50001 DROP VIEW IF EXISTS `v_category_inventory_summary`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_category_inventory_summary` AS SELECT 
 1 AS `warehouse_name`,
 1 AS `category_name`,
 1 AS `item_type`,
 1 AS `item_count`,
 1 AS `total_quantity`,
 1 AS `avg_quantity`,
 1 AS `min_quantity`,
 1 AS `max_quantity`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_inventory_alerts`
--

DROP TABLE IF EXISTS `v_inventory_alerts`;
/*!50001 DROP VIEW IF EXISTS `v_inventory_alerts`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_inventory_alerts` AS SELECT 
 1 AS `warehouse_name`,
 1 AS `item_code`,
 1 AS `item_name`,
 1 AS `category_name`,
 1 AS `unit_name`,
 1 AS `quantity`,
 1 AS `min_quantity`,
 1 AS `max_quantity`,
 1 AS `batch_number`,
 1 AS `expiry_date`,
 1 AS `alert_type`,
 1 AS `alert_message`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_stock_by_warehouse`
--

DROP TABLE IF EXISTS `v_stock_by_warehouse`;
/*!50001 DROP VIEW IF EXISTS `v_stock_by_warehouse`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_stock_by_warehouse` AS SELECT 
 1 AS `prod_unit_id`,
 1 AS `warehouse_name`,
 1 AS `stock_id`,
 1 AS `item_type`,
 1 AS `item_code`,
 1 AS `item_name`,
 1 AS `category_name`,
 1 AS `unit_name`,
 1 AS `quantity`,
 1 AS `min_quantity`,
 1 AS `max_quantity`,
 1 AS `batch_number`,
 1 AS `production_date`,
 1 AS `expiry_date`,
 1 AS `storage_location`,
 1 AS `remark`,
 1 AS `created_time`,
 1 AS `updated_time`,
 1 AS `inventory_status`,
 1 AS `expiry_status`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_stock_summary`
--

DROP TABLE IF EXISTS `v_stock_summary`;
/*!50001 DROP VIEW IF EXISTS `v_stock_summary`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_stock_summary` AS SELECT 
 1 AS `stock_id`,
 1 AS `prod_unit_id`,
 1 AS `warehouse_code`,
 1 AS `warehouse_name`,
 1 AS `item_type`,
 1 AS `item_id`,
 1 AS `item_code`,
 1 AS `item_name`,
 1 AS `category_name`,
 1 AS `unit_name`,
 1 AS `quantity`,
 1 AS `min_quantity`,
 1 AS `max_quantity`,
 1 AS `inventory_status`,
 1 AS `batch_number`,
 1 AS `production_date`,
 1 AS `expiry_date`,
 1 AS `days_to_expire`,
 1 AS `expiry_status`,
 1 AS `storage_location`,
 1 AS `stock_status`,
 1 AS `remark`,
 1 AS `created_time`,
 1 AS `updated_time`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `v_warehouse_inventory_overview`
--

DROP TABLE IF EXISTS `v_warehouse_inventory_overview`;
/*!50001 DROP VIEW IF EXISTS `v_warehouse_inventory_overview`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_warehouse_inventory_overview` AS SELECT 
 1 AS `prod_unit_id`,
 1 AS `warehouse_code`,
 1 AS `warehouse_name`,
 1 AS `total_items`,
 1 AS `category_count`,
 1 AS `total_quantity`,
 1 AS `low_stock_items`,
 1 AS `expiring_items`,
 1 AS `last_update`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `work_order`
--

DROP TABLE IF EXISTS `work_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `work_order` (
  `work_order_id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '工单唯一标识',
  `work_order_code` varchar(50) NOT NULL COMMENT '工单编号（唯一性约束）',
  `work_order_name` varchar(100) NOT NULL COMMENT '工单名称',
  `preparation_id` bigint unsigned NOT NULL COMMENT '制剂ID',
  `preparation_code` varchar(50) NOT NULL COMMENT '制剂编码',
  `preparation_name` varchar(100) NOT NULL COMMENT '制剂名称',
  `batch_qty` decimal(18,3) DEFAULT NULL COMMENT '批量',
  `producer` varchar(100) DEFAULT NULL COMMENT '生产单位',
  `receiver` varchar(100) DEFAULT NULL COMMENT '收货单位',
  `delivery_time` datetime DEFAULT NULL COMMENT '交付时间',
  `invoice_price` decimal(18,3) DEFAULT NULL COMMENT '开票单价',
  `insurance_price` decimal(18,3) DEFAULT NULL COMMENT '医保单价',
  `settlement_price` decimal(18,3) DEFAULT NULL COMMENT '结算单价',
  `batch_number` varchar(50) DEFAULT NULL COMMENT '批号',
  `outbound_qty` decimal(18,3) DEFAULT NULL COMMENT '出库量',
  `receipt_amount` decimal(18,3) DEFAULT NULL COMMENT '收款金额',
  `actual_receipt_amount` decimal(18,3) DEFAULT NULL COMMENT '实收款',
  `invoice_amount` decimal(18,3) DEFAULT NULL COMMENT '开票金额',
  `settlement_amount` decimal(18,3) DEFAULT NULL COMMENT '结算金额',
  `return_amount` decimal(18,3) DEFAULT NULL COMMENT '返款金额',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已删除',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `created_by` bigint unsigned NOT NULL COMMENT '创建人ID',
  `updated_by` bigint unsigned NOT NULL COMMENT '更新人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`work_order_id`),
  UNIQUE KEY `uk_work_order_code` (`work_order_code`),
  KEY `fk_work_order_preparation` (`preparation_id`),
  KEY `fk_work_order_created_by` (`created_by`),
  KEY `fk_work_order_updated_by` (`updated_by`),
  KEY `idx_work_order_code` (`work_order_code`),
  KEY `idx_preparation_code` (`preparation_code`),
  KEY `idx_delivery_time` (`delivery_time`),
  KEY `idx_created_time` (`created_time`),
  CONSTRAINT `fk_work_order_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_work_order_preparation` FOREIGN KEY (`preparation_id`) REFERENCES `preparation` (`preparation_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_work_order_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=193 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'erp_db'
--
/*!50003 DROP PROCEDURE IF EXISTS `ConfirmStockIn` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`%` PROCEDURE `ConfirmStockIn`(IN p_in_id BIGINT UNSIGNED, IN p_operator_id BIGINT UNSIGNED)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            ROLLBACK;
            RESIGNAL;
        END;

    START TRANSACTION;

    -- 检查库存是否足够（对于出库才需要，入库不需要）
    -- 更新入库单状态
    UPDATE stock_in
    SET in_status = 'completed',
        updated_by = p_operator_id,
        updated_time = NOW()
    WHERE in_id = p_in_id AND in_status = 'confirmed';

    COMMIT;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `ConfirmStockOut` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`%` PROCEDURE `ConfirmStockOut`(IN p_out_id BIGINT UNSIGNED, IN p_operator_id BIGINT UNSIGNED)
BEGIN
    DECLARE insufficient_stock INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
        BEGIN
            ROLLBACK;
            RESIGNAL;
        END;

    START TRANSACTION;

    -- 检查库存是否足够
    SELECT COUNT(*) INTO insufficient_stock
    FROM stock_out_detail sod
             JOIN stock s ON sod.stock_id = s.stock_id
    WHERE sod.out_id = p_out_id
      AND s.quantity < sod.quantity;

    IF insufficient_stock > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '库存不足，无法出库';
    END IF;

    -- 更新出库单状态
    UPDATE stock_out
    SET out_status = 'completed',
        updated_by = p_operator_id,
        updated_time = NOW()
    WHERE out_id = p_out_id AND out_status = 'confirmed';

    COMMIT;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `GenerateStockInCode` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`%` PROCEDURE `GenerateStockInCode`(OUT in_code VARCHAR(50))
BEGIN
    DECLARE today_date DATE;
    DECLARE seq_num INT;

    SET today_date = CURDATE();

    -- 获取或创建序列号
    INSERT INTO stock_in_sequence (seq_date, seq_number)
    VALUES (today_date, 1)
    ON DUPLICATE KEY UPDATE seq_number = seq_number + 1;

    SELECT seq_number INTO seq_num FROM stock_in_sequence WHERE seq_date = today_date;

    SET in_code = CONCAT('IN', DATE_FORMAT(today_date, '%Y%m%d'), LPAD(seq_num, 4, '0'));
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 DROP PROCEDURE IF EXISTS `GenerateStockOutCode` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`root`@`%` PROCEDURE `GenerateStockOutCode`(OUT out_code VARCHAR(50))
BEGIN
    DECLARE today_date DATE;
    DECLARE seq_num INT;

    SET today_date = CURDATE();

    INSERT INTO stock_out_sequence (seq_date, seq_number)
    VALUES (today_date, 1)
    ON DUPLICATE KEY UPDATE seq_number = seq_number + 1;

    SELECT seq_number INTO seq_num FROM stock_out_sequence WHERE seq_date = today_date;

    SET out_code = CONCAT('OUT', DATE_FORMAT(today_date, '%Y%m%d'), LPAD(seq_num, 4, '0'));
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Final view structure for view `v_category_inventory_summary`
--

/*!50001 DROP VIEW IF EXISTS `v_category_inventory_summary`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_category_inventory_summary` AS select `pu`.`prod_unit_name` AS `warehouse_name`,`s`.`category_name` AS `category_name`,`s`.`item_type` AS `item_type`,count(`s`.`stock_id`) AS `item_count`,sum(`s`.`quantity`) AS `total_quantity`,avg(`s`.`quantity`) AS `avg_quantity`,min(`s`.`quantity`) AS `min_quantity`,max(`s`.`quantity`) AS `max_quantity` from (`stock` `s` join `production_unit` `pu` on((`s`.`prod_unit_id` = `pu`.`prod_unit_id`))) where (`s`.`stock_status` = 'normal') group by `pu`.`prod_unit_name`,`s`.`category_name`,`s`.`item_type` order by `pu`.`prod_unit_name`,`s`.`category_name` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_inventory_alerts`
--

/*!50001 DROP VIEW IF EXISTS `v_inventory_alerts`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_inventory_alerts` AS select `pu`.`prod_unit_name` AS `warehouse_name`,`s`.`item_code` AS `item_code`,`s`.`item_name` AS `item_name`,`s`.`category_name` AS `category_name`,`s`.`unit_name` AS `unit_name`,`s`.`quantity` AS `quantity`,`s`.`min_quantity` AS `min_quantity`,`s`.`max_quantity` AS `max_quantity`,`s`.`batch_number` AS `batch_number`,`s`.`expiry_date` AS `expiry_date`,(case when (`s`.`quantity` <= `s`.`min_quantity`) then '库存不足' when (`s`.`quantity` >= `s`.`max_quantity`) then '库存过剩' when (`s`.`expiry_date` <= curdate()) then '已过期' when (`s`.`expiry_date` <= (curdate() + interval 30 day)) then '即将过期' else '正常' end) AS `alert_type`,(case when (`s`.`quantity` <= `s`.`min_quantity`) then concat('当前库存',`s`.`quantity`,'低于最低库存',`s`.`min_quantity`) when (`s`.`quantity` >= `s`.`max_quantity`) then concat('当前库存',`s`.`quantity`,'超过最高库存',`s`.`max_quantity`) when (`s`.`expiry_date` <= curdate()) then concat('产品已于',`s`.`expiry_date`,'过期') when (`s`.`expiry_date` <= (curdate() + interval 30 day)) then concat('产品将于',`s`.`expiry_date`,'过期') else '库存正常' end) AS `alert_message` from (`stock` `s` join `production_unit` `pu` on((`s`.`prod_unit_id` = `pu`.`prod_unit_id`))) where ((`s`.`stock_status` = 'normal') and ((`s`.`quantity` <= `s`.`min_quantity`) or (`s`.`quantity` >= `s`.`max_quantity`) or (`s`.`expiry_date` <= (curdate() + interval 30 day)))) order by `pu`.`prod_unit_name`,(case when (`s`.`quantity` <= `s`.`min_quantity`) then '库存不足' when (`s`.`quantity` >= `s`.`max_quantity`) then '库存过剩' when (`s`.`expiry_date` <= curdate()) then '已过期' when (`s`.`expiry_date` <= (curdate() + interval 30 day)) then '即将过期' else '正常' end),`s`.`item_name` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_stock_by_warehouse`
--

/*!50001 DROP VIEW IF EXISTS `v_stock_by_warehouse`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_stock_by_warehouse` AS select `pu`.`prod_unit_id` AS `prod_unit_id`,`pu`.`prod_unit_name` AS `warehouse_name`,`s`.`stock_id` AS `stock_id`,`s`.`item_type` AS `item_type`,`s`.`item_code` AS `item_code`,`s`.`item_name` AS `item_name`,`s`.`category_name` AS `category_name`,`s`.`unit_name` AS `unit_name`,`s`.`quantity` AS `quantity`,`s`.`min_quantity` AS `min_quantity`,`s`.`max_quantity` AS `max_quantity`,`s`.`batch_number` AS `batch_number`,`s`.`production_date` AS `production_date`,`s`.`expiry_date` AS `expiry_date`,`s`.`storage_location` AS `storage_location`,`s`.`remark` AS `remark`,`s`.`created_time` AS `created_time`,`s`.`updated_time` AS `updated_time`,(case when (`s`.`quantity` <= `s`.`min_quantity`) then '低于最低库存' when (`s`.`quantity` >= `s`.`max_quantity`) then '超过最高库存' else '库存正常' end) AS `inventory_status`,(case when (`s`.`expiry_date` is null) then '无有效期' when (`s`.`expiry_date` <= curdate()) then '已过期' when (`s`.`expiry_date` <= (curdate() + interval 30 day)) then '即将过期' else '有效期正常' end) AS `expiry_status` from (`stock` `s` join `production_unit` `pu` on((`s`.`prod_unit_id` = `pu`.`prod_unit_id`))) where (`s`.`stock_status` = 'normal') */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_stock_summary`
--

/*!50001 DROP VIEW IF EXISTS `v_stock_summary`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_stock_summary` AS select `s`.`stock_id` AS `stock_id`,`s`.`prod_unit_id` AS `prod_unit_id`,`pu`.`prod_unit_code` AS `warehouse_code`,`pu`.`prod_unit_name` AS `warehouse_name`,`s`.`item_type` AS `item_type`,`s`.`item_id` AS `item_id`,`s`.`item_code` AS `item_code`,`s`.`item_name` AS `item_name`,`s`.`category_name` AS `category_name`,`s`.`unit_name` AS `unit_name`,`s`.`quantity` AS `quantity`,`s`.`min_quantity` AS `min_quantity`,`s`.`max_quantity` AS `max_quantity`,(case when (`s`.`quantity` <= `s`.`min_quantity`) then '低于最低库存' when (`s`.`quantity` >= `s`.`max_quantity`) then '超过最高库存' else '库存正常' end) AS `inventory_status`,`s`.`batch_number` AS `batch_number`,`s`.`production_date` AS `production_date`,`s`.`expiry_date` AS `expiry_date`,(to_days(`s`.`expiry_date`) - to_days(curdate())) AS `days_to_expire`,(case when (`s`.`expiry_date` is null) then '无有效期' when ((to_days(`s`.`expiry_date`) - to_days(curdate())) <= 0) then '已过期' when ((to_days(`s`.`expiry_date`) - to_days(curdate())) <= 30) then '即将过期(30天内)' else '有效期正常' end) AS `expiry_status`,`s`.`storage_location` AS `storage_location`,`s`.`stock_status` AS `stock_status`,`s`.`remark` AS `remark`,`s`.`created_time` AS `created_time`,`s`.`updated_time` AS `updated_time` from (`stock` `s` join `production_unit` `pu` on((`s`.`prod_unit_id` = `pu`.`prod_unit_id`))) where (`s`.`stock_status` = 'normal') */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_warehouse_inventory_overview`
--

/*!50001 DROP VIEW IF EXISTS `v_warehouse_inventory_overview`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `v_warehouse_inventory_overview` AS select `pu`.`prod_unit_id` AS `prod_unit_id`,`pu`.`prod_unit_code` AS `warehouse_code`,`pu`.`prod_unit_name` AS `warehouse_name`,count(`s`.`stock_id`) AS `total_items`,count(distinct `s`.`category_name`) AS `category_count`,sum(`s`.`quantity`) AS `total_quantity`,count((case when (`s`.`quantity` <= `s`.`min_quantity`) then 1 end)) AS `low_stock_items`,count((case when (`s`.`expiry_date` <= (curdate() + interval 30 day)) then 1 end)) AS `expiring_items`,max(`s`.`updated_time`) AS `last_update` from (`production_unit` `pu` left join `stock` `s` on(((`pu`.`prod_unit_id` = `s`.`prod_unit_id`) and (`s`.`stock_status` = 'normal')))) where (`pu`.`prod_unit_status` = 1) group by `pu`.`prod_unit_id`,`pu`.`prod_unit_code`,`pu`.`prod_unit_name` order by `pu`.`prod_unit_name` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-01 10:17:24
