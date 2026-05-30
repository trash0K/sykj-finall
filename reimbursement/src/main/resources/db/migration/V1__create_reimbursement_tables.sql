-- 报销单主表
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

-- 补录行程表
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补录行程表';

-- 补助信息表
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补助信息表';

-- 补助日历表
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补助日历表';

-- 费用归属及分摊表
DROP TABLE IF EXISTS `fk_reim_allocation`;
CREATE TABLE `fk_reim_allocation` (
                                      `id` varchar(32) NOT NULL COMMENT '主键ID',
                                      `main_id` varchar(32) DEFAULT NULL COMMENT '主表ID，关联报销单主表主键ID',
                                      `attribution_id` varchar(50) DEFAULT NULL COMMENT '费用归属ID',
                                      `attribution_name` varchar(100) DEFAULT NULL COMMENT '费用归属名称',
                                      `project_id` varchar(50) DEFAULT NULL COMMENT '项目ID',
                                      `project_no` varchar(50) DEFAULT NULL COMMENT '项目编号',
                                      `project_name` varchar(100) DEFAULT NULL COMMENT '项目名称',
                                      `allocation_ratio` varchar(20) DEFAULT NULL COMMENT '分摊比例',
                                      `allocation_amount` varchar(20) DEFAULT NULL COMMENT '分摊金额',
                                      PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用归属及分摊表';