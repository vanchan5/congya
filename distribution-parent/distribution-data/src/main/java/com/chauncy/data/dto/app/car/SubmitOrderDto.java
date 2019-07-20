package com.chauncy.data.dto.app.car;

import com.chauncy.data.valid.annotation.NeedExistConstraint;
import com.chauncy.data.vo.app.car.StoreOrderVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * @Author zhangrt
 * @Date 2019/7/14 18:39
 **/
@Data
@ApiModel(description = "购物车提交订单")
@Accessors(chain = true)
public class SubmitOrderDto {

    @NotNull(message = "收货地址不能为空！")
    @NeedExistConstraint(tableName = "um_area_shipping",message = "收货地址不存在！")
    @ApiModelProperty(value = "我的收货地址id")
    private Long umAreaShipId;

    @NotNull(message = "是否使用葱鸭钱包不能为空！")
    private Boolean isUseWallet;

    @ApiModelProperty(value = "使用购物券")
    private BigDecimal shopTicket;



    @ApiModelProperty(value = "使用红包")
    private BigDecimal redEnvelops;

    @ApiModelProperty(value = "可抵扣金额")
    private BigDecimal deductionMoney;

    @ApiModelProperty(value = "商品总额")
    private BigDecimal totalMoney;

    @ApiModelProperty(value = "预计奖励购物券")
    private BigDecimal rewardShopTicket;



    @ApiModelProperty(value = "运费")
    private BigDecimal shipMoney;

    @ApiModelProperty(value = "税费")
    private BigDecimal taxMoney;

    @ApiModelProperty("根据店铺与商品类型拆单列表")
    private List<StoreOrderVo> storeOrderVos;

    @ApiModelProperty(value = "总数量")
    private int totalNumber;

    @ApiModelProperty(value = "合计优惠")
    private BigDecimal totalDiscount;

    @ApiModelProperty(value = "应付总额")
    private BigDecimal realPayMoney;
}