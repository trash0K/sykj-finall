package com.kjd.reimbursement.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 主键生成器：RCBX + 年月日(8位) + 三位数序号(001~999)
 * 例：RCBX20260525001, RCBX20260525002
 */
@Component
public class IdGenerator {

    private static final String PREFIX = "RCBX";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 生成下一个ID
     * @param service 对应实体的IService
     * @return 新ID
     */
    public <T> String nextId(IService<T> service) {
        String today = LocalDate.now().format(DATE_FMT);
        String prefix = PREFIX + today;

        // 查询当日最大ID: where id like 'RCBX20260525%' order by id desc limit 1
        String maxId = service.getBaseMapper().selectObjs(
                new LambdaQueryWrapper<T>()
                        .apply("id like {0}", prefix + "%")
                        .last("order by id desc limit 1")
        ).stream().findFirst().map(Object::toString).orElse(null);

        int seq = 1;
        if (maxId != null && maxId.length() == prefix.length() + 3) {
            try {
                seq = Integer.parseInt(maxId.substring(prefix.length())) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }

        if (seq > 999) {
            throw new RuntimeException("当日ID序号已超上限(999)，请稍后重试");
        }

        return prefix + String.format("%03d", seq);
    }
}
