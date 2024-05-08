package com.easypan.controller;/*
 * ClassName:FileInfoController
 * Package:com.easypan.controller
 * Description:
 * @Author ly
 * @Create 2024/5/2 18:32
 * @Version 1.0
 */

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.enums.FileCategoryEnums;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.enums.FileFolderTypeEnums;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.service.FileInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 文明信息
 */
@RestController("fileInfoController")
@RequestMapping("/file")
public class FileInfoController extends CommonFileController{
    @Resource
    private FileInfoService fileInfoService;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadDataList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadDataList(FileInfoQuery query, HttpSession session, String category){
        FileCategoryEnums categoryEnums = FileCategoryEnums.getByCode(category);
        if(categoryEnums !=null){
            query.setFileCategory(categoryEnums.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);

        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * 上传文件
     * @param session
     * @param fileId
     * @param file
     * @param fileName
     * @param filePid
     * @param fileMd5
     * @param chunkIndex
     * @param chunks
     * @return
     */
    @RequestMapping("/uploadFile")
    @GlobalInterceptor(checkParams = true)
    //第一个文件传的时候没有fileId，后面再传的时候会把fileId带上，这样就知道是前面第一个文件的分片
    //Md5是前端需要的，用来实现秒传
    //chunkIndex是分片的序号，chunks是分片的总数
    public ResponseVO uploadFile(HttpSession session,
                                 String fileId,
                                 MultipartFile file,
                                 @VerifyParam(required = true) String fileName,
                                 @VerifyParam(required = true) String filePid,
                                 @VerifyParam(required = true) String fileMd5,
                                 @VerifyParam(required = true) Integer chunkIndex,
                                 @VerifyParam(required = true) Integer chunks){
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        UploadResultDto resultDto = fileInfoService.uploadFile(webUserDto,fileId,file,fileName,filePid,fileMd5,chunkIndex,chunks);
        return getSuccessResponseVO(resultDto);
    }

    /**
     * 获取照片
     * @param response
     * @param imageFolder
     * @param imageName
     */
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName) {
        super.getImage(response, imageFolder, imageName);
    }

    /**
     * 获取视频信息
     * @param response
     * @param session
     * @param fileId
     */
    @RequestMapping("/ts/getVideoInfo/{fileId}")
    public void getVideoInfo(HttpServletResponse response, HttpSession session, @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    /**
     * 获取文件
     * @param response
     * @param session
     * @param fileId
     */
    @RequestMapping("/getFile/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getFile(HttpServletResponse response, HttpSession session, @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    /**
     * 新增文件夹
     * @param session
     * @param filePid
     * @param fileName
     * @return
     */
    @RequestMapping("/newFolder")
    @GlobalInterceptor(checkParams = true)
    //filePid是用来校验其上一级目录
    public ResponseVO newFolder( HttpSession session,
                           @VerifyParam(required = true) String filePid,
                           @VerifyParam(required = true) String fileName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(filePid, webUserDto.getUserId(), fileName);
        return getSuccessResponseVO(CopyTools.copy(fileInfo,FileInfoVO.class));
    }

    /**
     * 获取文件目录信息
     * @param session
     * @param path
     * @return
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    //文件目录信息、分享时也会用到、超级管理员看整个用户也需要用到文件列表
    //是一个公共方法，建议写到其父类里去
    public ResponseVO getFolderInfo( HttpSession session,
                                 @VerifyParam(required = true) String path) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        return super.getFolderInfo(path, webUserDto.getUserId());
    }

    /**
     * 文件重命名
     * @param session
     * @param fileId
     * @param fileName
     * @return
     */
    @RequestMapping("/rename")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO rename( HttpSession session,
                                     @VerifyParam(required = true) String fileId,
                                     @VerifyParam(required = true) String fileName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo  fileInfo = fileInfoService.rename(fileId,webUserDto.getUserId(),fileName);
        //这样做就避免了把所有信息都传给前端
        return getSuccessResponseVO(CopyTools.copy(fileInfo,FileInfoVO.class));
    }

    /**
     * 获取所有文件目录
     * @param session
     * @param filePid
     * @param currentFileIds
     * @return
     */
    @RequestMapping("/loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadAllFolder( HttpSession session,
                              @VerifyParam(required = true) String filePid,
                               String currentFileIds) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfoQuery infoQuery = new FileInfoQuery();
        infoQuery.setUserId(webUserDto.getUserId());
        infoQuery.setFilePid(filePid);
        //设置好文件类型，只针对文件夹
        infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        if(!StringTools.isEmpty(currentFileIds)){
            infoQuery.setExcludeFileIdArray(currentFileIds.split(","));
        }
        infoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        infoQuery.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList,FileInfoVO.class));
    }

    /**
     * 移动文件
     * @param session
     * @param fileIds
     * @param filePid
     * @return
     */

    @RequestMapping("/changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO changeFileFolder( HttpSession session,
                                     @VerifyParam(required = true) String fileIds,
                                        @VerifyParam(required = true) String filePid) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);

        fileInfoService.changeFileFolder(fileIds,filePid, webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * 创建下载链接(code)
     * @param session
     * @param fileId
     * @return
     */
    @RequestMapping("/createDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO createDownloadUrl( HttpSession session,
                                        @VerifyParam(required = true) @PathVariable("fileId") String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
            return super.createDownload(fileId, webUserDto.getUserId());

    }

    /**
     * 下载文件
     * @param response
     * @param request
     * @param code
     * @throws UnsupportedEncodingException
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void download(HttpServletResponse response, HttpServletRequest request,
                               @VerifyParam(required = true) @PathVariable("code") String code) throws UnsupportedEncodingException {
         super.download(request,response,code);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delFile(HttpSession session,
                         @VerifyParam(required = true) String fileIds)  {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

}
