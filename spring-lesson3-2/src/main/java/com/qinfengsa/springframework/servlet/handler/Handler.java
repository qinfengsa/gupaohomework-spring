package com.qinfengsa.springframework.servlet.handler;

import com.qinfengsa.springframework.annotation.MyRequestParam;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author: qinfengsa
 * @date: 2019/4/5 16:51
 */
@Data
public class Handler {

    /**
     * url路径 java提供的正则表达式
     */
    private Pattern url;
    /**
     * 控制器类
     */
    private Object controller;



    /**
     * 方法
     */
    private Method method;

    /**
     * 参数集合
     */
    private Map<String,Integer> paramIndexMapping;

    /**
     * 构造方法
     * @param url
     * @param controller
     * @param method
     */
    public Handler(Pattern url, Object controller, Method method) {
        this.url = url;
        this.controller = controller;
        this.method = method;
        this.paramIndexMapping = new HashMap<>();
        putParamIndexMapping(method);
    }


    private void putParamIndexMapping(Method method) {
        Annotation[][] pa = method.getParameterAnnotations();
        for (int i = 0; i < pa.length ; i ++) {
            for (Annotation a : pa[i]) {
                // 带有注解的参数
                if(a instanceof MyRequestParam){
                    String paramName = ((MyRequestParam) a).value();
                    if (StringUtils.isNotBlank(paramName)) {
                        paramIndexMapping.put(paramName,i);
                    }
                }
            }
        }
        //提取方法中的 request 和 response 参数
        Class<?> [] paramsTypes = method.getParameterTypes();
        for (int i = 0; i < paramsTypes.length ; i ++) {

            Class<?> type = paramsTypes[i];
            if(type == HttpServletRequest.class ||
                    type == HttpServletResponse.class){
                paramIndexMapping.put(type.getName(),i);
            }
        }
    }


}
