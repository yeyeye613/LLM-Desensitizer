package com.hdu.apisensitivities.entity;

import java.util.List;

/**
 * 统一API响应结果封装类
 */
public class Message<T> {
    private int code;
    private String msg;
    private T data;

    // 历史记录字段（对应 setHistory 报错）
    private List<Message<T>> history;

    // ==========================================
    // 1. 修复报错的核心：增加 (String, String) 构造函数
    // 测试代码里用了 new Message("...", "...")，所以必须要有这个
    // ==========================================
    public Message(String msg, String status) {
        this.msg = msg;
        // 这里假设第二个字符串如果是 "success" 就设为200，否则500
        // 或者你可以直接硬编码，视你的测试代码逻辑而定
        if ("success".equals(status) || "200".equals(status)) {
            this.code = 200;
        } else {
            this.code = 500;
        }
    }

    // 保留之前的全参构造函数（为了兼容其他可能的调用）
    public Message(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ==========================================
    // 2. 修复 setHistory 报错
    // ==========================================
    public void setHistory(List<Message<T>> history) {
        this.history = history;
    }

    public List<Message<T>> getHistory() {
        return history;
    }

    // Getter 和 Setter
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    // 快捷方法（可选）
    public static <T> Message<T> success(T data) {
        return new Message<>(200, "Success", data);
    }

    public static <T> Message<T> error(String msg) {
        return new Message<>(500, msg, null);
    }

	public void setRole(String string) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'setRole'");
	}
}