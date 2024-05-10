# Lothar云盘已上线，欢迎体验（尚未完成）

http://124.70.161.151:8010/login

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



## Redis出现异常前缀的原因

**异常现象**

![image-20240501152503904](F:/Java%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0/img/image-20240501152503904.png)



需要定义key的序列化方式，还有value的序列化方式

![image-20240501152414499](F:/Java%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0/img/image-20240501152414499.png)



处理后：

![image-20240501152532037](F:/Java%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0/img/image-20240501152532037.png)





# 文件上传

将大文件切成一小片一小片，需要知道文件总共需要切多少片，例如100M文件按10M来切，则需要传10次，到最后一个分片传完结束时，需要合并



## 大图片和视频的缩略图显示失败

![image-20240503140038385](F:/Java%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0/img/image-20240503140038385.png)



pom文件中没有导入ffmpeg的包，导入后仍然失败，测试ffmpeg功能正常。多次测试后发现，文件夹名字中不能带有空格，否则ffmpeg不会进行图片压缩的复制，`FFmpeg`在处理视频、音频时，对路径或[文件名](https://so.csdn.net/so/search?q=文件名&spm=1001.2101.3001.7020)中有空格时，默认会按空格进行切割，导致参数错误。

[Cmd调用 FFmpeg操作时文件路径或文件名中有空格的处理_ffmpeg 文件名含有空格-CSDN博客](https://blog.csdn.net/MMDFCZ/article/details/132552938)



视频分片播放，如果不分片，直接解析MP4文件，非常占内存和带宽



# 问题日志

**2024/05/04 登录时前端不能正常显示验证码错误或密码错误**

2024/05/06 创建同名文件夹，前端不报错，后端显示

2024/05/07 文件名重复前端不提示，后端显示

# 更新日志

## 2024/05/06 

新增创建文件夹功能。

新增newFolder方法、新增checkFileName方法（校验文件夹名称避免重复）

新增获取文件夹目录方法（是一个公共方法）

![image-20240506132943266](E:/JAVAProject/LotharCloudPan/image-20240506132943266.png)



## 2024/05/07

文件重命名

获取所有目录（移动文件功能的前置功能，想要移动文件，必须先获取所有目录）

移动文件



## **2024/05/08**

创建下载链接code（需要校验登录状态）

下载文件

删除文件

为何不直接给一个下载连接直接下载，还要分两步来下载呢？就是为了解决有些浏览器是集成了迅雷下载的插件的，我们下载的时候需要校验是否登录，这样迅雷无法直接下载，所以我们先校验登录，登录后给到一个code，拿着这个code去得到真正的下载链接。



## 2024/05/09

回收站

**恢复回收站文件（难点：删除目录时要把其子目录即文件都删除）**

彻底删除文件

这里文件还原的时候，不会回到它原来的位置，而是直接还原到根目录，这样有个好处就是，用户不用一级一级再去找那个文件，而是可以直接在根目录下找到这个文件

文件恢复的时候，如果根目录有相同名字的文件时，会给文件重命名

文件彻底删除后只是在数据库上彻底删除，服务器内仍有这个文件。这样在别的用户再上传相同文件的时候，仍可以做到秒传



## 2024/05/10

获取分享文件列表

分享文件

在分享文件的时候要获得文件名，这里需要进行关联查询来获取文件名，这里是一个难点

![image-20240510101119667](F:/Java%E5%AD%A6%E4%B9%A0%E7%AC%94%E8%AE%B0/img/image-20240510101119667-17153070810891.png)

取消分享



管理员功能：

获取系统设置

修改系统设置

修改用户状态：禁用用户（禁止登陆并且清空云盘文件）

修改用户空间



获取所有文件

获取文件目录

获取文件信息（预览）

获取视频信息

创建下载链接

下载文件

删除文件（彻底删除）

传参的时候是文件id和用户id用“_”分割，多个直接用“，”分割
