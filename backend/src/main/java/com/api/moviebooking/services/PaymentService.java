package com.api.moviebooking.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.api.moviebooking.helpers.exceptions.CustomException;
import com.api.moviebooking.helpers.mapstructs.PaymentMapper;
import com.api.moviebooking.models.dtos.payment.ConfirmPaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentResponse;
import com.api.moviebooking.models.dtos.payment.IpnResponse;
import com.api.moviebooking.models.dtos.payment.PaymentResponse;
import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.repositories.PaymentRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PayPalService payPalService;
    private final MomoService momoService;
    private final PaymentRepo paymentRepo;
    private final PaymentMapper paymentMapper;
    private final RefundService refundService;

    public InitiatePaymentResponse createOrder(InitiatePaymentRequest request) {

        String method = request.getPaymentMethod().toLowerCase();
        switch (method) {
            case "paypal":
                return payPalService.createOrder(request);
            case "momo":
                return momoService.createOrder(request);
            default:
                throw new CustomException("Unsupported payment method", HttpStatus.BAD_REQUEST);
        }
    }

    public PaymentResponse confirmPayment(ConfirmPaymentRequest request) {
        String method = request.getPaymentMethod().toLowerCase();
        switch (method) {
            case "paypal":
                // Call PayPalService to capture order (transaction not yet finished)
                return payPalService.captureOrder(request.getTransactionId());
            case "momo":
                // Call MomoService to confirm payment (transaction has been made, just verify)
                return momoService.verifyPayment(request.getTransactionId());
            default:
                throw new CustomException("Unsupported payment method", HttpStatus.BAD_REQUEST);
        }

    }

    public IpnResponse processMomoIpn(HttpServletRequest request) {
        var params = extractParams(request);
        return momoService.processIpn(params);
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> map.put(k, v != null && v.length > 0 ? v[0] : ""));
        return map;
    }

    public List<PaymentResponse> searchPayments(UUID bookingId,
            UUID userId,
            String transactionId,
            String status,
            String method,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        List<Payment> payments = paymentRepo.findAll();
        List<Payment> filteredPayments = payments.stream()
                .filter(p -> bookingId == null || p.getBooking().getId().equals(bookingId))
                .filter(p -> userId == null || p.getBooking().getUser().getId().equals(userId))
                .filter(p -> transactionId == null || p.getTransactionId().equals(transactionId))
                .filter(p -> status == null || p.getStatus().name().equalsIgnoreCase(status))
                .filter(p -> method == null || p.getMethod().name().equalsIgnoreCase(method))
                .filter(p -> startDate == null || !p.getCreatedAt().isBefore(startDate))
                .filter(p -> endDate == null || !p.getCreatedAt().isAfter(endDate))
                .toList();

        return filteredPayments.stream()
                .map(paymentMapper::toPaymentResponse)
                .toList();
    }

    public PaymentResponse refundPayment(UUID paymentId, String reason) {
        Payment payment = refundService.processRefund(paymentId, reason);
        return paymentMapper.toPaymentResponse(payment);
    }

}