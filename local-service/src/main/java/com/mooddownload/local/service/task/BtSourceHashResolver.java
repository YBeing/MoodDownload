package com.mooddownload.local.service.task;

import com.mooddownload.local.common.enums.ErrorCode;
import com.mooddownload.local.common.exception.BizException;
import com.mooddownload.local.service.task.state.TaskSourceType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 解析 BT 来源的唯一哈希，供重复任务复用。
 */
@Component
public class BtSourceHashResolver {

    private static final String XT_PREFIX = "urn:btih:";

    /**
     * 按任务来源解析 BT 哈希。
     *
     * @param sourceType 来源类型
     * @param sourceUri 来源地址
     * @param torrentFilePath 种子文件路径
     * @return 统一大写的 BT 哈希，非 BT 任务返回 null
     */
    public String resolve(String sourceType, String sourceUri, String torrentFilePath) {
        TaskSourceType taskSourceType = TaskSourceType.fromCode(sourceType);
        if (taskSourceType == null) {
            return null;
        }
        switch (taskSourceType) {
            case MAGNET:
                return resolveMagnetHash(sourceUri);
            case TORRENT:
                return resolveTorrentHash(torrentFilePath);
            default:
                return null;
        }
    }

    /**
     * 从 magnet 链接中解析 btih。
     *
     * @param magnetUri magnet 地址
     * @return 大写哈希
     */
    public String resolveMagnetHash(String magnetUri) {
        if (!StringUtils.hasText(magnetUri)) {
            return null;
        }
        String normalizedUri = magnetUri.trim();
        int queryIndex = normalizedUri.indexOf('?');
        if (queryIndex < 0 || queryIndex >= normalizedUri.length() - 1) {
            return null;
        }
        String query = normalizedUri.substring(queryIndex + 1);
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator >= 0 ? pair.substring(0, separator) : pair;
            String value = separator >= 0 ? pair.substring(separator + 1) : "";
            if (!"xt".equalsIgnoreCase(key) || !StringUtils.hasText(value)) {
                continue;
            }
            String normalizedValue = value.trim();
            if (normalizedValue.regionMatches(true, 0, XT_PREFIX, 0, XT_PREFIX.length())) {
                return normalizeHash(normalizedValue.substring(XT_PREFIX.length()));
            }
        }
        return null;
    }

    /**
     * 从 .torrent 文件中计算 infohash。
     *
     * @param torrentFilePath 种子文件路径
     * @return 大写哈希
     */
    public String resolveTorrentHash(String torrentFilePath) {
        if (!StringUtils.hasText(torrentFilePath)) {
            return null;
        }
        byte[] torrentContent;
        try {
            torrentContent = Files.readAllBytes(Paths.get(torrentFilePath.trim()));
        } catch (IOException exception) {
            throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "读取种子文件失败");
        }
        byte[] infoBytes = extractInfoBytes(torrentContent);
        return sha1Hex(infoBytes);
    }

    private String normalizeHash(String rawHash) {
        String normalizedHash = rawHash == null ? null : rawHash.trim();
        if (!StringUtils.hasText(normalizedHash)) {
            return null;
        }
        if (normalizedHash.length() == 40) {
            for (int index = 0; index < normalizedHash.length(); index++) {
                if (!isHexChar(normalizedHash.charAt(index))) {
                    throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "无效的 BT 哈希");
                }
            }
            return normalizedHash.toUpperCase(Locale.ROOT);
        }
        if (normalizedHash.length() == 32) {
            return sha1BytesToHex(decodeBase32(normalizedHash));
        }
        throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "无效的 BT 哈希");
    }

    private byte[] extractInfoBytes(byte[] torrentContent) {
        if (torrentContent == null || torrentContent.length == 0) {
            throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "种子文件内容为空");
        }
        Cursor cursor = new Cursor();
        if (torrentContent[cursor.position] != 'd') {
            throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "无效的种子文件");
        }
        cursor.position++;
        while (cursor.position < torrentContent.length && torrentContent[cursor.position] != 'e') {
            String key = readString(torrentContent, cursor);
            if ("info".equals(key)) {
                int start = cursor.position;
                skipValue(torrentContent, cursor);
                int end = cursor.position;
                byte[] infoBytes = new byte[end - start];
                System.arraycopy(torrentContent, start, infoBytes, 0, infoBytes.length);
                return infoBytes;
            }
            skipValue(torrentContent, cursor);
        }
        throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "种子文件缺少 info 字段");
    }

    private void skipValue(byte[] content, Cursor cursor) {
        ensureAvailable(content, cursor);
        byte current = content[cursor.position];
        if (current == 'i') {
            cursor.position++;
            while (cursor.position < content.length && content[cursor.position] != 'e') {
                cursor.position++;
            }
            if (cursor.position >= content.length) {
                throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "种子文件整数编码非法");
            }
            cursor.position++;
            return;
        }
        if (current == 'l' || current == 'd') {
            cursor.position++;
            while (cursor.position < content.length && content[cursor.position] != 'e') {
                if (current == 'd') {
                    readString(content, cursor);
                }
                skipValue(content, cursor);
            }
            if (cursor.position >= content.length) {
                throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "种子文件集合编码非法");
            }
            cursor.position++;
            return;
        }
        if (current >= '0' && current <= '9') {
            readBytes(content, cursor);
            return;
        }
        throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "种子文件编码非法");
    }

    private String readString(byte[] content, Cursor cursor) {
        return new String(readBytes(content, cursor), StandardCharsets.UTF_8);
    }

    private byte[] readBytes(byte[] content, Cursor cursor) {
        int length = readLength(content, cursor);
        ensureRemaining(content, cursor, length);
        byte[] value = new byte[length];
        System.arraycopy(content, cursor.position, value, 0, length);
        cursor.position += length;
        return value;
    }

    private int readLength(byte[] content, Cursor cursor) {
        ensureAvailable(content, cursor);
        int length = 0;
        while (cursor.position < content.length && content[cursor.position] != ':') {
            byte current = content[cursor.position];
            if (current < '0' || current > '9') {
                throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "种子文件字符串长度非法");
            }
            length = (length * 10) + (current - '0');
            cursor.position++;
        }
        if (cursor.position >= content.length || content[cursor.position] != ':') {
            throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "种子文件字符串缺少分隔符");
        }
        cursor.position++;
        return length;
    }

    private void ensureAvailable(byte[] content, Cursor cursor) {
        if (cursor.position >= content.length) {
            throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "种子文件已截断");
        }
    }

    private void ensureRemaining(byte[] content, Cursor cursor, int expected) {
        if (expected < 0 || cursor.position + expected > content.length) {
            throw new BizException(ErrorCode.TORRENT_PARSE_FAILED, "种子文件长度越界");
        }
    }

    private String sha1Hex(byte[] content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            return sha1BytesToHex(messageDigest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-1", exception);
        }
    }

    private String sha1BytesToHex(byte[] content) {
        StringBuilder builder = new StringBuilder(content.length * 2);
        for (byte current : content) {
            builder.append(String.format(Locale.ROOT, "%02X", current & 0xFF));
        }
        return builder.toString();
    }

    private byte[] decodeBase32(String value) {
        String normalized = value.trim().replace("=", "").toUpperCase(Locale.ROOT);
        ByteAccumulator accumulator = new ByteAccumulator();
        int bits = 0;
        int buffer = 0;
        for (int index = 0; index < normalized.length(); index++) {
            int digit = decodeBase32Digit(normalized.charAt(index));
            buffer = (buffer << 5) | digit;
            bits += 5;
            while (bits >= 8) {
                bits -= 8;
                accumulator.write((buffer >> bits) & 0xFF);
            }
        }
        return accumulator.toByteArray();
    }

    private int decodeBase32Digit(char value) {
        if (value >= 'A' && value <= 'Z') {
            return value - 'A';
        }
        if (value >= '2' && value <= '7') {
            return value - '2' + 26;
        }
        throw new BizException(ErrorCode.COMMON_PARAM_INVALID, "无效的 BT 哈希");
    }

    private boolean isHexChar(char value) {
        return (value >= '0' && value <= '9')
            || (value >= 'a' && value <= 'f')
            || (value >= 'A' && value <= 'F');
    }

    private static final class Cursor {
        private int position;
    }

    private static final class ByteAccumulator {

        private byte[] values = new byte[32];

        private int size;

        private void write(int value) {
            if (size >= values.length) {
                byte[] newValues = new byte[values.length * 2];
                System.arraycopy(values, 0, newValues, 0, values.length);
                values = newValues;
            }
            values[size++] = (byte) value;
        }

        private byte[] toByteArray() {
            byte[] result = new byte[size];
            System.arraycopy(values, 0, result, 0, size);
            return result;
        }
    }
}
