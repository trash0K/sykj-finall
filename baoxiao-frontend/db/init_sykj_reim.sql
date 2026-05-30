-- ===========================================================
-- 数据库初始化脚本：sykj_reim（与 sykj-finall-main 后端对应）
-- 来源：reimbursement/src/main/resources/db/migration/V1__create_reimbursement_tables.sql
-- 用法（PowerShell 示例）：
--   mysql -uroot -p123456 < e:\JavaProject\baoxiao-frontend\db\init_sykj_reim.sql
-- ===========================================================

SET NAMES utf8mb4;

-- 1. 创建数据库
DROP DATABASE IF EXISTS `sykj_reim`;
CREATE DATABASE `sykj_reim`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `sykj_reim`;

-- 2. 报销单主表
DROP TABLE IF EXISTS `fk_reim_main`;
CREATE TABLE `fk_reim_main` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `creation_time` varchar(32) DEFAULT NULL COMMENT '创建时间',
  `reimbursement_title` varchar(32) DEFAULT NULL COMMENT '报销标题',
  `reimburser_id` varchar(32) DEFAULT NULL COMMENT '报销人ID',
  `reimburser_no` varchar(20) DEFAULT NULL COMMENT '报销人工号',
  `reimburser_name` varchar(20) DEFAULT NULL COMMENT '报销人姓名',
  `reim_department_id` varchar(20) DEFAULT NULL COMMENT '报销部门ID',
  `reim_department_no` varchar(20) DEFAULT NULL COMMENT '报销部门编号',
  `reim_department_name` varchar(20) DEFAULT NULL COMMENT '报销部门名称',
  `reim_company_id` varchar(20) DEFAULT NULL COMMENT '费用归属公司ID',
  `reim_company_no` varchar(20) DEFAULT NULL COMMENT '费用归属公司编号',
  `reim_company_name` varchar(20) DEFAULT NULL COMMENT '费用归属公司名称',
  `business_type_id` varchar(20) DEFAULT NULL COMMENT '业务类型ID',
  `business_type_no` varchar(20) DEFAULT NULL COMMENT '业务类型编号',
  `business_type_name` varchar(20) DEFAULT NULL COMMENT '业务类型名称',
  `business_trip_reason` varchar(20) DEFAULT NULL COMMENT '出差事由',
  `subsidy_total` varchar(20) DEFAULT NULL COMMENT '补助总金额',
  `meal_allowance` varchar(20) DEFAULT NULL COMMENT '餐费补助',
  `transportation_allowance` varchar(20) DEFAULT NULL COMMENT '交通补助',
  `phone_allowance` varchar(20) DEFAULT NULL COMMENT '通讯补助',
  `remarks` varchar(20) DEFAULT NULL COMMENT '备注信息',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报销单主表';

