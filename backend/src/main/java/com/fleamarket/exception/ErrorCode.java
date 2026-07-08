package com.fleamarket.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "操作成功"),

    BAD_REQUEST(400, "请求参数校验失败"),
    UNAUTHORIZED(401, "未登录或 Token 过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "业务冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 认证相关
    USERNAME_EXISTS(409, "用户名已被使用"),
    INVALID_CREDENTIALS(401, "用户名或密码错误，或账号不可用"),
    USER_DISABLED(401, "用户名或密码错误，或账号不可用"),
    PASSWORD_MISMATCH(400, "两次输入的密码不一致"),

    // 商品相关
    PRODUCT_NOT_FOUND(404, "商品不存在或已下架"),
    PRODUCT_NOT_ACTIVE(400, "商品已被购买或已下架"),
    CATEGORY_NOT_FOUND(404, "分类不存在"),
    CATEGORY_NAME_EXISTS(409, "分类名称重复"),
    CANNOT_BUY_OWN_PRODUCT(400, "不能购买自己发布的商品"),
    IMAGE_COUNT_EXCEEDED(400, "商品图片最多 3 张，每张不超过 5MB"),

    // 订单相关
    ORDER_NOT_FOUND(404, "订单不存在"),
    INVALID_STATUS_TRANSITION(400, "当前状态不支持该操作"),
    ORDER_ENDED(400, "订单已结束"),
    CANNOT_CANCEL_SHIPPED(400, "订单已发货不可取消"),

    // 评价相关
    ORDER_NOT_COMPLETED(400, "订单尚未完成，无法评价"),
    ALREADY_RATED(409, "您已对该订单做出过评价"),

    // 交换相关
    NO_ACTIVE_PRODUCT(400, "您暂无在售商品可交换"),
    SWAP_ORDER_NOT_FOUND(404, "交换订单不存在"),

    // 举报相关
    DUPLICATE_REPORT(409, "您已提交过对该对象的举报，请等待处理结果"),
    CANNOT_REPORT_SELF(400, "不得举报自己的内容"),
    REPORT_NOT_FOUND(404, "举报不存在"),
    CANNOT_APPEAL(400, "当前状态不可申诉"),
    ALREADY_APPEALED(409, "已提交过申诉，请等待处理结果"),

    // 管理员相关
    ADMIN_ALREADY_EXISTS(409, "管理员已初始化，不可重复创建");

    private final int code;
    private final String message;
}
