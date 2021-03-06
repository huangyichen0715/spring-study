package com.hxl;

import com.hxl.framework.HxlApplicationContext;
import com.hxl.service.userService;

public class Test {

    public static void main(String[] args) {
        //启动,扫描，创建bean(非懒加载的单例bean)
        HxlApplicationContext hxlApplicationContext=new HxlApplicationContext(AppConfig.class);

        //懒加载：第一次用的时候才会加载
        Object userService=hxlApplicationContext.getBean("userService");
        Object userService1=hxlApplicationContext.getBean("userService");
        Object userService2=hxlApplicationContext.getBean("userService");
        System.out.println(userService);
        System.out.println(userService1);
        System.out.println(userService2);

        userService userService4=(userService)hxlApplicationContext.getBean("userService");
        userService4.test();
    }
}
