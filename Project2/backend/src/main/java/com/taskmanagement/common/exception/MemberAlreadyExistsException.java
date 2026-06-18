package com.taskmanagement.common.exception;

public class MemberAlreadyExistsException extends BusinessException {

    public MemberAlreadyExistsException() {
        super(409, "该用户已在团队中");
    }
}