-- 3. 补录行程表
DROP TABLE IF EXISTS `fk_reim_itinerary`;
CREATE TABLE `fk_reim_itinerary` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `main_id` varchar(32) NOT NULL COMMENT '主表ID，关联报销单主表主键ID',
  `traveler_id` varchar(20) DEFAULT NULL COMMENT '出行人ID',
  `traveler_no` varchar(32) DEFAULT NULL COMMENT '出行人工号',
  `traveler_name` varchar(20) DEFAULT NULL COMMENT '出行人姓名',
  `departure_date` varchar(20) DEFAULT NULL COMMENT '出发日期',
  `arrival_date` varchar(20) DEFAULT NULL COMMENT '到达日期',
  `departure_city` varchar(20) DEFAULT NULL COMMENT '出发城市',
  `departure_city_no` varchar(20) DEFAULT NULL COMMENT '出发城市编号',
  `arriving_city` varchar(20) DEFAULT NULL COMMENT '到达城市',
  `arriving_city_no` varchar(20) DEFAULT NULL COMMENT '到达城市编号',
  `itinerary_instructions` varchar(20) DEFAULT NULL COMMENT '行程说明',
  PRIMARY KEY (`id`),
  KEY `idx_main_id` (`main_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补录行程表';

-- 4. 补助信息表
DROP TABLE IF EXISTS `fk_reim_subsidy`;
CREATE TABLE `fk_reim_subsidy` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `main_id` varchar(32) NOT NULL COMMENT '主表ID，关联报销单主表主键ID',
  `traveler_id` varchar(20) DEFAULT NULL COMMENT '出行人ID',
  `traveler_no` varchar(20) DEFAULT NULL COMMENT '出行人工号',
  `traveler_name` varchar(20) DEFAULT NULL COMMENT '出行人姓名',
  `departure_date` varchar(20) DEFAULT NULL COMMENT '出发日期',
  `arrival_date` varchar(20) DEFAULT NULL COMMENT '到达日期',
  `subsidy_days` varchar(20) DEFAULT NULL COMMENT '补助天数',
  `departure_city` varchar(20) DEFAULT NULL COMMENT '出发城市',
  `departure_city_no` varchar(20) DEFAULT NULL COMMENT '出发城市编号',
  `arriving_city` varchar(20) DEFAULT NULL COMMENT '到达城市',
  `arriving_city_no` varchar(20) DEFAULT NULL COMMENT '到达城市编号',
  `application_amount` varchar(20) DEFAULT NULL COMMENT '申请金额',
  `subsidy_amount` varchar(20) DEFAULT NULL COMMENT '补助金额',
  `meal_allowance` varchar(20) DEFAULT NULL COMMENT '餐费补助',
  `transportation_allowance` varchar(20) DEFAULT NULL COMMENT '交通补助',
  `phone_allowance` varchar(20) DEFAULT NULL COMMENT '通讯补助',
  `business_type_id` varchar(20) DEFAULT NULL COMMENT '业务类型ID',
  `business_type_no` varchar(20) DEFAULT NULL COMMENT '业务类型编号',
  `business_type_name` varchar(20) DEFAULT NULL COMMENT '业务类型名称',
  PRIMARY KEY (`id`),
  KEY `idx_main_id` (`main_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补助信息表';

-- 5. 补助日历表（main_id 关联补助信息表的 id）
DROP TABLE IF EXISTS `fk_subsidy_calendar`;
CREATE TABLE `fk_subsidy_calendar` (
  `id` varchar(32) NOT NULL COMMENT '主键ID',
  `main_id` varchar(32) NOT NULL COMMENT '主表ID，关联补助信息表主键ID',
  `travel_date` varchar(20) NOT NULL COMMENT '出差日期',
  `travel_date_week` varchar(32) DEFAULT NULL COMMENT '出差日期星期',
  `subsidized_cities` varchar(32) DEFAULT NULL COMMENT '补助城市',
  `subsidized_city_number` varchar(32) DEFAULT NULL COMMENT '补助城市编号',
  `remark` varchar(32) DEFAULT NULL COMMENT '备注',
  `standard_meal_expenses_amount` varchar(32) DEFAULT NULL COMMENT '餐费标准金额',
  `standard_traffic_amount` varchar(32) DEFAULT NULL COMMENT '交通标准金额',
  `standard_communication_amount` varchar(32) DEFAULT NULL COMMENT '通讯标准金额',
  `meal_expenses_amount` varchar(32) DEFAULT NULL COMMENT '餐费金额',
  `traffic_amount` varchar(32) DEFAULT NULL COMMENT '交通金额',
  `communication_amount` varchar(32) DEFAULT NULL COMMENT '通讯金额',
  `is_reimbursed` varchar(32) DEFAULT NULL COMMENT '是否报销，控制复选框是否选中',
  PRIMARY KEY (`id`),
  KEY `idx_main_id` (`main_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补助日历表';

-- ===========================================================
-- 演示数据（可选）：插入 1 条完整的报销单，方便联调列表/详情接口
-- 如不需要，删除以下 INSERT 语句即可
-- ===========================================================
INSERT INTO `fk_reim_main` VALUES (
  '20260529001', '2026-05-29', '徐年年项目出差差旅报销单',
  '13AB3A3F72409002', '202101497', '徐年年',
  '14515BB4BFB92003', '072003', '企业费控事业部',
  '1C61686865DA8000', '0409', '胜意科技武汉分公司',
  '1B5FEB7DD4396000', '10010010101', '项目出差',
  '客户拜访及项目交付', '900', '500', '200', '200', ''
);

INSERT INTO `fk_reim_itinerary` VALUES (
  '20260529001001', '20260529001',
  '13AB3A3F72409002', '202101497', '徐年年',
  '2026-05-20', '2026-05-24',
  '武汉', '10458', '北京', '10119', '客户现场支持'
);

INSERT INTO `fk_reim_subsidy` VALUES (
  '20260529001001', '20260529001',
  '13AB3A3F72409002', '202101497', '徐年年',
  '2026-05-20', '2026-05-24', '5',
  '武汉', '10458', '北京', '10119',
  '900', '900', '500', '200', '200',
  '1B5FEB7DD4396000', '10010010101', '项目出差'
);

INSERT INTO `fk_subsidy_calendar` VALUES
('20260529001001001', '20260529001001', '2026-05-20', '周三', '北京', '10119', NULL, '100', '40', '40', '100', '40', '40', '1'),
('20260529001001002', '20260529001001', '2026-05-21', '周四', '北京', '10119', NULL, '100', '40', '40', '100', '40', '40', '1'),
('20260529001001003', '20260529001001', '2026-05-22', '周五', '北京', '10119', NULL, '100', '40', '40', '100', '40', '40', '1'),
('20260529001001004', '20260529001001', '2026-05-23', '周六', '北京', '10119', NULL, '100', '40', '40', '100', '40', '40', '1'),
('20260529001001005', '20260529001001', '2026-05-24', '周日', '北京', '10119', NULL, '100', '40', '40', '100', '40', '40', '1');

-- 校验
SELECT '初始化完成，当前主表记录数：' AS info, COUNT(*) AS cnt FROM `fk_reim_main`;
