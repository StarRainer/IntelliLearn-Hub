package edu.jlu.intellilearnhub.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.jlu.intellilearnhub.server.entity.Banner;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 轮播图服务接口
 */
public interface BannerService extends IService<Banner> {

    /**
     * 完成轮播图的上传
     * @param file 上传的文件
     * @return 回显的地址
     */
    String uploadBannerImage(MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
}