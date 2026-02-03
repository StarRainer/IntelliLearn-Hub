package edu.jlu.intellilearnhub.server.service.impl;

import edu.jlu.intellilearnhub.server.entity.User;
import edu.jlu.intellilearnhub.server.mapper.UserMapper;
import edu.jlu.intellilearnhub.server.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Service;

/**
 * 用户Service实现类
 * 实现用户相关的业务逻辑
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

} 