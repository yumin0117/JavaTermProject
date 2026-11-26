package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.Jdbc;
import kr.ac.yumin.playlist.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** 실청구가 없는 과제용 결제. 상태의 원본은 Oracle이며 파일에 유료 상태를 저장하지 않는다. */
public final class MembershipService {
    public static final int PRICE_WON = 4900;
    private final String member;

    public MembershipService(Member member) {
        this.member = member.id();
    }

    public PassStatus status() throws SQLException {
        try (Connection c = Database.open()) {
            return status(c);
        }
    }

    private PassStatus status(Connection c) throws SQLException {
        return Jdbc.list(
                        c,
                        """
SELECT (SELECT MAX(valid_until) FROM MEMBERSHIP_ORDER WHERE member_id=? AND status='PAID'),
CAST(SYSTIMESTAMP AS TIMESTAMP) FROM DUAL
""",
                        r ->
                                new PassStatus(
                                        r.getTimestamp(1) == null
                                                ? null
                                                : r.getTimestamp(1).toLocalDateTime(),
                                        r.getTimestamp(2).toLocalDateTime()),
                        member)
                .get(0);
    }

    public List<PaymentOrder> history() throws SQLException {
        try (Connection c = Database.open()) {
            return Jdbc.list(
                    c,
                    "SELECT order_id,amount_won,paid_at,valid_until,status FROM MEMBERSHIP_ORDER"
                            + " WHERE member_id=? ORDER BY paid_at DESC,order_id",
                    r ->
                            new PaymentOrder(
                                    r.getString(1),
                                    r.getInt(2),
                                    r.getTimestamp(3).toLocalDateTime(),
                                    r.getTimestamp(4).toLocalDateTime(),
                                    r.getString(5)),
                    member);
        }
    }

    public void purchase(String requestId) throws SQLException {
        UUID.fromString(requestId);
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                lockMember(c);
                var existing =
                        Jdbc.list(
                                c,
                                "SELECT member_id FROM MEMBERSHIP_ORDER WHERE order_id=?",
                                r -> r.getString(1),
                                requestId);
                if (!existing.isEmpty()) {
                    if (!member.equals(existing.get(0)))
                        throw new AppException("다른 보관함의 결제 요청입니다.");
                    c.rollback();
                    return;
                }
                PassStatus current = status(c);
                LocalDateTime start =
                        current.active() ? current.expiresAt() : current.databaseNow();
                Jdbc.update(
                        c,
                        "INSERT INTO"
                            + " MEMBERSHIP_ORDER(order_id,member_id,amount_won,valid_until,status,payment_method)"
                            + " VALUES(?,?,?,?,'PAID','DEMO')",
                        requestId,
                        member,
                        PRICE_WON,
                        Timestamp.valueOf(start.plusDays(30)));
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void cancelLatest() throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                lockMember(c);
                var ids =
                        Jdbc.list(
                                c,
                                "SELECT order_id FROM MEMBERSHIP_ORDER WHERE member_id=? AND"
                                        + " status='PAID' ORDER BY valid_until DESC,paid_at DESC",
                                r -> r.getString(1),
                                member);
                if (ids.isEmpty()) throw new AppException("취소할 이용권이 없습니다.");
                Jdbc.update(
                        c,
                        "UPDATE MEMBERSHIP_ORDER SET status='CANCELLED' WHERE order_id=? AND"
                                + " member_id=? AND status='PAID'",
                        ids.get(0),
                        member);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void lockMember(Connection c) throws SQLException {
        if (Jdbc.list(
                        c,
                        "SELECT member_id FROM MEMBER WHERE member_id=? FOR UPDATE WAIT 5",
                        r -> r.getString(1),
                        member)
                .isEmpty()) throw new AppException("개인 보관함을 찾을 수 없습니다.");
    }
}
