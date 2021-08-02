package com.bihu.trace.filter;

import com.bihu.trace.constants.TraceConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * dubbo服务提供者方的过滤器，接收traceId，如果没有则自己生成，并设置MDC
 *
 * @Author zhongshenghua
 * @Date 2019/5/24 15:00
 */
@Slf4j
@Activate(group = {CommonConstants.PROVIDER})
public class DubboProviderTraceIDFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        setTraceId(inv);
        long time = 0;
        if(log.isDebugEnabled()){
            time = System.currentTimeMillis();
            log.debug("dubbo provider url Path: {},{}", inv.getInvoker().getUrl().getPath(), inv.getMethodName());
        }
        try {
            return invoker.invoke(inv);
        } finally {
            MDC.clear();
            if(log.isDebugEnabled()){
                long sub = System.currentTimeMillis() - time;
                log.debug("dubbo provider request end. time:{},{},{}", sub, inv.getInvoker().getUrl().getPath(), inv.getMethodName());
            }
        }
    }

    /**
     * 设置traceId
     */
    private void setTraceId(Invocation inv) {
        String traceId = inv.getAttachment(TraceConstants.TRACE_LOG_MDC_KEY);
        if(StringUtils.isBlank(traceId)){
            traceId = UUID.randomUUID().toString().replaceAll("-", "");
        }
        MDC.put(TraceConstants.TRACE_LOG_MDC_KEY, traceId);
    }
}