package com.kjd.reimbursement.exception;

public enum ErrorCode {

    PARAM_ERROR(400, "请求参数错误"),
    MAIN_DATA_EMPTY(1001, "主单数据不能为空"),
    REIMBURSEMENT_ID_EMPTY(1002, "报销单ID不能为空"),
    REIMBURSEMENT_NOT_FOUND(1003, "报销单不存在"),
    ITINERARY_DUPLICATE(1004, "行程重复"),
    DATE_INVALID(1005, "日期不合法"),
    AMOUNT_CHECK_FAILED(1006, "金额校验失败"),
    ID_GENERATE_FAILED(1007, "ID生成失败"),
    SYSTEM_ERROR(500, "系统内部错误");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
