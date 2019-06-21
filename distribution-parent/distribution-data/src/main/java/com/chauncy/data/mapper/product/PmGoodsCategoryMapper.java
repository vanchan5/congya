package com.chauncy.data.mapper.product;

import com.chauncy.data.domain.po.product.PmGoodsCategoryPo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chauncy.data.mapper.IBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 商品分类表 IBaseMapper 接口
 * </p>
 *
 * @author huangwancheng
 * @since 2019-05-21
 */
public interface PmGoodsCategoryMapper extends IBaseMapper<PmGoodsCategoryPo> {
    /**
     * 查询分类详情
     * @param id
     * @return
     */
    Map<String,Object> loadById(Long id);

    /**
     * 根据分类id、type、属性名称集合查找属性id
     * @param names
     * @param type
     * @param cId
     * @return
     */
    List<Long> getAttributeIdsByNamesAndCategoryId(@Param("names")List<String> names,
                                                   @Param("type") Integer type,
                                                   @Param("cId") Long cId);

}
