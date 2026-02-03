package edu.jlu.intellilearnhub.server.service.impl;

import edu.jlu.intellilearnhub.server.entity.Banner;
import edu.jlu.intellilearnhub.server.mapper.BannerMapper;
import edu.jlu.intellilearnhub.server.service.BannerService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播图服务实现类
 */
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

} 