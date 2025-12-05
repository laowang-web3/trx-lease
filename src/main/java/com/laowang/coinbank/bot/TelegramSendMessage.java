package com.laowang.coinbank.bot;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laowang.coinbank.config.TelegramBotProperties;
import com.laowang.coinbank.service.OkxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class TelegramSendMessage {

    // Bot令牌（从@BotFather获取）
    private static final String BOT_TOKEN = "8479174862:aaafasfsa";
    // 管理员的chat_id 5852052090
    private static final String ADMIN_CHAT_ID = "-00000";
    // Telegram Bot API基础URL
    private static final String BASE_URL = "https://api.telegram.org/bot" + BOT_TOKEN;

    public static void main(String[] args) {
//        // 测试发送消息给管理员
//        sendMessageToAdmin(energy);
//        TelegramSendMessage telegramSendMessage= new TelegramSendMessage();
//        telegramSendMessage.buildEnergyText("1","65000","AAHnxL02Pk3TC6UQMlWNorwjeV1uFup57hA","20d25f3c81e8433df2373472c2592bb3f4b0bd053a2abf534cadf9699e9604f0");
    }
    //能量闪租
    public static String energy = "✅能量闪租 下发完成\n" +
            "➖➖➖➖➖➖➖➖➖➖\n" +
            "能量闪租：1小时{0}笔\n" +
            "能量数量：{1}\n" +
            "接收地址：{2}\n" +
            "交易哈希: <a href=\"https://tronscan.org/?utm_source=tronlink#/transaction/{3}\">{4}</a>";
    //闪兑
    public static String flash = "✅{0} 兑换 {1}成功\n" +
            "➖➖➖➖➖➖➖➖➖➖\n" +
            "兑入金额：{2} \n" +
            "兑出金额：{3} \n" +
            "兑换地址：{4}\n" +
            "交易哈希: <a href=\"https://tronscan.org/?utm_source=tronlink#/transaction/{5}\">{6}</a>";

    //会员下单
    public static String premium = "✅飞机会员 开通成功\n" +
            "➖➖➖➖➖➖➖➖➖➖\n" +
            "飞机会员：{0}个月飞机会员\n" +
            "交易金额：{1} USDT\n" +
            "交易地址：{2}\n" +
            "交易哈希: <a href=\"https://tronscan.org/?utm_source=tronlink#/transaction/{3}\">{4}</a>";
    //笔数套餐
    public static String packageCount = "✅笔数套餐 下单成功\n" +
            "➖➖➖➖➖➖➖➖➖➖\n" +
            "笔数套餐： {0}笔\n" +
            "交易金额：{1} \n" +
            "交易地址：{2}\n" +
            "交易哈希: <a href=\"https://tronscan.org/?utm_source=tronlink#/transaction/{3}?lang=zh\">{4}</a>";
    public String buildEnergyText(String count,String quantity,String address,String txid){
        address = address.substring(0,6)+"...."+address.substring(address.length()-6,address.length());
        String newTxid = txid.substring(0,6)+"...."+txid.substring(txid.length()-6,txid.length());
        MessageFormat mf = new MessageFormat(energy);
        String result = mf.format(new Object[]{count, quantity, address,txid,newTxid});
        sendMessageToAdmin(result);
        return result;
    }
    public String buildFlashText(String formCoin,String toCoin,String foAmount,String toAmount,String address,String txid){
        address = address.substring(0,6)+"...."+address.substring(address.length()-6,address.length());
        String newTxid = txid.substring(0,6)+"...."+txid.substring(txid.length()-6,txid.length());
        MessageFormat mf = new MessageFormat(flash);
        String result = mf.format(new Object[]{formCoin, toCoin,foAmount+" "+formCoin,toAmount+" "+toCoin, address,txid,newTxid});
        sendMessageToAdmin(result);
        return result;
    }
    public String buildPremiumText(String num,String amount,String address,String txid){
        address = address.substring(0,6)+"...."+address.substring(address.length()-6,address.length());
        String newTxid = txid.substring(0,6)+"...."+txid.substring(txid.length()-6,txid.length());
        MessageFormat mf = new MessageFormat(premium);
        String result = mf.format(new Object[]{num, amount, address,txid,newTxid});
        sendMessageToAdmin(result);
        return result;
    }
    public String buildPackageCountText(String num,String amount,String address,String txid){
        address = address.substring(0,6)+"...."+address.substring(address.length()-6,address.length());
        String newTxid = txid.substring(0,6)+"...."+txid.substring(txid.length()-6,txid.length());
        MessageFormat mf = new MessageFormat(packageCount);
        String result = mf.format(new Object[]{num, amount, address,txid,newTxid});
        sendMessageToAdmin(result);
        return result;
    }


    public static void sendMessageToAdmin(String message) {
        if(!ADMIN_CHAT_ID.equals("-00000")){
            // 构建请求URL，包含必要参数
            String url = BASE_URL + "/sendMessage?chat_id=" + ADMIN_CHAT_ID + "&text=" + encodeMessage(message)+"&parse_mode=html&disable_web_page_preview=True";
            String result = HttpRequest.get(url)
                    .timeout(20000)
                    .execute().body();
            System.out.println("-------sendMessage--result----------" + result);
            JSONObject jsonObject = JSONUtil.parseObj(result);
            if (null != jsonObject) {
                Boolean code = jsonObject.getBool("ok");
                if (code) {
                    System.out.println("-------发送成功----------");
                }
            }
            // 发送请求并处理响应
        }
    }

    /**
     * 对消息内容进行URL编码，处理特殊字符
     * @param message 原始消息
     * @return 编码后的消息
     */
    private static String encodeMessage(String message) {
        try {
            return java.net.URLEncoder.encode(message, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return message;
        }
    }
}
