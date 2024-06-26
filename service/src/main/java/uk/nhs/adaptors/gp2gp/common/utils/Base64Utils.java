package uk.nhs.adaptors.gp2gp.common.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;

public class Base64Utils {

    public static String toBase64String(String nonBase64FileContent) {
        return toBase64String(nonBase64FileContent.getBytes(UTF_8));
    }

    public static String toBase64String(byte[] nonBase64FileContent) {
        return new String(Base64.getEncoder().encode(nonBase64FileContent), UTF_8);
    }
}
