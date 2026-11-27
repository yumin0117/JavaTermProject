package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.model.*;
import kr.ac.yumin.playlist.service.MembershipService;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class MembershipDialog extends JDialog {
    private final MembershipService service;
    private final Runnable changed;
    private final JLabel status = Ui.text("이용권 확인 중", 18, true);
    private final JButton buy, cancel;
    private final DefaultTableModel history = Ui.model("결제일", "모의 금액", "사용 기한", "상태");
    private String requestId = UUID.randomUUID().toString();

    public MembershipDialog(Window owner, MembershipService service, Runnable changed) {
        super(owner, "muse plus · 이용권", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.changed = changed;
        JPanel p = Ui.padded(30), top = Ui.column();
        JLabel brand = Ui.text("muse plus", 32, true);
        brand.setForeground(Ui.ACCENT);
        top.add(brand);
        top.add(Box.createVerticalStrut(10));
        top.add(Ui.text("좋아하는 음악을 끝까지.", 23, true));
        top.add(Box.createVerticalStrut(12));
        top.add(Ui.muted("무료 등급: 1분 미리듣기  ·  Plus 등급: 전체 길이 재생"));
        top.add(Ui.muted("과제에서 재생 등급을 확인하는 모의 이용권입니다."));
        top.add(Box.createVerticalStrut(22));
        top.add(Ui.text("30일 이용권   ₩4,900", 22, true));
        top.add(Box.createVerticalStrut(8));
        top.add(Ui.muted("과제용 모의 결제입니다. 실제 청구 금액은 0원입니다."));
        top.add(Box.createVerticalStrut(20));
        top.add(status);
        top.add(Box.createVerticalStrut(12));
        buy =
                Ui.primary(
                        "모의 결제하고 전체 듣기",
                        () -> {
                            Ui.work(
                                    this,
                                    () -> {
                                        service.purchase(requestId);
                                        return true;
                                    },
                                    ok -> {
                                        requestId = UUID.randomUUID().toString();
                                        changed.run();
                                        load();
                                    });
                        });
        cancel =
                Ui.quiet(
                        "최근 모의 결제 취소",
                        () -> {
                            if (JOptionPane.showConfirmDialog(
                                            this,
                                            "최근 모의 결제를 취소하고 이용권 상태를 갱신할까요?",
                                            "모의 결제 취소",
                                            JOptionPane.YES_NO_OPTION)
                                    == JOptionPane.YES_OPTION)
                                Ui.work(
                                        this,
                                        () -> {
                                            service.cancelLatest();
                                            return true;
                                        },
                                        ok -> {
                                            changed.run();
                                            load();
                                        });
                        });
        top.add(Ui.row(buy, cancel));
        p.add(top, BorderLayout.NORTH);
        JTable table = Ui.table(history);
        table.setRowHeight(38);
        JScrollPane scroll = Ui.scroll(table);
        scroll.setPreferredSize(new Dimension(600, 155));
        p.add(scroll, BorderLayout.CENTER);
        p.add(Ui.row(Ui.quiet("닫기", this::dispose)), BorderLayout.SOUTH);
        setContentPane(p);
        setSize(700, 670);
        setMinimumSize(new Dimension(640, 600));
        setLocationRelativeTo(owner);
        SwingUtilities.invokeLater(this::load);
    }

    private record Data(PassStatus pass, java.util.List<PaymentOrder> orders) {}

    private void load() {
        Ui.work(
                this,
                () -> new Data(service.status(), service.history()),
                data -> {
                    boolean active = data.pass().active();
                    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
                    status.setText(
                            active
                                    ? "Plus 이용 중 · " + data.pass().expiresAt().format(f) + "까지"
                                    : "무료 이용 중 · 곡당 1분 미리듣기");
                    buy.setEnabled(!active);
                    cancel.setEnabled(
                            data.orders().stream().anyMatch(o -> o.status().equals("PAID")));
                    history.setRowCount(0);
                    for (PaymentOrder o : data.orders())
                        history.addRow(
                                new Object[] {
                                    o.paidAt().format(f),
                                    "₩" + o.amount(),
                                    o.validUntil().format(f),
                                    o.status().equals("PAID") ? "모의 결제 완료" : "모의 결제 취소"
                                });
                });
    }
}
