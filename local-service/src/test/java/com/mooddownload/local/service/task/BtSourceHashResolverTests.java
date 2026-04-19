package com.mooddownload.local.service.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * BT 来源哈希解析测试。
 */
class BtSourceHashResolverTests {

    private final BtSourceHashResolver btSourceHashResolver = new BtSourceHashResolver();

    @Test
    void shouldResolveMagnetBtihAsUppercaseHash() {
        assertThat(btSourceHashResolver.resolve("MAGNET",
            "magnet:?xt=urn:btih:c8295ce630f2064f08440db1534e4992cfe4862a&dn=demo", null))
            .isEqualTo("C8295CE630F2064F08440DB1534E4992CFE4862A");
    }

    @Test
    void shouldResolveTorrentInfoHash() throws Exception {
        Path torrentFile = Files.createTempFile("bt-hash-resolver", ".torrent");
        Files.write(
            torrentFile,
            ("d4:infod6:lengthi12345e4:name8:test.txt12:piece lengthi16384e"
                + "6:pieces20:12345678901234567890ee")
                .getBytes(StandardCharsets.UTF_8)
        );

        assertThat(btSourceHashResolver.resolve("TORRENT", null, torrentFile.toString()))
            .isEqualTo("0EE1C40FCD46E1A3EAA4352B7D7D0EA9FEEC6895");
    }
}
