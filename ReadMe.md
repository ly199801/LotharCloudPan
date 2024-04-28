#  技术架构

![image-20240427091055339](F:/Java%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0/img/image-20240427091055339.png)



  ![image-20240427091720962](F:/Java%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0/img/image-20240427091720962.png)

# @RestController VS @Controller

[【SpringBoot】带你一文彻底搞懂RestController和Controller的关系与区别-CSDN博客](https://blog.csdn.net/miles067/article/details/132567377)

都是SpringFramework中用于定义控制器的注解

​		@RestController 是一个组合注解，结合了@controller 和@ResponseBody注解的功能。在使用 @RestController 注解标记的类中，每个方法的返回值都会以 JSON 或 XML 的形式直接写入 HTTP 响应体中，相当于在每个方法上都添加了 @ResponseBody 注解。

​		@Controller 注解标记的类则是传统的控制器类。它用于处理客户端发起的请求，并负责返回适当的视图（View）作为响应。在使用 @Controller 注解的类中，通常需要在方法上使用 @ResponseBody 注解来指示该方法的返回值要作为响应的主体内容，而不是解析为视图。
 		简而言之，@RestController 适用于构建 RESTful 风格的 API，其中每个方法的返回值会直接序列化为 JSON 或 XML 数据并发送给客户端。而 @Controller 适用于传统的 MVC 架构，它负责处理请求并返回相应的视图。（@RestController下的方法默认返回的是数据格式，@Controller注解标注的类下面的方法默认返回的就是以视图为格式）



# 登录注册

## 获取验证码

生成验证码这一段是直接在网上复制的

```
 @RequestMapping(value = "/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws
            IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }
```



## AOP能做什么？

参数的校验

先定义一个注解文件annotation

```
package com.easypan.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface GlobalInterceptor {

    /**
     * 校验登录
     *
     * @return
     */
    boolean checkLogin() default true;

    /**
     * 校验参数
     *
     * @return
     */
    boolean checkParams() default false;

    /**
     * 校验管理员
     *
     * @return
     */
    boolean checkAdmin() default false;
}

```

