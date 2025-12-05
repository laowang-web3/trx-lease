package com.laowang.coinbank.bot;

import com.laowang.coinbank.common.BusinessEnum;
import com.laowang.coinbank.config.TelegramBotProperties;
import com.laowang.coinbank.order.dao.entity.ProductOrderDO;
import com.laowang.coinbank.order.service.ProductOrderService;
import com.laowang.coinbank.service.ApiTrxService;
import com.laowang.coinbank.service.OkxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TelegramBot extends TelegramLongPollingBot {
    // 注入配置类
    @Autowired
    private TelegramBotProperties telegramBotProperties;
    @Autowired
    private OkxService okxService;
    @Autowired
    private ProductOrderService productOrderService;
    @Autowired
    private ApiTrxService apiTrxService;


    private Map<Long,ProductOrderDO> queue = new ConcurrentHashMap<>();
//    private Map<String,BigDecimal> unitPriceMap = new ConcurrentHashMap<>();
    //闪租内联笔数
    private final String flashCountItems [] = {"1笔","2笔","3笔","5笔","10笔"};
    //会员商品列表
    private final Object[][] energyItems = {{"3", "15"}, {"6", "25"}, {"12", "45"}};
    //笔数套餐商品列表
    private final Object[][] packageItems = {{"1", "4"}, {"5", "4"}, {"10", "4"},
                                            {"20", "3.9"},{"30", "3.9"},{"50", "3.9"},
                                             {"100", "3.8"},{"200", "3.8"},{"500", "3.8"}};
    //1小时能量卖价
//    public static final BigDecimal EnergyUnitPrice = new BigDecimal( 3.3).setScale(1,BigDecimal.ROUND_UP);
//    private final String WidthItems [] = {"5K","1W","3W","5W","10W"};
    //监控收款地址
//    private final String CollectMoney  = "TP2MPAMh2uFNyx3DZouJJdfiHgXzKshrVm";
    //客服
//    private final String service  = "@laowang2025";

    @Override
    public void onUpdateReceived(Update update) {

        // 1. 检查更新是否包含消息，且消息是否为文本类型
        if (update.hasMessage() && update.getMessage().hasText()) {
            // 2. 获取文本内容
            String userText = update.getMessage().getText();
            // 3. 获取聊天ID（用于回复消息）
            long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getFirstName()+update.getMessage().getChat().getLastName();
            String account = update.getMessage().getChat().getUserName();
            System.out.println("---account----"+account);
            System.out.println("---userName----"+userName);
            System.out.println("===chatId=====:"+chatId);
            System.out.println("---userText----"+userText);
            //接收批量开通会员
            if(null != queue.get(chatId)&&(userText.contains("@")||userText.contains("https://t.me"))){
                StringBuffer errorName = new StringBuffer();
                StringBuffer haveName = new StringBuffer();
                String namestr = filterNames(userText);
                String[] names = namestr.split(",");
                int num = 0;
                int haveNum = 0;
                for (String name : names){
                    //检查输入的账号对不对
                    int b = apiTrxService.queryUser(name);
                    if(b==1){
                        errorName.append(name+",");
                        num++;
                    }
                    if(b==2){
                        haveName.append(name+",");
                        haveNum++;
                    }
                }
                if(errorName.length()>0||haveName.length()>0){
                    StringBuffer text = new StringBuffer();
                    if(num>0){
                        String sendName =  errorName.toString().replaceAll("^,|,$", "");
                        text.append("\uD83D\uDEAB无效异常账号："+num+"个 \n"+sendName);
                    }
                    if(haveNum>0){
                        String sendName =  haveName.toString().replaceAll("^,|,$", "");
                        if(text.length()>0){
                            text.append("\n");
                        }
                        text.append("\uD83D\uDEAB已是会员账号："+haveNum+"个 \n"+sendName);
                    }
                    sendMsg(chatId,text.toString());
                }else {
                    ProductOrderDO productOrderDO1 = queue.get(chatId);
                    ProductOrderDO order = productOrderService.createOrert(chatId,userName,namestr,
                            BusinessEnum.PREMIUM,productOrderDO1.getBusiness(),productOrderDO1.getUnitPrice().multiply(new BigDecimal(names.length))
                            ,names.length,telegramBotProperties.getCollectMoneyAddress());
                    if(null == order){
                        log.info("-----生成订单失败-------"+chatId);
                    }else {
                      String  callbackText =  "⚠\uFE0F↓↓请按金额支付，否则无法到账↓↓\n" +
                                "---------------------------------\n" +
                                "\uD83D\uDD38飞机会员："+productOrderDO1.getBusiness()+"\n" +
                                "\uD83D\uDD38用户账号："+namestr+"\n" +
                                "\uD83D\uDD38开通数量："+names.length+"\n" +
                                "\uD83D\uDD38支付金额："+order.getUsdtAmount().stripTrailingZeros().toPlainString()+" USDT\n" +
                                "\uD83D\uDD38收款地址：<code>"+telegramBotProperties.getCollectMoneyAddress()+"</code>\n" +
                                "（点击地址复制）\n" +
                                "---------------------------------\n" +
                                "\n" +
                                "‼\uFE0F请务必核对金额尾数，金额带小数\n" +
                                "\uD83D\uDEAB请勿使用交易所或中心化钱包转账";
                        setInlineKeyboard32(chatId,callbackText);
                        queue.remove(chatId);
                    }

                }
            }
            switch (userText) {
                case "/start":
                    //设置键盘
                    setKeyboard(chatId);
                    break;
                case "\uD83D\uDD0B能量闪租":
                    setInlineKeyboard2(chatId,"\uD83D\uDD0B能量闪租\n" +
                            "➖➖➖➖➖➖➖➖➖➖\n" +
                            "\uD83C\uDF08使用能量可节省 80% 转U手续费\n" +
                            "\n" +
                            "\uD83D\uDD391笔对方地址【有U】 "+telegramBotProperties.getEnergyPrice().stripTrailingZeros().toPlainString()+" TRX  (1小时有效)\n" +
                            "\uD83D\uDD391笔对方地址【无U】 "+(telegramBotProperties.getEnergyPrice().multiply(new BigDecimal(2))).stripTrailingZeros().toPlainString()+" TRX  (1小时有效)\n" +
                            "\n" +
                            "\uD83D\uDD0B小时套餐（1小时有效）\n" +
                            "\uD83D\uDD38转账 "+telegramBotProperties.getEnergyPrice().stripTrailingZeros().toPlainString()+" TRX = 免费1笔转账\n" +
                            "\uD83D\uDD38转账 "+(telegramBotProperties.getEnergyPrice().multiply(new BigDecimal(2))).stripTrailingZeros().toPlainString()+" TRX = 免费2笔转账\n" +
                            "\uD83D\uDD38转账 "+(telegramBotProperties.getEnergyPrice().multiply(new BigDecimal(3))).stripTrailingZeros().toPlainString()+" TRX = 免费3笔转账\n" +
                            "\uD83D\uDD38以此类推 3×笔数，单次10笔封顶\n" +
                            "\n" +
                            "\uD83D\uDCE3转 TRX 到下方地址，能量自动到账\n" +
                            "<code>"+telegramBotProperties.getCollectMoneyAddress()+"</code>\n" +
                            "(点击地址复制)\n" +
                            "\n" +
                            "✅全自动到账，默认返回原地址\n" +
                            "\uD83D\uDEAB请勿使用交易所或中心化钱包转账");
                    break;
                case "\uD83D\uDCB9TRX闪兑":
                    sendMsg(chatId,"\uD83D\uDCB9USDT ≈ TRX 实时汇率\n" +
                            "➖➖➖➖➖➖➖➖➖➖\n" +
                            "\uD83D\uDD381 USDT = "+okxService.getTrxAmount(new BigDecimal(1))+"   TRX\n" +
                            "\uD83D\uDD381 TRX = "+okxService.getUsdtAmount(new BigDecimal(1))+"   USDT\n" +
                            "\n" +
                            "\uD83D\uDD39转 U 兑 T【    1 U 起兑无上限】\n" +
                            "\uD83D\uDD39转 T 兑 U【100 T 起兑无上限】\n" +
                            "\uD83D\uDCCC频道唯一兑换地址\n" +
                            "<code>"+telegramBotProperties.getCollectMoneyAddress()+"</code>\n" +
                            "(点击地址复制)\n" +
                            "\n" +
                            "✅全自动到账，默认返回原地址\n" +
                            "\uD83D\uDEAB请勿使用交易所或中心化钱包转账" );
                    break;
                case "\uD83D\uDC64个人中心":
                    sendMsg(chatId,"\uD83D\uDC64 个人中心\n" +
                            "➖➖➖➖➖➖➖➖➖➖\n" +
                            "用户ID：5852052090\n" +
                            "用户昵称：老王\n" +
                            "邀请人数：0\n" +
                            "积分余额：0\n" +
                            "剩余带宽：0 / 0\n" +
                            "剩余笔数：0 / 0 笔\n" +
                            "当前余额：0 TRX + 0 USDT\n" +
                            "邀请链接：https://t.me/TRXX6Bot?start=5852052090\n" +
                            "创建时间：2025-08-22 18:53:51" );
                    break;
                case "\uD83D\uDD25笔数套餐":
                    StringBuffer text1 = new StringBuffer("\uD83D\uDD0B笔数套餐\n" +
                            "➖➖➖➖➖➖➖➖➖➖\n" +
                            "\uD83C\uDF08使用能量可节省 80% 转U手续费\n" +
                            "\n" );
                            for (int i=0;i< packageItems.length;i++){
                                text1.append("套餐"+(i+1)+"：    "+packageItems[i][0]+"笔 = "+packageItems[i][1]+" TRX /笔  (包宽带)\n");
                            }
//                            for (Object [] obj:packageItems){
//                                text1.append("套餐1：    20笔 = 2.9 TRX/笔(包宽带)\n");
//                            }
                            text1.append("\n");
                            text1.append("\uD83D\uDCA5不限时间、不限地址，用1笔扣1笔；\n" +
                            "\uD83D\uDD25笔数套餐，非常适用于每天都有转账的地址使用，每笔都发送131000能量，转账1笔收1次费用，不限时长，24小时没有转账会扣除1笔滞留费用，连续72小时没有转账自动暂停，您可在机器人手动开启，请根据个人需求点击下方按钮下单；\n" +
                            "\n" +
                            "\uD83D\uDCB0剩余笔数：0 笔\n" +
                            "⚡\uFE0F如需帮助，请联系客服 "+telegramBotProperties.getCustomerService());
                    setInlineKeyboard4(chatId,text1.toString());
                    break;
                case "\uD83D\uDC51飞机会员":
                    StringBuffer text  = new StringBuffer("✈\uFE0F Telegram会员官方代开\n" +
                            " \n" +
                            "欢迎使用 Telegram Premium 自助开通服务。\n" +
                            "\n" +
                            "\uD83D\uDCB0 当前价格：\n" );
//                            "\uD83D\uDD52  3 个月 "+unitPriceMap.get("3")+" USDT\n" +
//                            "\uD83D\uDD55  6 个月 "+unitPriceMap.get("6")+" USDT\n" +
//                            "\uD83D\uDD5B 12 个月 "+unitPriceMap.get("12")+" USDT\n" );
//                            Set<String> set = unitPriceMap.keySet();
                            for (Object [] obj:energyItems){
                                text.append("\uD83D\uDD52  "+obj[0]+" 个月 "+obj[1]+" USDT\n");
                            }
                           text.append("\n");
                           text.append("\uD83D\uDC49 请选择下方按钮操作");

                    setInlineKeyboard3(chatId,text.toString());
                    break;
            }
        }
        // 检查是否有回调查询
        if (update.hasCallbackQuery()) {

            String account = update.getCallbackQuery().getFrom().getUserName();
            String userName = update.getCallbackQuery().getFrom().getFirstName()+update.getCallbackQuery().getFrom().getLastName();
            long usdtId = update.getCallbackQuery().getMessage().getChatId();
            if(null != queue.get(usdtId)){
                queue.remove(usdtId);
            }
            // 提取回调查询
            String callbackData = update.getCallbackQuery().getData();
            // 做出相应的响应，例如回复用户
            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
            answerCallbackQuery.setCallbackQueryId(update.getCallbackQuery().getId());
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            System.out.println("===message=Id====:"+messageId);
            String callbackText;
            if(callbackData.contains("EnergyCount")){
                int index = callbackData.indexOf("笔");
                String numStr = callbackData.substring(0, index);
                Integer number = Integer.parseInt(numStr);
                callbackText =  "⚠\uFE0F↓↓请按金额支付，否则无法到账↓↓\n" +
                        "---------------------------------\n" +
                        "\uD83D\uDD38能量闪租：1小时"+number+"笔\n" +
                        "\uD83D\uDD38支付金额："+(telegramBotProperties.getEnergyPrice().multiply(new BigDecimal(number))).stripTrailingZeros().toPlainString()+" TRX\n" +
                        "\uD83D\uDD38收款地址：<code>"+telegramBotProperties.getCollectMoneyAddress()+"</code>\n" +
                        "    （点击地址复制）\n" +
                        "---------------------------------\n" +
                        "\n" +
                        "✅全自动到账，能量即回原地址\n" +
                        "\uD83D\uDEAB请勿使用交易所或中心化钱包转账";
                sendMsg(usdtId,callbackText);
            }
            if(callbackData.contains("packageItem")){
                String num = callbackData.substring("packageItem".length(),callbackData.length());
                String price = "0";
                for (Object [] obj:packageItems){
                    if(num.equals(obj[0])){
                        price = (String) obj[1];
                    }
                }
                String title  = "笔数套餐："+num+"笔 = "+price+" TRX/笔";
                ProductOrderDO order = productOrderService.createOrert(usdtId,userName,account,
                        BusinessEnum.COUNT,title,new BigDecimal(price),Integer.parseInt(num),telegramBotProperties.getCollectMoneyAddress());
                if(null == order){
                    log.info("-----生成订单失败-------"+usdtId);
                }
                callbackText =  "⚠\uFE0F↓↓请按金额支付，否则无法到账↓↓\n" +
                        "---------------------------------\n" +
                        "\uD83D\uDD38笔数套餐："+num+"笔 = "+price+" TRX/笔\n" +
                        "\uD83D\uDD38支付金额："+order.getTrxAmount()+" TRX 或 "+order.getUsdtAmount()+" USDT\n" +
                        "\uD83D\uDD38收款地址：<code>"+telegramBotProperties.getCollectMoneyAddress()+"</code>\n" +
                        "（点击地址复制）\n" +
                        "---------------------------------\n" +
                        "\n" +
                        "‼\uFE0F请务必核对金额尾数，金额带小数\n" +
                        "\uD83D\uDEAB请勿使用交易所或中心化钱包转账";
                setInlineKeyboard41(usdtId,callbackText);
            }
            if(callbackData.contains("vip")){
                String date = callbackData.substring("vip".length(),callbackData.length());
                String price = "0";
                for (Object [] obj:energyItems){
                    if(date.equals(obj[0])){
                        price = (String) obj[1];
                    }
                }
                String title  = date+"个月Telegram Premium会员";
                callbackText =  "请发送您需要开通Telegram会员账号\n" +
                        "▫\uFE0F例如：\n" +
                        "▫\uFE0F@"+account+"\n" +
                        "▫\uFE0F<code> https://t.me/"+account+ "</code>\n" +
                        "\n" +
                        "多个飞机账号请用空格、或逗号隔开\n" +
                        "▫\uFE0F例如：\n" +
                        "▫\uFE0F@username1\n" +
                        "▫\uFE0F@username2\n" +
                        "\n" +
                        "您选择了 "+title+"\n" +
                        "▫\uFE0F售价: "+price+"U";
                setInlineKeyboard31(date,usdtId,callbackText);
                ProductOrderDO productOrderDO =  new ProductOrderDO();
                productOrderDO.setBusiness(title);
                productOrderDO.setUnitPrice(new BigDecimal(price));
                queue.put(usdtId,productOrderDO);
            }
            if(callbackData.contains("premiumForme")){
                String date = callbackData.substring("premiumForme".length(),callbackData.length());
                String price = "0";
                for (Object [] obj:energyItems){
                    if(date.equals(obj[0])){
                        price = (String) obj[1];
                    }
                }
                String title  = date+"个月Telegram Premium会员";
                ProductOrderDO order = productOrderService.createOrert(usdtId,userName,account,
                        BusinessEnum.PREMIUM,title,new BigDecimal(price),1,telegramBotProperties.getCollectMoneyAddress());
                if(null == order){
                    log.info("-----生成订单失败-------"+usdtId);
                }
                callbackText =  "⚠\uFE0F↓↓请按金额支付，否则无法到账↓↓\n" +
                        "---------------------------------\n" +
                        "\uD83D\uDD38飞机会员："+title+"\n" +
                        "\uD83D\uDD38用户账号：@"+account+"\n" +
                        "\uD83D\uDD38用户昵称："+userName+"\n" +
                        "\uD83D\uDD38支付金额："+order.getUsdtAmount().stripTrailingZeros().toPlainString()+" USDT\n" +
                        "\uD83D\uDD38收款地址：<code>"+telegramBotProperties.getCollectMoneyAddress()+"</code>\n" +
                        "（点击地址复制）\n" +
                        "---------------------------------\n" +
                        "\n" +
                        "‼\uFE0F请务必核对金额尾数，金额带小数\n" +
                        "\uD83D\uDEAB请勿使用交易所或中心化钱包转账";
                setInlineKeyboard32(usdtId,callbackText);
                deleteLastMessage(usdtId,messageId);
            }
            if(callbackData.equals("cancelPremiumForme")){
                //取消二级页面，
                productOrderService.cancelOrder(usdtId);
                deleteLastMessage(usdtId,messageId);
            }
        }
    }

    /**
     * 删除上一条消息
     */
    private void deleteLastMessage(Long lastChatId,Integer lastMessageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(lastChatId.toString());
        deleteMessage.setMessageId(lastMessageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            // 常见错误：消息已过期（超过48小时无法删除）、无权限
            e.printStackTrace();
        }
    }


    /**
     * 能量闪租内联按扭
     * @param msg
     */
    public void setInlineKeyboard2(Long chatId,String msg){
        // 创建InlineKeyboard
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List< List< InlineKeyboardButton>> keyboard = new ArrayList<>();
        List< InlineKeyboardButton> row = new ArrayList<>();
        // 添加按钮
        InlineKeyboardButton titleButton = InlineKeyboardButton.builder().text("⬇\uFE0F选择能量笔数,有效期1小时⬇\uFE0F")
                .callbackData("title1") .build();
        row.add(titleButton);
        keyboard.add(row);
        List< InlineKeyboardButton> row2 = new ArrayList<>();
        for (String btn : flashCountItems){
            InlineKeyboardButton callBackEnergyButton1 = InlineKeyboardButton.builder().text(btn)
                    .callbackData(btn+"EnergyCount") .build();
            row2.add(callBackEnergyButton1);
        }
        // 将行添加到键盘
        keyboard.add(row2);
        // 设置键盘到消息
        inlineKeyboardMarkup.setKeyboard(keyboard);
        sendMsg(chatId,msg,inlineKeyboardMarkup);
    }

    /**
     * 飞机会员设置内联菜单
     * @param msg
     */
    public void setInlineKeyboard3(Long chatId,String msg){
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List< List< InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (Object [] obj:energyItems){
             List< InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton vip1Button1 = InlineKeyboardButton.builder().text("\uD83D\uDC51 "+obj[0]+"月会员 售价"+obj[1]+"U")
                    .callbackData("vip"+obj[0]) .build();
            row1.add(vip1Button1);
            keyboard.add(row1);
            }
//        }
        inlineKeyboardMarkup.setKeyboard(keyboard);
        sendMsg(chatId,msg,inlineKeyboardMarkup);
    }
    /**
     * 笔数套餐内联菜单2
     * @param msg
     */
    public void setInlineKeyboard41(Long chatId,String msg){
        // 创建InlineKeyboard
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List< List< InlineKeyboardButton>> keyboard = new ArrayList<>();
        List< InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton vip1Button1 = InlineKeyboardButton.builder().text("取消订单")
                .callbackData("cancelPremiumForme") .build();
        row1.add(vip1Button1);
        keyboard.add(row1);
        // 设置键盘到消息
        inlineKeyboardMarkup.setKeyboard(keyboard);
        sendMsg(chatId,msg,inlineKeyboardMarkup);
    }
    /**
     * 飞机会员设置内联菜单2
     * @param msg
     */
    public void setInlineKeyboard31(String index,Long chatId,String msg){
        // 创建InlineKeyboard
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List< List< InlineKeyboardButton>> keyboard = new ArrayList<>();
        List< InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton vip1Button1 = InlineKeyboardButton.builder().text("\uD83C\uDF81 给当前账号开通")
                .callbackData("premiumForme"+index) .build();
        row1.add(vip1Button1);
        keyboard.add(row1);
        // 设置键盘到消息
        inlineKeyboardMarkup.setKeyboard(keyboard);
        sendMsg(chatId,msg,inlineKeyboardMarkup);
    }
    /**
     * 飞机会员设置内联菜单3
     * @param msg
     */
    public void setInlineKeyboard32(Long chatId,String msg){
        // 创建InlineKeyboard
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List< List< InlineKeyboardButton>> keyboard = new ArrayList<>();
        List< InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton vip1Button1 = InlineKeyboardButton.builder().text("取消订单")
                .callbackData("cancelPremiumForme") .build();
        row1.add(vip1Button1);
        keyboard.add(row1);
        // 设置键盘到消息
        inlineKeyboardMarkup.setKeyboard(keyboard);
        sendMsg(chatId,msg,inlineKeyboardMarkup);
    }
    /**
     * 笔数套餐内联菜单
     * @param msg
     */
    public void setInlineKeyboard4(Long chatId,String msg){
        // 创建InlineKeyboard
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List< List< InlineKeyboardButton>> keyboard = new ArrayList<>();
        List< InlineKeyboardButton> row = new ArrayList<>();
        // 添加按钮
        InlineKeyboardButton titleButton = InlineKeyboardButton.builder().text("⬇\uFE0F笔数套餐包购买⬇\uFE0F")
                .callbackData("title1") .build();
        row.add(titleButton);
        keyboard.add(row);
        int i =0;
        List< InlineKeyboardButton> row1 = null;
        for (Object [] obj:packageItems){
            if((i%3)==0){
                if(null != row1){
                    keyboard.add(row1);
                }
                 row1 = new ArrayList<>();
                InlineKeyboardButton vip1Button1 = InlineKeyboardButton.builder().text((String) obj[0])
                        .callbackData("packageItem"+obj[0]) .build();
                row1.add(vip1Button1);
            }else {
                InlineKeyboardButton vip1Button1 = InlineKeyboardButton.builder().text((String) obj[0])
                        .callbackData("packageItem"+obj[0]) .build();
                row1.add(vip1Button1);
            }
            i++;
        }
        keyboard.add(row1);
        // 设置键盘到消息
        inlineKeyboardMarkup.setKeyboard(keyboard);
        sendMsg(chatId,msg,inlineKeyboardMarkup);
    }

    public static void main(String[] args) {
        for (int i =0;i<9;i++){
            if((i%3)==0){
               System.out.println("--------换行--------");
            }else {
                System.out.println("--------旧的行--------");
            }
        }
    }

    /**
    * 设置菜单
     */
    public void setKeyboard(Long chatId){
        // 创建自定义键盘
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false); // 不自动隐藏键盘
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("\uD83D\uDD0B能量闪租");
        row1.add("\uD83D\uDCB9TRX闪兑");
//      row1.add("\uD83D\uDC64个人中心");
        KeyboardRow row2 = new KeyboardRow();
//      row2.add("\uD83D\uDC51飞机会员");
//      row2.add("\uD83C\uDF81积分兑换");
//      row2.add("\uD83D\uDC64个人中心");
        row2.add("\uD83D\uDD25笔数套餐");
        row2.add("\uD83D\uDC51飞机会员");
        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        sendMsg(chatId," \uD83C\uDFE0 欢迎使用TG多功能机器人！本机器人提供以下服务：\n" +
                "\n" +
                "▪\uFE0F 能量租赁：转U即可省 80% TRX手续费\n" +
                "▪\uFE0F 笔数套餐：不限时间地址，用1笔扣1笔\n" +
                "▪\uFE0F TRX 闪兑：全网最高汇率，兑换秒到账\n" +
                "▪\uFE0F 飞机会员：24小时自动开通，官方秒开\n" +
                "\n" +
                "⚡\uFE0F如需帮助，请联系客服 "+telegramBotProperties.getCustomerService(),keyboardMarkup);
    }
    public void sendMsg(Long chatId,String msg) {
        sendMsg(chatId,msg,null);
    }
    /**
     * 构建回复消息
     */
    public void sendMsg(Long chatId, String msg, ReplyKeyboard replyKeyboard) {
        SendMessage message = SendMessage.builder()
                .text(msg)
                .chatId(chatId)
                 .replyMarkup(replyKeyboard)
                .build();
        message.setParseMode("HTML");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    /**
     * 检查用户是否存在
     * @param userId 用户的 ID（如 123456789）或用户名（如 "@username"）
     * @return true：存在；false：不存在
     */
    public boolean checkUserExists(String userId) {
        GetChat getChat = new GetChat();
        getChat.setChatId("5852052090"); // 设置要查询的用户 ID 或用户名

        try {
            // 调用 API 查询用户
            Chat chat = execute(getChat);
            // 若成功返回 Chat 对象，说明用户存在
            return true;
        } catch (TelegramApiException e) {
            // 捕获错误：若为 "chat not found"，说明用户不存在
            if (e.getMessage().contains("chat not found")) {
                return false;
            }
            // 其他错误（如网络问题、权限不足等）
            System.err.println("查询失败：" + e.getMessage());
            return false;
        }
    }

    /**
     * 解析用户输入内容
     */
    private String filterNames(String inputText){
        if((inputText.contains("https://t.me/"))){
             inputText = inputText.replaceAll("https://t.me/", "@");
        }
        //接收批量开通会员
        if((inputText.contains("@"))){
            inputText = inputText.replaceAll("[\\s、]+", ",")
                    .replaceAll("^,|,$", "");
        }
        //去重
        String[] split = inputText.split(",");
        Set<String> set = new LinkedHashSet<>(Arrays.asList(split));
        return String.join(",",set);
    }
    @Override
    public void onRegister() {
       log.info("------已连接----");
        // 设置Bot命令
        setBotCommands();
    }
    public void setBotCommands() {
        List commands = Arrays.asList(
                new BotCommand("/start","菜单")
        );
        SetMyCommands setMyCommands = new SetMyCommands();
        setMyCommands.setCommands(commands);
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotToken() {
        return telegramBotProperties.getToken();
    }

    @Override
    public String getBotUsername() {
        return "TRX闪兑｜能量闪租｜飞机会员";
    }

}
