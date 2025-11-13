package com.api.moviebooking.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.moviebooking.helpers.exceptions.CustomException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.helpers.mapstructs.PaymentMapper;
import com.api.moviebooking.helpers.utils.SecurityUtils;
import com.api.moviebooking.helpers.utils.VNPayParamUtils;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentRequest;
import com.api.moviebooking.models.dtos.payment.InitiatePaymentResponse;
import com.api.moviebooking.models.dtos.payment.IpnResponse;
import com.api.moviebooking.models.dtos.payment.PaymentResponse;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.PaymentMethod;
import com.api.moviebooking.models.enums.PaymentStatus;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.PaymentRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VNPayService {

    @Value("${vnpay.version}")
    private String vnpVersion;

    @Value("${vnpay.command}")
    private String vnpCommand;

    @Value("${vnpay.tmn.code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash.secret}")
    private String vnpHashSecret;

    @Value("${vnpay.currency}")
    private String vnpCurrCode;

    @Value("${vnpay.locale}")
    private String vnpLocale;

    @Value("${vnpay.return.url}")
    private String vnpReturnUrl;

    @Value("${vnpay.ipn.url}")
    private String vnpIpnUrl;

    @Value("${vnpay.payment.url}")
    private String vnpPaymentUrl;

    private final PaymentRepo paymentRepo;
    private final BookingService bookingService;
    private final BookingRepo bookingRepo;
    private final PaymentMapper paymentMapper;

    // Step 1: Build payment URL (called by FE to redirect user)
    @Transactional
    public InitiatePaymentResponse createOrder(InitiatePaymentRequest request) {

        // Validate booking exists and is in correct status
        Booking booking = bookingService.getBookingById(request.getBookingId());

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new CustomException("Booking must be confirmed before payment", HttpStatus.BAD_REQUEST);
        }

        // Verify amount matches booking total
        if (request.getAmount().compareTo(booking.getTotalPrice()) != 0) {
            throw new CustomException("Payment amount does not match booking total", HttpStatus.BAD_REQUEST);
        }

        // Check if payment already exists for this booking
        Optional<Payment> existingPayment = paymentRepo.findByBookingIdAndMethodAndStatus(booking.getId(),
                PaymentMethod.VNPAY, PaymentStatus.PENDING);

        // Create or reuse a PENDING payment
        Payment payment = existingPayment
                .orElseGet(() -> {
                    Payment newPayment = new Payment();
                    newPayment.setMethod(PaymentMethod.VNPAY);
                    newPayment.setStatus(PaymentStatus.PENDING);
                    newPayment.setCurrency("VND");
                    newPayment.setAmount(request.getAmount());
                    newPayment.setBooking(booking);
                    return paymentRepo.save(newPayment);
                });

        String txnRef = payment.getId().toString(); // use payment ID as txn ref

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnpVersion);
        params.put("vnp_Command", vnpCommand);
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_Amount", request.getAmount()
                .multiply(BigDecimal.valueOf(100)).toBigInteger().toString()); // VND * 100 because VNPAY works in cents
        params.put("vnp_CurrCode", vnpCurrCode);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Booking " + booking.getId());
        params.put("vnp_Locale", vnpLocale);
        params.put("vnp_IpAddr", request.getIpAddress());
        params.put("vnp_ReturnUrl", vnpReturnUrl);

        String createDate = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        params.put("vnp_CreateDate", createDate);

        // optional expire date (15 minutes later)
        String expireDate = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(15)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        params.put("vnp_ExpireDate", expireDate);

        // IPN URL
        params.put("vnp_IpnUrl", vnpIpnUrl);

        // Compute secure hash
        String hashData = VNPayParamUtils.buildHashData(params);
        String secureHash = SecurityUtils.HmacSHA512sign(vnpHashSecret, hashData);
        params.put("vnp_SecureHashType", "HmacSHA512");
        params.put("vnp_SecureHash", secureHash);

        String query = VNPayParamUtils.buildQuery(params);
        String paymentUrl = vnpPaymentUrl + "?" + query;

        // Save reference on payment for later reconciliation
        payment.setTransactionId(txnRef);
        paymentRepo.save(payment);

        return new InitiatePaymentResponse(null, txnRef, paymentUrl);
    }

    // Step 2: Process IPN (server-to-server callback from VNPay)
    @Transactional
    public IpnResponse processIpn(Map<String, String> allParams) {
        String receivedHash = allParams.get("vnp_SecureHash");
        if (receivedHash == null)
            return IpnResponse.invalidChecksum();

        String hashData = VNPayParamUtils.buildHashData(allParams);
        String calc = SecurityUtils.HmacSHA512sign(vnpHashSecret, hashData);
        if (!calc.equalsIgnoreCase(receivedHash)) {
            return IpnResponse.invalidChecksum();
        }

        String txnRef = allParams.get("vnp_TxnRef");
        String amountStr = allParams.get("vnp_Amount");
        String responseCode = allParams.get("vnp_ResponseCode"); // "00" = success
        String transactionStatus = allParams.get("vnp_TransactionStatus"); // often "00" on success too

        if (txnRef == null || amountStr == null)
            return IpnResponse.orderNotFound();

        Optional<Payment> optPay = paymentRepo.findByTransactionId(txnRef);
        if (optPay.isEmpty())
            return IpnResponse.orderNotFound();

        Payment payment = optPay.get();
        // Verify amount
        BigDecimal expected = payment.getAmount().multiply(BigDecimal.valueOf(100)); // we sent VND*100
        if (!expected.toBigInteger().toString().equals(amountStr)) {
            return IpnResponse.amountInvalid();
        }

        // Idempotency: if already completed, return OK
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return IpnResponse.orderAlreadyConfirmed();
        }

        // Success check
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);
        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepo.save(payment);
            return IpnResponse.ok();
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepo.save(payment);

            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CANCELED);
            bookingRepo.save(booking);

            return IpnResponse.paymentFailed();
        }
    }

    public PaymentResponse verifyPayment(String transactionId) {
        Payment payment = paymentRepo.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionId", transactionId));

        return paymentMapper.toPaymentResponse(payment);
    }

}
