package edu.jlu.intellilearnhub.server.service.impl;

import edu.jlu.intellilearnhub.server.entity.Banner;
import edu.jlu.intellilearnhub.server.exception.CommonException;
import edu.jlu.intellilearnhub.server.mapper.BannerMapper;
import edu.jlu.intellilearnhub.server.service.BannerService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.jlu.intellilearnhub.server.service.FileUploadService;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播图服务实现类
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    @Autowired
    private FileUploadService fileUploadService;

    @Override
    public String uploadBannerImage(MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (file.isEmpty()) {
            throw new CommonException("上传轮播图失败，轮播图文件不能为空");
        }

        String contentType = file.getContentType();
        if (ObjectUtils.isEmpty(contentType) || !contentType.startsWith("image")) {
            throw new CommonException("上传轮播图失败，轮播图文件类型错误");
        }

        return fileUploadService.upload("banners", file);
    }
}