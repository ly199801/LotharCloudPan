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
import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.CreateImageCode;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.enums.VerifyRegexEnum;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.StringTools;
import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 用户信息 Controller
 */
@RestController("accountController")
public class AccountController extends ABaseController {

    private static final String CONTENT_TYPE="Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";

    private static final Logger logger= LoggerFactory.getLogger(AccountController.class);

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private EmailCodeService emailCodeService;
    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

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

    /**
     * 注册
     * @param session
     * @param email
     * @param nickName
     * @param password
     * @param checkCode
     * @param emailCode
     * @return
     */
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

    /**
     * 登录
     * @param session
     * @param email
     * @param password
     * @param checkCode
     * @return
     */
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

    /**
     * 重置密码
     * @param session
     * @param email
     * @param password
     * @param checkCode
     * @param emailCode
     * @return
     */
    @RequestMapping(value = "/resetPwd")
    @GlobalInterceptor(checkParams = true) //aop切面，用来进行参数的校验
    public ResponseVO resetPwd(HttpSession session,
                               @VerifyParam(required = true,regex = VerifyRegexEnum.EMAIL,max=150) String email,
                               @VerifyParam(required = true,regex = VerifyRegexEnum.PASSWORD,min=8,max=18)String password,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true) String emailCode) {
        try {
//            如果验证码不匹配则抛出异常
            if(!checkCode.equalsIgnoreCase((String)session.getAttribute(Constants.CHECK_CODE_KEY))){
                throw new BusinessException("图片验证码不匹配");
            }
            userInfoService.resetPwd(email,password,emailCode);
            return getSuccessResponseVO(null);
        }finally {
            //验证码使用后立即删除,不然别人可以用一个验证码不断尝试破解密码，必须没用一次重新生成一个验证码
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 获取头像
     * @param response
     * @param userId
     */
    @RequestMapping(value = "/getAvatar/{userId}")
    @GlobalInterceptor(checkParams = true)
    public void getAvatar(HttpServletResponse response, @VerifyParam(required = true)  @PathVariable("userId") String userId) {

        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File folder = new File(appConfig.getProjectFolder()+avatarFolderName);
        if (!folder.exists()) {
            folder.mkdirs();//目录不存在则自动创建
        }

        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarPath);
        if (!file.exists()) {//如果头像不存在，则给一个默认头像
            if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT).exists()) {//默认头像不存在，则报错
                printNoDefaultImage(response);
                return;
            }
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT;
        }
        response.setContentType("image/jpg");

        readFile(response, avatarPath);

    }

    /**
     * 提示设置默认头像
     * @param response
     */
    private void printNoDefaultImage(HttpServletResponse response){
        response.setHeader(CONTENT_TYPE,CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer =null;
        try {
            writer=response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        }catch (Exception e){
            logger.error("输出无默认图失败",e);
        }finally {
            writer.close();
        }
    }

    /**
     * 获取用户信息
     * @param session
     * @return
     */
    @RequestMapping(value = "/getUserInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getUserInfo(HttpSession session) {
        SessionWebUserDto sessionWebUserDto =getUserInfoFromSession(session);
        return getSuccessResponseVO(sessionWebUserDto);
    }

    /**
     * 获取用户使用空间
     * @param session
     * @return
     */
    @RequestMapping(value = "/getUserSpace")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getUserSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto =getUserInfoFromSession(session);

        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId());

        return getSuccessResponseVO(spaceDto);
    }

    /**
     * 退出登录
     * @param session
     * @return
     */
    @RequestMapping(value = "/logout")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO logout(HttpSession session) {
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    /**
     * 更新用户头像
     * @param session
     * @param avatar
     * @return
     */
    @RequestMapping("/updateUserAvatar")
    @GlobalInterceptor
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if(!targetFileFolder.exists()){
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" +webUserDto.getUserId() + Constants.AVATAR_SUFFIX);

        try {
            avatar.transferTo(targetFile);
        }catch (Exception e){
            logger.error("上传头像失败",e);
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setQqAvatar("");//如果使用qq登录，则会上传qq头像，当用户更新头像时，需要把qq头像设置为空。这样本地上传头像的优先级才会大于qq头像
                                // 因为读取头像的逻辑是，先看有没有qq头像，有就读取，没有再根据userid去读头像
        userInfoService.updateUserInfoByUserId(userInfo, webUserDto.getUserId());
        webUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY,webUserDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 修改密码
     * @param session
     * @param password
     * @return
     */
    @RequestMapping(value = "/updatePwd")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updatePwd(HttpSession session,
                               @VerifyParam(required = true,regex = VerifyRegexEnum.PASSWORD,min=8,max=18)String password) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfoService.updateUserInfoByUserId(userInfo,sessionWebUserDto.getUserId());
        return getSuccessResponseVO(null);
    }



}
