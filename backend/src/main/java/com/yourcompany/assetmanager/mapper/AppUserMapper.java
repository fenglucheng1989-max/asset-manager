package com.yourcompany.assetmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.assetmanager.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}
