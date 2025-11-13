package com.api.moviebooking.helpers.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class VNPayParamUtils {

    // Build "key=value&..." with keys sorted ASC, excluding vnp_SecureHash*
    public static String buildHashData(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");
        StringBuilder sb = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> it = sorted.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> e = it.next();
            sb.append(e.getKey()).append("=").append(e.getValue());
            if (it.hasNext())
                sb.append("&");
        }
        return sb.toString();
    }

    // Build URL query string with proper encoding
    public static String buildQuery(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> it = sorted.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> e = it.next();
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            if (it.hasNext())
                sb.append("&");
        }
        return sb.toString();
    }
}