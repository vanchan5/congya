package com.chauncy.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.chauncy.common.enums.system.ResultCode;
import com.chauncy.common.exception.sys.ServiceException;
import com.chauncy.common.util.BigDecimalUtil;
import com.chauncy.common.util.ListUtil;
import com.chauncy.data.bo.app.car.MoneyShipBo;
import com.chauncy.data.bo.base.BaseBo;
import com.chauncy.data.bo.supplier.good.GoodsValueBo;
import com.chauncy.data.core.AbstractService;
import com.chauncy.data.domain.po.area.AreaRegionPo;
import com.chauncy.data.domain.po.order.OmOrderPo;
import com.chauncy.data.domain.po.order.OmGoodsTempPo;
import com.chauncy.data.domain.po.order.OmShoppingCartPo;
import com.chauncy.data.domain.po.product.*;
import com.chauncy.data.domain.po.store.SmStorePo;
import com.chauncy.data.domain.po.sys.BasicSettingPo;
import com.chauncy.data.domain.po.user.PmMemberLevelPo;
import com.chauncy.data.domain.po.user.UmAreaShippingPo;
import com.chauncy.data.domain.po.user.UmUserPo;
import com.chauncy.data.dto.app.car.SettleAccountsDto;
import com.chauncy.data.dto.app.car.SettleDto;
import com.chauncy.data.dto.app.car.SubmitOrderDto;
import com.chauncy.data.dto.app.order.cart.add.AddCartDto;
import com.chauncy.data.dto.app.order.cart.select.SearchCartDto;
import com.chauncy.data.mapper.area.AreaRegionMapper;
import com.chauncy.data.mapper.order.OmShoppingCartMapper;
import com.chauncy.data.mapper.product.*;
import com.chauncy.data.mapper.store.SmStoreMapper;
import com.chauncy.data.mapper.sys.BasicSettingMapper;
import com.chauncy.data.mapper.user.PmMemberLevelMapper;
import com.chauncy.data.mapper.user.UmAreaShippingMapper;
import com.chauncy.data.vo.app.car.*;
import com.chauncy.data.vo.app.goods.SpecifiedGoodsVo;
import com.chauncy.data.vo.app.goods.SpecifiedSkuVo;
import com.chauncy.data.mapper.product.PmGoodsMapper;
import com.chauncy.data.mapper.product.PmGoodsSkuMapper;
import com.chauncy.data.vo.app.order.cart.CartVo;
import com.chauncy.data.vo.app.order.cart.StoreGoodsVo;
import com.chauncy.data.vo.supplier.GoodsStandardVo;
import com.chauncy.data.vo.supplier.StandardValueAndStatusVo;
import com.chauncy.order.service.IOmShoppingCartService;
import com.chauncy.security.util.SecurityUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import org.assertj.core.util.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车列表 服务实现类
 * </p>
 *
 * @author huangwancheng
 * @since 2019-07-04
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class OmShoppingCartServiceImpl extends AbstractService<OmShoppingCartMapper, OmShoppingCartPo> implements IOmShoppingCartService {

    @Autowired
    private OmShoppingCartMapper mapper;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private PmGoodsSkuMapper skuMapper;

    @Autowired
    private PmGoodsMapper goodsMapper;

    @Autowired
    private PmGoodsAttributeMapper goodsAttributeMapper;

    @Autowired
    private PmGoodsRelAttributeValueSkuMapper relAttributeValueSkuMapper;

    @Autowired
    private PmGoodsAttributeValueMapper attributeValueMapper;

    @Autowired
    private BasicSettingMapper basicSettingMapper;

    @Autowired
    private PmMemberLevelMapper levelMapper;

    @Autowired
    private SmStoreMapper storeMapper;

    @Autowired
    private UmAreaShippingMapper umAreaShippingMapper;

    @Autowired
    private AreaRegionMapper areaRegionMapper;

    @Autowired
    private PmMoneyShippingMapper moneyShippingMapper;

    //需要进行实行认证且计算税率的商品类型
    private final List<String> needRealGoodsType = Lists.newArrayList("BONDED", "OVERSEA");

    /**
     * 添加商品到购物车
     *
     * @param addCartDto
     * @return
     */
    @Override
    public void addToCart(AddCartDto addCartDto) {
        //获取当前app用户信息
        UmUserPo umUserPo = securityUtil.getAppCurrUser();
        //判断购物车是否存在该商品
        Map<String, Object> map = new HashMap<>();
        map.put("sku_id", addCartDto.getSkuId());
        List<OmShoppingCartPo> shoppingCartPos = mapper.selectByMap(map);
        boolean exit = shoppingCartPos != null && shoppingCartPos.size() != 0;
        //判断当前库存是否足够
        Integer originStock = skuMapper.selectById(addCartDto.getSkuId()).getStock();

        //查找购物车是否已经存在该商品
        if (exit) {
            QueryWrapper queryWrapper = new QueryWrapper();
            queryWrapper.eq("sku_id", addCartDto.getSkuId());
            OmShoppingCartPo shoppingCartPo = mapper.selectOne(queryWrapper);
            shoppingCartPo.setNum(shoppingCartPo.getNum() + addCartDto.getNum());
            if (originStock < shoppingCartPo.getNum()) {
                throw new ServiceException(ResultCode.NSUFFICIENT_INVENTORY, "库存不足!");
            }
            mapper.updateById(shoppingCartPo);
        }
        //不存在
        else {
            if (originStock < addCartDto.getNum()) {
                throw new ServiceException(ResultCode.NSUFFICIENT_INVENTORY, "库存不足!");
            }
            OmShoppingCartPo omShoppingCartPo = new OmShoppingCartPo();
            BeanUtils.copyProperties(addCartDto, omShoppingCartPo);
            omShoppingCartPo.setUserId(umUserPo.getId());
            omShoppingCartPo.setId(null);
            omShoppingCartPo.setCreateBy(umUserPo.getName());
            mapper.insert(omShoppingCartPo);
        }
    }

    /**
     * 查看购物车
     *
     * @return
     */
    @Override
    public PageInfo<CartVo> SearchCart(SearchCartDto searchCartDto) {
        //获取当前用户
        UmUserPo userPo = securityUtil.getAppCurrUser();
        Integer pageNo = searchCartDto.getPageNo() == null ? defaultPageNo : searchCartDto.getPageNo();
        Integer pageSize = searchCartDto.getPageSize() == null ? defaultPageSize : searchCartDto.getPageSize();
        PageInfo<CartVo> cartVoPageInfo = PageHelper.startPage(pageNo, pageSize)
                .doSelectPageInfo(() -> mapper.searchCart(userPo.getId()));
        //对购物车库存处理
        cartVoPageInfo.getList().forEach(a -> {
            List<StoreGoodsVo> storeGoodsVos = Lists.newArrayList();
            a.getStoreGoodsVoList().forEach(b -> {
                Integer sum = skuMapper.selectById(b.getSkuId()).getStock();
                b.setSum(sum);
                //库存不足处理
                if (sum == 0) {
                    b.setIsSoldOut(true);
                    b.setNum(0);
                } else if (b.getNum() >= sum) {
                    b.setNum(sum);
                }
                //下架处理,宝贝失效处理
                if (goodsMapper.selectById(skuMapper.selectById(b.getSkuId()).getGoodsId()).getPublishStatus() != null) {
                    boolean publish = goodsMapper.selectById(skuMapper.selectById(b.getSkuId()).getGoodsId()).getPublishStatus();
                    if (!publish) {
                        b.setIsObtained(true);
                    }
                }
                storeGoodsVos.add(b);
            });
            a.setStoreGoodsVoList(storeGoodsVos);
        });

        return cartVoPageInfo;
    }

    /**
     * 批量删除购物车
     *
     * @return
     */
    @Override
    public void delCart(Long[] ids) {
        //判断商品是否存在
        Arrays.asList(ids).forEach(a -> {
            if (mapper.selectById(a) == null) {
                throw new ServiceException(ResultCode.NO_EXISTS, "数据不存在");
            }
        });
        mapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 修改购物车商品
     *
     * @param updateCartDto
     * @return
     */
    @Override
    public void updateCart(AddCartDto updateCartDto) {
        OmShoppingCartPo cartPo = new OmShoppingCartPo();
        BeanUtils.copyProperties(updateCartDto, cartPo);
        mapper.updateById(cartPo);
    }

    @Override
    public TotalCarVo searchByIds(SettleDto settleDto, UmUserPo currentUser) {
        List<SettleAccountsDto> settleAccountsDtos = settleDto.getSettleAccountsDtos();
        List<Long> skuIds = settleAccountsDtos.stream().map(x -> x.getSkuId()).collect(Collectors.toList());
        List<ShopTicketSoWithCarGoodVo> shopTicketSoWithCarGoodVos = mapper.searchByIds(skuIds);

        //sku对应的数量
        shopTicketSoWithCarGoodVos.forEach(x -> {
            //为查询后的id匹配上用户下单的数量
            x.setNumber(settleAccountsDtos.stream().filter(y -> y.getSkuId() == x.getId()).findFirst().get().getNumber());
        });

        TotalCarVo totalCarVo = new TotalCarVo();


        //判断商品是否保税仓或者海外直邮，是则需要进行实名认证，且计算税率
        List<ShopTicketSoWithCarGoodVo> needRealSkuList = shopTicketSoWithCarGoodVos.stream().filter(x -> needRealGoodsType.contains(x.getGoodsType())).collect(Collectors.toList());
        if (!ListUtil.isListNullAndEmpty(needRealSkuList)) {
            // TODO: 2019/7/14  实名认证
        }

        //获取系统基本设置
        BasicSettingPo basicSettingPo = basicSettingMapper.selectOne(new QueryWrapper<>());

        //设置会员等级比例和购物券比例
        setPurchasePresentAndMoneyToShopTicket(shopTicketSoWithCarGoodVos, currentUser, basicSettingPo);
        //拆单后的信息包括红包 购物券 金额 优惠 运费等
        List<StoreOrderVo> storeOrderVos = getBySkuIds(shopTicketSoWithCarGoodVos, currentUser, basicSettingPo, settleDto.getAreaShipId());

        totalCarVo.setStoreOrderVos(storeOrderVos);

        //商品总额=所有订单金额的总和
        BigDecimal totalMoney = storeOrderVos.stream().map(x ->
                x.getGoodsTypeOrderVos().stream().map(GoodsTypeOrderVo::getTotalMoney).reduce(BigDecimal.ZERO, BigDecimal::add)
        ).reduce(BigDecimal.ZERO, BigDecimal::add);
        //总数量
        int totalNumber = settleAccountsDtos.stream().mapToInt(SettleAccountsDto::getNumber).sum();
        //奖励购物券=所有订单的总和
        BigDecimal totalRewardShopTicket = storeOrderVos.stream().map(x ->
                x.getGoodsTypeOrderVos().stream().map(GoodsTypeOrderVo::getRewardShopTicket).reduce(BigDecimal.ZERO, BigDecimal::add)
        ).reduce(BigDecimal.ZERO, BigDecimal::add);
        //红包=所有订单的总和
        BigDecimal totalRedEnvelops = storeOrderVos.stream().map(x ->
                x.getGoodsTypeOrderVos().stream().map(y -> y.getRedEnvelops()).reduce(BigDecimal.ZERO, BigDecimal::add)
        ).reduce(BigDecimal.ZERO, BigDecimal::add);

        //购物券=所有订单的总和
        BigDecimal totalShopTicket = storeOrderVos.stream().map(x ->
                x.getGoodsTypeOrderVos().stream().map(GoodsTypeOrderVo::getShopTicket).reduce(BigDecimal.ZERO, BigDecimal::add)
        ).reduce(BigDecimal.ZERO, BigDecimal::add);
        //可抵扣金额=所有订单的总和
        BigDecimal totalDeductionMoney = storeOrderVos.stream().map(x ->
                x.getGoodsTypeOrderVos().stream().map(GoodsTypeOrderVo::getDeductionMoney).reduce(BigDecimal.ZERO, BigDecimal::add)
        ).reduce(BigDecimal.ZERO, BigDecimal::add);
        //总优惠=所有订单的总和
        BigDecimal totalDiscount = storeOrderVos.stream().map(x ->
                x.getGoodsTypeOrderVos().stream().map(GoodsTypeOrderVo::getTotalDiscount).reduce(BigDecimal.ZERO, BigDecimal::add)
        ).reduce(BigDecimal.ZERO, BigDecimal::add);


        //总实付金额=所有订单的总和
        BigDecimal totalRealPayMoney = storeOrderVos.stream().map(x ->
                x.getGoodsTypeOrderVos().stream().map(GoodsTypeOrderVo::getRealPayMoney).reduce(BigDecimal.ZERO, BigDecimal::add)
        ).reduce(BigDecimal.ZERO, BigDecimal::add);
        //总邮费=所有订单的总和
        BigDecimal totalShipMoney = storeOrderVos.stream().map(x ->
                x.getGoodsTypeOrderVos().stream().map(y->y.getShipMoney()).reduce(BigDecimal.ZERO, BigDecimal::add)
        ).reduce(BigDecimal.ZERO, BigDecimal::add);
        //总税费=所有订单的总和
        BigDecimal totalTaxMoney = storeOrderVos.stream().map(x ->
                x.getGoodsTypeOrderVos().stream().map(y->y.getTaxMoney()==null?BigDecimal.ZERO:y.getTaxMoney()).reduce(BigDecimal.ZERO, BigDecimal::add)
        ).reduce(BigDecimal.ZERO, BigDecimal::add);
        //红包和购物券可以抵扣多少钱
        BigDecimal totalShopTicketMoney=BigDecimalUtil.safeDivide(totalShopTicket,basicSettingPo.getMoneyToShopTicket());
        BigDecimal totalRedEnvelopsMoney=BigDecimalUtil.safeDivide(totalRedEnvelops,basicSettingPo.getMoneyToCurrentRedEnvelops());

        totalCarVo.setTotalMoney(totalMoney).setTotalNumber(totalNumber).setTotalRewardShopTicket(totalRewardShopTicket)
        .setTotalRedEnvelops(totalRedEnvelops).setTotalShopTicket(totalShopTicket).setTotalDeductionMoney(totalDeductionMoney)
        .setTotalDiscount(totalDiscount).setTotalRealPayMoney(totalRealPayMoney).setTotalShipMoney(totalShipMoney)
        .setTotalTaxMoney(totalTaxMoney).setTotalRedEnvelopsMoney(totalRedEnvelopsMoney).setTotalShopTicketMoney(totalShopTicketMoney);


        return totalCarVo;
    }

    /**
     * 根据skuids组装成订单，并根据商家和商品类型进行拆单
     *
     * @param shopTicketSoWithCarGoodVo
     * @return
     */
    private List<StoreOrderVo> getBySkuIds(List<ShopTicketSoWithCarGoodVo> shopTicketSoWithCarGoodVo, UmUserPo currentUser,
                                           BasicSettingPo basicSettingPo, Long areaShipId) {

        //加快sql查询，用代码对店铺和商品类型进行分组拆单
        Map<String, Map<String, List<ShopTicketSoWithCarGoodVo>>> map
                = shopTicketSoWithCarGoodVo.stream().collect(
                Collectors.groupingBy(
                        ShopTicketSoWithCarGoodVo::getStoreName, Collectors.groupingBy(ShopTicketSoWithCarGoodVo::getGoodsType)
                )
        );

        //商家分组集合
        List<StoreOrderVo> storeOrderVos = Lists.newArrayList();
        //遍历map,将map组装成vo
        Iterator<Map.Entry<String, Map<String, List<ShopTicketSoWithCarGoodVo>>>> it = map.entrySet().iterator();
        //遍历店铺
        while (it.hasNext()) {
            Map.Entry<String, Map<String, List<ShopTicketSoWithCarGoodVo>>> entry = it.next();
            StoreOrderVo storeOrderVo = new StoreOrderVo();
            storeOrderVo.setStoreName(entry.getKey());
            //商品类型分组集合
            List<GoodsTypeOrderVo> goodsTypeOrderVos = Lists.newArrayList();
            //遍历商品类型,每个类型为一个订单
            for (Map.Entry<String, List<ShopTicketSoWithCarGoodVo>> entry1 : entry.getValue().entrySet()) {
                List<ShopTicketSoWithCarGoodVo> shopTicketSoWithCarGoodVos = entry1.getValue();
                GoodsTypeOrderVo goodsTypeOrderVo = new GoodsTypeOrderVo();
                goodsTypeOrderVo.setGoodsType(entry1.getKey());
                //判断商品是否保税仓或者海外直邮，且计算税率
                if (needRealGoodsType.contains(entry1.getKey())) {
                    //商品的税率=数量*销售价*百分比/100
                    BigDecimal taxMoney = shopTicketSoWithCarGoodVos.stream().map(x -> BigDecimalUtil.safeMultiply(x.getNumber(),
                            BigDecimalUtil.safeMultiply(x.getSellPrice(), transfromDecimal(x.getCustomTaxRate())))).
                            reduce(BigDecimal.ZERO, BigDecimal::add);
                    goodsTypeOrderVo.setTaxMoney(taxMoney);
                }
                else {
                    goodsTypeOrderVo.setTaxMoney(BigDecimal.ZERO);
                }
                //计算邮费
                BigDecimal shipMoney = shopTicketSoWithCarGoodVos.stream().map(x -> getShipMoney(x, areaShipId, currentUser)).reduce(BigDecimal.ZERO, BigDecimal::add);
                goodsTypeOrderVo.setShipMoney(shipMoney);
                //商品详细信息
                goodsTypeOrderVo.setShopTicketSoWithCarGoodVos(shopTicketSoWithCarGoodVos);
                //拆单后商品总数量
                goodsTypeOrderVo.setTotalNumber(shopTicketSoWithCarGoodVos.stream().mapToInt(x -> x.getNumber()).sum());
                //商品总额=数量*单价
                BigDecimal totalMoney = shopTicketSoWithCarGoodVos.stream().map(x -> BigDecimalUtil.safeMultiply(x.getNumber(), x.getSellPrice())).
                        reduce(BigDecimal.ZERO, BigDecimal::add);
                //奖励购物券
                BigDecimal rewardShopTicket = shopTicketSoWithCarGoodVos.stream().map(x -> BigDecimalUtil.safeMultiply(x.getNumber(), x.getRewardShopTicket()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                goodsTypeOrderVo.setTotalMoney(totalMoney).setRewardShopTicket(rewardShopTicket);
                //设置可用红包、购物券、可抵扣金额 需要用到：totalMoney shipMoney taxMoney
                setDiscount(goodsTypeOrderVo, currentUser, basicSettingPo);
                //第一版本总优惠=红包+抵用券
                goodsTypeOrderVo.setTotalDiscount(goodsTypeOrderVo.getDeductionMoney());
                //实付金额 需要用到： totalMoney, shipMoney, taxMoney,totalDiscount
                goodsTypeOrderVo.setRealPayMoney(goodsTypeOrderVo.calculationRealPayMoney());
                goodsTypeOrderVos.add(goodsTypeOrderVo);
            }
            storeOrderVo.setGoodsTypeOrderVos(goodsTypeOrderVos);
            storeOrderVos.add(storeOrderVo);
        }
        return storeOrderVos;

    }

    /**
     * 设置会员等级比例和购物券比例
     *
     * @param shopTicketSoWithCarGoodVoList
     */
    private void setPurchasePresentAndMoneyToShopTicket(List<ShopTicketSoWithCarGoodVo> shopTicketSoWithCarGoodVoList
            , UmUserPo currentUser, BasicSettingPo basicSettingPo
    ) {
        PmMemberLevelPo memberLevelPo = levelMapper.selectById(currentUser.getMemberLevelId());
        shopTicketSoWithCarGoodVoList.forEach(x -> x.setPurchasePresent(memberLevelPo.getPurchasePresent())
                .setMoneyToShopTicket(basicSettingPo.getMoneyToShopTicket())
        );

    }

    /**
     * 设置可用红包、购物券、可抵扣金额
     *
     * @param goodsTypeOrderVo
     */
    private void setDiscount(GoodsTypeOrderVo goodsTypeOrderVo, UmUserPo currentUser, BasicSettingPo basicSettingPo) {
        BigDecimal totalMoney=BigDecimalUtil.safeAdd(goodsTypeOrderVo.getTotalMoney(),goodsTypeOrderVo.getShipMoney()
        ,goodsTypeOrderVo.getTaxMoney());
        //一个购物券=多少元
        BigDecimal shopTicketToMoney = BigDecimalUtil.safeDivide(1, basicSettingPo.getMoneyToShopTicket());
        //一个红包=多少元
        BigDecimal redEnvelopsToMoney = BigDecimalUtil.safeDivide(1, basicSettingPo.getMoneyToCurrentRedEnvelops());
        //当前用户所拥有的红包折算后的金额
        BigDecimal redEnvelopMoney = BigDecimalUtil.safeMultiply(redEnvelopsToMoney, currentUser.getCurrentRedEnvelops());
        //当前用户所拥有的购物券折算后的金额
        BigDecimal shopTicketMoney = BigDecimalUtil.safeMultiply(shopTicketToMoney, currentUser.getCurrentShopTicket());
        //-1表示小于，0是等于，1是大于。
        //如果总金额<=红包折算后的金额
        if (totalMoney.compareTo(redEnvelopMoney) <= 0) {
            //需要用多少个红包
            goodsTypeOrderVo.setRedEnvelops(BigDecimalUtil.safeMultiply(totalMoney, basicSettingPo.getMoneyToCurrentRedEnvelops()));
            goodsTypeOrderVo.setShopTicket(BigDecimal.ZERO);
            goodsTypeOrderVo.setDeductionMoney(totalMoney);
        } else {
            //总金额>红包，红包可以全用
            goodsTypeOrderVo.setRedEnvelops(currentUser.getCurrentRedEnvelops());
            //红包抵扣后还剩下多少钱
            BigDecimal removeRed = BigDecimalUtil.safeSubtract(totalMoney, redEnvelopMoney);
            //如果剩下的金额小于等于购物券折算后的金额
            if (removeRed.compareTo(shopTicketMoney) <= 0) {
                goodsTypeOrderVo.setShopTicket(BigDecimalUtil.safeMultiply(removeRed, basicSettingPo.getMoneyToShopTicket()));
                goodsTypeOrderVo.setDeductionMoney(totalMoney);
            } else {
                //剩下金额大于可抵扣优惠券，优惠券可以全用
                goodsTypeOrderVo.setShopTicket(currentUser.getCurrentShopTicket());
                //扣除优惠券和红包剩下的金额
                goodsTypeOrderVo.setDeductionMoney(BigDecimalUtil.safeAdd(redEnvelopMoney, shopTicketMoney));
            }
        }
        //用了多少红包、购物券、还剩下多少
        currentUser.setCurrentRedEnvelops(BigDecimalUtil.safeSubtract(currentUser.getCurrentRedEnvelops(),goodsTypeOrderVo.getRedEnvelops()));
        currentUser.setCurrentShopTicket(BigDecimalUtil.safeSubtract(currentUser.getCurrentShopTicket(),goodsTypeOrderVo.getShopTicket()));


    }

    /**
     * 算出运费
     *
     * @param shopTicketSoWithCarGoodVo
     * @return
     */
    private BigDecimal getShipMoney(ShopTicketSoWithCarGoodVo shopTicketSoWithCarGoodVo, Long areaShipId, UmUserPo currentUser) {
        //如果该商品包邮
        if (shopTicketSoWithCarGoodVo.getIsFreePostage()){
            return BigDecimal.ZERO;
        }
        //收货城市
        Long myCityId;
        //地址为空，采用默认地址
        if (areaShipId == null) {
            QueryWrapper<UmAreaShippingPo> myAreaWrapper = new QueryWrapper();
            myAreaWrapper.lambda().eq(UmAreaShippingPo::getIsDefault, true);
            myAreaWrapper.lambda().eq(UmAreaShippingPo::getUmUserId, currentUser.getId());
            UmAreaShippingPo umAreaShippingPo = umAreaShippingMapper.selectOne(myAreaWrapper);
            //如果用户没有设置默认地址
            if (umAreaShippingPo == null) {
                return BigDecimal.ZERO;
            } else {
                areaShipId = umAreaShippingPo.getAreaId();
            }
        }
        //收货城市
        myCityId = areaRegionMapper.selectById(areaShipId).getCityId();
        //sku对应的运费信息
        List<MoneyShipBo> moneyShipBos = moneyShippingMapper.loadBySkuId(shopTicketSoWithCarGoodVo.getShippingTemplateId());
        //匹配是否在指定地区内
        MoneyShipBo moneyShipBo = moneyShipBos.stream().filter(x -> myCityId.equals(x.getDestinationId())).
                findFirst().orElse(null);
        //采用默认运费
        if (moneyShipBo==null){
            return getShipByMoney(shopTicketSoWithCarGoodVo,moneyShipBos.get(0).getDefaultFullMoney()
            ,moneyShipBos.get(0).getDefaultPostMoney(),moneyShipBos.get(0).getDefaultFreight()
            );

        }
        //采用指定运费
        else {
            return getShipByMoney(shopTicketSoWithCarGoodVo,moneyShipBo.getDestinationFullMoney()
                    ,moneyShipBo.getDestinationPostMoney(),moneyShipBo.getDestinationBasisFreight()
            );
        }

    }

    /**
     *
     * @param fullMoney 满金额条件
     * @param postMoney 满足后的运费
     * @param basisFreight 基础运费
     * @return
     */
    private BigDecimal getShipByMoney(ShopTicketSoWithCarGoodVo carGoodVo,BigDecimal fullMoney,
                                      BigDecimal postMoney,BigDecimal basisFreight){
        //这个商品买了多少钱=售价*数量
        BigDecimal price=BigDecimalUtil.safeMultiply(carGoodVo.getSellPrice(),carGoodVo.getNumber());
        //满足金额优惠条件
        if (price.compareTo(fullMoney)>=0){
            return postMoney;
        }
        else {
            return BigDecimalUtil.safeMultiply(carGoodVo.getNumber(),basisFreight);
        }




    }

    /**
     * 将百分比转换成小数
     *
     * @param bigDecimal
     * @return
     */
    private BigDecimal transfromDecimal(BigDecimal bigDecimal) {
        return BigDecimalUtil.safeDivide(bigDecimal, 100);
    }


    /**
     * 查看商品详情
     *
     * @param goodsId
     * @return
     */
    @Override
    public SpecifiedGoodsVo selectSpecifiedGoods(Long goodsId) {

        SpecifiedGoodsVo specifiedGoodsVo = new SpecifiedGoodsVo();
        List<GoodsStandardVo> goodsStandardVoList = Lists.newArrayList();

        PmGoodsPo goodsPo = goodsMapper.selectById(goodsId);
        //判断该商品是否存在
        if (goodsPo == null) {
            throw new ServiceException(ResultCode.NO_EXISTS, "数据库不存在该商品！");
        }

        /**获取商品下的所有规格信息*/
        //获取对应的分类ID
        Long categoryId = goodsPo.getGoodsCategoryId();
        //获取分类下所有的规格
        List<BaseBo> goodsAttributePos = goodsAttributeMapper.findStandardName(categoryId);
        //遍历规格名称
        goodsAttributePos.forEach(x -> {
            List<GoodsValueBo> goodsValues = goodsMapper.findGoodsValue(goodsId, x.getId());
            if (goodsValues != null && goodsValues.size() != 0) {
                //获取规格名称和规格ID
                GoodsStandardVo goodsStandardVo = new GoodsStandardVo();
                goodsStandardVo.setAttributeId(x.getId());
                goodsStandardVo.setAttributeName(x.getName());
                //获取该商品下的属性下的所属的规格值信息
                List<StandardValueAndStatusVo> attributeValueInfos = Lists.newArrayList();
                goodsValues.forEach(a -> {
                    StandardValueAndStatusVo standardValueAndStatusVo = new StandardValueAndStatusVo();
                    standardValueAndStatusVo.setIsInclude(true);
                    standardValueAndStatusVo.setAttributeValueId(a.getId());
                    standardValueAndStatusVo.setAttributeValue(a.getName());
                    attributeValueInfos.add(standardValueAndStatusVo);
                });
                goodsStandardVo.setAttributeValueInfos(attributeValueInfos);
                goodsStandardVoList.add(goodsStandardVo);
            }
        });
        specifiedGoodsVo.setGoodsStandardVoList(goodsStandardVoList);

        /**获取商品规格的具体信息*/
        Map<String, Object> query = new HashMap<>();
        query.put("goods_id", goodsId);
        query.put("del_flag", false);
        List<PmGoodsSkuPo> goodsSkuPos = skuMapper.selectByMap(query);
        if (goodsSkuPos == null && goodsSkuPos.size() == 0) {
            return null;
        }
        Map<String, SpecifiedSkuVo> skuDetail = Maps.newHashMap();
        //循环获取sku信息
        goodsSkuPos.forEach(b -> {
            SpecifiedSkuVo specifiedSkuVo = new SpecifiedSkuVo();
            specifiedSkuVo.setHoldQuantity(goodsPo.getPurchaseLimit());
            specifiedSkuVo.setStock(b.getStock());
            specifiedSkuVo.setSellAbleQuantity(b.getStock());
            specifiedSkuVo.setOverSold(false);
            specifiedSkuVo.setPrice(b.getSellPrice());
            specifiedSkuVo.setSkuId(b.getId());
            specifiedSkuVo.setPictrue(b.getPicture());
            if (b.getStock() == 0) {
                specifiedSkuVo.setOverSold(true);
            }
            QueryWrapper queryWrapper = new QueryWrapper();
            queryWrapper.eq("del_flag", false);
            queryWrapper.eq("goods_sku_id", b.getId());
            List<PmGoodsRelAttributeValueSkuPo> relAttributeValueSkuPoList = relAttributeValueSkuMapper.selectList(queryWrapper);
            //拼接规格ID和规格值ID
            StringBuffer relIds = new StringBuffer(";");
            relAttributeValueSkuPoList.forEach(c -> {
                PmGoodsAttributeValuePo attributeValuePo = attributeValueMapper.selectById(c.getGoodsAttributeValueId());

                Long attributeValueId = attributeValuePo.getId();
                String attributeValue = attributeValuePo.getValue();
                Long attributeId = attributeValuePo.getProductAttributeId();
                relIds.append(attributeId).append(":").append(attributeValueId).append(";");
            });
            skuDetail.put(String.valueOf(relIds), specifiedSkuVo);
            specifiedGoodsVo.setSkuDetail(skuDetail);
        });
        return specifiedGoodsVo;
    }

    @Override
    public void submitOrder(SubmitOrderDto submitOrderDto, UmUserPo currentUser) {

        //生成订单快照
        OmGoodsTempPo saveOrderTemp = new OmGoodsTempPo();
        //复制商品总额、预计奖励购物券、使用购物券、使用红包、运费、税费
        //实际付款、总优惠
        BeanUtils.copyProperties(submitOrderDto, saveOrderTemp);

        List<OmOrderPo> saveOrders = Lists.newArrayList();
        //循环遍历 生成订单
        submitOrderDto.getStoreOrderVos().forEach(x -> {
            //店铺id
            QueryWrapper<SmStorePo> storeWrapper = new QueryWrapper();
            storeWrapper.lambda().eq(SmStorePo::getName, x.getStoreName());
            Long storeId = storeMapper.selectOne(storeWrapper).getId();
            x.getGoodsTypeOrderVos().forEach(y -> {
                //拆单后订单总金额
                BigDecimal totalMoney = y.getShopTicketSoWithCarGoodVos().stream().
                        map(ShopTicketSoWithCarGoodVo::getSellPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                //拆单后订单总数量
                int totalNumber = y.getShopTicketSoWithCarGoodVos().stream().mapToInt(ShopTicketSoWithCarGoodVo::getNumber).sum();
                //订单金额占所有商品金额的比例
                BigDecimal ratio = BigDecimalUtil.safeDivide(totalMoney, submitOrderDto.getTotalMoney());
                //订单按比例换算后的购物券
                BigDecimal orderShopTicket = BigDecimalUtil.safeMultiply(ratio, submitOrderDto.getShopTicket());
                //订单按比例换算后的红包
                BigDecimal orderRedEnvelops = BigDecimalUtil.safeMultiply(ratio, submitOrderDto.getRedEnvelops());

                //生成订单
                OmOrderPo saveOrder = new OmOrderPo();
                BeanUtils.copyProperties(submitOrderDto, saveOrder);
                saveOrder.setUmUserId(currentUser.getId()).setStoreId(storeId).setCreateBy(currentUser.getPhone()).
                        setTotalMoney(totalMoney).setTotalNumber(totalNumber).setGoodsType(y.getGoodsType()).
                        setRemark(y.getRemark()).setShopTicket(orderShopTicket).setRedEnvelops(orderRedEnvelops);
                saveOrders.add(saveOrder);
            });
        });


    }
}
