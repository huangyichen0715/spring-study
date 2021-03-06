package com.hxl.service;


import com.hxl.framework.*;

@Component("userService")
@Scope("prototype")
@Lazy
public class userService implements BeanNameAware,InitializingBean{
    //实现自动注入
    @Autowired
    private OrderService orderService;


    private String beanName;

    public void test(){

        System.out.println(orderService);
        System.out.println(beanName);
    }

    @Override
    public void setBeanName(String name) { //想知道自己bean的名字可以回调这个方法
        this.beanName=name;
    }

    @Override
    public void afterPropertiesSet() {    //实现初始化逻辑，整合mybatis的时候用到这个方法
        //检查这个属性是不是符合规则，判断bean有没有创建好
        //如果bean是null，抛异常或者打印日志
        if(orderService==null){

        }
    }
}
