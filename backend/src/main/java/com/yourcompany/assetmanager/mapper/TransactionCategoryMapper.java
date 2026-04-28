package com.yourcompany.assetmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.assetmanager.entity.TransactionCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionCategoryMapper extends BaseMapper<TransactionCategory> {
}
