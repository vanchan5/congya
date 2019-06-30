package com.chauncy.data.dto.app.user;

import com.chauncy.common.util.CommonVerifyUtil;
import com.chauncy.data.valid.annotation.NeedExistConstraint;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * @Author zhangrt
 * @Date 2019/6/28 17:52
 **/

@Data
@ApiModel(value = "新增用户")
public class AddUserDto {

    @ApiModelProperty(value = "手机号码")
    @NeedExistConstraint(tableName = "um_user",isNeedExists = false,field = "phone",message = "该手机号码已经被注册")
    @Pattern(regexp = "^1[3|4|5|8][0-9]\\d{8}$",message = "手机号码不符合格式！")
    private String phone;

    @ApiModelProperty(value = "邀请码")
    private String inviteCode;

    @ApiModelProperty(value = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @ApiModelProperty(value = "验证码")
    @NotBlank(message = "验证码不能为空！")
    private String verifyCode;




}
