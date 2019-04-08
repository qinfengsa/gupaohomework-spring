package com.qinfengsa.demo.service;

import com.qinfengsa.springframework.annotation.MyService;

/**
 *
 * DemoServiceImpl
 * @author qinfengsa
 * @date 2019/4/4 15:12
 */
@MyService
public class DemoServiceImpl implements DemoService {
	@Override
	public int add(int x, int y) {
		return x + y;
	}

	@Override
	public int sub(int x, int y) {
		return x - y;
	}

	@Override
	public int compare(int x, int y) {
		return x < y ? -1 : (x == y ? 0: 1);
	}
}
