package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.config.Database;
import kr.ac.yumin.playlist.dao.MemberDao;
import kr.ac.yumin.playlist.model.Member;
import kr.ac.yumin.playlist.security.PasswordHasher;

import java.sql.*;
import java.util.Arrays;

public final class AuthService {
    private final MemberDao dao = new MemberDao();

    public static String text(String s, String label, int max, boolean required) {
        String v = s == null ? "" : s.strip();
        if (required && v.isEmpty()) throw new AppException(label + "을(를) 입력해 주세요.");
        if (v.codePointCount(0, v.length()) > max)
            throw new AppException(label + "은(는) " + max + "자 이하여야 합니다.");
        return v;
    }

    public boolean available(String id) throws SQLException {
        id = text(id, "아이디", 30, true);
        try (Connection c = Database.open()) {
            return dao.find(c, id).isEmpty();
        }
    }

    public void register(String id, char[] password, char[] confirm, String nickname, String email)
            throws SQLException {
        try {
            id = text(id, "아이디", 30, true);
            nickname = text(nickname, "닉네임", 50, true);
            email = text(email, "이메일", 254, true);
            if (password.length == 0) throw new AppException("비밀번호를 입력해 주세요.");
            if (password.length > 1024) throw new AppException("비밀번호는 1024자 이하여야 합니다.");
            if (!Arrays.equals(password, confirm)) throw new AppException("비밀번호 확인이 일치하지 않습니다.");
            String hash = PasswordHasher.hash(password);
            try (Connection c = Database.open()) {
                if (dao.find(c, id).isPresent()) throw new AppException("이미 사용 중인 아이디입니다.");
                dao.insert(c, new Member(id, nickname, email), hash);
            } catch (SQLException e) {
                if (e.getErrorCode() == 1) throw new AppException("이미 사용 중인 아이디입니다.");
                throw e;
            }
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    public Member login(String id, char[] password) throws SQLException {
        try {
            id = text(id, "아이디", 30, true);
            try (Connection c = Database.open()) {
                var found = dao.find(c, id);
                if (found.isEmpty() || !PasswordHasher.verify(password, found.get().hash()))
                    throw new AppException("아이디 또는 비밀번호가 올바르지 않습니다. 예시 계정 대신 새로 회원가입해 주세요.");
                return found.get().member();
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
