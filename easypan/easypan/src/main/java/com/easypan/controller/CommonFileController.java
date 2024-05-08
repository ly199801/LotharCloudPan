package com.easypan.controller;/*
 * ClassName:CommonFileController
 * Package:com.easypan.controller
 * Description:
 * @Author ly
 * @Create 2024/5/3 11:39
 * @Version 1.0
 */

import com.easypan.component.RedisComponent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.DownloadFileDto;
import com.easypan.entity.enums.FileCategoryEnums;
import com.easypan.entity.enums.FileFolderTypeEnums;
import com.easypan.entity.enums.FileTypeEnums;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.FileInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class CommonFileController extends ABaseController{
    @Resource
    private AppConfig appConfig;

    @Resource
    protected FileInfoService fileInfoService;

    @Resource
    private RedisComponent redisComponent;


    /**
     * 读取图片
     * @param response
     * @param imageFolder
     * @param imageName
     */
    protected void getImage(HttpServletResponse response,String imageFolder,String imageName){
        if(StringTools.isEmpty(imageFolder) || StringTools.isEmpty(imageName) || !StringTools.pathIsOk(imageFolder) || !StringTools.pathIsOk(imageName)){
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath =appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" +imageName;
        imageSuffix = imageSuffix.replace(".","");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control","max-age=2592000");
        readFile(response,filePath);
    }

    protected void getFile(HttpServletResponse response, String fileId, String userId) {
        String filePath = null;
        if (fileId.endsWith(".ts")) {
            String[] tsAarray = fileId.split("_");
            String realFileId = tsAarray[0];
            //根据原文件的id查询出一个文件集合
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realFileId, userId);
            if (fileInfo == null) {
                //分享的视频，ts路径记录的是原视频的id,这里通过id直接取出原视频
                FileInfoQuery fileInfoQuery = new FileInfoQuery();
                fileInfoQuery.setFileId(realFileId);
                List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);
                fileInfo = fileInfoList.get(0);
                if (fileInfo == null) {
                    return;
                }

                //更具当前用户id和路径去查询当前用户是否有该文件，如果没有直接返回
                fileInfoQuery = new FileInfoQuery();
                fileInfoQuery.setFilePath(fileInfo.getFilePath());
                fileInfoQuery.setUserId(userId);
                Integer count = fileInfoService.findCountByParam(fileInfoQuery);
                if (count == 0) {
                    return;
                }
            }
            String fileName = fileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
        } else {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
            if (fileInfo == null) {
                return;
            }
            //视频文件读取.m3u8文件
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                //重新设置文件路径
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        readFile(response, filePath);
    }

    /**
     * 获取文件目录信息
     * @param path
     * @param userId
     * @return
     */
    protected ResponseVO getFolderInfo(String path,String userId){
        String[] pathArray = path.split("/");
        FileInfoQuery infoQuery = new FileInfoQuery();
        infoQuery.setUserId(userId);
        infoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        infoQuery.setFileIdArray(pathArray);
        //对查询的目录要进行排序，应该是按照它的层级来排，也就是其url中path后面出现的先后顺序来排,SQL语句中有引号所以需要转义符号来添加“
        //select * from file_info where file_id in("fileId_1","fileId_2") order by field(file_id,"fileId_1","fileId_2")
        String orderBy="field(file_id,\""+ StringUtils.join(pathArray,"\",\"")+"\")";
        infoQuery.setOrderBy(orderBy);
        List<FileInfo> fileInfoList=fileInfoService.findListByParam(infoQuery);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FileInfoVO.class));
    }

    /**
     * 创建下载链接(code)
     * @param fileId
     * @param userId
     * @return
     */
    protected ResponseVO createDownload(String fileId, String userId){
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId,userId);
        if(fileInfo==null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if(FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        //放进Redis中，给其十分钟的实效性
        String code = StringTools.getRandomString(Constants.LENGTH_50);

        DownloadFileDto fileDto = new DownloadFileDto();
        fileDto.setDownloadCode(code);
        fileDto.setFilePath(fileInfo.getFilePath());
        fileDto.setFileName(fileInfo.getFileName());

        redisComponent.saveDownloadCode(code,fileDto);

        return getSuccessResponseVO(code);
    }

    /**
     * 用code去下载文件
     * @param request
     * @param response
     * @param code
     * @throws UnsupportedEncodingException
     */
    protected void download(HttpServletRequest request, HttpServletResponse response, String code) throws UnsupportedEncodingException {
        DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
        if(downloadFileDto==null){
            return;
        }
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload;charset=UTF-8");
        if(request.getHeader("User-Agent").toLowerCase().indexOf("msie")>0){//IE浏览器
            fileName = URLEncoder.encode(fileName, "UTF-8");
        }else{
            fileName=new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response,filePath);
    }
}
