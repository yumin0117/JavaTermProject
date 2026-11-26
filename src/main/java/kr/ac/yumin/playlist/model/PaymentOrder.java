package kr.ac.yumin.playlist.model;

import java.time.LocalDateTime;

public record PaymentOrder(
        String id, int amount, LocalDateTime paidAt, LocalDateTime validUntil, String status) {}
