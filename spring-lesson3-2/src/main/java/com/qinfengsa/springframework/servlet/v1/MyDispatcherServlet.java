package com.qinfengsa.springframework.servlet.v1;

import com.qinfengsa.springframework.annotation.MyAutowired;
import com.qinfengsa.springframework.annotation.MyController;
import com.qinfengsa.springframework.annotation.MyRequestMapping;
import com.qinfengsa.springframework.annotation.MyService;
import com.qinfengsa.springframework.servlet.handler.Handler;
import com.qinfengsa.springframework.util.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义 Servlet
 * MyDispatcherServlet
 * @author qinfengsa
 * @date 2019/4/1 13:25
 */
@Slf4j
public class MyDispatcherServlet extends HttpServlet {


	/**
	 * classpath替换
	 */
	public static final String ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION = "classpath:";

	/**
	 * 配置
	 */
	private Properties contextConfig = new Properties();

	/**
	 * 保存class集合
	 */
	private List<String> classNameList = new ArrayList<>();

	/**
	 * ioc容器
	 */
	private Map<String,Object> ioc = new ConcurrentHashMap<>() ;

	private List<Handler> handlerMappings = new ArrayList<>();

	/**
	 * contextConfigLocation
	 */
	public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("doGet");
		try {
			doService(req, resp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("doPost");
		try {
			doService(req, resp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	@Override
	public void init(ServletConfig config) throws ServletException {
		log.debug("读取配置文件");
		//1、加载配置文件
		doLoadConfig(config.getInitParameter(CONFIG_LOCATION_PARAM));

		//2、扫描相关的类
		doSCanner(contextConfig.getProperty("scanPackage"));

		//3、初始化扫描到的类,注入到IOC容器中
		doInstance();

		//4、依赖注入
		doAutowired();

		//5、初始化HandlerMapping
		initHandlerMapping();

		log.debug("Spring Framework is inited");

	}

	/**
	 * 加载配置文件
	 * @param initParameter
	 */
	private void doLoadConfig(String initParameter)   {


		log.debug("initParameter,{}",initParameter);
		initParameter = initParameter.replaceAll(ORIGINAL_DEFAULT_PERSISTENCE_UNIT_ROOT_LOCATION,"");
		//FileInputStream fis = new FileInputStream(new File(initParameter));
		InputStream fis = this.getClass().getClassLoader().getResourceAsStream(initParameter);
		try {

			contextConfig.load(fis);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
			e.printStackTrace();
		} finally {
			if (Objects.nonNull(fis)) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * 扫描相关的类
	 * @param scanPackage
	 */
	private void doSCanner(String scanPackage) {
		log.debug("scanPackage:{}",scanPackage);
		URL url = this.getClass().getClassLoader().getResource("/"
				+ scanPackage.replaceAll("\\.","//"));
		log.debug("url:{}",url.getPath());
		File classPath = new File(url.getPath());
		if (Objects.isNull(classPath)) {
			log.error("scanPackage路径错误");
			return;
		}
		for (File file : classPath.listFiles()) {
			if (file.isDirectory()) {
				StringBuilder sb = new StringBuilder(scanPackage);
				sb.append(".");
				sb.append(file.getName());
				// 递归
				doSCanner(sb.toString());
			} else {
				// 不是class文件，跳过
				if (!file.getName().endsWith(".class")) {
					continue;
				}
				StringBuilder className = new StringBuilder(scanPackage);
				className.append(".");
				className.append(file.getName().replaceAll(".class",""));
				log.debug("className:{}",className);
				classNameList.add(className.toString());
			}

		}
	}

	/**
	 * 实例化相关的类，并放入IOC容器
	 */
	private void doInstance() {
		if (classNameList.isEmpty()) {
			return;
		}

		try {
			for (String className : classNameList) {
				Class<?> clazz = Class.forName(className);
				//beanName
				String beanName = BeanUtils.toLowerFirstName(clazz.getSimpleName());
				if (clazz.isAnnotationPresent(MyController.class)) {
					MyController annotation = clazz.getAnnotation(MyController.class);

					if (StringUtils.isNotBlank(annotation.value())) {
						beanName = annotation.value();
					}
					//实例化
					ioc.put(beanName,clazz.newInstance());
				} else if (clazz.isAnnotationPresent(MyService.class)) {
					MyService annotation = clazz.getAnnotation(MyService.class);

					if (StringUtils.isNotBlank(annotation.value())) {
						beanName = annotation.value();
					}
					Object instance = clazz.newInstance();
					//实例化
					ioc.put(beanName,instance);

					// 除了通过名称注入IOC，还要service的接口也要注入IOC
					for (Class inface : clazz.getInterfaces()) {

						if (ioc.containsKey(inface.getName())) {
							throw new Exception("The Bean " + inface.getName()  + " is exists");
						}

						ioc.put(inface.getName(),instance);


					}
				}

			}
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}


	/**
	 * 依赖注入
	 */
	private void doAutowired() {

	    if (ioc.isEmpty()) {
	        return;
        }
        for (Map.Entry<String,Object> entry : ioc.entrySet()) {
			//所有字段
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                //判断 MyAutowired 有没有值，有值按名称注入，没有按接口类注入

                String beanName = autowired.value();
                if (StringUtils.isBlank(beanName)) {
                    beanName = field.getType().getName();
                }
                // 强制获取权限
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    log.error(e.getMessage(),e);
                    e.printStackTrace();
                }


            }

        }
	}

	/**
	 * 初始化HandlerMapping
	 */
	private void initHandlerMapping() {

        if (ioc.isEmpty()) {
            return;
        }
        StringBuilder sbHead = new StringBuilder();
		StringBuilder sbUrl = new StringBuilder();
        for (Map.Entry<String,Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            // 没有Controller注解,跳过
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }
			sbHead.setLength(0);
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);

                if (StringUtils.isNotBlank(requestMapping.value())) {
					sbHead.append("/");
					sbHead.append(requestMapping.value());
                }
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                // 没有MyRequestMapping注解,跳过
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
					continue;
				}
				sbUrl.setLength(0);
				sbUrl.append(sbHead);
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                if (StringUtils.isNotBlank(requestMapping.value())) {
                    sbUrl.append("/");
                    sbUrl.append(requestMapping.value());
                }
                String url = sbUrl.toString();
                url = url.replaceAll("/+","/");
                Pattern pattern = Pattern.compile(url);
                handlerMappings.add(new Handler(pattern,entry.getValue(),method));
            }
        }
	}


	/**
	 * 根据url获取对应的handler
	 * @param request
	 * @return
	 */
	private Handler getHandler(HttpServletRequest request) {
		if (handlerMappings.isEmpty()) {
			return null;
		}
		String url = request.getRequestURI();
		String contextPath = request.getContextPath();
		url = url.replaceAll(contextPath,"");
		for (Handler handler : handlerMappings) {
			Matcher matcher = handler.getUrl().matcher(url);
			// 匹配不上，继续匹配
			if (!matcher.matches()) {
				continue;
			}
			return handler;
		}

		return null;
	}


	private void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Handler handler = getHandler(request);
		if (Objects.isNull(handler)) {
			response.getWriter().write("404 NOT FIND!");
			return;
		}
		Map<String,Integer> paramIndexMapping = handler.getParamIndexMapping();



		Class<?>[] paramTypes = handler.getMethod().getParameterTypes();

		Object[] paramValues = new Object[paramTypes.length];


		Map<String,String[]> paramMap = request.getParameterMap();
		for (Map.Entry<String,String[]> param : paramMap.entrySet()) {
			if (!paramIndexMapping.containsKey(param.getKey())) {
				continue;
			}
			String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","")
					.replaceAll("\\s",",");


			int index = paramIndexMapping.get(param.getKey());
			paramValues[index] = convert(paramTypes[index],value);
		}

		if (paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
			int reqIndex = paramIndexMapping.get(HttpServletRequest.class.getName());
			paramValues[reqIndex] = request;
		}

		if (paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
			int respIndex = paramIndexMapping.get(HttpServletResponse.class.getName());
			paramValues[respIndex] = response;
		}
		Object returnValue = handler.getMethod().invoke(handler.getController(),paramValues);
		if (returnValue == null || returnValue instanceof Void){ return; }
		response.getWriter().write(returnValue.toString());
	}


	/**
	 * 通过类型自定义转换
	 * @param type
	 * @param value
	 * @return
	 */
	private Object convert(Class<?> type,String value) {


		//如果是int
		if(Integer.class == type || int.class == type){
			return Integer.valueOf(value);
		}
		else if(Double.class == type || double.class == type){
			return Double.valueOf(value);
		}
		return value;
	}




}
