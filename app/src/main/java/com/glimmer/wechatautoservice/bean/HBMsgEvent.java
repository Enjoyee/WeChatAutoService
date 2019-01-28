package com.glimmer.wechatautoservice.bean;

/**
 * 红包消息
 *
 * @author Glimmer
 * @date 2018/2/2
 */

public class HBMsgEvent {
    private String content;
    private String money;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMoney() {
        return money;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    @Override
    public String toString() {
        return "HBMsgEvent{" +
                "content='" + content + '\'' +
                ", money='" + money + '\'' +
                '}';
    }
}
