package com.easypan.controller;/*
 * ClassName:AccountController
 * Package:com.easypan.controller
 * Description:
 * @Author ly
 * @Create 2024/4/27 16:43
 * @Version 1.0
 */

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.CreateImageCode;
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

    @RequestMapping(value = "sendEmailCode")
    public ResponseVO sendEmailCode(HttpSession session,String email,String checkCode,Integer type) {
        try {
            //如果验证码不匹配则抛出异常
            if(!checkCode.equalsIgnoreCase((String)session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))){
                throw new BusinessException("验证码不匹配");
            }
            emailCodeService.sendEmailCode(email,type);
            return getSuccessResponseVO(null);
        }finally {
            //验证码使用后立即删除
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }


}
