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
 * dubbo服务消费者方的过滤器，把MDC的traceId透传到服务提供方
 *
 * @Author zhongshenghua
 * @Date 2019/5/24 15:00
 */
@Slf4j
@Activate(group = {CommonConstants.CONSUMER})
public class DubboConsumerTraceIDFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        boolean newTraceId = setTraceId(inv);
        long time = 0;
        if(log.isDebugEnabled()){
            time = System.currentTimeMillis();
            log.debug("dubbo consumer url Path: {},{}", inv.getInvoker().getUrl().getPath(), inv.getMethodName());
        }
        try {
            return invoker.invoke(inv);
        } finally {
            if (newTraceId) {
                MDC.clear();
            }
            if(log.isDebugEnabled()){
                long sub = System.currentTimeMillis() - time;
                log.debug("dubbo consumer request end. time:{},{},{}", sub, inv.getInvoker().getUrl().getPath(), inv.getMethodName());
            }
        }
    }

    /**
     * 设置TraceId
     * @return 返回true表示traceId是此请求发出的，需要自己清理
     */
    private boolean setTraceId(Invocation inv) {
        boolean newTraceId = false;
        // 如果MDC获取失败，则生成一个traceId
        String traceId = MDC.get(TraceConstants.TRACE_LOG_MDC_KEY);
        if(StringUtils.isBlank(traceId)){
            traceId = UUID.randomUUID().toString().replaceAll("-", "");
            MDC.put(TraceConstants.TRACE_LOG_MDC_KEY, traceId);
            newTraceId = true;
        }
        // dubbo透传
        inv.getAttachments().put(TraceConstants.TRACE_LOG_MDC_KEY, traceId);
        return newTraceId;
    }
}