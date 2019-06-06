package com.chauncy.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;

/**
 * 定制版MyBatis Mapper
 *
 * @author huangwancheng
 * @since 2019-05-22
 */
public interface Mapper<T> extends BaseMapper<T> {


    Map<String, Object> findByUserName(@Param("username") String username);

    /**
     * 根据数据库表名和parentId获取所有子级，当parentId=null时获取所有数据
     * 表中必须存在parentId和id字段
     * @param parentId
     * @param tableName
     * @return
     */
    List<Long> getChildIds(@Param("parentId") Long parentId, @Param("tableName") String tableName);

}
