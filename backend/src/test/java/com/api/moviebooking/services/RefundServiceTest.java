package com.api.moviebooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;

import com.api.moviebooking.helpers.exceptions.CustomException;
import com.api.moviebooking.helpers.exceptions.ResourceNotFoundException;
import com.api.moviebooking.models.entities.Booking;
import com.api.moviebooking.models.entities.Payment;
import com.api.moviebooking.models.entities.Refund;
import com.api.moviebooking.models.enums.BookingStatus;
import com.api.moviebooking.models.enums.PaymentMethod;
import com.api.moviebooking.models.enums.PaymentStatus;
import com.api.moviebooking.repositories.BookingRepo;
import com.api.moviebooking.repositories.PaymentRepo;
import com.api.moviebooking.repositories.RefundRepo;
import com.api.moviebooking.tags.RegressionTest;
import com.api.moviebooking.tags.SanityTest;
import com.api.moviebooking.tags.SmokeTest;

/**
 * Unit tests for RefundService.
 * Tests manual and automatic refund processing with validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService Unit Tests")
class RefundServiceTest {

    @Mock
    private PaymentRepo paymentRepo;

    @Mock
    private BookingRepo bookingRepo;

    @Mock
    private RefundRepo refundRepo;

    @Mock
    private PayPalService payPalService;

    @Mock
    private MomoService momoService;

    @Mock
    private CheckoutLifecycleService checkoutLifecycleService;

    @InjectMocks
    private RefundService refundService;

    private UUID paymentId;
    private Payment payment;
    private Booking booking;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();

        booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setFinalPrice(new BigDecimal("100000"));
        booking.setRefunded(false);

        payment = new Payment();
        payment.setId(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setMethod(PaymentMethod.PAYPAL);
        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("100000"));
    }

    // ========================================================================
    // Manual Refund Tests
    // ========================================================================

    @Nested
    @DisplayName("Manual Refund Processing")
    class ManualRefundTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("Should process PayPal refund successfully")
        void testProcessRefund_PayPal_Success() throws Exception {
            String reason = "Customer request";
            String gatewayTxnId = "PAYPAL-REFUND-123";

            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));
            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
            when(payPalService.refundPayment(eq(payment), any(BigDecimal.class), eq(reason)))
                    .thenReturn(gatewayTxnId);

            Payment result = refundService.processRefund(paymentId, reason);

            assertNotNull(result);
            verify(paymentRepo).findById(paymentId);
            verify(payPalService).refundPayment(eq(payment), any(BigDecimal.class), eq(reason));
            verify(checkoutLifecycleService).handleRefundSuccess(eq(payment), any(Refund.class),
                    eq(gatewayTxnId));
        }

        @Test
        @SanityTest
        @RegressionTest
        @DisplayName("Should process Momo refund successfully")
        void testProcessRefund_Momo_Success() throws Exception {
            payment.setMethod(PaymentMethod.MOMO);
            String reason = "Admin initiated";
            String gatewayTxnId = "MOMO-REFUND-456";

            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));
            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
            when(momoService.refundPayment(eq(payment), any(BigDecimal.class), eq(reason)))
                    .thenReturn(gatewayTxnId);

            Payment result = refundService.processRefund(paymentId, reason);

            assertNotNull(result);
            verify(momoService).refundPayment(eq(payment), any(BigDecimal.class), eq(reason));
            verify(checkoutLifecycleService).handleRefundSuccess(eq(payment), any(Refund.class),
                    eq(gatewayTxnId));
        }

        @Test
        @RegressionTest
        @DisplayName("Should throw exception when payment not found")
        void testProcessRefund_PaymentNotFound() {
            when(paymentRepo.findById(paymentId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                refundService.processRefund(paymentId, "reason");
            });

            verify(payPalService, never()).refundPayment(any(), any(), any());
            verify(momoService, never()).refundPayment(any(), any(), any());
        }

        @Test
        @RegressionTest
        @DisplayName("Should throw exception when booking not confirmed")
        void testProcessRefund_BookingNotConfirmed() {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);

            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));

            CustomException exception = assertThrows(CustomException.class, () -> {
                refundService.processRefund(paymentId, "reason");
            });

            assertTrue(exception.getMessage().contains("confirmed bookings"));
            verify(payPalService, never()).refundPayment(any(), any(), any());
        }

        @Test
        @RegressionTest
        @DisplayName("Should throw exception when payment not successful")
        void testProcessRefund_PaymentNotSuccess() {
            payment.setStatus(PaymentStatus.PENDING);

            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));

            CustomException exception = assertThrows(CustomException.class, () -> {
                refundService.processRefund(paymentId, "reason");
            });

            assertTrue(exception.getMessage().contains("successful payments"));
            verify(payPalService, never()).refundPayment(any(), any(), any());
        }

        @Test
        @RegressionTest
        @DisplayName("Should throw exception when already refunded")
        void testProcessRefund_AlreadyRefunded() {
            booking.setRefunded(true);

            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));

            CustomException exception = assertThrows(CustomException.class, () -> {
                refundService.processRefund(paymentId, "reason");
            });

            assertTrue(exception.getMessage().contains("already refunded"));
            verify(payPalService, never()).refundPayment(any(), any(), any());
        }

        @Test
        @RegressionTest
        @DisplayName("Should throw exception when payment method unavailable")
        void testProcessRefund_NoPaymentMethod() {
            payment.setMethod(null);

            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));

            CustomException exception = assertThrows(CustomException.class, () -> {
                refundService.processRefund(paymentId, "reason");
            });

            assertTrue(exception.getMessage().contains("Payment method unavailable"));
        }

        @Test
        @SanityTest
        @RegressionTest
        @DisplayName("Should rollback on refund failure")
        void testProcessRefund_RollbackOnFailure() throws Exception {
            String reason = "Test rollback";

            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));
            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
            when(payPalService.refundPayment(any(), any(), any()))
                    .thenThrow(new RuntimeException("Gateway error"));

            assertThrows(CustomException.class, () -> {
                refundService.processRefund(paymentId, reason);
            });

            verify(checkoutLifecycleService).handleRefundFailure(eq(payment), anyString());
            verify(paymentRepo, atLeast(2)).save(payment); // Original save + rollback
            verify(bookingRepo, atLeast(2)).save(booking); // Original save + rollback
        }
    }

    // ========================================================================
    // Automatic Refund Tests
    // ========================================================================

    @Nested
    @DisplayName("Automatic Refund Processing")
    class AutomaticRefundTests {

        @Test
        @SmokeTest
        @SanityTest
        @RegressionTest
        @DisplayName("Should process automatic refund for failed payment")
        void testProcessAutomaticRefund_FailedPayment() throws Exception {
            payment.setStatus(PaymentStatus.FAILED);
            String reason = "Late payment failure";
            String gatewayTxnId = "AUTO-REFUND-789";

            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
            when(payPalService.refundPayment(eq(payment), any(BigDecimal.class), eq(reason)))
                    .thenReturn(gatewayTxnId);

            Payment result = refundService.processAutomaticRefund(payment, reason);

            assertNotNull(result);
            verify(payPalService).refundPayment(eq(payment), any(BigDecimal.class), eq(reason));
            verify(checkoutLifecycleService).handleRefundSuccess(eq(payment), any(Refund.class),
                    eq(gatewayTxnId));
        }

        @Test
        @SanityTest
        @RegressionTest
        @DisplayName("Should process automatic refund for success payment")
        void testProcessAutomaticRefund_SuccessPayment() throws Exception {
            // payment status is SUCCESS by default
            String reason = "Automatic system refund";
            String gatewayTxnId = "AUTO-REFUND-999";

            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
            when(payPalService.refundPayment(eq(payment), any(BigDecimal.class), eq(reason)))
                    .thenReturn(gatewayTxnId);

            Payment result = refundService.processAutomaticRefund(payment, reason);

            assertNotNull(result);
            verify(checkoutLifecycleService).handleRefundSuccess(eq(payment), any(Refund.class),
                    eq(gatewayTxnId));
        }

        @Test
        @RegressionTest
        @DisplayName("Should reject automatic refund for pending payment")
        void testProcessAutomaticRefund_PendingPayment() {
            payment.setStatus(PaymentStatus.PENDING);
            String reason = "Test";

            CustomException exception = assertThrows(CustomException.class, () -> {
                refundService.processAutomaticRefund(payment, reason);
            });

            assertTrue(exception.getMessage().contains("Cannot auto-refund payment"));
            verify(payPalService, never()).refundPayment(any(), any(), any());
        }

        @Test
        @SanityTest
        @RegressionTest
        @DisplayName("Should use Momo for automatic refund")
        void testProcessAutomaticRefund_Momo() throws Exception {
            payment.setMethod(PaymentMethod.MOMO);
            payment.setStatus(PaymentStatus.FAILED);
            String reason = "Auto refund";
            String gatewayTxnId = "MOMO-AUTO-123";

            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
            when(momoService.refundPayment(eq(payment), any(BigDecimal.class), eq(reason)))
                    .thenReturn(gatewayTxnId);

            Payment result = refundService.processAutomaticRefund(payment, reason);

            assertNotNull(result);
            verify(momoService).refundPayment(eq(payment), any(BigDecimal.class), eq(reason));
        }
    }

    // ========================================================================
    // Refund Amount Tests
    // ========================================================================

    @Nested
    @DisplayName("Refund Amount Validation")
    class RefundAmountTests {

        @Test
        @SanityTest
        @RegressionTest
        @DisplayName("Should refund correct amount from booking")
        void testProcessRefund_CorrectAmount() throws Exception {
            BigDecimal expectedAmount = new BigDecimal("123456.78");
            booking.setFinalPrice(expectedAmount);

            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));
            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> {
                Refund refund = i.getArgument(0);
                assertEquals(expectedAmount, refund.getAmount());
                return refund;
            });
            when(payPalService.refundPayment(eq(payment), eq(expectedAmount), anyString()))
                    .thenReturn("TXN-123");

            refundService.processRefund(paymentId, "Test");

            verify(payPalService).refundPayment(eq(payment), eq(expectedAmount), anyString());
        }

        @Test
        @RegressionTest
        @DisplayName("Should set refund method from payment method")
        void testProcessRefund_RefundMethod() throws Exception {
            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));
            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> {
                Refund refund = i.getArgument(0);
                assertEquals("PAYPAL", refund.getRefundMethod());
                return refund;
            });
            when(payPalService.refundPayment(any(), any(), any())).thenReturn("TXN-123");

            refundService.processRefund(paymentId, "Test");

            verify(refundRepo, atLeast(1)).save(any(Refund.class));
        }
    }

    // ========================================================================
    // Status Transition Tests
    // ========================================================================

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitionTests {

        @Test
        @SanityTest
        @RegressionTest
        @DisplayName("Should set status to REFUND_PENDING before gateway call")
        void testProcessRefund_PendingStatus() throws Exception {
            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));
            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
            when(payPalService.refundPayment(any(), any(), any())).thenAnswer(invocation -> {
                // At this point, status should already be REFUND_PENDING
                assertEquals(PaymentStatus.REFUND_PENDING, payment.getStatus());
                assertEquals(BookingStatus.REFUND_PENDING, booking.getStatus());
                return "TXN-123";
            });

            refundService.processRefund(paymentId, "Test");

            verify(paymentRepo, atLeast(1)).save(payment);
            verify(bookingRepo, atLeast(1)).save(booking);
        }

        @Test
        @SanityTest
        @RegressionTest
        @DisplayName("Should restore original status on failure")
        void testProcessRefund_RestoreStatusOnFailure() throws Exception {
            PaymentStatus originalPaymentStatus = payment.getStatus();
            BookingStatus originalBookingStatus = booking.getStatus();

            when(paymentRepo.findById(paymentId)).thenReturn(Optional.of(payment));
            when(refundRepo.save(any(Refund.class))).thenAnswer(i -> i.getArgument(0));
            when(payPalService.refundPayment(any(), any(), any()))
                    .thenThrow(new RuntimeException("Gateway error"));

            try {
                refundService.processRefund(paymentId, "Test");
            } catch (CustomException e) {
                // Expected
            }

            assertEquals(originalPaymentStatus, payment.getStatus());
            assertEquals(originalBookingStatus, booking.getStatus());
        }
    }
}
