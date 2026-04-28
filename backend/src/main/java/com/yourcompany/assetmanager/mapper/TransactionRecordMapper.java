package com.yourcompany.assetmanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yourcompany.assetmanager.entity.TransactionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransactionRecordMapper extends BaseMapper<TransactionRecord> {
}
