package com.api.moviebooking.helpers.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MomoParamUtils {

    /**
     * Build signature string for Momo
     * Format: key=value&key=value... with keys sorted alphabetically
     */
    public static String buildSignatureData(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        // Remove signature from params
        sorted.remove("signature");

        StringBuilder sb = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> it = sorted.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> e = it.next();
            sb.append(e.getKey()).append("=").append(e.getValue());
            if (it.hasNext())
                sb.append("&");
        }
        return sb.toString();
    }

    /**
     * Build URL query string with proper encoding
     */
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
