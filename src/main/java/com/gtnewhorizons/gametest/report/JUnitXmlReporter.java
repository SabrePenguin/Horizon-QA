package com.gtnewhorizons.gametest.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.gtnewhorizons.gametest.core.GameTestInstance;
import com.gtnewhorizons.gametest.core.GameTestStatus;

public final class JUnitXmlReporter {

    private static final double TICKS_PER_SECOND = 20.0;

    private JUnitXmlReporter() {}

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

        pw.printf("  <testcase name=\"%s\" classname=\"%s\" time=\"%.3f\">%n", escape(name), escape(classname), time);

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
            pw.printf("    <error message=\"Test did not complete (status: %s)\" type=\"GameTestError\"/>%n", status);
        }

        pw.println("  </testcase>");
    }

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

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static String escapeBody(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
