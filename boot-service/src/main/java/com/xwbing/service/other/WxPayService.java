package com.xwbing.service.other;

import com.xwbing.domain.entity.pay.wxpay.*;
import com.xwbing.exception.BusinessException;
import com.xwbing.exception.PayException;
import com.xwbing.util.payWxpay.ClientCustomSSL;
import com.xwbing.util.payWxpay.RandomKit;
import com.xwbing.util.payWxpay.WxSignKit;
import com.xwbing.util.payWxpay.XmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 说明: 微信支付接口实现
 * 项目名称: spring-demo
 * 创建时间: 2017/5/10 17:30
 * 作者:  xiangwb
 */
@Service
@PropertySource("classpath:pay.properties")
public class WxPayService {
    private final Logger logger = LoggerFactory.getLogger(WxPayService.class);
    /**
     * 商户支付密钥 生成签名时用
     */
    @Value("${wx.apiKey}")
    private static String apiKey;
    /**
     * 公众账号ID(企业号corpid)
     */
    @Value("${wx.appId}")
    private static String appId;
    /**
     * 商户号
     */
    @Value("${wx.mchId}")
    private static String mchId;
    /**
     * 刷卡支付url
     */
    @Value("${wx.barCodePayUrl}")
    private static String barCodePayUrl;
    /**
     * 申请退款url
     */
    @Value("${wx.refundUrl}")
    private static String refundUrl;
    /**
     * 查询订单url
     */
    @Value("${wx.orderQueryUrl}")
    private static String orderQueryUrl;
    /**
     * 退款查询url
     */
    @Value("${wx.refundQueryUrl}")
    private static String refundQueryUrl;

    /**
     * 条形码扫码付
     *
     * @param param
     * @return
     */
    public WxBarCodePayResult barCodePay(WxBarCodePayParam param) {
        WxBarCodePayResult result = new WxBarCodePayResult(false);
        HttpPost post = new HttpPost(barCodePayUrl);
        //输入参数为转为strXml
        String reqBody = buildBarCodeRequestBody(param);
        post.setEntity(new StringEntity(reqBody, "UTF-8"));
        CloseableHttpClient httpclient;
        try {
            //根据mchId读取微信证书,SSL创建安全连接
            httpclient = ClientCustomSSL.getCloseableHttpClient(mchId);
            CloseableHttpResponse response = httpclient.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                //返回结果为strXml
                String content = EntityUtils.toString(entity, "UTF-8");
                // 解析返回值
                Map<String, String> returnMap;
                returnMap = XmlUtil.doXMLParse(content);
                //返回状态码SUCCESS/FAIL
                result.setResultCode(returnMap.get("return_code"));
                //返回信息  非空,为错误原因
                result.setMessage(returnMap.get("return_msg"));
                if ("FAIL".equalsIgnoreCase(returnMap.get("return_code"))) {
                    logger.info("wx barCodePay failed!");
                    result.setSuccess(false);
                    return result;
                }
                logger.info("outRefundNo=" + param.getOutTradeNo() + ",err_code=" + returnMap.get("err_code") + ",result_code=" + returnMap.get("result_code") + ",err_code_des=" + returnMap.get("err_code_des"));
                //业务结果SUCCESS/FAIL
                if ("SUCCESS".equals(returnMap.get("result_code"))) {
                    result.setSuccess(true);
                    result.setAppid(returnMap.get("appid"));
                    result.setMchId(returnMap.get("mch_id"));
                    result.setNonceStr(returnMap.get("nonce_str"));
                    result.setSign(returnMap.get("sign"));
                    result.setOpenId(returnMap.get("openid"));
                    result.setIsSubscribe(returnMap.get("is_subscribe"));
                    result.setTradeType(returnMap.get("trade_type"));
                    result.setBankType(returnMap.get("bank_type"));
                    result.setTotalFee(Integer.valueOf(returnMap.get("total_fee")));
                    result.setCashFee(Integer.valueOf(returnMap.get("cash_fee")));
                    result.setTransactionId(returnMap.get("transaction_id"));
                    result.setOutTradeNo(returnMap.get("out_trade_no"));
                    result.setTimeEnd(returnMap.get("time_end"));
                } else {
                    result.setResultCode(returnMap.get("err_code"));
                    result.setMessage(returnMap.get("err_code_des"));
                }
            } else {
                result.setMessage("error response");
                result.setSuccess(false);
            }
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (JDOMException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        }
        return result;
    }

