package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.api.moviebooking.helpers.exceptions.CustomException;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Minimal exchange rate client used to convert the application's base
 * currency (VND) to the currencies required by remote gateways (e.g. USD for
 * PayPal). Rates are cached for a configurable TTL so we don't hit the public
 * API on every checkout request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final RestTemplate restTemplate;

    @Value("${currency.exchange.api:https://latest.currency-api.pages.dev/v1/currencies}")
    private String apiBaseUrl;

    @Value("${currency.exchange.cache-ttl-seconds:900}")
    private long cacheTtlSeconds;

    private final Map<String, CachedRate> cache = new ConcurrentHashMap<>();

    public CurrencyConversion convert(BigDecimal amount, String sourceCurrency, String targetCurrency) {
        if (amount == null) {
            throw new CustomException("Amount is required for conversion", HttpStatus.BAD_REQUEST);
        }

        String normalizedSource = normalizeCurrency(sourceCurrency);
        String normalizedTarget = normalizeCurrency(targetCurrency);

        if (normalizedSource.equals(normalizedTarget)) {
            BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
            return new CurrencyConversion(amount, normalizedSource, normalizedAmount, normalizedTarget,
                    BigDecimal.ONE);
        }

        BigDecimal rate = getRate(normalizedSource, normalizedTarget);
        BigDecimal converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new CurrencyConversion(amount, normalizedSource, converted, normalizedTarget, rate);
    }

    private BigDecimal getRate(String sourceCurrency, String targetCurrency) {
        String cacheKey = sourceCurrency + "_" + targetCurrency;
        CachedRate cached = cache.get(cacheKey);
        Instant now = Instant.now();

        if (cached != null && !isExpired(cached.timestamp(), now)) {
            return cached.rate();
        }

        try {
            String url = buildUrl(sourceCurrency);
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            if (root == null) {
                throw new CustomException("Exchange API returned empty response", HttpStatus.BAD_GATEWAY);
            }

            JsonNode rateNode = root.path(sourceCurrency.toLowerCase(Locale.ROOT))
                    .path(targetCurrency.toLowerCase(Locale.ROOT));
            if (rateNode.isMissingNode() || rateNode.isNull()) {
                throw new CustomException(
                        String.format("Exchange rate %s -> %s not available", sourceCurrency, targetCurrency),
                        HttpStatus.BAD_REQUEST);
            }

            BigDecimal rate = rateNode.decimalValue();
            cache.put(cacheKey, new CachedRate(rate, now));
            return rate;
        } catch (RestClientException ex) {
            log.error("Failed to fetch exchange rate {} -> {}", sourceCurrency, targetCurrency, ex);
            if (cached != null) {
                log.warn("Using stale exchange rate for {} -> {}", sourceCurrency, targetCurrency);
                return cached.rate();
            }
            throw new CustomException("Unable to fetch exchange rates at the moment. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String buildUrl(String sourceCurrency) {
        String base = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
        return base + "/" + sourceCurrency.toLowerCase(Locale.ROOT) + ".json";
    }

    private boolean isExpired(Instant timestamp, Instant now) {
        if (cacheTtlSeconds <= 0) {
            return true;
        }
        return Duration.between(timestamp, now).getSeconds() > cacheTtlSeconds;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new CustomException("Currency code is required", HttpStatus.BAD_REQUEST);
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    public record CurrencyConversion(
            BigDecimal sourceAmount,
            String sourceCurrency,
            BigDecimal targetAmount,
            String targetCurrency,
            BigDecimal rate) {
    }

    private record CachedRate(BigDecimal rate, Instant timestamp) {
    }
}
