package com.jason.wechat.interfaces.controller;

import java.util.Date;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.jason.framework.util.ExceptionUtils;
import com.jason.framework.web.support.ControllerSupport;
import com.jason.wechat.application.chat.ChatService;
import com.jason.wechat.application.message.constant.MessageType;
import com.jason.wechat.application.message.handle.ReqMessageHandler;
import com.jason.wechat.application.music.MusicService;
import com.jason.wechat.domain.message.Message;
import com.jason.wechat.domain.message.req.ReqEventMessage;
import com.jason.wechat.domain.message.req.ReqImageMessage;
import com.jason.wechat.domain.message.req.ReqLinkMessage;
import com.jason.wechat.domain.message.req.ReqLocationMessage;
import com.jason.wechat.domain.message.req.ReqTextMessage;
import com.jason.wechat.domain.message.req.ReqVideoMessage;
import com.jason.wechat.domain.message.req.ReqVoiceMessage;
import com.jason.wechat.domain.message.resp.RespMusicMessage;
import com.jason.wechat.domain.message.resp.RespTextMessage;
import com.jason.wechat.domain.message.resp.model.Music;
import com.jason.wechat.infrastruture.util.HttpUtils;
import com.jason.wechat.infrastruture.util.MessageUtil;
import com.jason.wechat.infrastruture.verify.VerifyToken;

/**
 * 微信 控制层
 * @author Jason
 * @data 2014-4-15 下午03:44:05
 */
@Controller
@RequestMapping(value = "/wechat")
public class WeChatController extends ControllerSupport {
	
	@Autowired
	private ReqMessageHandler reqMessageHandler ;
	
	@Autowired
	private ChatService chatService ;
	
	@Autowired
	private MusicService musicService ;
	
    /**
     * get请求
     * 
     * @param request
     * @param response
     * @param signature 微信加密签名
     * @param timestamp 时间戳 
     * @param nonce 随机数 
     * @param echostr 随机字符串 
     * @throws Exception
     */
    @RequestMapping(value = { "/", "" }, method = RequestMethod.GET)
    public void get(HttpServletRequest request,
                           HttpServletResponse response,
                           @RequestParam(value = "signature") String signature,
                           @RequestParam(value = "timestamp") String timestamp,
                           @RequestParam(value = "nonce") String nonce,
                           @RequestParam(value = "echostr") String echostr) throws Exception {

    	boolean  flag = VerifyToken.checkSignature(timestamp, nonce, echostr, signature);
    	String outPut = "error";
        if (flag) {
        	outPut = echostr;
        }
        super.getLogger().info(
        		String.format("check Signature Result：%s,signature:%s,timestamp:%s,nonce:%s,echostr:%s", 
        				flag,signature,timestamp,nonce,echostr));
        response.getWriter().print(outPut);
        response.getWriter().close();
    }
    

