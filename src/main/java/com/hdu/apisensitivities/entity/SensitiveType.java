package com.hdu.apisensitivities.entity;

public enum SensitiveType {
    PHONE_NUMBER,
    EMAIL,
    ID_CARD,
    BANK_CARD,
    API_KEY,
    CUSTOM,
    CREDIT_CARD,
    PASSPORT,
    SOCIAL_SECURITY,
    BIRTH_DATE,
    PASSWORD,
    NAME,
    IP_ADDRESS,
    LICENSE_PLATE,
    PERSON,       // 对应 HanLP 的 nr
    ADDRESS,      // 对应 HanLP 的 ns
    ORGANIZATION  // 对应 HanLP 的 nt
}
