package com.easypan.controller;/*
 * ClassName:WebShareController
 * Package:com.easypan.controller
 * Description:
 * @Author ly
 * @Create 2024/5/11 17:12
 * @Version 1.0
 */

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionShareDto;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.po.FileShare;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.entity.vo.ShareInfoVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.FileInfoService;
import com.easypan.service.FileShareService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController("webShareController")
@RequestMapping("/showShare")
public class WebShareController extends CommonFileController{

    @Resource
    private FileShareService fileShareService;
    @Resource
    private FileInfoService fileInfoService;
    @Resource
    private UserInfoService userInfoService;


    /**
     * 获取用户登录信息
     * @param session
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO getShareLoginInfo(HttpSession session, @VerifyParam(required = true) String shareId){
        SessionShareDto sessionShareDto = getSessionShareFromSession(session,shareId);
        if(sessionShareDto==null) return getSuccessResponseVO(null);

        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        //判断是否是当前用户分享的文件
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (userDto!=null && userDto.getUserId().equals(sessionShareDto.getShareUserId())){
            shareInfoVO.setCurrentUser(true);
        }else{
            shareInfoVO.setCurrentUser(false);
        }
        return getSuccessResponseVO(shareInfoVO);
    }


    /**
     * 获取分享信息
     * @param shareId
     * @return
     */
    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO getShareInfo(@VerifyParam(required = true) String shareId){

        return getSuccessResponseVO(getShareInfoCommon(shareId));
    }

    /**
     * 公用方法：获取分享信息
     * @param shareId
     * @return
     */
    private ShareInfoVO getShareInfoCommon(String shareId){
        FileShare share = fileShareService.getFileShareByShareId(shareId);
        if(share==null || (share.getExpireTime() !=null && new Date().after(share.getExpireTime()))){
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        ShareInfoVO shareInfoVO = CopyTools.copy(share, ShareInfoVO.class);
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(share.getFileId(),share.getUserId());
        if(fileInfo==null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())){
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }

        shareInfoVO.setFileName(fileInfo.getFileName());
        UserInfo userInfo = userInfoService.getUserInfoByUserId(share.getUserId());
        shareInfoVO.setNickName(userInfo.getNickName());
        shareInfoVO.setAvatar(userInfo.getQqAvatar());
        shareInfoVO.setUserId(userInfo.getUserId());
        return shareInfoVO;
    }

    /**
     * 校验提取码
     * @param session
     * @param shareId
     * @param code
     * @return
     */
    @RequestMapping("/checkShareCode")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO checkShareCode(HttpSession session,
                                    @VerifyParam(required = true) String shareId,
                                     @VerifyParam(required = true) String code ){
        SessionShareDto sessionShareDto = fileShareService.checkShareCode(shareId,code);
        session.setAttribute(Constants.SESSION_SHARE_KEY+shareId,sessionShareDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取文件列表
     * @param session
     * @param shareId
     * @param filePid
     * @return
     */
    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO loadFileList(HttpSession session,
                                   @VerifyParam(required = true) String shareId,
                                   String filePid){
        SessionShareDto sessionShareDto =checkShare(session,shareId);

        FileInfoQuery query = new FileInfoQuery();
        if (!StringTools.isEmpty(filePid) && !Constants.ZERO_STR.equals(filePid)){
            //这一块难点，对传入的文件Pid要进行校验
            fileInfoService.checkRootFilePid(sessionShareDto.getFileId(),sessionShareDto.getShareUserId(),filePid);
            query.setFilePid(filePid);
        }else{
            query.setFileId(sessionShareDto.getFileId());
        }
        query.setUserId(sessionShareDto.getShareUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);

        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    private SessionShareDto checkShare(HttpSession session, String shareId){
        SessionShareDto sessionShareDto = getSessionShareFromSession(session, shareId);
        if (sessionShareDto==null){
            throw new BusinessException(ResponseCodeEnum.CODE_903.getMsg());
        }
        if(sessionShareDto.getExpireTime() !=null && new Date().after(sessionShareDto.getExpireTime())){
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        return sessionShareDto;
    }

}
