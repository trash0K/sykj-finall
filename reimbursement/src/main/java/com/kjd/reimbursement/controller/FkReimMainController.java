package com.kjd.reimbursement.controller;

import com.kjd.reimbursement.exception.BusinessException;
import com.kjd.reimbursement.exception.ErrorCode;
import com.kjd.reimbursement.pojo.vo.PageResult;
import com.kjd.reimbursement.pojo.vo.Result;
import com.kjd.reimbursement.service.FkReimMainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/reimbursement")
public class FkReimMainController {

    @Autowired
    private FkReimMainService fkReimMainService;

    /**
     * 获取报销单列表
     */
    @GetMapping("/list")
    public Result<PageResult> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String reimbursementTitle,
            @RequestParam(required = false) String reimburserName,
            @RequestParam(required = false) String reimDepartmentName,
            @RequestParam(required = false) String reimCompanyName,
            @RequestParam(required = false) String businessTripReason,
            @RequestParam(required = false) String businessTypeName) {
        log.info("查询报销单列表: page={}, size={}", page, size);
        PageResult result = fkReimMainService.getReimbursementList(page, size, id, reimbursementTitle, reimburserName, reimDepartmentName, reimCompanyName, businessTripReason, businessTypeName);
        return Result.success(result);
    }

    /**
     * 获取报销单详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable String id) {
        log.info("查询报销单详情: id={}", id);
        try {
            Map<String, Object> data = fkReimMainService.getReimbursementDetail(id);
            return Result.success(data);
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    /**
     * 保存报销单
     */
    @PostMapping("/save")
    public Result<String> save(@RequestBody Map<String, Object> params) {
        log.info("保存报销单");
        try {
            String id = fkReimMainService.saveReimbursement(params);
            return Result.success(id);
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    /**
     * 删除报销单
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        log.info("删除报销单: id={}", id);
        try {
            fkReimMainService.deleteReimbursement(id);
            return Result.success();
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    /**
     * 更新报销单
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody Map<String, Object> params) {
        log.info("更新报销单");
        try {
            String id = fkReimMainService.updateReimbursement(params);
            return Result.success(id);
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    /**
     * 复制报销单
     */
    @PostMapping("/{id}/copy")
    public Result<String> copy(@PathVariable String id) {
        log.info("复制报销单: id={}", id);
        try {
            String newId = fkReimMainService.copyReimbursement(id);
            return Result.success(newId);
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    /**
     * 提交报销单（状态改为已完成）
     */
    @PutMapping("/{id}/submit")
    public Result<String> submit(@PathVariable String id) {
        log.info("提交报销单: id={}", id);
        try {
            fkReimMainService.submitReimbursement(id);
            return Result.success();
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    /**
     * 作废报销单（状态改为已作废）
     */
    @PutMapping("/{id}/void")
    public Result<String> voidDoc(@PathVariable String id) {
        log.info("作废报销单: id={}", id);
        try {
            fkReimMainService.voidReimbursement(id);
            return Result.success();
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    private <T> Result<T> handleException(RuntimeException e) {
        if (e instanceof BusinessException businessException) {
            log.warn("业务异常: code={}, msg={}", businessException.getCode(), businessException.getMessage());
            return Result.error(businessException.getCode(), businessException.getMessage());
        }
        log.error("系统异常", e);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage());
    }
}
