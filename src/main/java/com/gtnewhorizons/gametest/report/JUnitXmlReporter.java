package com.gtnewhorizons.gametest.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.gtnewhorizons.gametest.core.GameTestInstance;
import com.gtnewhorizons.gametest.core.GameTestStatus;

/**
 * Writes a JUnit-compatible XML report ({@code TEST-gametest.xml}) consumable by CI tools
 * (Jenkins, GitHub Actions, etc.).
 *
 * <p>Format:
 *
 * <pre>{@code
 * <testsuite name="gametest" tests="N" failures="F" errors="E" time="T">
 *   <testcase name="methodName" classname="batch" time="T">
 *     <failure message="..." type="GameTestAssertException">stacktrace</failure>
 *   </testcase>
 * </testsuite>
 * }</pre>
 *
 * <p>Tick counts are converted to seconds using a 20 ticks-per-second constant.
 */
public final class JUnitXmlReporter {

    private static final double TICKS_PER_SECOND = 20.0;

    private JUnitXmlReporter() {}

    /**
     * Write the JUnit XML report for {@code instances} to {@code outputFile}. Parent directories are
     * created automatically if they do not exist.
     *
     * @throws IOException if the file cannot be written
     */
    public static void write(List<GameTestInstance> instances, File outputFile) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null) parent.mkdirs();

        long failures = 0, errors = 0;
        for (GameTestInstance inst : instances) {
            GameTestStatus s = inst.getStatus();
            if (s == GameTestStatus.FAILED) failures++;
            else if (s == GameTestStatus.TIMED_OUT) errors++;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile))) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.printf(
                "<testsuite name=\"gametest\" tests=\"%d\" failures=\"%d\" errors=\"%d\" time=\"%.3f\">%n",
                instances.size(),
                failures,
                errors,
                suiteDuration(instances));

            for (GameTestInstance inst : instances) {
                writeTestCase(pw, inst);
            }

            pw.println("</testsuite>");
        }
    }

    private static void writeTestCase(PrintWriter pw, GameTestInstance inst) {
        String testId = inst.getDefinition()
            .getTestId();
        // testId format is "namespace.ClassName#methodName" or "batch:id" — use as-is for name,
        // derive classname from everything before the last dot/hash.
        int sep = Math.max(testId.lastIndexOf('.'), testId.lastIndexOf('#'));
        String classname = sep > 0 ? testId.substring(0, sep) : "gametest";
        String name = sep > 0 ? testId.substring(sep + 1) : testId;
        double time = inst.getTickCount() / TICKS_PER_SECOND;

        GameTestStatus status = inst.getStatus();

        if (status == GameTestStatus.PASSED) {
            pw.printf(
                "  <testcase name=\"%s\" classname=\"%s\" time=\"%.3f\"/>%n",
                escape(name),
                escape(classname),
                time);
            return;
        }

        pw.printf(
            "  <testcase name=\"%s\" classname=\"%s\" time=\"%.3f\">%n",
            escape(name),
            escape(classname),
            time);

        if (status == GameTestStatus.FAILED) {
            Throwable cause = inst.getFailureCause();
            String message = cause != null ? cause.getMessage() : "Test failed";
            String type = cause != null ? cause.getClass()
                .getName() : "java.lang.AssertionError";
            pw.printf("    <failure message=\"%s\" type=\"%s\">%n", escape(message), escape(type));
            if (cause != null) {
                pw.print(escapeBody(stackTrace(cause)));
            }
            pw.println("    </failure>");
        } else if (status == GameTestStatus.TIMED_OUT) {
            pw.printf(
                "    <error message=\"Timed out after %d ticks\" type=\"GameTestTimeoutError\"/>%n",
                inst.getTickCount());
        } else {
            pw.printf(
                "    <error message=\"Test did not complete (status: %s)\" type=\"GameTestError\"/>%n",
                status);
        }

        pw.println("  </testcase>");
    }

    /** Total suite wall-clock time: the highest individual tick count converted to seconds. */
    private static double suiteDuration(List<GameTestInstance> instances) {
        int max = 0;
        for (GameTestInstance inst : instances) {
            if (inst.getTickCount() > max) max = inst.getTickCount();
        }
        return max / TICKS_PER_SECOND;
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /** Escape XML attribute values (double-quoted). */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    /** Escape XML text content (CDATA-style body, no quote escaping needed). */
    private static String escapeBody(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
