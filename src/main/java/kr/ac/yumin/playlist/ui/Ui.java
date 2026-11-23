package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.service.AppException;

import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class Ui {
    private Ui() {}

    public static void theme() {
        UIManager.put("defaultFont", new Font("맑은 고딕", Font.PLAIN, 14));
        for (Object key : UIManager.getDefaults().keySet().toArray())
            if (key.toString().endsWith(".font"))
                UIManager.put(key, new Font("맑은 고딕", Font.PLAIN, 14));
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.selectionBackground", new Color(222, 234, 255));
        UIManager.put("Table.selectionForeground", new Color(20, 35, 55));
    }

    public static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    public static JPanel row(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        for (Component c : components) p.add(c);
        return p;
    }

    public static JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 25f));
        return l;
    }

    public static DefaultTableModel model(String... headers) {
        return new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(33);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        t.setFillsViewportHeight(true);
        return t;
    }

    public static void info(Component owner, String message) {
        JOptionPane.showMessageDialog(owner, message, "안내", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component owner, Throwable e) {
        while ((e instanceof ExecutionException || e instanceof CompletionException)
                && e.getCause() != null) e = e.getCause();
        String text;
        if (e instanceof AppException) text = e.getMessage();
        else if (e instanceof SQLException s) {
            text =
                    switch (s.getErrorCode()) {
                        case 1 -> "이미 등록된 항목입니다. 새로고침 후 다시 확인해 주세요.";
                        case 1017 -> "Oracle 사용자 이름 또는 비밀번호를 확인해 주세요.";
                        case 942, 2289 -> "DB 테이블 또는 시퀀스가 없습니다. README의 SQL 설정 순서를 확인해 주세요.";
                        case 54, 30006 -> "다른 작업이 목록을 변경 중입니다. 잠시 후 다시 시도해 주세요.";
                        case 2291 -> "연결할 회원 또는 곡이 없습니다. 새로고침해 주세요.";
                        default ->
                                s.getSQLState() == null
                                                && s.getErrorCode() == 0
                                                && s.getMessage() != null
                                                && s.getMessage().startsWith("DB 설정")
                                        ? s.getMessage()
                                        : s.getMessage() != null
                                                        && s.getMessage()
                                                                .startsWith("config/db.properties")
                                                ? s.getMessage()
                                                : "데이터베이스 작업을 완료하지 못했습니다. Oracle 실행 상태와 외부 설정을 확인해"
                                                      + " 주세요. (오류 번호: "
                                                        + s.getErrorCode()
                                                        + ")";
                    };
        } else text = "작업을 완료하지 못했습니다. 입력 내용을 확인하고 다시 시도해 주세요.";
        JOptionPane.showMessageDialog(owner, text, "처리 안내", JOptionPane.WARNING_MESSAGE);
    }

    /** DB와 암호화는 작업 스레드, 화면 반영은 done()의 EDT에서 실행한다. */
    public static <T> void work(Component owner, Callable<T> job, Consumer<T> success) {
        JRootPane root = SwingUtilities.getRootPane(owner);
        if (root != null && Boolean.TRUE.equals(root.getClientProperty("busy"))) return;
        if (root != null) {
            root.putClientProperty("busy", true);
            JPanel glass = new JPanel();
            glass.setOpaque(false);
            glass.addMouseListener(new MouseAdapter() {});
            glass.setFocusTraversalKeysEnabled(false);
            glass.addKeyListener(new KeyAdapter() {});
            root.setGlassPane(glass);
            glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            glass.setVisible(true);
            glass.setFocusable(true);
            glass.requestFocusInWindow();
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return job.call();
            }

            @Override
            protected void done() {
                if (root != null) {
                    root.getGlassPane().setVisible(false);
                    root.putClientProperty("busy", false);
                }
                try {
                    T result = get();
                    if (owner.isDisplayable()) success.accept(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | RuntimeException e) {
                    if (owner.isDisplayable()) error(owner, e);
                }
            }
        }.execute();
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }
}
