package bm.b0b0b0.soulCrates.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

final class CrateYamlPresenter {

    private static final Pattern LIST_ID_COMMENT_BREAK = Pattern.compile(
            "(?m)^  -     #[^\r\n]*\r?\nid: \""
    );
    private static final Pattern EMPTY_COMMANDS = Pattern.compile("(?m)^    commands: \\[\\]\\r?\\n");
    private static final Pattern EMPTY_ALT_COMMANDS = Pattern.compile("(?m)^      commands: \\[\\]\\r?\\n");
    private static final Pattern EMPTY_GRANTS = Pattern.compile("(?m)^    grants: \\[\\]\\r?\\n");
    private static final Pattern ROOT_COMMANDS_BLOCK = Pattern.compile(
            "(?m)^commands:\\r?\\n((?:  - \"[^\"]*\"\\r?\\n)+)"
    );

    private CrateYamlPresenter() {
    }

    static void polish(Path crateFile) {
        if (crateFile == null || !Files.isRegularFile(crateFile)) {
            return;
        }
        try {
            String content = Files.readString(crateFile, StandardCharsets.UTF_8);
            content = LIST_ID_COMMENT_BREAK.matcher(content).replaceAll("  - id: \"");
            content = ROOT_COMMANDS_BLOCK.matcher(content).replaceAll("    commands:\n$1");
            content = EMPTY_COMMANDS.matcher(content).replaceAll("");
            content = EMPTY_ALT_COMMANDS.matcher(content).replaceAll("");
            content = EMPTY_GRANTS.matcher(content).replaceAll("");
            Files.writeString(crateFile, content, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
