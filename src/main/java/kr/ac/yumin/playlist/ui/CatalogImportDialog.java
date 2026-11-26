package kr.ac.yumin.playlist.ui;

import kr.ac.yumin.playlist.service.CatalogService;

import java.awt.*;

import javax.swing.*;

final class CatalogImportDialog extends JDialog {
    CatalogImportDialog(Window owner, Runnable changed) {
        super(owner, "음악 더 불러오기", ModalityType.APPLICATION_MODAL);
        JPanel root = Ui.padded(26), body = Ui.column();
        body.add(Ui.text("음악 더 불러오기", 24, true));
        body.add(Box.createVerticalStrut(12));
        body.add(Ui.muted("최신곡 목록이나 멜론 곡 번호로 보관함에 추가하세요."));
        body.add(Box.createVerticalStrut(22));
        JLabel status = Ui.muted("곡 정보·앨범 커버를 저장합니다. 음원 파일은 별도로 연결하세요.");
        body.add(
                Ui.primary(
                        "최신곡 목록 불러오기",
                        () ->
                                Ui.work(
                                        this,
                                        () -> new CatalogService().importLatest(),
                                        n -> {
                                            status.setText(
                                                    n == 0
                                                            ? "모두 보관함에 등록된 곡입니다."
                                                            : n + "곡을 새로 보관함에 추가했습니다.");
                                            changed.run();
                                        })));
        body.add(Box.createVerticalStrut(24));
        body.add(Ui.text("곡 하나 추가", 14, true));
        body.add(Box.createVerticalStrut(10));
        JTextField input = new JTextField();
        input.putClientProperty("JTextField.placeholderText", "멜론 곡 상세 URL 또는 곡 번호");
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        body.add(input);
        body.add(Box.createVerticalStrut(10));
        body.add(
                Ui.button(
                        "보관함에 추가",
                        () -> {
                            String value = input.getText();
                            Ui.work(
                                    this,
                                    () -> new CatalogService().importSong(value),
                                    n -> {
                                        status.setText(
                                                n == 0
                                                        ? "이미 등록된 곡입니다. 커버를 눌러 앨범 정보를 확인하세요."
                                                        : "곡을 추가했습니다. 커버를 누르면 앨범 정보를 볼 수 있습니다.");
                                        changed.run();
                                    });
                        }));
        body.add(Box.createVerticalStrut(24));
        body.add(status);
        root.add(body, BorderLayout.CENTER);
        root.add(Ui.row(Ui.quiet("닫기", this::dispose)), BorderLayout.SOUTH);
        setContentPane(root);
        setSize(650, 430);
        setLocationRelativeTo(owner);
    }
}
