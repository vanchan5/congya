package com.chauncy.message.advice.impl;

import com.chauncy.data.domain.po.message.advice.MmAdvicePo;
import com.chauncy.data.mapper.message.advice.MmAdviceMapper;
import com.chauncy.data.core.AbstractService;
import com.chauncy.message.advice.IMmAdviceService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 广告基本信息表 服务实现类
 * </p>
 *
 * @author huangwancheng
 * @since 2019-08-14
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MmAdviceServiceImpl extends AbstractService<MmAdviceMapper, MmAdvicePo> implements IMmAdviceService {

    @Autowired
    private MmAdviceMapper mapper;

}