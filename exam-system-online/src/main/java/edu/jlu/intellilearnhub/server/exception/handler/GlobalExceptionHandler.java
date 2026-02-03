package edu.jlu.intellilearnhub.server.exception.handler;

import edu.jlu.intellilearnhub.server.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("代码出现异常，异常信息为：{}", e.getMessage(), e);
        return Result.error(e.getMessage());
    }
}
