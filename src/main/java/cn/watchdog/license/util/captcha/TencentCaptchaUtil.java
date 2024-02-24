package cn.watchdog.license.util.captcha;

import cn.watchdog.license.common.ReturnCode;
import cn.watchdog.license.exception.BusinessException;
import cn.watchdog.license.model.dto.CaptchaResult;
import cn.watchdog.license.util.NetUtil;
import com.tencentcloudapi.captcha.v20190722.CaptchaClient;
import com.tencentcloudapi.captcha.v20190722.models.DescribeCaptchaResultRequest;
import com.tencentcloudapi.captcha.v20190722.models.DescribeCaptchaResultResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Data
@Slf4j
/*
  腾讯验证码工具类
 */
public class TencentCaptchaUtil {
	private String secretId;
	private String secretKey;
	private String appSecretKey;
	private Credential credential;
	private CaptchaClient client;

	public TencentCaptchaUtil(String secretId, String secretKey, String appSecretKey) {
		this.credential = new Credential(secretId, secretKey);
		this.appSecretKey = appSecretKey;
		// 实例化要请求产品的client对象,clientProfile是可选的
		this.client = new CaptchaClient(this.credential, "");
	}

	public void isCaptchaValid(CaptchaResult captchaResult, long appId, HttpServletRequest request) throws TencentCloudSDKException {
		// 实例化一个请求对象,每个接口都会对应一个request对象
		DescribeCaptchaResultRequest req = new DescribeCaptchaResultRequest();
		String ip = NetUtil.getIpAddress(request);
		req.setUserIp(ip);
		req.setCaptchaAppId(appId);
		req.setRandstr(captchaResult.getRandstr());
		req.setTicket(captchaResult.getTicket());
		req.setAppSecretKey(appSecretKey);
		req.setCaptchaType(9L);


		// 返回的resp是一个DescribeCaptchaResultResponse的实例，与请求对象对应
		DescribeCaptchaResultResponse resp = this.client.DescribeCaptchaResult(req);
		/*
		  1 OK 验证通过
		  7 captcha no match 传入的Randstr不合法，请检查Randstr是否与前端返回的Randstr一致
		  8 ticket expired 传入的Ticket已过期（Ticket有效期5分钟），请重新生成Ticket、Randstr进行校验
		  9 ticket reused 传入的Ticket被重复使用，请重新生成Ticket、Randstr进行校验
		  15 decrypt fail 传入的Ticket不合法，请检查Ticket是否与前端返回的Ticket一致
		  16 appid-ticket mismatch 传入的CaptchaAppId错误，请检查CaptchaAppId是否与前端传入的CaptchaAppId一致，并且保障CaptchaAppId是从验证码控制台【验证管理】->【基础配置】中获取
		  21 diff 票据校验异常，可能的原因是（1）若Ticket包含terror前缀，一般是由于用户网络较差，导致前端自动容灾，而生成了容灾票据，业务侧可根据需要进行跳过或二次处理。（2）若Ticket不包含terror前缀，则是由于验证码风控系统发现请求有安全风险，业务侧可根据需要进行拦截。
		  100 appid-secretkey-ticket mismatch 参数校验错误，（1）请检查CaptchaAppId与AppSecretKey是否正确，CaptchaAppId、AppSecretKey需要在验证码控制台【验证管理】>【基础配置】中获取（2）请检查传入的Ticket是否由传入的CaptchaAppId生成
		 */
		long captchaCode = resp.getCaptchaCode();
		String captchaMsg = resp.getCaptchaMsg();
		captchaMsg = StringUtils.isAnyBlank(captchaMsg) ? "" : captchaMsg;
		String requestId = resp.getRequestId();
		captchaMsg = "RequestId: " + requestId + ", " + captchaMsg;
		if (captchaCode == 1) {
			/*
			  无感验证模式下，该参数返回验证结果：
			  EvilLevel=0 请求无恶意
			  EvilLevel=100 请求有恶意
			  注意：此字段可能返回 null，表示取不到有效值。
			 */
			Long evilLevel = resp.getEvilLevel();
			if (evilLevel != null && evilLevel == 100) {
				throw new BusinessException(ReturnCode.VALIDATION_FAILED, "人机验证校验失败，请求有恶意 " + captchaMsg, request);
			}
		} else if (captchaCode == 7) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "(7) 人机验证不匹配 " + captchaMsg, request);
		} else if (captchaCode == 8) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "(8) 人机验证已过期 " + captchaMsg, request);
		} else if (captchaCode == 9) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "(9) 人机验证已被使用 " + captchaMsg, request);
		} else if (captchaCode == 15) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "(15) 人机验证解密失败 " + captchaMsg, request);
		} else if (captchaCode == 16) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "(16) 人机验证AppId不匹配 " + captchaMsg, request);
		} else if (captchaCode == 21) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "(21) 人机验证校验异常 " + captchaMsg, request);
		} else if (captchaCode == 100) {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "(100) 人机验证校验错误 " + captchaMsg, request);
		} else {
			throw new BusinessException(ReturnCode.VALIDATION_FAILED, "(UNKNOWN) 人机验证校验失败 " + captchaMsg, request);
		}
	}
}
