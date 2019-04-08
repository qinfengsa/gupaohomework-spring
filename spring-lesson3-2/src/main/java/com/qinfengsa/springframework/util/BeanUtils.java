package com.qinfengsa.springframework.util;

/**
 * 工具类
 * BeanUtils
 * @author qinfengsa
 * @date 2019/4/4 16:24
 */
public class BeanUtils {



	/**
	 * 首字母小写
	 * @param className
	 * @return
	 */
	public static String toLowerFirstName(String className) {

		char[] chars = className.toCharArray();

		chars[0] += 32;


		return String.valueOf(chars);
	}
}
