package com.chauncy.order.pay;

import com.chauncy.data.vo.app.order.pay.UnifiedOrderVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * @author yeJH
 * @since 2019/7/5 16:22
 */
public interface IWxService {
    /**
     * 调用官方SDK 获取前端调起支付接口的参数
     * @return
     * @throws Exception
     */
    UnifiedOrderVo unifiedOrder(String ipAddr, Long payOrderId) throws Exception;
    /**
     * 微信支付结果通知
     * @param notifyData 异步通知后的XML数据
     * @return
     */
    String payBack(String notifyData) throws Exception;
    /**
     *   微信查询订单接口  订单未操作的做业务更新
     *   官方文档 ：https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=9_2&index=4
     */
    void orderQuery(Long payOrderId) throws Exception;

    /**
     * 调用官方SDK申请退款
     * @return
     * @throws Exception
     */
    void refund(Long payOrderId) throws Exception;
}