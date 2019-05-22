package com.chauncy.web.controller.generator;


import com.chauncy.data.temp.product.service.ITbUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author huangwancheng
 * @since 2019-05-22
 */
@RestController
@RequestMapping("/data")
public class TbUserController {

 @Autowired
 private ITbUserService service;

 @GetMapping("/test")
 public Map<String, Object> findByUserUame(String username) {
  Map<String, Object> result = service.findByUserUame(username);
  return result;
 }
}
