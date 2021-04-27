package com.atguigu.gmall.ums.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        switch (type){
            case 1: queryWrapper.eq("username",data); break;
            case 2: queryWrapper.eq("phone",data); break;
            case 3: queryWrapper.eq("email",data); break;
            default:
                return null;
        }
         return this.count(queryWrapper)==0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {

        // TODO：1.校验验证码：查询redis中的code 和用户输入的code比较

        // 2.对密码加盐加密
        String salt = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        userEntity.setSalt(salt);

        // 加盐加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword()+salt));

        // 3.新增用户信息
        userEntity.setLevelId(1l);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());
        this.save(userEntity);

        // TODO: 4.删除redis中的短信验证码
    }

    @Override
    public UserEntity queryUser(String username, String password) {
        //根据用户名查询用户信息
        List<UserEntity> userEntities = this.list(new QueryWrapper<UserEntity>().eq("username", username)
                .or().eq("phone", username)
                .or().eq("email", username));

        // 判断用户是否为空
        if (CollectionUtils.isEmpty(userEntities)){
            return null;
        }

        for (UserEntity userEntity : userEntities) {
            // 获取用户信息中的盐，对用户输入的明文密码加盐加密
            String userLoginPwd = DigestUtils.md5Hex(password + userEntity.getSalt());

            // 用户信息中的密文密码（数据库） 和 上一步的密码比较（用户输入的） 比较
            if (StringUtils.equals(userEntity.getPassword(),userLoginPwd)){
                return userEntity;
            }
        }
        return null;
    }

}