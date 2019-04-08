package com.qinfengsa.demo.service;

/**
 *
 * DemoService
 * @author qinfengsa
 * @date 2019/4/4 15:12
 */
public interface DemoService {

	/**
	 * 求和
	 * @param x
	 * @param y
	 * @return
	 */
	int add(int x, int y);

	/**
	 * 求差
	 * @param x
	 * @param y
	 * @return
	 */
	int sub(int x, int y);


	/**
	 * 比较大小
	 * @param x
	 * @param y
	 * @return
	 */
	int compare(int x, int y);
}
