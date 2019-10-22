package com.chauncy.data.dto.manage.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 平台基本设置
 * </p>
 *
 * @author huangwancheng
 * @since 2019-07-10
 */
@Data
@Accessors(chain = true)
@ApiModel( description = "平台基本设置")
public class UpdateBasicSettingDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID")
    private Long id;


    @ApiModelProperty(value = "修改者")
    private String updateBy;


    @ApiModelProperty(value = "平台电话")
    private String phone;

    @ApiModelProperty(value = "上架多少天未新品")
    private Integer newProductDay;

    @ApiModelProperty(value = "提现手续费")
    private BigDecimal withdrawCommission;

    @ApiModelProperty(value = "个人消费的订单金额1元=多少经验值")
    private BigDecimal moneyToExperience;

    @ApiModelProperty(value = "上一层得到订单金额 % 的经验值 ")
    private BigDecimal lastLevelExperience;

    @ApiModelProperty(value = "上两层得到订单金额%的经验值")
    private BigDecimal lastTwoLevelExperience;

    @ApiModelProperty(value = "下一层得到订单金额%的经验值")
    private BigDecimal nextLevelExperience;

    @ApiModelProperty(value = "好友注册成功获得经验值")
    private BigDecimal goodFriendRegisterExperience;

    @ApiModelProperty(value = "好友首次消费获得经验值")
    private BigDecimal goodFriendPayExperience;

    @ApiModelProperty(value = "每天首次登录获得积分")
    private BigDecimal dayFirstLoginIntegrate;

    @ApiModelProperty(value = "个人消费的订单金额1元=多少积分")
    private BigDecimal moneyToIntegrate;

    @ApiModelProperty(value = "上一层得到订单金额 % 的积分")
    private BigDecimal lastLevelIntegrate;

    @ApiModelProperty(value = "上两层得到订单金额%的积分")
    private BigDecimal lastTwoLevelIntegrate;

    @ApiModelProperty(value = "下一层得到订单金额%的积分")
    private BigDecimal nextLevelIntegrate;

    @ApiModelProperty(value = "好友注册成功获得积分")
    private BigDecimal goodFriendRegisterIntegrate;

    @ApiModelProperty(value = "好友首次消费获得积分")
    private BigDecimal goodFriendPayIntegrate;

    @ApiModelProperty(value = "个人消费的订单金额1元=多少购物券")
    private BigDecimal moneyToShopTicket;

    @ApiModelProperty(value = "个人消费的订单金额1元=多少红包")
    private BigDecimal moneyToCurrentRedEnvelops;

    @ApiModelProperty(value = "订单下单未付款，n天后自动关闭，空为不自动关闭")
    private Integer autoCloseOrderDay;

    @ApiModelProperty(value = "订单发货后，用户收货的天数，如果在期间未确认收货，系统自动完成收货，空为不自动收货")
    private Integer autoReceiveDay;

    @ApiModelProperty(value = "(含税商品)订单收货后 ，用户在x天内可以发起退款申请，设置0天不允许完成订单退款")
    private Integer taxRefundDay;

    @ApiModelProperty(value = "(不含税商品)订单收货后 ，用户在x天内可以发起退款申请，设置0天不允许完成订单退款")
    private Integer refundDay;

    @ApiModelProperty(value = "订单收货后 ，用户在x天内未评论未进行评论，则默认为默认评价")
    private Integer autoCommentDay;

    @ApiModelProperty(value = "是否展示提现按钮 1-是 0-否")
    private Boolean isShowWithdraw;

    @ApiModelProperty(value = "个人消费得到订单金额%的积分")
    private BigDecimal ownRewardIntegrate;
}
