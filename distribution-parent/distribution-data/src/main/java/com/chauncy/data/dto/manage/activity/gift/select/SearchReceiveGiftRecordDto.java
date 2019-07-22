package com.chauncy.data.dto.manage.activity.gift.select;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import java.time.LocalDate;

/**
 * @Author cheng
 * @create 2019-07-22 20:28
 *
 * 多条件查询新人礼包领取记录
 */
@Data
@ApiModel(description = "查询新人礼包领取记录条件")
@Accessors(chain = true)
public class SearchReceiveGiftRecordDto {

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户名称")
    private String userName;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("领取起始时间")
    private LocalDate startTime;

    @ApiModelProperty("领取截止时间")
    private LocalDate lastTime;

    @Min(1)
    @ApiModelProperty(value = "页码")
    private Integer pageNo;

    @Min(1)
    @ApiModelProperty(value = "分页大小")
    private Integer pageSize;

}
