package com.api.moviebooking.models.dtos.payment;

public record IpnResponse(
        String RspCode,
        String Message) {
    public static IpnResponse ok() {
        return new IpnResponse("00", "Confirm Success");
    }

    public static IpnResponse invalidChecksum() {
        return new IpnResponse("97", "Invalid Checksum");
    }

    public static IpnResponse orderNotFound() {
        return new IpnResponse("01", "Order not found");
    }

    public static IpnResponse amountInvalid() {
        return new IpnResponse("04", "Invalid Amount");
    }

    public static IpnResponse orderAlreadyConfirmed() {
        return new IpnResponse("02", "Order already confirmed");
    }

    public static IpnResponse error() {
        return new IpnResponse("99", "Unknown error");
    }

    public static IpnResponse paymentFailed() {
        return new IpnResponse("10", "Payment Failed");
    }
}