package com.hxl.framework;

import com.hxl.AppConfig;
import com.sun.javafx.collections.MappingChange;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HxlApplicationContext {

    private Class configClass;//spring容器配置类

    private Map<String ,BeanDefinition> beanDefinitionMap=new HashMap<>(); //存储bean定义的beanDefinitionMap
    private Map<String ,Object> singletonObjects=new HashMap<>(); //单例池   spring里面的单例bean和单例模式不太一样
    //真正的单例模式一个类在整个jvm只能产生一个对象，单例bean不是
    private List<BeanPostProcessor> beanPostProcessors =new ArrayList<>();  //记录处理器

    public HxlApplicationContext(Class appConfigClass) {
        this.configClass=appConfigClass;

        //扫描--beanDefinition
        scan(configClass);

        //创建非懒加载的单例bean
        createNonLazySingleton();
    }

    private void createNonLazySingleton() {
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition=beanDefinitionMap.get(beanName);
            //非懒加载的单例bean
            if(beanDefinition.getScope().equals("singleton")&&!beanDefinition.isLazy()){
                 //创建bean
                Object bean=createBean(beanDefinition,beanName);//单例
                singletonObjects.put(beanName,bean); //存入单例池
            }
        }
    }
    //创建bean
    private Object createBean(BeanDefinition beanDefinition,String beanName) {
        //声明周期   bean是对象，要创建对象
        Class beanClass=beanDefinition.getBeanClass();
        //反射，这里需要推断构造方法
        try {
            Object instance = beanClass.getDeclaredConstructor().newInstance();//得到实例对象


            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                beanPostProcessor.autowired();
            }
            //填充对象的属性--》依赖注入
            //遍历所有的字段
            for(Field field:beanClass.getDeclaredFields()){
                if(field.isAnnotationPresent(Autowired.class)){
                    //byType ,byName,先byType再byName，如果只拿到一个则用这个，如果多个再byName，
                    //如果先byName去找bean没有就是没有，找到的话也可能类型不同
                    //spring内部的一个bean
                    //这里只是byName
                    Object bean=getBean(field.getName());//拿当前属性的名字得到bean
                    field.setAccessible(true);
                    field.set(instance,bean);//给当前属性赋值
                }

                //spring 里面Autowired和Resource注解类似，但是注入的逻辑不一样，需要分批次处理，这里省略Resource注入
                //Autowired是AutowiredAnnotationBeanPostProcessor后置处理器来实现逻辑
                //Resource是CommonAnnotationBeanPostProcessor后置处理器来实现逻辑
                //一个bean实例化后可以设置一些处理器，处理这个对象，spring支持多个处理器，还支持自己定义的后置处理器

                if(instance instanceof  BeanNameAware){ //回调setBeanName
                    ( (BeanNameAware)instance).setBeanName(beanName);
                }
                 //源码里面逻辑就是这样
                if(instance instanceof  InitializingBean){
                    ( (InitializingBean)instance).afterPropertiesSet(); //判断属性是否实现
                }
            }
            return instance;
        }catch (InstantiationException e){
            e.printStackTrace();
        }catch (IllegalAccessException e){
            e.printStackTrace();
        }catch(NoSuchMethodException e){
            e.printStackTrace();
        }catch (InvocationTargetException e){
            e.printStackTrace();
        }

        return null;
    }

    private void scan(Class configClass) {
        if(configClass.isAnnotationPresent(ComponentScan.class)){
            //拿到注解信息
            ComponentScan componentScanAnnotation=(ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path =componentScanAnnotation.value();//取value的值，扫描路径，不是真正意义的路径，是一个包名

            System.out.println(path);//扫描的是.class文件的路径
            //每一个类加载器都会有一个自己的目录，从这个目录下开始加载 AppClassLoader--》加载classpath指定的目录

            path=path.replace(".","/");
            //先拿到当前AppClassLoader
            ClassLoader classLoader= HxlApplicationContext.class.getClassLoader();
             URL resource= classLoader.getResource(path);//获取一个资源，传一个相对路径
            File file=new File(resource.getFile());  //拿到文件夹

            for(File f:file.listFiles()){
                //拿到文件
                //System.out.println(listFile); //class文件路径名

               String s= f.getAbsolutePath();     //截取获得类路径
                if(s.endsWith(".class")){
                    s=s.substring(s.indexOf("com"),s.indexOf(".class"));
                    s=s.replace("\\",".");

                    try {
                        //判断类里面有没有加上注解，spring源码里面是判断字节码
                        Class clazz = classLoader.loadClass(s);//把类加载进来
                        System.out.println(clazz); //把class都加载进来了
                        if (clazz.isAnnotationPresent(Component.class)) {
                              //如果有component注解表示是一个bean
                             //只需要创建非懒加载的单例bean
                            //获取componnet注解的信息
                            if(BeanPostProcessor.class.isAssignableFrom(clazz)){
                                //如果实现后置处理器，需要记录下来
                                BeanPostProcessor o= (BeanPostProcessor)clazz.getDeclaredConstructor().newInstance();
                             beanPostProcessors.add(o);
                            }

                            BeanDefinition beanDefinition=new BeanDefinition();
                            beanDefinition.setBeanClass(clazz);

                            Component componentAnnotation=(Component) clazz.getAnnotation(Component.class);
                           String beanName=componentAnnotation.value();//得到bean的名字

                            if(clazz.isAnnotationPresent(Lazy.class)){
                                 beanDefinition.setLazy(true); //懒加载
                            }
                            if(clazz.isAnnotationPresent(Scope.class)){

                                Scope scopeAnnotation=(Scope) clazz.getAnnotation(Scope.class);
                                String value =scopeAnnotation.value();
                                beanDefinition.setScope(value); //有什么设置成什么
                            }else{
                                beanDefinition.setScope("singleton"); //单例bean
                            }
                            //bean的定义生成完了

                            beanDefinitionMap.put(beanName,beanDefinition);//存入bean定义，找不到代表这个项目里没有这个bean
                            //到这里扫描就结束了
                        }
                    }catch (ClassNotFoundException e){
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }


            }
        }
    }

    public Object getBean(String beanName){
        //map
        //除了得到类还得判断是单例的还是原型的，单例的从另一个地方拿，原型重新创建
        //BeanDefinition表示一个bean的定义，
        if(!beanDefinitionMap.containsKey(beanName)){
            throw new NullPointerException();
        }else{
            //懒加载没实现
            BeanDefinition beanDefinition=beanDefinitionMap.get(beanName);
            if(beanDefinition.getScope().equals("singleton")){
              //如果是单例，单例池里面拿
                Object o=singletonObjects.get(beanName);
                if(o==null){
                   Object bean= createBean(beanDefinition,beanName);
                   singletonObjects.put(beanName,bean);
                   return bean;
                }
                return o;

            }else if(beanDefinition.getScope().equals("prototype")){//如果是原型
                //创建一个bean
                Object bean=createBean(beanDefinition,beanName);
                return bean;
            }
        }
        return null;
    }
}