    /**
     * 退款
     *
     * @param param
     * @return
     */
    public WxRefundResult refund(WxRefundParam param) {
        WxRefundResult result = new WxRefundResult(false);
        HttpPost post = new HttpPost(refundUrl);
        //输入参数为转为strXml
        String reqBody = buildRefundBarCodeRequestBody(param);
        post.setEntity(new StringEntity(reqBody, "UTF-8"));
        CloseableHttpClient httpclient;
        try {
            //根据mchId读取微信证书,SSL创建安全连接
            httpclient = ClientCustomSSL.getCloseableHttpClient(mchId);
            CloseableHttpResponse response = httpclient.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                //返回结果为strXml
                HttpEntity entity = response.getEntity();
                String content = EntityUtils.toString(entity, "UTF-8");
                // 解析返回值
                Map<String, String> returnMap;
                returnMap = com.xwbing.util.payWxpay.XmlUtil.doXMLParse(content);
                //返回状态码SUCCESS/FAIL
                result.setResultCode(returnMap.get("return_code"));
                //返回信息  非空,为错误原因
                result.setMessage(returnMap.get("return_msg"));
                if ("FAIL".equalsIgnoreCase(returnMap.get("return_code"))) {
                    logger.info("wx barCodePay failed!");
                    result.setSuccess(false);
                    return result;
                }
                logger.info("outRefundNo=" + param.getOutTradeNo() + ",err_code=" + returnMap.get("err_code") + ",result_code=" + returnMap.get("result_code") + ",err_code_des=" + returnMap.get("err_code_des"));
                //业务结果SUCCESS/FAIL
                if ("SUCCESS".equals(returnMap.get("result_code"))) {
                    result.setSuccess(true);
                    result.setAppid(returnMap.get("appid"));
                    result.setMchId(returnMap.get("mch_id"));
                    result.setNonceStr(returnMap.get("nonce_str"));
                    result.setSign(returnMap.get("sign"));
                    result.setTransactionId(returnMap.get("transaction_id"));
                    result.setOutTradeNo(returnMap.get("out_trade_no"));
                    result.setOutRefundNo(returnMap.get("out_refund_no"));
                    result.setRefundId(returnMap.get("refund_id"));
                    result.setRefundFee(Integer.valueOf(returnMap.get("refund_fee")));
                    result.setTotalFee(Integer.valueOf(returnMap.get("total_fee")));
                    result.setCashFee(Integer.valueOf(returnMap.get("cash_fee")));
                } else {
                    result.setResultCode(returnMap.get("err_code"));
                    result.setMessage(returnMap.get("err_code_des"));
                }
            } else {
                result.setMessage("error response");
                result.setSuccess(false);
            }
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (JDOMException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (Exception e1) {
            logger.error(e1.getMessage());
            throw new PayException(e1);
        }
        return result;
    }

    /**
     * 查询订单状态
     * 根据订单号 交易号查询 只需要一个即可
     * 如果isSuccess，根据tradeStatus，遍历WxTradeStatusEnum获取交易支付状态
     *
     * @param outTradeNo    商户订单号
     * @param transactionId 微信的订单号(推荐)
     * @return
     */
    public WxQueryResult orderQuery(String outTradeNo, String transactionId) {
        WxQueryResult result = new WxQueryResult(false);
        HttpPost post = new HttpPost(orderQueryUrl);
        //输入参数为转为strXml
        String reqBody = buildQueryRequestBody(outTradeNo, transactionId);
        post.setEntity(new StringEntity(reqBody, "UTF-8"));
        CloseableHttpClient httpclient;
        try {
            //根据mchId读取微信证书,SSL创建安全连接
            httpclient = ClientCustomSSL.getCloseableHttpClient(mchId);
            CloseableHttpResponse response = httpclient.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                //返回结果为strXml
                String content = EntityUtils.toString(entity, "UTF-8");
                // 解析返回值
                Map<String, String> returnMap;
                returnMap = XmlUtil.doXMLParse(content);
                result.setResultCode(returnMap.get("return_code"));
                result.setMessage(returnMap.get("return_msg"));
                //此字段是通信标识，非交易标识
                if ("FAIL".equalsIgnoreCase(returnMap.get("return_code"))) {
                    logger.info("wx barCodePay failed!");
                    result.setSuccess(false);
                    return result;
                }
                logger.info("outTradeNo=" + outTradeNo + ",transactionId=" + transactionId);
                //业务结果
                if ("SUCCESS".equals(returnMap.get("result_code"))) {
                    result.setSuccess(true);
                    //交易状态
                    result.setTradeStatus(returnMap.get("trade_state"));
                    return result;
                } else {
                    result.setResultCode(returnMap.get("err_code"));
                    result.setMessage(returnMap.get("err_code_des"));
                }
            } else {
                result.setMessage("error response");
                result.setSuccess(false);
            }
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (JDOMException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (Exception e1) {
            logger.error(e1.getMessage());
            throw new PayException(e1);
        }
        return result;
    }

    /**
     * 查询退款
     * 参数四选一即可
     * 如果isSuccess，根据refundStatus，遍历WxRefundStatusEnum获取退款状态
     *
     * @param outTradeNo    商户订单号
     * @param transactionId 微信的订单号
     * @param ouRefundNo    商户退款单号(推荐)
     * @param refundid      微信退款单号(推荐)
     * @return
     */
    public WxQueryResult refundQuery(String outTradeNo, String transactionId, String ouRefundNo, String refundid) {
        WxQueryResult result = new WxQueryResult(false);
        HttpPost post = new HttpPost(refundQueryUrl);
        //输入参数为转为strXml
        String reqBody = buildRefundQueryRequestBody(outTradeNo, transactionId, ouRefundNo, refundid);
        post.setEntity(new StringEntity(reqBody, "UTF-8"));
        CloseableHttpClient httpclient;
        try {
            //根据mchId读取微信证书,SSL创建安全连接
            httpclient = ClientCustomSSL.getCloseableHttpClient(mchId);
            CloseableHttpResponse response = httpclient.execute(post);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                //返回结果为strXml
                String content = EntityUtils.toString(entity, "UTF-8");
                // 解析返回值
                Map<String, String> returnMap;
                returnMap = XmlUtil.doXMLParse(content);
                result.setResultCode(returnMap.get("return_code"));
                result.setMessage(returnMap.get("return_msg"));
                //此字段是通信标识，非交易标识
                if ("FAIL".equalsIgnoreCase(returnMap.get("return_code"))) {
                    logger.error("wx barCodePay failed!");
                    result.setSuccess(false);
                    return result;
                }
                logger.info("outTradeNo=" + outTradeNo + ",transactionId=" + transactionId + "ouRefundNo=" + ouRefundNo + "refundId=" + refundid);
                //业务结果
                if ("SUCCESS".equals(returnMap.get("result_code"))) {
                    result.setSuccess(true);
                    //第一笔退款状态
                    result.setRefundStatus(returnMap.get("refund_status_0"));
                    return result;
                } else {
                    result.setResultCode(returnMap.get("err_code"));
                    result.setMessage(returnMap.get("err_code_des"));
                }
            } else {
                result.setMessage("error response");
                result.setSuccess(false);
            }
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (JDOMException e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new PayException(e);
        }
        return result;
    }

    /**
     * 构建扫码参数
     *
     * @param param 微信扫码支付接口参数实体
     * @return
     */
    private String buildBarCodeRequestBody(WxBarCodePayParam param) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("appid", appId);
        params.put("mch_id", mchId);
        String nonceStr = RandomKit.buildRandom(32);
        params.put("nonce_str", nonceStr);
        params.put("body", param.getBody());
        params.put("out_trade_no", param.getOutTradeNo());
        //单位分
        params.put("total_fee", String.valueOf(param.getTotalFee()));
        params.put("spbill_create_ip", param.getSpblillCreateIp());
        params.put("auth_code", param.getAuthCode());
        //签名放最后的
        String sign = WxSignKit.buildSign(params, apiKey);
        params.put("sign", sign);
        StringBuilder reqBody = new StringBuilder();
        reqBody.append("<xml>");
        for (String key : params.keySet()) {
            String value = params.get(key);
            reqBody.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
        }
        reqBody.append("</xml>");
        logger.info(reqBody + "~~~------------");
        return reqBody.toString();
    }

    /**
     * 构建退款参数
     *
     * @param param 微信退款参数实体
     * @return
     */
    private String buildRefundBarCodeRequestBody(WxRefundParam param) {
        Map<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("mch_id", mchId);
        String nonceStr = RandomKit.buildRandom(32);
        params.put("nonce_str", nonceStr);
        if (StringUtils.isEmpty(param.getTransactionId()) && StringUtils.isEmpty(param.getOutTradeNo())) {
            throw new PayException("商户订单号和微信订单号不能同时为空!");
        }
        if (StringUtils.isNotEmpty(param.getTransactionId())) {
            params.put("transaction_id", param.getTransactionId());
        }
        if (StringUtils.isNotEmpty(param.getOutTradeNo())) {
            params.put("out_trade_no", param.getOutTradeNo());
        }
        params.put("out_refund_no", param.getOutRefundNo());
        params.put("total_fee", String.valueOf(param.getTotalFee()));
        params.put("refund_fee", String.valueOf(param.getRefundFee()));
        params.put("op_user_id", param.getOpUserId());
        String sign = WxSignKit.buildSign(params, apiKey);
        params.put("sign", sign);
        //签名放最后的
        StringBuilder reqBody = new StringBuilder();
        reqBody.append("<xml>");
        for (String key : params.keySet()) {
            String value = params.get(key);
            reqBody.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
        }
        reqBody.append("</xml>");
        logger.info(reqBody + "~~~------------");
        return reqBody.toString();
    }

    /**
     * 查询封装参数
     *
     * @param outTradeNo    商户订单号
     * @param transactionId 微信订单号
     * @return
     */
    private String buildQueryRequestBody(String outTradeNo, String transactionId) {
        Map<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("mch_id", mchId);
        if (StringUtils.isEmpty(transactionId) && StringUtils.isEmpty(outTradeNo)) {
            throw new PayException("商户订单号和微信订单号不能同时为空!");
        }
        if (StringUtils.isNotEmpty(transactionId)) {
            params.put("transaction_id", transactionId);
        }
        if (StringUtils.isNotEmpty(outTradeNo)) {
            params.put("out_trade_no", outTradeNo);
        }
        String nonceStr = RandomKit.buildRandom(32);
        params.put("nonce_str", nonceStr);
        //签名放最后的
        String sign = WxSignKit.buildSign(params, apiKey);
        params.put("sign", sign);
        StringBuilder reqBody = new StringBuilder();
        reqBody.append("<xml>");
        for (String key : params.keySet()) {
            String value = params.get(key);
            reqBody.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
        }
        reqBody.append("</xml>");
        logger.info(reqBody + "~~~------------");
        return reqBody.toString();
    }

    /**
     * 查询封装参数
     *
     * @param outTradeNo    商户订单号
     * @param transactionId 微信订单号
     * @param ouRefundNo    商户退款单号
     * @param refundid      微信退款单号
     * @return
     */
    private String buildRefundQueryRequestBody(String outTradeNo, String transactionId, String ouRefundNo, String refundid) {
        Map<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("mch_id", mchId);
        if (StringUtils.isEmpty(transactionId) && StringUtils.isEmpty(outTradeNo) && StringUtils.isEmpty(ouRefundNo) && StringUtils.isEmpty(refundid)) {
            throw new PayException("商户订单号,微信订单号,商户退款单号和微信退款单号不能同时为空!");
        }
        if (StringUtils.isNotEmpty(transactionId)) {
            params.put("transaction_id", transactionId);
        }
        if (StringUtils.isNotEmpty(outTradeNo)) {
            params.put("out_trade_no", outTradeNo);
        }
        if (StringUtils.isNotEmpty(ouRefundNo)) {
            params.put("out_refund_no", ouRefundNo);
        }
        if (StringUtils.isNotEmpty(refundid)) {
            params.put("refund_id", refundid);
        }
        String nonceStr = RandomKit.buildRandom(32);
        params.put("nonce_str", nonceStr);
        //签名放最后的
        String sign = WxSignKit.buildSign(params, apiKey);
        params.put("sign", sign);
        StringBuilder reqBody = new StringBuilder();
        reqBody.append("<xml>");
        for (String key : params.keySet()) {
            String value = params.get(key);
            reqBody.append("<").append(key).append(">").append(value).append("</").append(key).append(">");
        }
        reqBody.append("</xml>");
        logger.info(reqBody + "~~~------------");
        return reqBody.toString();
    }

    public static void main(String[] args) {
        WxPayService wxPayBuilder = new WxPayService();
        //刷卡支付
//        String authCode = "130203463134616871";
//        WxBarCodePayParam payParam = new WxBarCodePayParam("2017051200", "127.0.0.0", authCode, "test", 1);
//        WxBarCodePayResult result = wxPayBuilder.barCodePay(payParam);
//        System.out.println(result.isSuccess() + result.getMessage());

        //查询订单
        String outTradeNo = "2017051200";
        String transactionId = "4001082001201705120512385115";
        WxQueryResult queryResult = wxPayBuilder.orderQuery(outTradeNo, transactionId);
        if (!queryResult.isSuccess()) {
            throw new BusinessException(queryResult.getMessage());
        }
        String tradeStatus = queryResult.getTradeStatus();
        for (WxTradeStatusEnum status : WxTradeStatusEnum.values()) {
            if (Objects.equals(tradeStatus, status.getCode())) {
                System.out.println(status.getName());
                break;
            }
        }

        //退款操作
        WxRefundParam param = new WxRefundParam(transactionId, "2017051201", "xwbing", 1, 1);
//        WxRefundResult refundResult = wxPayBuilder.refund(param);
//        System.out.println(refundResult.isSuccess() + refundResult.getMessage());

        //查询退款
        WxQueryResult refundQueryResult = wxPayBuilder.refundQuery(outTradeNo, transactionId, "", "");
        if (!refundQueryResult.isSuccess()) {
            throw new BusinessException(refundQueryResult.getMessage());
        }
        String refundStatus = refundQueryResult.getRefundStatus();
        for (WxRefundStatusEnum status : WxRefundStatusEnum.values()) {
            if (Objects.equals(refundStatus, status.getCode())) {
                System.out.println(status.getName());
                break;
            }
        }
    }
}
