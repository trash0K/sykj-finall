-- ===========================================================
-- Navicat one-click initialization script for sykj_reim
-- Target: MySQL 8.x
-- Includes: latest table structure + demo data
-- Tables:
--   fk_reim_main
--   fk_reim_itinerary
--   fk_reim_subsidy
--   fk_subsidy_calendar
--   fk_reim_allocation
-- ===========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `sykj_reim`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `sykj_reim`;

DROP TABLE IF EXISTS `fk_subsidy_calendar`;
DROP TABLE IF EXISTS `fk_reim_allocation`;
DROP TABLE IF EXISTS `fk_reim_subsidy`;
DROP TABLE IF EXISTS `fk_reim_itinerary`;
DROP TABLE IF EXISTS `fk_reim_main`;

-- ===========================================================
-- 1. 报销单主表
-- ===========================================================
CREATE TABLE `fk_reim_main` (
  `id` varchar(32) NOT NULL COMMENT '主键ID/报销单号',
  `creation_time` varchar(32) DEFAULT NULL COMMENT '创建时间',
  `reimbursement_title` varchar(500) DEFAULT NULL COMMENT '报销标题',
  `reimburser_id` varchar(32) DEFAULT NULL COMMENT '报销人ID',
  `reimburser_no` varchar(20) DEFAULT NULL COMMENT '报销人工号',
  `reimburser_name` varchar(20) DEFAULT NULL COMMENT '报销人姓名',
  `reim_department_id` varchar(32) DEFAULT NULL COMMENT '报销部门ID',
  `reim_department_no` varchar(20) DEFAULT NULL COMMENT '报销部门编号',
  `reim_department_name` varchar(50) DEFAULT NULL COMMENT '报销部门名称',
  `reim_company_id` varchar(32) DEFAULT NULL COMMENT '费用归属公司ID',
  `reim_company_no` varchar(20) DEFAULT NULL COMMENT '费用归属公司编号',
  `reim_company_name` varchar(100) DEFAULT NULL COMMENT '费用归属公司名称',
  `business_type_id` varchar(32) DEFAULT NULL COMMENT '业务类型ID',
  `business_type_no` varchar(32) DEFAULT NULL COMMENT '业务类型编号',
  `business_type_name` varchar(50) DEFAULT NULL COMMENT '业务类型名称',
  `business_trip_reason` varchar(500) DEFAULT NULL COMMENT '出差事由',
  `subsidy_total` varchar(20) DEFAULT '0.00' COMMENT '补助总金额',
  `meal_allowance` varchar(20) DEFAULT '0.00' COMMENT '餐费补助',
  `transportation_allowance` varchar(20) DEFAULT '0.00' COMMENT '交通补助',
  `phone_allowance` varchar(20) DEFAULT '0.00' COMMENT '通讯补助',
  `doc_status` varchar(10) DEFAULT '0' COMMENT '单据状态: 0=草稿, 1=已完成, 2=已作废',
  `doc_type` varchar(50) DEFAULT '日常报销单' COMMENT '单据类型',
  `remarks` varchar(1000) DEFAULT NULL COMMENT '备注信息',
  PRIMARY KEY (`id`),
  KEY `idx_creation_time` (`creation_time`),
  KEY `idx_doc_status` (`doc_status`),
  KEY `idx_reimburser_name` (`reimburser_name`),
  KEY `idx_reim_company_name` (`reim_company_name`),
  KEY `idx_business_type_name` (`business_type_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报销单主表';

-- ===========================================================
-- 2. 补录行程表
-- ===========================================================
CREATE TABLE `fk_reim_itinerary` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `main_id` varchar(32) NOT NULL COMMENT '主表ID，关联报销单主表主键ID',
  `traveler_id` varchar(32) DEFAULT NULL COMMENT '出行人ID',
  `traveler_no` varchar(32) DEFAULT NULL COMMENT '出行人工号',
  `traveler_name` varchar(20) DEFAULT NULL COMMENT '出行人姓名',
  `departure_date` varchar(20) DEFAULT NULL COMMENT '出发日期',
  `arrival_date` varchar(20) DEFAULT NULL COMMENT '到达日期',
  `departure_city` varchar(20) DEFAULT NULL COMMENT '出发城市',
  `departure_city_no` varchar(20) DEFAULT NULL COMMENT '出发城市编号',
  `arriving_city` varchar(20) DEFAULT NULL COMMENT '到达城市',
  `arriving_city_no` varchar(20) DEFAULT NULL COMMENT '到达城市编号',
  `itinerary_instructions` varchar(500) DEFAULT NULL COMMENT '行程说明',
  PRIMARY KEY (`id`),
  KEY `idx_main_id` (`main_id`),
  KEY `idx_traveler_date` (`traveler_id`, `departure_date`, `arrival_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='补录行程表';

-- ===========================================================
-- 3. 补助信息表
-- ===========================================================
CREATE TABLE `fk_reim_subsidy` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `main_id` varchar(32) NOT NULL COMMENT '主表ID，关联报销单主表主键ID',
  `traveler_id` varchar(32) DEFAULT NULL COMMENT '出行人ID',
  `traveler_no` varchar(20) DEFAULT NULL COMMENT '出行人工号',
  `traveler_name` varchar(20) DEFAULT NULL COMMENT '出行人姓名',
  `departure_date` varchar(20) DEFAULT NULL COMMENT '出发日期',
  `arrival_date` varchar(20) DEFAULT NULL COMMENT '到达日期',
  `subsidy_days` varchar(20) DEFAULT NULL COMMENT '补助天数',
  `departure_city` varchar(20) DEFAULT NULL COMMENT '出发城市',
  `departure_city_no` varchar(20) DEFAULT NULL COMMENT '出发城市编号',
  `arriving_city` varchar(20) DEFAULT NULL COMMENT '到达城市',
  `arriving_city_no` varchar(20) DEFAULT NULL COMMENT '到达城市编号',
  `application_amount` varchar(20) DEFAULT '0.00' COMMENT '申请金额',
  `subsidy_amount` varchar(20) DEFAULT '0.00' COMMENT '补助金额',
  `meal_allowance` varchar(20) DEFAULT '0.00' COMMENT '餐费补助',
  `transportation_allowance` varchar(20) DEFAULT '0.00' COMMENT '交通补助',
  `phone_allowance` varchar(20) DEFAULT '0.00' COMMENT '通讯补助',
  `business_type_id` varchar(32) DEFAULT NULL COMMENT '业务类型ID',
  `business_type_no` varchar(32) DEFAULT NULL COMMENT '业务类型编号',
  `business_type_name` varchar(50) DEFAULT NULL COMMENT '业务类型名称',
  PRIMARY KEY (`id`),
  KEY `idx_main_id` (`main_id`),
  KEY `idx_traveler_date` (`traveler_id`, `departure_date`, `arrival_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='补助信息表';

-- ===========================================================
-- 4. 补助日历表
-- main_id 关联 fk_reim_subsidy.id
-- ===========================================================
CREATE TABLE `fk_subsidy_calendar` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `main_id` varchar(32) NOT NULL COMMENT '主表ID，关联补助信息表主键ID',
  `travel_date` varchar(20) NOT NULL COMMENT '出差日期',
  `travel_date_week` varchar(32) DEFAULT NULL COMMENT '出差日期星期',
  `subsidized_cities` varchar(32) DEFAULT NULL COMMENT '补助城市',
  `subsidized_city_number` varchar(32) DEFAULT NULL COMMENT '补助城市编号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `standard_meal_expenses_amount` varchar(32) DEFAULT '0.00' COMMENT '餐费标准金额',
  `standard_traffic_amount` varchar(32) DEFAULT '0.00' COMMENT '交通标准金额',
  `standard_communication_amount` varchar(32) DEFAULT '0.00' COMMENT '通讯标准金额',
  `meal_expenses_amount` varchar(32) DEFAULT '0.00' COMMENT '餐费金额',
  `traffic_amount` varchar(32) DEFAULT '0.00' COMMENT '交通金额',
  `communication_amount` varchar(32) DEFAULT '0.00' COMMENT '通讯金额',
  `is_reimbursed` varchar(32) DEFAULT '1' COMMENT '是否报销，控制复选框是否选中',
  PRIMARY KEY (`id`),
  KEY `idx_main_id` (`main_id`),
  KEY `idx_travel_date` (`travel_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='补助日历表';

-- ===========================================================
-- 5. 费用归属及分摊表
-- ===========================================================
CREATE TABLE `fk_reim_allocation` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `main_id` varchar(32) DEFAULT NULL COMMENT '主表ID，关联报销单主表主键ID',
  `attribution_id` varchar(50) DEFAULT NULL COMMENT '费用归属ID',
  `attribution_name` varchar(100) DEFAULT NULL COMMENT '费用归属名称',
  `project_id` varchar(50) DEFAULT NULL COMMENT '项目ID',
  `project_no` varchar(50) DEFAULT NULL COMMENT '项目编号',
  `project_name` varchar(100) DEFAULT NULL COMMENT '项目名称',
  `allocation_ratio` varchar(20) DEFAULT '0.00' COMMENT '分摊比例',
  `allocation_amount` varchar(20) DEFAULT '0.00' COMMENT '分摊金额',
  PRIMARY KEY (`id`),
  KEY `idx_main_id` (`main_id`),
  KEY `idx_attribution_id` (`attribution_id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='费用归属及分摊表';

-- ===========================================================
-- Demo data
-- 说明:
--   北京为一线城市，餐补100/天，交通40/天，通讯40/天。
--   上海为一线城市，餐补100/天，交通40/天，通讯40/天。
--   杭州为二线城市，餐补80/天，交通40/天，通讯40/天。
-- ===========================================================

INSERT INTO `fk_reim_main` (
  `id`, `creation_time`, `reimbursement_title`, `reimburser_id`, `reimburser_no`, `reimburser_name`,
  `reim_department_id`, `reim_department_no`, `reim_department_name`,
  `reim_company_id`, `reim_company_no`, `reim_company_name`,
  `business_type_id`, `business_type_no`, `business_type_name`,
  `business_trip_reason`, `subsidy_total`, `meal_allowance`, `transportation_allowance`, `phone_allowance`,
  `doc_status`, `doc_type`, `remarks`
) VALUES
(
  'RCBX20260609001', '2026-06-09', '徐年年项目出差差旅报销单',
  '13AB3A3F72409002', '74541', '徐年年',
  '14515BB4BFB92003', '072003', '企业费控事业部',
  '1C61686865DA8000', '0409', '胜意科技武汉分公司',
  '1B5FEB7DD4396000', '10010010101', '项目出差',
  '客户拜访及项目交付', '900.00', '500.00', '200.00', '200.00',
  '0', '日常报销单', '演示数据：草稿单据，可编辑后保存。'
),
(
  'RCBX20260609002', '2026-06-09', '郑雨雪市场拓展差旅报销单',
  '13AB498CC6409002', '74008', '郑雨雪',
  '14055D22BB808001', '072007', '营销事业部',
  '19218A262C976000', '0408', '胜意科技上海分公司',
  '1A92E43082EFC000', '10010010102', '市场拓展出差',
  '华东客户拜访及方案沟通', '480.00', '240.00', '120.00', '120.00',
  '1', '日常报销单', '演示数据：已完成单据，用于查看状态。'
),
(
  'RCBX20260609003', '2026-06-09', '王成军售后维护差旅报销单',
  '13AB591FE8009002', '80681', '王成军',
  '13AB8D7B52A9B002', '072001', '客户成功事业部',
  '1C54557F1782E000', '0407', '胜意科技北京分公司',
  '13AB3A4154008001', '10010010202', '售后维护出差',
  '客户现场售后维护', '360.00', '200.00', '80.00', '80.00',
  '2', '日常报销单', '演示数据：已作废单据，用于查看作废状态。'
);

INSERT INTO `fk_reim_itinerary` (
  `id`, `main_id`, `traveler_id`, `traveler_no`, `traveler_name`,
  `departure_date`, `arrival_date`, `departure_city`, `departure_city_no`, `arriving_city`, `arriving_city_no`,
  `itinerary_instructions`
) VALUES
(
  'RCBX20260609004', 'RCBX20260609001',
  '13AB3A3F72409002', '74541', '徐年年',
  '2026-06-01', '2026-06-05', '武汉', '10458', '北京', '10119',
  '客户现场支持和项目交付'
),
(
  'RCBX20260609005', 'RCBX20260609002',
  '13AB498CC6409002', '74008', '郑雨雪',
  '2026-06-03', '2026-06-05', '上海', '10621', '杭州', '10216',
  '客户方案沟通'
),
(
  'RCBX20260609006', 'RCBX20260609003',
  '13AB591FE8009002', '80681', '王成军',
  '2026-06-07', '2026-06-08', '北京', '10119', '上海', '10621',
  '售后维护支持'
);

INSERT INTO `fk_reim_subsidy` (
  `id`, `main_id`, `traveler_id`, `traveler_no`, `traveler_name`,
  `departure_date`, `arrival_date`, `subsidy_days`,
  `departure_city`, `departure_city_no`, `arriving_city`, `arriving_city_no`,
  `application_amount`, `subsidy_amount`, `meal_allowance`, `transportation_allowance`, `phone_allowance`,
  `business_type_id`, `business_type_no`, `business_type_name`
) VALUES
(
  'RCBX20260609007', 'RCBX20260609001',
  '13AB3A3F72409002', '74541', '徐年年',
  '2026-06-01', '2026-06-05', '5',
  '武汉', '10458', '北京', '10119',
  '900.00', '900.00', '500.00', '200.00', '200.00',
  '1B5FEB7DD4396000', '10010010101', '项目出差'
),
(
  'RCBX20260609008', 'RCBX20260609002',
  '13AB498CC6409002', '74008', '郑雨雪',
  '2026-06-03', '2026-06-05', '3',
  '上海', '10621', '杭州', '10216',
  '480.00', '480.00', '240.00', '120.00', '120.00',
  '1A92E43082EFC000', '10010010102', '市场拓展出差'
),
(
  'RCBX20260609009', 'RCBX20260609003',
  '13AB591FE8009002', '80681', '王成军',
  '2026-06-07', '2026-06-08', '2',
  '北京', '10119', '上海', '10621',
  '360.00', '360.00', '200.00', '80.00', '80.00',
  '13AB3A4154008001', '10010010202', '售后维护出差'
);

INSERT INTO `fk_subsidy_calendar` (
  `id`, `main_id`, `travel_date`, `travel_date_week`, `subsidized_cities`, `subsidized_city_number`, `remark`,
  `standard_meal_expenses_amount`, `standard_traffic_amount`, `standard_communication_amount`,
  `meal_expenses_amount`, `traffic_amount`, `communication_amount`, `is_reimbursed`
) VALUES
('RCBX20260609010', 'RCBX20260609007', '2026-06-01', '星期一', '北京', '10119', NULL, '100.00', '40.00', '40.00', '100.00', '40.00', '40.00', '1'),
('RCBX20260609011', 'RCBX20260609007', '2026-06-02', '星期二', '北京', '10119', NULL, '100.00', '40.00', '40.00', '100.00', '40.00', '40.00', '1'),
('RCBX20260609012', 'RCBX20260609007', '2026-06-03', '星期三', '北京', '10119', NULL, '100.00', '40.00', '40.00', '100.00', '40.00', '40.00', '1'),
('RCBX20260609013', 'RCBX20260609007', '2026-06-04', '星期四', '北京', '10119', NULL, '100.00', '40.00', '40.00', '100.00', '40.00', '40.00', '1'),
('RCBX20260609014', 'RCBX20260609007', '2026-06-05', '星期五', '北京', '10119', NULL, '100.00', '40.00', '40.00', '100.00', '40.00', '40.00', '1'),

('RCBX20260609015', 'RCBX20260609008', '2026-06-03', '星期三', '杭州', '10216', NULL, '80.00', '40.00', '40.00', '80.00', '40.00', '40.00', '1'),
('RCBX20260609016', 'RCBX20260609008', '2026-06-04', '星期四', '杭州', '10216', NULL, '80.00', '40.00', '40.00', '80.00', '40.00', '40.00', '1'),
('RCBX20260609017', 'RCBX20260609008', '2026-06-05', '星期五', '杭州', '10216', NULL, '80.00', '40.00', '40.00', '80.00', '40.00', '40.00', '1'),

('RCBX20260609018', 'RCBX20260609009', '2026-06-07', '星期日', '上海', '10621', NULL, '100.00', '40.00', '40.00', '100.00', '40.00', '40.00', '1'),
('RCBX20260609019', 'RCBX20260609009', '2026-06-08', '星期一', '上海', '10621', NULL, '100.00', '40.00', '40.00', '100.00', '40.00', '40.00', '1');

INSERT INTO `fk_reim_allocation` (
  `id`, `main_id`, `attribution_id`, `attribution_name`, `project_id`, `project_no`, `project_name`,
  `allocation_ratio`, `allocation_amount`
) VALUES
(
  'RCBX20260609020', 'RCBX20260609001',
  '1C61686865DA8000', '胜意科技武汉分公司',
  '1C811ABF96195000', 'centralChina', '华中客户定制化项目',
  '0.6000', '540.00'
),
(
  'RCBX20260609021', 'RCBX20260609001',
  '1C54557F1782E000', '胜意科技北京分公司',
  '1771EC45F2443000', 'northChina', '华北客户定制化项目',
  '0.4000', '360.00'
),
(
  'RCBX20260609022', 'RCBX20260609002',
  '19218A262C976000', '胜意科技上海分公司',
  '1762792DB4E9A002', 'eastChina', '华东客户定制化项目',
  '1.0000', '480.00'
),
(
  'RCBX20260609023', 'RCBX20260609003',
  '1C54557F1782E000', '胜意科技北京分公司',
  '12BC248B25083001', 'nonProjectRelated', '非项目类费用归集',
  '1.0000', '360.00'
);

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'sykj_reim latest schema and demo data initialized' AS `message`,
       (SELECT COUNT(*) FROM `fk_reim_main`) AS `main_count`,
       (SELECT COUNT(*) FROM `fk_reim_itinerary`) AS `itinerary_count`,
       (SELECT COUNT(*) FROM `fk_reim_subsidy`) AS `subsidy_count`,
       (SELECT COUNT(*) FROM `fk_subsidy_calendar`) AS `calendar_count`,
       (SELECT COUNT(*) FROM `fk_reim_allocation`) AS `allocation_count`;
