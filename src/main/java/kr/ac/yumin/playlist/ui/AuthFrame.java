package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.service.AuthService;

import java.awt.*;

import javax.swing.*;

public final class AuthFrame extends JFrame {
    private final AuthService auth = new AuthService();

    public AuthFrame() {
        super("음악 플레이리스트 공유 시스템 | 로그인");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(620, 540));
        setSize(740, 620);
        JPanel p = new JPanel(new BorderLayout(0, 28));
        p.setBorder(BorderFactory.createEmptyBorder(45, 65, 40, 65));
        JPanel intro = Ui.column();
        intro.add(Ui.heading("음악을 모으고, 취향을 나누세요"));
        intro.add(Box.createVerticalStrut(16));
        intro.add(new JLabel("음악 플레이리스트 공유 시스템"));
        p.add(intro, BorderLayout.NORTH);
        JTextField id = new JTextField(24);
        JPasswordField pw = new JPasswordField(24);
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 0, 10, 14);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        g.gridx = 0;
        g.gridy = 0;
        fields.add(new JLabel("회원 아이디"), g);
        g.gridy = 1;
        fields.add(id, g);
        g.gridy = 2;
        fields.add(new JLabel("비밀번호"), g);
        g.gridy = 3;
        fields.add(pw, g);
        JButton login =
                Ui.button(
                        "로그인",
                        () -> {
                            String name = id.getText();
                            char[] password = pw.getPassword();
                            pw.setText("");
                            Ui.work(
                                    this,
                                    () -> auth.login(name, password),
                                    member -> {
                                        new MainFrame(member).setVisible(true);
                                        dispose();
                                    });
                        });
        g.gridy = 4;
        fields.add(login, g);
        g.gridy = 5;
        fields.add(Ui.button("새 계정 만들기", () -> register()), g);
        p.add(fields, BorderLayout.CENTER);
        p.add(new JLabel("처음 사용한다면 회원가입하세요. 기존 예시 계정은 로그인용이 아닙니다."), BorderLayout.SOUTH);
        setContentPane(p);
        getRootPane().setDefaultButton(login);
        setLocationRelativeTo(null);
    }

    private void register() {
        JDialog dialog = new JDialog(this, "회원가입", true);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JTextField id = new JTextField(22),
                nickname = new JTextField(22),
                email = new JTextField(22);
        JPasswordField pw = new JPasswordField(22), confirm = new JPasswordField(22);
        Component[] fields = {id, pw, confirm, nickname, email};
        String[] labels = {"회원 아이디 (30자)", "비밀번호", "비밀번호 확인", "닉네임 (50자)", "이메일 (254자)"};
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < fields.length; i++) {
            g.gridy = i;
            g.gridx = 0;
            g.weightx = 0;
            form.add(new JLabel(labels[i]), g);
            g.gridx = 1;
            g.weightx = 1;
            form.add(fields[i], g);
        }
        JButton check =
                Ui.button(
                        "아이디 중복 검사",
                        () -> {
                            String value = id.getText();
                            Ui.work(
                                    dialog,
                                    () -> auth.available(value),
                                    available ->
                                            Ui.info(
                                                    dialog,
                                                    available
                                                            ? "사용 가능한 아이디입니다."
                                                            : "이미 사용 중인 아이디입니다."));
                        });
        g.gridy = 5;
        g.gridx = 0;
        g.gridwidth = 2;
        form.add(check, g);
        JButton submit =
                Ui.button(
                        "가입하기",
                        () -> {
                            String a = id.getText(), n = nickname.getText(), e = email.getText();
                            char[] b = pw.getPassword(), c = confirm.getPassword();
                            pw.setText("");
                            confirm.setText("");
                            Ui.work(
                                    dialog,
                                    () -> {
                                        auth.register(a, b, c, n, e);
                                        return true;
                                    },
                                    ok -> {
                                        Ui.info(dialog, "회원가입이 완료되었습니다. 새 계정으로 로그인해 주세요.");
                                        dialog.dispose();
                                    });
                        });
        g.gridy = 6;
        form.add(submit, g);
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setMinimumSize(dialog.getSize());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
