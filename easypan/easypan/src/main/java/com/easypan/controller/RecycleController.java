package com.easypan.controller;/*
 * ClassName:RecycleController
 * Package:com.easypan.controller
 * Description:
 * @Author ly
 * @Create 2024/5/9 12:14
 * @Version 1.0
 */

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.service.FileInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("recycleController")
@RequestMapping("/recycle")
public class RecycleController extends ABaseController {
    @Resource
    private FileInfoService fileInfoService;

    /**
     * 回收站列表
     * @param session
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping("/loadRecycleList")
    @GlobalInterceptor
    public ResponseVO loadRecycleList(HttpSession session, Integer pageNo, Integer pageSize){
        FileInfoQuery query = new FileInfoQuery();
        query.setPageSize(pageSize);
        query.setPageNo(pageNo);
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("recovery_time desc");
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * 恢复回收站文件
     * @param session
     * @param fileIds
     * @return
     */
    @RequestMapping("/recoverFile")
    @GlobalInterceptor
    public ResponseVO recoverFile(HttpSession session, @VerifyParam(required = true) String fileIds){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.recoverFileBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 回收站彻底删除文件
     * @param session
     * @param fileIds
     * @return
     */
    @RequestMapping("/delFile")
    @GlobalInterceptor
    public ResponseVO delFile(HttpSession session, @VerifyParam(required = true) String fileIds){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.delFileBatch(webUserDto.getUserId(), fileIds,false);
        return getSuccessResponseVO(null);
    }


}
