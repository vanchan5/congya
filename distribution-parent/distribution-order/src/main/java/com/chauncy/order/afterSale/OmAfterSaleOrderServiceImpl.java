package com.chauncy.order.afterSale;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.chauncy.common.constant.SecurityConstant;
import com.chauncy.common.enums.app.order.OrderStatusEnum;
import com.chauncy.common.enums.app.order.afterSale.AfterSaleLogEnum;
import com.chauncy.common.enums.app.order.afterSale.AfterSaleStatusEnum;
import com.chauncy.common.enums.app.order.afterSale.AfterSaleTypeEnum;
import com.chauncy.common.enums.system.ResultCode;
import com.chauncy.common.exception.sys.ServiceException;
import com.chauncy.common.util.BigDecimalUtil;
import com.chauncy.common.util.SnowFlakeUtil;
import com.chauncy.data.domain.po.afterSale.OmAfterSaleLogPo;
import com.chauncy.data.domain.po.afterSale.OmAfterSaleOrderPo;
import com.chauncy.data.domain.po.order.OmGoodsTempPo;
import com.chauncy.data.domain.po.order.OmOrderPo;
import com.chauncy.data.domain.po.store.SmStorePo;
import com.chauncy.data.domain.po.sys.SysUserPo;
import com.chauncy.data.domain.po.user.UmUserPo;
import com.chauncy.data.dto.app.order.my.afterSale.ApplyRefundDto;
import com.chauncy.data.dto.base.BasePageDto;
import com.chauncy.data.dto.manage.order.afterSale.SearchAfterSaleOrderDto;
import com.chauncy.data.mapper.afterSale.OmAfterSaleOrderMapper;
import com.chauncy.data.core.AbstractService;
import com.chauncy.data.mapper.order.OmGoodsTempMapper;
import com.chauncy.data.mapper.order.OmOrderMapper;
import com.chauncy.data.mapper.store.SmStoreMapper;
import com.chauncy.data.vo.app.order.my.afterSale.AfterSaleDetailVo;
import com.chauncy.data.vo.manage.order.afterSale.AfterSaleListVo;
import com.chauncy.order.service.IOmAfterSaleLogService;
import com.chauncy.data.vo.app.order.my.afterSale.ApplyAfterSaleVo;
import com.chauncy.data.vo.app.order.my.afterSale.MyAfterSaleOrderListVo;
import com.chauncy.security.util.SecurityUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 售后订单表 服务实现类
 * </p>
 *
 * @author huangwancheng
 * @since 2019-08-21
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class OmAfterSaleOrderServiceImpl extends AbstractService<OmAfterSaleOrderMapper, OmAfterSaleOrderPo> implements IOmAfterSaleOrderService {

    @Autowired
    private OmAfterSaleOrderMapper mapper;

    @Autowired
    private OmGoodsTempMapper goodsTempMapper;

    @Autowired
    private OmOrderMapper omOrderMapper;

    @Autowired
    private SmStoreMapper storeMapper;

    @Autowired
    private SecurityUtil securityUtil;


    @Autowired
    private IOmAfterSaleLogService afterSaleLogService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplyAfterSaleVo validCanAfterSaleVo(Long goodsTempId) {

        OmGoodsTempPo queryGoodsTemp = goodsTempMapper.selectById(goodsTempId);

        //用户点击过一次售后就不能再点击了
        if (!queryGoodsTemp.getCanAfterSale()) {
            throw new ServiceException(ResultCode.FAIL, "该商品已售后过，不能再进行售后！");
        }

        OmOrderPo queryOrder = omOrderMapper.selectById(queryGoodsTemp.getOrderId());

        OrderStatusEnum orderStatus = queryOrder.getStatus();
        if (!orderStatus.canAfterSale()) {
            throw new ServiceException(ResultCode.FAIL, String.format("%s的订单状态没有售后资格！", orderStatus.getName()));
        }
        String goodsType = queryOrder.getGoodsType();
        if ("自取".equals(goodsType) || "服务类".equals(goodsType)) {
            throw new ServiceException(ResultCode.FAIL, "自取或服务类商品不能进行售后！");
        }

        //售后截止时间
        LocalDateTime afterSaleDeadline = queryOrder.getAfterSaleDeadline();

        if (afterSaleDeadline != null && LocalDateTime.now().isAfter(afterSaleDeadline)) {
            throw new ServiceException(ResultCode.FAIL, "已超过售后截止时间，该商品不能进行售后！");
        }

        //设置用户不可再进行售后
        OmGoodsTempPo updateGoodsTemp = new OmGoodsTempPo();
        updateGoodsTemp.setId(queryGoodsTemp.getId()).setCanAfterSale(false);
        goodsTempMapper.updateById(updateGoodsTemp);


        ApplyAfterSaleVo applyAfterSaleVo = new ApplyAfterSaleVo();
        BeanUtils.copyProperties(queryGoodsTemp, applyAfterSaleVo);
        applyAfterSaleVo.setGoodsTempId(queryGoodsTemp.getId());

        return applyAfterSaleVo;
    }

    @Override
    public List<ApplyAfterSaleVo> searchGoodTempsByOrderId(Long orderId) {
        return mapper.searchGoodsTempByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(ApplyRefundDto applyRefundDto) {
        UmUserPo currentUser=securityUtil.getAppCurrUser();

        QueryWrapper<OmGoodsTempPo> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().in(OmGoodsTempPo::getId,applyRefundDto.getGoodsTempIds());
        List<OmGoodsTempPo> queryGoodsTempPos = goodsTempMapper.selectList(queryWrapper);
        //该退款列表最大能退多少钱
        BigDecimal realPayMoney=queryGoodsTempPos.stream().map(x-> BigDecimalUtil.safeMultiply(x.getRealPayMoney(),x.getNumber())).
                reduce(BigDecimal.ZERO,BigDecimal::add);
        if (realPayMoney.compareTo(applyRefundDto.getRefundMoney())<0){
            throw new ServiceException(ResultCode.FAIL,String.format("操作失败！退款金额大于实付金额:【%s】>【%s】",applyRefundDto.getRefundMoney()
            ,realPayMoney));
        }

        List<OmAfterSaleOrderPo> saveAfterSaleOrders= Lists.newArrayList();
        List<OmAfterSaleLogPo> saveAfterSaleLogs=Lists.newArrayList();
        applyRefundDto.getGoodsTempIds().forEach(x->{
            //商品快照详情
            OmAfterSaleOrderPo saveAfterSaleOrder=new OmAfterSaleOrderPo();
            //复制reason、refundMoney、pictures
            BeanUtils.copyProperties(applyRefundDto,saveAfterSaleOrder,"goodsTempId");
            saveAfterSaleOrder.setGoodsTempId(x);
            //商品名称和数量
            OmGoodsTempPo queryGoodsTemp = queryGoodsTempPos.stream().filter(y -> y.getId().equals(x)).findFirst().orElse(null);
            saveAfterSaleOrder.setGoodsName(queryGoodsTemp.getName()).setNumber(queryGoodsTemp.getNumber());
            //订单号、店铺id、订单类型
            OmOrderPo queryOrder=omOrderMapper.selectById(queryGoodsTemp.getOrderId());
            saveAfterSaleOrder.setOrderId(queryOrder.getId()).setStoreId(queryOrder.getStoreId()).setGoodsType(queryOrder.getGoodsType());
            //店铺名称
            SmStorePo queryStore = storeMapper.selectById(queryOrder.getStoreId());
            saveAfterSaleOrder.setStoreName(queryStore.getName());
            //创建者、用户手机
            saveAfterSaleOrder.setCreateBy(currentUser.getId().toString()).setPhone(currentUser.getPhone()).setAfterSaleType(applyRefundDto.getType())
            .setStatus(AfterSaleStatusEnum.NEED_STORE_DO).setId(SnowFlakeUtil.getFlowIdInstance().nextId());
            saveAfterSaleOrders.add(saveAfterSaleOrder);

            //售后进度日志
            OmAfterSaleLogPo saveAfterSaleLog=new OmAfterSaleLogPo();
            saveAfterSaleLog.setCreateBy(currentUser.getId().toString()).setAfterSaleOrderId(saveAfterSaleOrder.getId()).
                    setDescribes(applyRefundDto.getDescribe());
            if (applyRefundDto.getType()==AfterSaleTypeEnum.ONLY_REFUND){
                saveAfterSaleLog.setNode(AfterSaleLogEnum.ONLY_REFUND_BUYER_START);
            }
            else {
                saveAfterSaleLog.setNode(AfterSaleLogEnum.BUYER_START);

            }
            saveAfterSaleLogs.add(saveAfterSaleLog);

        });
        saveBatch(saveAfterSaleOrders);
        afterSaleLogService.saveBatch(saveAfterSaleLogs);


        //该商品设置不可再进行售后
        OmGoodsTempPo updateGoodsTemp=new OmGoodsTempPo();
        updateGoodsTemp.setCanAfterSale(false);
        UpdateWrapper<OmGoodsTempPo> updateWrapper=new UpdateWrapper<>();
        updateWrapper.lambda().in(OmGoodsTempPo::getId,applyRefundDto.getGoodsTempIds());
        goodsTempMapper.update(updateGoodsTemp,updateWrapper);

    }

    @Override
    public PageInfo<MyAfterSaleOrderListVo> searchAfterSaleOrderList(BasePageDto basePageDto) {
        UmUserPo currentUser=securityUtil.getAppCurrUser();


        return PageHelper.startPage(basePageDto.getPageNo(), basePageDto.getPageSize()).
                doSelectPageInfo(() -> mapper.searchAfterSaleOrderList(currentUser.getId()));

    }

    @Override
    public PageInfo<AfterSaleListVo> searchAfterList(SearchAfterSaleOrderDto searchAfterSaleOrderDto) {
        SysUserPo currUser = securityUtil.getCurrUser();
        Long storeId = null;
        //如果是商家，只能查找自己店铺下的售后订单
        if (currUser.getSystemType().equals(SecurityConstant.SYS_TYPE_SUPPLIER)){
            storeId=currUser.getStoreId();
        }
        Long finalStoreId = storeId;
        PageInfo<AfterSaleListVo> afterSaleListVoPageInfo = PageHelper.startPage(searchAfterSaleOrderDto.getPageNo(), searchAfterSaleOrderDto.getPageSize()).
                doSelectPageInfo(() -> mapper.searchAfterList(searchAfterSaleOrderDto, finalStoreId));
        afterSaleListVoPageInfo.getList().forEach(x->{
            x.setAfterSaleLogVos(mapper.searchCheckList(x.getAfterSaleOrderId()));
        });
        return afterSaleListVoPageInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void permitRefund(Long afterSaleOrderId) {

        SysUserPo currUser = securityUtil.getCurrUser();
        // TODO: 2019/9/2 调用退款接口
        OmAfterSaleOrderPo queryAfterSaleOrder=mapper.selectById(afterSaleOrderId);

        //添加售后进度
        OmAfterSaleLogPo saveAfterSaleLog=new OmAfterSaleLogPo();
        //仅退款只有待商家处理状态才可以进行退款
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.ONLY_REFUND){
            if (queryAfterSaleOrder.getStatus()!=AfterSaleStatusEnum.NEED_STORE_DO){
                throw new ServiceException(ResultCode.FAIL,String.format("当前售后订单状态为【%s】,不允许退款",queryAfterSaleOrder.getStatus().getName()));
            }
            else {
                saveAfterSaleLog.setNode(AfterSaleLogEnum.ONLY_REFUND_STORE_AGREE);
            }
        }
        //退货退款只有待商家退款状态才可以进行退款
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.RETURN_GOODS){
            if (queryAfterSaleOrder.getStatus()!=AfterSaleStatusEnum.NEED_STORE_REFUND){
                throw new ServiceException(ResultCode.FAIL,String.format("当前售后订单状态为【%s】,不允许退款",queryAfterSaleOrder.getStatus().getName()));
            }
            else {
                saveAfterSaleLog.setNode(AfterSaleLogEnum.STORE_AGREE_REFUND);
            }
        }
        saveAfterSaleLog.setAfterSaleOrderId(afterSaleOrderId).setCreateBy(currUser.getId());
        afterSaleLogService.save(saveAfterSaleLog);

        //改变售后订单状态
        OmAfterSaleOrderPo updateAfterOrder=new OmAfterSaleOrderPo();
        updateAfterOrder.setId(afterSaleOrderId).setUpdateBy(currUser.getId()).setStatus(AfterSaleStatusEnum.SUCCESS);
        mapper.updateById(updateAfterOrder);

        //设置商品快照表售后成功
        OmGoodsTempPo updateGoodsTemp=new OmGoodsTempPo();
        updateGoodsTemp.setId(queryAfterSaleOrder.getGoodsTempId()).setIsAfterSale(true).setUpdateBy(currUser.getId());
        goodsTempMapper.updateById(updateGoodsTemp);


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refuseRefund(Long afterSaleOrderId) {
        SysUserPo currUser = securityUtil.getCurrUser();

        OmAfterSaleOrderPo queryAfterSaleOrder=mapper.selectById(afterSaleOrderId);

        //添加售后进度
        OmAfterSaleLogPo saveAfterSaleLog=new OmAfterSaleLogPo();
        //仅退款只有待商家处理状态才可以进行退款
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.ONLY_REFUND){
            if (queryAfterSaleOrder.getStatus()!=AfterSaleStatusEnum.NEED_STORE_DO){
                throw new ServiceException(ResultCode.FAIL,String.format("当前售后订单状态为【%s】,不允许拒绝退款",queryAfterSaleOrder.getStatus().getName()));
            }
            else {
                saveAfterSaleLog.setNode(AfterSaleLogEnum.ONLY_REFUND_STORE_REFUSE);
            }
        }
        //退货退款只有待商家退款状态才可以进行退款
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.RETURN_GOODS){
            if (queryAfterSaleOrder.getStatus()!=AfterSaleStatusEnum.NEED_STORE_REFUND){
                throw new ServiceException(ResultCode.FAIL,String.format("当前售后订单状态为【%s】,不允许拒绝退款",queryAfterSaleOrder.getStatus().getName()));
            }
            else {
                saveAfterSaleLog.setNode(AfterSaleLogEnum.STORE_REFUSE_REFUND);
            }
        }
        saveAfterSaleLog.setAfterSaleOrderId(afterSaleOrderId).setCreateBy(currUser.getId());
        afterSaleLogService.save(saveAfterSaleLog);
        //改变售后订单状态
        OmAfterSaleOrderPo updateAfterOrder=new OmAfterSaleOrderPo();
        updateAfterOrder.setId(afterSaleOrderId).setUpdateBy(currUser.getId()).setStatus(AfterSaleStatusEnum.NEED_BUYER_DO);
        mapper.updateById(updateAfterOrder);

    }

    @Override
    public void permitReturnGoods(Long afterSaleOrderId) {

        SysUserPo currUser = securityUtil.getCurrUser();

        OmAfterSaleOrderPo queryAfterSaleOrder=mapper.selectById(afterSaleOrderId);

        //添加售后进度
        OmAfterSaleLogPo saveAfterSaleLog=new OmAfterSaleLogPo();
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.ONLY_REFUND) {
            throw new ServiceException(ResultCode.FAIL,"仅退款订单不允许确认退货！");

        }
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.RETURN_GOODS){
            if (queryAfterSaleOrder.getStatus()!=AfterSaleStatusEnum.NEED_STORE_DO){
                throw new ServiceException(ResultCode.FAIL,String.format("当前售后订单状态为【%s】,不允许确认退货",queryAfterSaleOrder.getStatus().getName()));
            }
            else {
                saveAfterSaleLog.setNode(AfterSaleLogEnum.STORE_AGREE_GOODS);
            }
        }
        saveAfterSaleLog.setAfterSaleOrderId(afterSaleOrderId).setCreateBy(currUser.getId());
        afterSaleLogService.save(saveAfterSaleLog);
        //改变售后订单状态
        OmAfterSaleOrderPo updateAfterOrder=new OmAfterSaleOrderPo();
        updateAfterOrder.setId(afterSaleOrderId).setUpdateBy(currUser.getId()).setStatus(AfterSaleStatusEnum.NEED_BUYER_RETURN);
        mapper.updateById(updateAfterOrder);

    }

    @Override
    public void refuseReturnGoods(Long afterSaleOrderId) {
        SysUserPo currUser = securityUtil.getCurrUser();

        OmAfterSaleOrderPo queryAfterSaleOrder=mapper.selectById(afterSaleOrderId);

        //添加售后进度
        OmAfterSaleLogPo saveAfterSaleLog=new OmAfterSaleLogPo();
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.ONLY_REFUND) {
            throw new ServiceException(ResultCode.FAIL,"仅退款订单不允许拒绝退货！");

        }
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.RETURN_GOODS){
            if (queryAfterSaleOrder.getStatus()!=AfterSaleStatusEnum.NEED_STORE_DO){
                throw new ServiceException(ResultCode.FAIL,String.format("当前售后订单状态为【%s】,不允许拒绝退货",queryAfterSaleOrder.getStatus().getName()));
            }
            else {
                saveAfterSaleLog.setNode(AfterSaleLogEnum.STORE_REFUSE_GOODS);
            }
        }
        saveAfterSaleLog.setAfterSaleOrderId(afterSaleOrderId).setCreateBy(currUser.getId());
        afterSaleLogService.save(saveAfterSaleLog);
        //改变售后订单状态
        OmAfterSaleOrderPo updateAfterOrder=new OmAfterSaleOrderPo();
        updateAfterOrder.setId(afterSaleOrderId).setUpdateBy(currUser.getId()).setStatus(AfterSaleStatusEnum.NEED_BUYER_DO);
        mapper.updateById(updateAfterOrder);

    }

    @Override
    public void permitReceiveGoods(Long afterSaleOrderId) {
        SysUserPo currUser = securityUtil.getCurrUser();

        OmAfterSaleOrderPo queryAfterSaleOrder=mapper.selectById(afterSaleOrderId);

        //添加售后进度
        OmAfterSaleLogPo saveAfterSaleLog=new OmAfterSaleLogPo();
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.ONLY_REFUND) {
            throw new ServiceException(ResultCode.FAIL,"仅退款订单不允许确认收货！");

        }
        if (queryAfterSaleOrder.getAfterSaleType()==AfterSaleTypeEnum.RETURN_GOODS){
            if (queryAfterSaleOrder.getStatus()!=AfterSaleStatusEnum.NEED_BUYER_RETURN){
                throw new ServiceException(ResultCode.FAIL,String.format("当前售后订单状态为【%s】,不允许确认收货",queryAfterSaleOrder.getStatus().getName()));
            }
            else {
                saveAfterSaleLog.setNode(AfterSaleLogEnum.BUYER_RETURN_GOODS);
            }
        }
        saveAfterSaleLog.setAfterSaleOrderId(afterSaleOrderId).setCreateBy(currUser.getId());
        afterSaleLogService.save(saveAfterSaleLog);
        //改变售后订单状态
        OmAfterSaleOrderPo updateAfterOrder=new OmAfterSaleOrderPo();
        updateAfterOrder.setId(afterSaleOrderId).setUpdateBy(currUser.getId()).setStatus(AfterSaleStatusEnum.NEED_STORE_REFUND);
        mapper.updateById(updateAfterOrder);

    }

    @Override
    public AfterSaleDetailVo getAfterSaleDetail(Long afterSaleOrderId) {
        AfterSaleDetailVo afterSaleDetail = mapper.getAfterSaleDetail(afterSaleOrderId);
        //售后关闭和售后成功都不需要倒计时
        if (afterSaleDetail.getAfterSaleStatusEnum()!=AfterSaleStatusEnum.CLOSE&&
                afterSaleDetail.getAfterSaleStatusEnum()!=AfterSaleStatusEnum.SUCCESS){
            LocalDateTime expireTime = afterSaleDetail.getOperatingTime().plusDays(3);
            Duration duration=Duration.between(LocalDateTime.now(),expireTime);
            afterSaleDetail.setRemainMinute(duration.toMinutes());
        }
        //售后说明和售后提示
        AfterSaleLogEnum node = afterSaleDetail.getNode();
        afterSaleDetail.setContentExplain(node.getContentExplain());
        afterSaleDetail.setContentTips(node.getContentTips());
        return afterSaleDetail;
    }
}