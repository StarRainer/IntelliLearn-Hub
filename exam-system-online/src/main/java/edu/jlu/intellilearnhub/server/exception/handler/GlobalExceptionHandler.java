package edu.jlu.intellilearnhub.server.exception.handler;

import dev.langchain4j.exception.HttpException;
import edu.jlu.intellilearnhub.server.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpException.class)
    public Result<String> handleLangChainHttpError(HttpException e) {
        log.error("AI 模型调用出错: code={}, msg={}", e.statusCode(), e.getMessage());

        if (e.statusCode() == 401) {
            return Result.error("系统配置错误：API Key 无效");
        }
        if (e.statusCode() == 429) {
            return Result.error("请求太频繁或余额不足");
        }
        return Result.error("AI 服务繁忙，请稍后再试");
    }


    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("代码出现异常，异常信息为：{}", e.getMessage(), e);
        return Result.error(e.getMessage());
    }
}
