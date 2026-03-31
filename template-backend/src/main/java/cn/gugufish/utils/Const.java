package cn.gugufish.utils;

public final class Const {
    public static final String JWT_BLACK_LIST = "jwt:blacklist:";
    public static final String JWT_FREQUENCY = "jwt:frequency:";

    public static final String FLOW_LIMIT_COUNTER = "flow:counter:";
    public static final String FLOW_LIMIT_BLOCK = "flow:block:";

    public static final String VERIFY_EMAIL_LIMIT = "verify:email:limit:";
    public static final String VERIFY_EMAIL_DATA = "verify:email:data:";
    public static final String CONSULTATION_IMAGE_COUNTER = "consultation:image:";

    public static final int ORDER_FLOW_LIMIT = -101;
    public static final int ORDER_CORS = -102;

    public static final String ATTR_USER_ID = "userId";
    public static final String MQ_MAIL = "mail";

    public static final String ROLE_DEFAULT = "user";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_DOCTOR = "doctor";

    private Const() {
    }
}
