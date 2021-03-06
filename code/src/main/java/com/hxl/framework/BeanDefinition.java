package com.hxl.framework;

public class BeanDefinition {
    //bean对象是根据bean定义来生成的，beandefinition是bean定义
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }

    private String scope;
    private boolean isLazy;
    private Class beanClass;
}
