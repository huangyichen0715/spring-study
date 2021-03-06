package com.hxl.framework;

@Component
public class AutowiredAnnotationBeanPostProcessor implements BeanPostProcessor{
    @Override
    public void autowired() {
        System.out.println("处理Autowired注解");
    }
}
