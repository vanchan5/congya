package com.chauncy.activity.gift;

import com.chauncy.data.core.Service;
import com.chauncy.data.domain.po.activity.gift.AmGiftPo;
import com.chauncy.data.dto.manage.activity.gift.add.SaveGiftDto;
import com.chauncy.data.dto.manage.activity.gift.select.SearchBuyGiftRecordDto;
import com.chauncy.data.dto.manage.activity.gift.select.SearchGiftDto;
import com.chauncy.data.dto.manage.activity.gift.select.SearchReceiveGiftRecordDto;
import com.chauncy.data.vo.manage.activity.gift.FindGiftVo;
import com.chauncy.data.vo.manage.activity.gift.SearchBuyGiftRecordVo;
import com.chauncy.data.vo.manage.activity.gift.SearchGiftListVo;
import com.chauncy.data.vo.manage.activity.gift.SearchReceiveGiftRecordVo;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * <p>
 * 礼包表 服务类
 * </p>
 *
 * @author huangwancheng
 * @since 2019-07-20
 */
public interface IAmGiftService extends Service<AmGiftPo> {

    /**
     * 保存礼包
     * @param saveGiftDto
     * @return
     */
    void saveGift(SaveGiftDto saveGiftDto);

    /**
     * 批量删除礼包关联的优惠券
     *
     * @param relIds
     * @return
     */
    void delRelCouponByIds(List<Long> relIds);

    /**
     * 根据ID查找礼包信息
     *
     * @param id
     * @return
     */
    FindGiftVo findById(Long id);

    /**
     * 多条件分页获取礼包信息
     * @param searchGiftDto
     * @return
     */
    PageInfo<SearchGiftListVo> searchGiftList(SearchGiftDto searchGiftDto);

    /**
     * 批量删除礼包
     *
     * @param ids
     * @return
     */
    void delByIds(List<Long> ids);

    /**
     * 新人领取礼包
     * @param giftId
     * @return
     */
    void receiveGift(Long giftId);

    /**
     * 判断用户是否领取过新人礼包
     *
     * @return
     */
    Boolean isReceive();

    /**
     * 多条件查询新人礼包领取记录
     *
     * @param searchReceiveRecordDto
     * @return
     */
    PageInfo<SearchReceiveGiftRecordVo> searchReceiveRecord(SearchReceiveGiftRecordDto searchReceiveRecordDto);

    /**
     * 多条件查询购买礼包记录
     *
     * @param searchBuyGiftRecordDto
     * @return
     */
    PageInfo<SearchBuyGiftRecordVo> searchBuyGiftRecord(SearchBuyGiftRecordDto searchBuyGiftRecordDto);
}