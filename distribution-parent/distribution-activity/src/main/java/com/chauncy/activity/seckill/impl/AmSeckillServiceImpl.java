package com.chauncy.activity.seckill.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.chauncy.activity.seckill.IAmSeckillService;
import com.chauncy.common.enums.app.activity.ActivityStatusEnum;
import com.chauncy.common.enums.app.activity.type.ActivityTypeEnum;
import com.chauncy.common.enums.system.ResultCode;
import com.chauncy.common.exception.sys.ServiceException;
import com.chauncy.common.util.ListUtil;
import com.chauncy.data.domain.po.activity.AmActivityRelActivityCategoryPo;
import com.chauncy.data.domain.po.activity.reduced.AmReducedPo;
import com.chauncy.data.domain.po.activity.seckill.AmSeckillPo;
import com.chauncy.data.domain.po.product.PmGoodsCategoryPo;
import com.chauncy.data.domain.po.sys.SysUserPo;
import com.chauncy.data.domain.po.user.PmMemberLevelPo;
import com.chauncy.data.dto.manage.activity.SearchActivityListDto;
import com.chauncy.data.dto.manage.activity.seckill.SaveSeckillDto;
import com.chauncy.data.mapper.activity.AmActivityRelActivityCategoryMapper;
import com.chauncy.data.mapper.activity.group.AmActivityGroupMapper;
import com.chauncy.data.mapper.activity.seckill.AmSeckillMapper;
import com.chauncy.data.core.AbstractService;
import com.chauncy.data.mapper.product.PmGoodsCategoryMapper;
import com.chauncy.data.mapper.user.PmMemberLevelMapper;
import com.chauncy.data.vo.manage.activity.SearchActivityListVo;
import com.chauncy.data.vo.manage.activity.SearchGoodsCategoryVo;
import com.chauncy.security.util.SecurityUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 秒杀活动管理 服务实现类
 * </p>
 *
 * @author huangwancheng
 * @since 2019-07-23
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AmSeckillServiceImpl extends AbstractService<AmSeckillMapper, AmSeckillPo> implements IAmSeckillService {

    @Autowired
    private AmSeckillMapper mapper;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private PmGoodsCategoryMapper categoryMapper;

    @Autowired
    private PmMemberLevelMapper memberLevelMapper;

    @Autowired
    private AmActivityRelActivityCategoryMapper relActivityCategoryMapper;


    /**
     * 保存秒杀活动信息
     *
     * @param saveSeckillDto
     * @return
     */
    @Override
    public void saveReduced(SaveSeckillDto saveSeckillDto) {

        SysUserPo userPo = securityUtil.getCurrUser();
        //判断分类是否存在
        List<Long> categoryIds = saveSeckillDto.getCategoryIds();
        categoryIds.forEach(a -> {
            if (categoryMapper.selectById(a) == null) {
                throw new ServiceException(ResultCode.NO_EXISTS, String.format("该分类不存在:[%s]", a));
            }else if (categoryMapper.selectById(a).getLevel()!=3){
                throw new ServiceException(ResultCode.NO_EXISTS,String.format("该分类:[%s]不是三级分类",categoryMapper.selectById(a).getName()));
            }
        });
        //时间判断
        LocalDateTime registrationStartTime = saveSeckillDto.getRegistrationStartTime();
        LocalDateTime registrationEndTime = saveSeckillDto.getRegistrationEndTime();
        LocalDateTime activityStartTime = saveSeckillDto.getActivityStartTime();
        LocalDateTime activityEndTime = saveSeckillDto.getActivityEndTime();
        if (registrationEndTime.isBefore(registrationStartTime) || registrationEndTime.equals(registrationStartTime)) {
            throw new ServiceException(ResultCode.FAIL, "报名结束时间不能小于报名开始时间");
        }
        if (activityEndTime.isBefore(activityStartTime) || activityEndTime.equals(activityStartTime)) {
            throw new ServiceException(ResultCode.FAIL, "活动结束时间不能小于活动开始时间");
        }
        if (activityStartTime.isBefore(registrationEndTime) || registrationEndTime.equals(activityStartTime)) {
            throw new ServiceException(ResultCode.FAIL, "活动开始时间不能小于报名结束时间");
        }

        //可领取会员为全部会员操作
        Long memberLevelId = 0L;
        if (saveSeckillDto.getMemberLevelId() == 0){
            PmMemberLevelPo memberLevelPo = memberLevelMapper.selectOne(new QueryWrapper<PmMemberLevelPo>().lambda()
                    .eq(PmMemberLevelPo::getLevel,1));
            if (memberLevelPo != null){
                memberLevelId = memberLevelPo.getId();
            }
        }else {
            memberLevelId = saveSeckillDto.getMemberLevelId();
        }

        //新增操作
        if (saveSeckillDto.getId() == 0) {
            AmSeckillPo seckillPo = new AmSeckillPo();
            BeanUtils.copyProperties(saveSeckillDto, seckillPo);
            seckillPo.setId(null);
            seckillPo.setCreateBy(userPo.getUsername());
            seckillPo.setMemberLevelId(memberLevelId);
            mapper.insert(seckillPo);
            //保存积分活动与分类的信息
            if (!ListUtil.isListNullAndEmpty(categoryIds)) {
                categoryIds.forEach(a -> {
                    AmActivityRelActivityCategoryPo relActivityCategoryPo = new AmActivityRelActivityCategoryPo();
                    relActivityCategoryPo.setCategoryId(a);
                    relActivityCategoryPo.setCreateBy(userPo.getUsername());
                    relActivityCategoryPo.setActivityType(ActivityTypeEnum.SECKILL.getId());
                    relActivityCategoryPo.setActivityId(seckillPo.getId());
                    relActivityCategoryMapper.insert(relActivityCategoryPo);
                });
            }
        }
        //修改操作
        else {
            AmSeckillPo seckillPo = mapper.selectById(saveSeckillDto.getId());
            BeanUtils.copyProperties(saveSeckillDto, seckillPo);
            seckillPo.setUpdateBy(userPo.getUsername());
            seckillPo.setMemberLevelId(memberLevelId);
            mapper.updateById(seckillPo);
            List<AmActivityRelActivityCategoryPo> relActivityCategoryPos = relActivityCategoryMapper.selectList(new QueryWrapper<AmActivityRelActivityCategoryPo>().eq("activity_id", saveSeckillDto.getId()));
            //删除关联
            relActivityCategoryMapper.deleteBatchIds(relActivityCategoryPos.stream().map(a -> a.getId()).collect(Collectors.toList()));
            //重新保存
            //保存积分活动与分类的信息
            if (!ListUtil.isListNullAndEmpty(categoryIds)) {
                categoryIds.forEach(a -> {
                    AmActivityRelActivityCategoryPo relActivityCategoryPo = new AmActivityRelActivityCategoryPo();
                    relActivityCategoryPo.setCategoryId(a);
                    relActivityCategoryPo.setCreateBy(userPo.getUsername());
                    relActivityCategoryPo.setActivityType(ActivityTypeEnum.INTEGRALS.getId());
                    relActivityCategoryPo.setActivityId(seckillPo.getId());
                    relActivityCategoryMapper.insert(relActivityCategoryPo);
                });
            }
        }
    }

    /**
     * 条件查询秒杀活动信息
     *
     * @param searchActivityListDto
     * @return
     */
    @Override
    public PageInfo<SearchActivityListVo> searchSeckillList(SearchActivityListDto searchActivityListDto) {

        Integer pageNo = searchActivityListDto.getPageNo()==null ? defaultPageNo : searchActivityListDto.getPageNo();
        Integer pageSize = searchActivityListDto.getPageSize()==null ? defaultPageSize : searchActivityListDto.getPageSize();

        PageInfo<SearchActivityListVo> searchActivityListVoPageInfo = PageHelper.startPage(pageNo, pageSize/*, defaultSoft*/)
                .doSelectPageInfo(() -> mapper.searchSeckillList(searchActivityListDto));
        searchActivityListVoPageInfo.getList().forEach(a->{

            //当可领取会员为全部会员时，memberLevelId返回0给前端
            PmMemberLevelPo memberLevelPo = memberLevelMapper.selectById(a.getMemberLevelId());
            Long memberLevelId = 0L;
            if (memberLevelPo != null){
                if (memberLevelPo.getLevel() != 1){
                    memberLevelId = a.getMemberLevelId();
                }
            }
            a.setMemberLevelId(memberLevelId);

            //处理报名状态、活动状态
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime registrationStartTime = a.getRegistrationStartTime();
            LocalDateTime registrationEndTime = a.getRegistrationEndTime();
            LocalDateTime activityStartTime = a.getActivityStartTime();
            LocalDateTime activityEndTime = a.getActivityEndTime();
            //报名待开始
            if (registrationStartTime.isAfter(now)){
                a.setRegistrationStatus(ActivityStatusEnum.TO_START.getName());
            }
            //报名中
            else if (registrationStartTime.isBefore(now) && registrationEndTime.isAfter(now)){
                a.setRegistrationStatus(ActivityStatusEnum.REGISTRATION.getName());
            }
            //报名已结束
            else if(registrationEndTime.isBefore(now)){
                a.setRegistrationStatus(ActivityStatusEnum.HAS_ENDED.getName());
            }

            //活动待开始
            if (activityStartTime.isAfter(now)){
                a.setActivityStatus(ActivityStatusEnum.TO_START.getName());
            }
            //活动中
            else if (activityStartTime.isBefore(now) && activityEndTime.isAfter(now)){
                a.setActivityStatus(ActivityStatusEnum.REGISTRATION.getName());
            }
            //活动已结束
            else if(activityEndTime.isBefore(now)){
                a.setActivityStatus(ActivityStatusEnum.HAS_ENDED.getName());
            }

            //处理分类
            List<AmActivityRelActivityCategoryPo> relActivityCategoryPos = relActivityCategoryMapper.selectList(new QueryWrapper<AmActivityRelActivityCategoryPo>().eq("activity_id",a.getId()));
            List<Long> categoryIds = relActivityCategoryPos.stream().map(b->b.getCategoryId()).collect(Collectors.toList());
            List<SearchGoodsCategoryVo> goodsCategoryVoList = Lists.newArrayList();
            List<PmGoodsCategoryPo> goodsCategoryPos = categoryMapper.selectBatchIds(categoryIds);
            categoryIds.forEach(c->{
                PmGoodsCategoryPo goodsCategoryPo = categoryMapper.selectById(c);
                if (goodsCategoryPo == null){
                    throw new ServiceException(ResultCode.NO_EXISTS,String.format("数据库不存在该分类:[%s]",c));
                }
                SearchGoodsCategoryVo searchGoodsCategoryVo = new SearchGoodsCategoryVo();
                searchGoodsCategoryVo.setId(c);
                searchGoodsCategoryVo.setName(goodsCategoryPo.getName());
                String level3 = goodsCategoryPo.getName();
                PmGoodsCategoryPo goodsCategoryPo2 = categoryMapper.selectById(goodsCategoryPo.getParentId());
                String level2 = goodsCategoryPo2.getName();
                String level1 = categoryMapper.selectById(goodsCategoryPo2.getParentId()).getName();

                String categoryName = level1 + "/" + level2 + "/" + level3;
                searchGoodsCategoryVo.setCategoryName(categoryName);
                goodsCategoryVoList.add(searchGoodsCategoryVo);
            });
            a.setGoodsCategoryVoList(goodsCategoryVoList);
        });

        return searchActivityListVoPageInfo;
    }

    /**
     * 批量删除活动
     *
     * @param ids
     * @return
     */
    @Override
    public void delByIds(List<Long> ids) {
        ids.forEach(id->{
            AmSeckillPo amSeckillPo = mapper.selectById(id);
            if (amSeckillPo == null){
                throw new ServiceException(ResultCode.NO_EXISTS,String.format("数据库不存在该活动:[%s],id"));
            }
            if (!amSeckillPo.getRegistrationStartTime().isAfter(LocalDateTime.now())){
                throw new ServiceException(ResultCode.FAIL,String.format("该活动[%s:%s]的报名状态不是待开始状态，不能删除",id,amSeckillPo.getName()));
            }
        });
        mapper.deleteBatchIds(ids);
    }
}
