package com.api.moviebooking.helpers.utils;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class SecurityUtils {
    public static String HmacSHA512sign(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(key);
            byte[] raw = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * raw.length);
            for (byte b : raw)
                sb.append(String.format("%02x", b)); // lower-case hex
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC SHA512 error: " + e.getMessage(), e);
        }
    }
}
