package com.chauncy.data.mapper.activity;

import com.chauncy.data.domain.po.activity.AmCouponPo;
import com.chauncy.data.dto.manage.activity.coupon.select.SearchCouponListDto;
import com.chauncy.data.dto.manage.activity.coupon.select.SearchDetailAssociationsDto;
import com.chauncy.data.dto.manage.activity.coupon.select.SearchReceiveRecordDto;
import com.chauncy.data.mapper.IBaseMapper;
import com.chauncy.data.vo.manage.activity.coupon.FindCouponDetailByIdVo;
import com.chauncy.data.vo.manage.activity.coupon.SearchCouponListVo;
import com.chauncy.data.vo.manage.activity.coupon.SearchDetailAssociationsVo;
import com.chauncy.data.vo.manage.activity.coupon.SearchReceiveRecordVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 优惠券 Mapper 接口
 * </p>
 *
 * @author huangwancheng
 * @since 2019-07-18
 */
public interface AmCouponMapper extends IBaseMapper<AmCouponPo> {

    /**
     * 条件分页查询优惠券列表
     *
     * @param searchCouponListDto
     * @return
     */
    List<SearchCouponListVo> searchCouponList(SearchCouponListDto searchCouponListDto);

    /**
     * 根据优惠券查找领取记录
     * @param searchReceiveRecordDto
     * @return
     */
    List<SearchReceiveRecordVo> searchReceiveRecord(SearchReceiveRecordDto searchReceiveRecordDto);

    /**
     * 根据ID查询优惠券详情除关联商品外的信息
     * @param id
     * @return
     */
    FindCouponDetailByIdVo findCouponDetailById(Long id);

    /**
     * 根据优惠券ID查找关联的分类信息
     *
     * @param searchDetailAssociationsDto
     * @return
     */
    List<SearchDetailAssociationsVo> searchDetailCategory(SearchDetailAssociationsDto searchDetailAssociationsDto);

    /**
     * 根据优惠券ID查找关联的商品信息
     *
     * @param searchDetailAssociationsDto
     * @return
     */
    List<SearchDetailAssociationsVo> searchDetailGoods(SearchDetailAssociationsDto searchDetailAssociationsDto);

    /**
     * 获取不同使用状态的优惠券的个数
     * @param couponId
     * @param status
     * @return
     */
    @Select("select count(id) from am_coupon_rel_coupon_user where use_status = #{status} and coupon_id =#{couponId}")
    Integer countNum(@Param("couponId") Long couponId, @Param("status") Integer status);
}