    @RequestMapping(value = { "/", "" }, method = RequestMethod.POST)
    public void post(HttpServletRequest request, HttpServletResponse response) {
    	super.getLogger().info("post controller -------------");
    	try {
			ServletInputStream in = request.getInputStream();
			String str = HttpUtils.inputStream2String(in);
	    	super.getLogger().info("inputStream2String method，str is:"+str);
	    	
	    	Message message  = reqMessageHandler.reqMessageHandle(str);
	    	String msgType = message.getMsgType();
	    	super.getLogger().info("message is : "+message.toString());
	    	
	    	String respContent = "请求处理异常，请稍候尝试！";
	    	// 文本消息
	    	if (StringUtils.equalsIgnoreCase(MessageType.REQ_MESSAGE_TYPE_TEXT.toString(), msgType)) {	
	    		ReqTextMessage textMessage = (ReqTextMessage) message;
	    		executeMessageTypeText(textMessage,response);
	    	}
	    	//图片消息
	    	else if(StringUtils.equalsIgnoreCase(MessageType.REQ_MESSAGE_TYPE_IMG.toString(), msgType)){	
	    		ReqImageMessage imageMessage = (ReqImageMessage) message;
	    		respContent = "您发送的是图片消息！";
	    		
	    	}
	    	//地理位置消息.
	    	else if(StringUtils.equalsIgnoreCase(MessageType.REQ_MESSAGE_TYPE_LOCATION.toString(), msgType)){	
	    		ReqLocationMessage imageMessage = (ReqLocationMessage) message;
	    		respContent = "您发送的是地理位置消息！";
	    		
	    	}
	    	//链接消息.
	    	else if(StringUtils.equalsIgnoreCase(MessageType.REQ_MESSAGE_TYPE_LINK.toString(), msgType)){	
	    		ReqLinkMessage linkMessage = (ReqLinkMessage) message;
	    		respContent = "您发送的是链接消息！";
	    		
	    	}
	    	//音频消息.
	    	else if(StringUtils.equalsIgnoreCase(MessageType.REQ_MESSAGE_TYPE_VOICE.toString(), msgType)){	
	    		ReqVoiceMessage voiceMessage = (ReqVoiceMessage) message;
	    		respContent = "您发送的是音频消息！";
	    		
	    	}
	    	//视频消息.
	    	else if(StringUtils.equalsIgnoreCase(MessageType.REQ_MESSAGE_TYPE_VIDEO.toString(), msgType)){	
	    		ReqVideoMessage videoMessage = (ReqVideoMessage) message;
	    		respContent = "您发送的是视频消息！";
	    		
	    	}
	    	//事件推送.
	    	else if(StringUtils.equalsIgnoreCase(MessageType.REQ_MESSAGE_TYPE_EVENT.toString(), msgType)){	
	    		ReqEventMessage eventMessage = (ReqEventMessage) message;
	    		
	    		//事件类型
	    		String event = eventMessage.getEvent();	
	    		
	    		// 订阅
                if (StringUtils.equalsIgnoreCase(MessageType.EVENT_TYPE_SUBSCRIBE.toString(), event)) {
                    respContent = "订阅事件";
                }
                // 取消订阅
                else if (StringUtils.equalsIgnoreCase(MessageType.EVENT_TYPE_UNSUBSCRIBE.toString(), event)) {
                    //取消订阅后用户再收不到公众号发送的消息，因此不需要回复消息
                }
	    	}
	    	
	    	//以上都不执行 ，则最后执行这里
            RespTextMessage text = new RespTextMessage();
	    	text.setContent(respContent);
	    	text.setCreateTime(message.getCreateTime());
	    	text.setFromUserName(message.getToUserName());
	    	text.setMsgType(MessageType.RESP_MESSAGE_TYPE_TEXT.toString());
	    	text.setToUserName(message.getFromUserName());
	    	
			writeXmlResult(response, MessageUtil.textMessageToXml(text));
			
			
		} catch (Exception e) {
			super.getLogger().error("post error",e);
		}
    }
    
    /**
     * 接收文本消息类的请求，进行回复
     * @param textMessage 请求文本消息类
     * @param response
     */
    private void executeMessageTypeText(ReqTextMessage textMessage,HttpServletResponse response){
    	String content = StringUtils.trim(textMessage.getContent());
    	if(StringUtils.equalsIgnoreCase(content, "?") || StringUtils.equalsIgnoreCase(content, "？")){
    		super.getLogger().info("textMessage ? -------------");
    		executeMenu(response, textMessage);
    	}else if(StringUtils.equalsIgnoreCase(content, "1")){
    		super.getLogger().info("textMessage 1 -------------");
    		StringBuffer buffer = new StringBuffer()
        	.append("歌曲点播操作指南").append("\n\n")
        	.append("回复：歌曲+歌曲关键字").append("\n")
        	.append("例如：歌曲存在").append("\n\n")
        	.append("回复“?”显示主菜单");
        
            RespTextMessage text = new RespTextMessage();
	    	text.setContent(buffer.toString());
	    	text.setCreateTime(textMessage.getCreateTime());
	    	text.setFromUserName(textMessage.getToUserName());
	    	text.setMsgType(MessageType.RESP_MESSAGE_TYPE_TEXT.toString());
	    	text.setToUserName(textMessage.getFromUserName());
	    	
			writeXmlResult(response, MessageUtil.textMessageToXml(text));
			
    	}else if(StringUtils.startsWith(content, "歌曲")){
    		super.getLogger().info("textMessage song -------------");
    		executeMusic(response, textMessage, content);
    	}else {
    		super.getLogger().info("textMessage xiaojo -------------");
    		
    		String chatStr = chatService.chat(textMessage.getContent(), textMessage.getFromUserName(), textMessage.getToUserName());
    		
    		RespTextMessage text = new RespTextMessage();
	    	text.setContent(chatStr);
	    	text.setCreateTime(textMessage.getCreateTime());
	    	text.setFromUserName(textMessage.getToUserName());
	    	text.setMsgType(MessageType.RESP_MESSAGE_TYPE_TEXT.toString());
	    	text.setToUserName(textMessage.getFromUserName());
	    	
			writeXmlResult(response, MessageUtil.textMessageToXml(text));
    	}
    }
    /**
     * 执行菜单 回复
     * @param response
     * @param message 请求文本消息类
     */
    private void executeMenu(HttpServletResponse response, ReqTextMessage message) {
    	StringBuffer buffer = new StringBuffer()
        	.append("您好，我是小杰森，请回复数字选择服务：").append("\n\n")
        	.append("1  歌曲点播").append("\n\n")
        	.append("回复“?”显示此帮助菜单");
    	RespTextMessage text = new RespTextMessage();
    	text.setContent(buffer.toString());
    	text.setCreateTime(message.getCreateTime());
    	text.setFromUserName(message.getToUserName());
    	text.setMsgType(MessageType.RESP_MESSAGE_TYPE_TEXT.toString());
    	text.setToUserName(message.getFromUserName());
    	
		writeXmlResult(response, MessageUtil.textMessageToXml(text));
	}


