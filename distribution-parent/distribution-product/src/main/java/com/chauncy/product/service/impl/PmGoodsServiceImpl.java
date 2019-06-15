package com.chauncy.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.chauncy.common.enums.system.ResultCode;
import com.chauncy.data.core.AbstractService;
import com.chauncy.data.domain.po.product.PmGoodsPo;
import com.chauncy.data.domain.po.product.PmGoodsRelAttributeCategoryPo;
import com.chauncy.data.domain.po.product.PmGoodsRelAttributeGoodPo;
import com.chauncy.data.dto.manage.good.add.GoodBaseDto;
import com.chauncy.data.mapper.product.PmGoodsAttributeMapper;
import com.chauncy.data.mapper.product.PmGoodsMapper;
import com.chauncy.data.mapper.product.PmGoodsRelAttributeCategoryMapper;
import com.chauncy.data.mapper.product.PmGoodsRelAttributeGoodMapper;
import com.chauncy.data.util.ResultUtil;
import com.chauncy.data.vo.JsonViewData;
import com.chauncy.data.vo.Result;
import com.chauncy.data.vo.supplier.PmGoodsAttributeValueVo;
import com.chauncy.product.service.IPmGoodsService;
import com.chauncy.security.util.SecurityUtil;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * <p>
 * 商品信息 服务实现类
 * </p>
 *
 * @author huangwancheng
 * @since 2019-05-21
 */
@Service
public class PmGoodsServiceImpl extends AbstractService<PmGoodsMapper, PmGoodsPo> implements IPmGoodsService {

    @Autowired
    private PmGoodsMapper mapper;

    @Autowired
    private PmGoodsRelAttributeGoodMapper attributeGoodMapper;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 添加商品基础信息
     *
     * @param goodBaseDto
     * @return
     */
    @Override
    public JsonViewData addBase(GoodBaseDto goodBaseDto) {

        LocalDateTime date = LocalDateTime.now();
        //获取当前用户
        String user = securityUtil.getCurrUser().getUsername();
        PmGoodsPo goodsPo = new PmGoodsPo();
        goodsPo.setCreateBy(user);
        goodsPo.setShippingTemplateId(goodBaseDto.getShippingId());
//        goodsPo.setCreateTime(date);
        //复制Dto对象到po
        BeanUtils.copyProperties(goodBaseDto,goodsPo);
        //先保存商品不关联信息
        mapper.insert(goodsPo);
        //处理商品属性
        for (Long attId : goodBaseDto.getAttributeIds()){
            PmGoodsRelAttributeGoodPo attributeGoodPo = new PmGoodsRelAttributeGoodPo();
            attributeGoodPo.setGoodsAttributeId(attId).setGoodsGoodId(goodsPo.getId()).setCreateBy(user);
            attributeGoodMapper.insert(attributeGoodPo);
        }


        return new JsonViewData(ResultCode.SUCCESS,"添加成功",goodBaseDto);
    }

    /**
     * 根据ID查找商品信息
     * @param id
     * @return
     */
    @Override
    public JsonViewData findBaseById(Long id) {


        return null;
    }

    /**
     * 供应商添加商品时需要的规格值
     *
     * @param categoryId
     * @return
     */
    @Override
    public JsonViewData searchStandard(Long categoryId) {

        List<PmGoodsAttributeValueVo> list = mapper.searchStandard(categoryId);

        return new JsonViewData(list);
    }


}
