package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.config.Database;

import java.awt.*;

import javax.swing.*;

public final class ConnectionDialog {
    private ConnectionDialog() {}

    public static void open(Window owner, Runnable connected) {
        JDialog dialog = new JDialog(owner, "음악 보관함 연결", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = Ui.padded(28);
        JPanel form = Ui.column();
        form.add(Ui.text("Oracle 보관함 연결", 23, true));
        form.add(Box.createVerticalStrut(8));
        form.add(Ui.muted("한 번 연결하면 다음부터 바로 음악 보관함이 열립니다."));
        form.add(Box.createVerticalStrut(22));
        var properties = Database.settings();
        JTextField
                url =
                        new JTextField(
                                properties.getProperty(
                                        "db.url", "jdbc:oracle:thin:@localhost:1521:xe"),
                                35),
                user = new JTextField(properties.getProperty("db.username", "c##dbsoft"), 35);
        JPasswordField password = new JPasswordField(properties.getProperty("db.password", ""), 35);
        for (var entry :
                new Object[][] {
                    {"JDBC URL", url}, {"Oracle 사용자 이름", user}, {"Oracle 비밀번호", password}
                }) {
            form.add(Ui.muted((String) entry[0]));
            form.add(Box.createVerticalStrut(6));
            form.add((Component) entry[1]);
            form.add(Box.createVerticalStrut(15));
        }
        form.add(Ui.muted("연결 전 sql/02와 sql/06 파일을 SQL Developer에서 실행해 주세요."));
        p.add(form, BorderLayout.CENTER);
        p.add(
                Ui.row(
                        Ui.primary(
                                "연결 확인하고 저장",
                                () -> {
                                    String u = url.getText(), name = user.getText();
                                    char[] secret = password.getPassword();
                                    Ui.work(
                                            dialog,
                                            () -> {
                                                Database.testAndSave(u, name, secret);
                                                return true;
                                            },
                                            ok -> {
                                                dialog.dispose();
                                                connected.run();
                                            });
                                }),
                        Ui.quiet("닫기", dialog::dispose)),
                BorderLayout.SOUTH);
        dialog.setContentPane(p);
        dialog.pack();
        dialog.setMinimumSize(dialog.getSize());
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