	/**
	 * 执行音乐回复
	 * @param response
	 * @param message  请求文本消息类
	 * @param content 关键字
	 */
	private void executeMusic(HttpServletResponse response,ReqTextMessage message,String content){
    	// 将歌曲2个字及歌曲后面的+、空格、-等特殊符号去掉  
        String keyWord = content.replaceAll("^歌曲[\\+ ~!@#%^-_=]?", "");
        if(StringUtils.isBlank(keyWord)){
        	StringBuffer buffer = new StringBuffer()
	        	.append("歌曲点播操作指南").append("\n\n")
	        	.append("回复：歌曲+歌曲关键字").append("\n")
	        	.append("例如：歌曲存在").append("\n\n")
	        	.append("回复“?”显示主菜单");
            
            RespTextMessage text = new RespTextMessage();
	    	text.setContent(buffer.toString());
	    	text.setCreateTime(message.getCreateTime());
	    	text.setFromUserName(message.getToUserName());
	    	text.setMsgType(MessageType.RESP_MESSAGE_TYPE_TEXT.toString());
	    	text.setToUserName(message.getFromUserName());
	    	
			writeXmlResult(response, MessageUtil.textMessageToXml(text));
        }else{

            // 搜索音乐  
            Music music = musicService.searchMusic(keyWord);  
            // 未搜索到音乐  
            if (null == music) {  
                RespTextMessage text = new RespTextMessage();
    	    	text.setContent("对不起，没有找到你想听的歌曲<" + keyWord + ">。");
    	    	text.setCreateTime(message.getCreateTime());
    	    	text.setFromUserName(message.getToUserName());
    	    	text.setMsgType(MessageType.RESP_MESSAGE_TYPE_TEXT.toString());
    	    	text.setToUserName(message.getFromUserName());
    	    	
    			writeXmlResult(response, MessageUtil.textMessageToXml(text));
            } else {  
                // 音乐消息  
            	RespMusicMessage musicMessage = new RespMusicMessage();  
                musicMessage.setToUserName(message.getFromUserName());  
                musicMessage.setFromUserName(message.getToUserName());  
                musicMessage.setCreateTime(message.getCreateTime());  
                musicMessage.setMsgType(MessageType.RESP_MESSAGE_TYPE_MUSIC.toString());  
                musicMessage.setMusic(music);
                
                writeXmlResult(response, MessageUtil.musicMessageToXml(musicMessage));
            }  
        }
    }
	/**
	 * xml 相应
	 * @param response
	 * @param message
	 */
	private static void writeXmlResult(HttpServletResponse response, Object message) {
		try {
			response.setContentType("text/xml");
			response.getWriter().write(String.format("%s", message));
		} catch (Exception e) {
			throw ExceptionUtils.toUnchecked(e);
		}
	}
	
	/**
	 * 测试
	 * @param response
	 */
	@RequestMapping(value="/test", method=RequestMethod.GET)
	public void test(HttpServletResponse response){
		RespTextMessage text = new RespTextMessage();
		text.setContent("test 测试");
		text.setCreateTime(new Date().getTime());
		text.setFromUserName("fromusername");
		text.setMsgType(MessageType.RESP_MESSAGE_TYPE_TEXT.toString());
		text.setToUserName("tousername");
		writeXmlResult(response, MessageUtil.textMessageToXml(text));
	}
}
