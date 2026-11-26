package kr.ac.yumin.playlist.service;

import kr.ac.yumin.playlist.model.Member;

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;

/** 로그인 UI 없이 이 PC 전용 계정을 만들되, 정상 PBKDF2 인증을 거쳐 소유권을 유지한다. */
public final class LocalProfileService {
    public Member resolve() throws IOException, SQLException {
        return resolve(
                Path.of(System.getProperty("playlist.profile", "config/local-profile.properties")));
    }

    public Member resolve(Path file) throws IOException, SQLException {
        file = file.toAbsolutePath();
        Files.createDirectories(file.getParent());
        try (FileChannel channel =
                        FileChannel.open(
                                file.resolveSibling("local-profile.lock"),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE);
                FileLock lock = channel.lock()) {
            Properties p = new Properties();
            if (Files.exists(file))
                try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    p.load(in);
                }
            else {
                p.setProperty(
                        "member.id",
                        "local_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
                p.setProperty("member.secret", UUID.randomUUID().toString());
                try (Writer out =
                        Files.newBufferedWriter(
                                file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
                    p.store(out, "Private local profile. Do not upload.");
                }
            }
            String id = p.getProperty("member.id", ""), secret = p.getProperty("member.secret", "");
            if (!id.matches("local_[a-f0-9]{20}") || secret.length() < 32)
                throw new AppException(
                        "개인 보관함 설정이 손상되었습니다. 기존 local-profile.properties 파일을 확인해 주세요.");
            AuthService auth = new AuthService();
            if (auth.available(id))
                auth.register(
                        id,
                        secret.toCharArray(),
                        secret.toCharArray(),
                        "나의 보관함",
                        id + "@local.invalid");
            return auth.login(id, secret.toCharArray());
        }
    }
}
