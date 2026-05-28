package com.kjd.reimbursement.controller;

import com.kjd.reimbursement.exception.BusinessException;
import com.kjd.reimbursement.exception.ErrorCode;
import com.kjd.reimbursement.pojo.vo.PageResult;
import com.kjd.reimbursement.pojo.vo.Result;
import com.kjd.reimbursement.service.FkReimMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        PageResult result = fkReimMainService.getReimbursementList(page, size, id, reimbursementTitle, reimburserName, reimDepartmentName, reimCompanyName, businessTripReason, businessTypeName);
        return Result.success(result);
    }

    /**
     * 获取报销单详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable String id) {
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
        try {
            String newId = fkReimMainService.copyReimbursement(id);
            return Result.success(newId);
        } catch (RuntimeException e) {
            return handleException(e);
        }
    }

    private <T> Result<T> handleException(RuntimeException e) {
        if (e instanceof BusinessException businessException) {
            return Result.error(businessException.getCode(), businessException.getMessage());
        }
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage());
    }
}
