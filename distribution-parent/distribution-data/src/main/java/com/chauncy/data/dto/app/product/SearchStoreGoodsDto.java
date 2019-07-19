package com.chauncy.data.dto.app.product;

import com.alibaba.fastjson.annotation.JSONField;
import com.chauncy.common.enums.app.sort.SortFileEnum;
import com.chauncy.common.enums.app.sort.SortWayEnum;
import com.chauncy.common.enums.goods.StoreGoodsListTypeEnum;
import com.chauncy.data.valid.annotation.EnumConstraint;
import com.chauncy.data.valid.annotation.NeedExistConstraint;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author yeJH
 * @since 2019/7/17 16:10
 */
@Data
@ApiModel(value = "SearchStoreGoodsDto对象", description = "查询店铺下的商品列表")
public class SearchStoreGoodsDto  implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "店铺id")
    @Min(value = 1, message = "店铺ID参数错误")
    @NeedExistConstraint(tableName = "sm_store",  concatWhereSql = "and enabled = 1")
    private Long storeId;

    @ApiModelProperty(value = "分类id")
    @NeedExistConstraint(tableName = "pm_goods_category")
    private Long goodsCategoryId;

    @ApiModelProperty(value = "店铺商品列表类型")
    @NotNull(message = "店铺商品列表类型不能为空")
    @EnumConstraint(target = StoreGoodsListTypeEnum.class)
    private StoreGoodsListTypeEnum storeGoodsListTypeEnum;

    @ApiModelProperty(value = "排序方式 默认降序（desc）")
    @EnumConstraint(target = SortWayEnum.class)
    private SortWayEnum sortWayEnum;

    @ApiModelProperty(value = "排序字段 默认综合排序（COMPREHENSIVE_SORT）")
    @EnumConstraint(target = SortFileEnum.class)
    private SortFileEnum sortFileEnum;

    @Min(1)
    @ApiModelProperty(value = "页码 默认1")
    private Integer pageNo;

    @Min(1)
    @ApiModelProperty(value = "分页大小 默认10")
    private Integer pageSize;

    /**
     * 新品的评判标准  上架几天内为新品
     */
    @ApiModelProperty(hidden = true)
    @JSONField(serialize = false)
    private Integer newGoodsDays;
}
