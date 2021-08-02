package com.bihu.trace.filter;

import com.alibaba.fastjson.JSON;
import com.bihu.common.base.exception.BusinessException;
import com.bihu.common.base.exception.ServerCode;
import com.bihu.common.base.response.CommonResponse;
import com.bihu.trace.constants.TraceConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

import java.lang.reflect.Method;

/**
 * 配置 @Activate 不需要 配置文件显示执行，默认加载
 * 配置order，即在最后
 * 功能：dubbo异常拦截器
 * @author zhongshenghua
 * @since 2021-05-31
 */
@Slf4j
@Activate(group = {CommonConstants.CONSUMER,CommonConstants.PROVIDER}, order = 30000)
public class DubboExceptionFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Result result = invoker.invoke(invocation);
        if (result.hasException()) {
            String msg = "系统繁忙，请稍后重试";
            if (result.getException() instanceof BusinessException) {
                log.debug("检测到BusinessException", result.getException());
                BusinessException se = (BusinessException) result.getException();
                msg = se.getMessage();
                log.info("检测到BusinessException | response: {}", JSON.toJSONString(se));
            } else {
                log.error("检测到未知系统异常", result.getException());
            }
            try {
                Method method = invoker.getInterface().getMethod(invocation.getMethodName(),
                    invocation.getParameterTypes());
                if (CommonResponse.class.isAssignableFrom(method.getReturnType())) {
                    CommonResponse<Object> response = CommonResponse.error(ServerCode.SERVER_ERROR, msg);
                    response.setTraceId(MDC.get(TraceConstants.TRACE_LOG_MDC_KEY));

                    AsyncRpcResult rpcResult = AsyncRpcResult.newDefaultAsyncResult(invocation);
                    rpcResult.setValue(response);
                    return rpcResult;
                }
                log.info("method返回类型不是 CommonResponse | method: {}, type: {}", method.getName(),
                    method.getReturnType().getName());
            } catch (Exception e) {
                log.error("获取 method 错误", e);
            }
        }
        return result;
    }

}
