package bm.b0b0b0.soulCrates.util;

import java.util.Locale;

public final class KeyCountLabels {

    private KeyCountLabels() {
    }

    public static String word(String localeId, int count) {
        if (localeId != null && localeId.toLowerCase(Locale.ROOT).startsWith("ru")) {
            return russian(count);
        }
        return count == 1 ? "key" : "keys";
    }

    private static String russian(int count) {
        int absolute = Math.abs(count);
        int mod10 = absolute % 10;
        int mod100 = absolute % 100;
        if (mod10 == 1 && mod100 != 11) {
            return "ключ";
        }
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return "ключа";
        }
        return "ключей";
    }
}
