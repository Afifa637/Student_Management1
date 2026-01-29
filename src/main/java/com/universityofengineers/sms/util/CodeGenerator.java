package com.universityofengineers.sms.util;

import java.security.SecureRandom;
import java.time.Year;

public final class CodeGenerator {
    private CodeGenerator() {}

    private static final SecureRandom RND = new SecureRandom();
    private static final String DIGITS = "0123456789";

    public static String newStudentNo() {
        // Example: UE-2026-123456
        return "UE-" + Year.now().getValue() + "-" + randomDigits(6);
    }

    public static String newEmployeeNo() {
        // Example: UE-T-123456
        return "UE-T-" + randomDigits(6);
    }

    private static String randomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(DIGITS.charAt(RND.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }
}
