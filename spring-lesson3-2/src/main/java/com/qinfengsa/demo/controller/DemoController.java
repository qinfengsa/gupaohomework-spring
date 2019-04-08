package com.qinfengsa.demo.controller;

import com.qinfengsa.demo.service.DemoService;
import com.qinfengsa.springframework.annotation.MyAutowired;
import com.qinfengsa.springframework.annotation.MyController;
import com.qinfengsa.springframework.annotation.MyRequestMapping;
import com.qinfengsa.springframework.annotation.MyRequestParam;

/**
 * 测试控制器
 * TestController
 * @author qinfengsa
 * @date 2019/4/1 13:06
 */
@MyController
@MyRequestMapping("/qinfengsa")
public class DemoController {

	@MyAutowired
	DemoService demoService;

	@MyRequestMapping("/test")
	public String test1() {
		return "test success!";
	}

	@MyRequestMapping("/add")
	public int add(@MyRequestParam("a") int a,@MyRequestParam("b") int b) {
		return demoService.add(a,b);
	}

	@MyRequestMapping("/sub")
	public int sub(@MyRequestParam("a") int a,@MyRequestParam("b") int b) {
		return demoService.sub(a,b);
	}

	@MyRequestMapping("/compare")
	public int compare(@MyRequestParam("a") int a,@MyRequestParam("b") int b) {
		return demoService.compare(a,b);
	}
}
