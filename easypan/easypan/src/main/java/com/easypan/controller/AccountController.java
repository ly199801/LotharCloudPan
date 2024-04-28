package com.easypan.controller;/*
 * ClassName:AccountController
 * Package:com.easypan.controller
 * Description:
 * @Author ly
 * @Create 2024/4/27 16:43
 * @Version 1.0
 */

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.CreateImageCode;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.VerifyRegexEnum;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 用户信息 Controller
 */
@RestController("accountController")
public class AccountController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private EmailCodeService emailCodeService;

    /**
     * 验证码
     *
     * @param response
     * @param session
     * @param type
     * @throws IOException
     */
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

    /**
     * 发送邮箱验证码
     * @param session
     * @param email
     * @param checkCode
     * @param type
     * @return
     */

    @RequestMapping(value = "/sendEmailCode")
    @GlobalInterceptor(checkParams = true) //aop切面，用来进行参数的校验
    public ResponseVO sendEmailCode(HttpSession session,
                                    @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL,max=150) String email,
                                    @VerifyParam(required = true) String checkCode,
                                    @VerifyParam(required = true) Integer type) {
        try {
            //如果验证码不匹配则抛出异常
            if(!checkCode.equalsIgnoreCase((String)session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))){
                throw new BusinessException("图片验证码不匹配");
            }
            emailCodeService.sendEmailCode(email,type);
            return getSuccessResponseVO(null);
        }finally {
            //验证码使用后立即删除
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }


    @RequestMapping(value = "/register")
    @GlobalInterceptor(checkParams = true) //aop切面，用来进行参数的校验
    public ResponseVO register(HttpSession session,
                               @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL,max=150) String email,
                               @VerifyParam(required = true)String nickName,
                               @VerifyParam(required = true,regex = VerifyRegexEnum.PASSWORD,min=8,max=18)String password,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true) String emailCode) {
        try {
//            如果验证码不匹配则抛出异常
            if(!checkCode.equalsIgnoreCase((String)session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("图片验证码不匹配");
            }
            userInfoService.register(email,nickName,password,emailCode);
            return getSuccessResponseVO(null);
        }finally {
            //验证码使用后立即删除,不然别人可以用一个验证码不断尝试破解密码，必须没用一次重新生成一个验证码
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping(value = "/login")
    @GlobalInterceptor(checkParams = true) //aop切面，用来进行参数的校验
    public ResponseVO login(HttpSession session,
                               @VerifyParam(required = true) String email,
                               @VerifyParam(required = true)String password,
                               @VerifyParam(required = true) String checkCode) {
        try {
//            如果验证码不匹配则抛出异常
            if(!checkCode.equalsIgnoreCase((String)session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("图片验证码不匹配");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password);
            //setAttribute 是用于在会话中设置属性的方法。这个方法接受两个参数：属性的键和属性的值
            session.setAttribute(Constants.SESSION_KEY,sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        }finally {
            //验证码使用后立即删除,不然别人可以用一个验证码不断尝试破解密码，必须没用一次重新生成一个验证码
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

}
