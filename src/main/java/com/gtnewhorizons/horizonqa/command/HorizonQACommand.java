package com.gtnewhorizons.horizonqa.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.gtnewhorizons.horizonqa.item.ItemRegistration;
import com.gtnewhorizons.horizonqa.structure.StructurePlacer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.WorldServer;

import com.gtnewhorizons.horizonqa.HorizonQAMod;
import com.gtnewhorizons.horizonqa.HorizonQAProperties;
import com.gtnewhorizons.horizonqa.HorizonQAProperties.PropertyIssue;
import com.gtnewhorizons.horizonqa.command.HorizonQACommandUtils.CellRecord;
import com.gtnewhorizons.horizonqa.internal.DiscoveryIssue;
import com.gtnewhorizons.horizonqa.internal.GameTestBatchRunner;
import com.gtnewhorizons.horizonqa.internal.GameTestDefinition;
import com.gtnewhorizons.horizonqa.internal.GameTestRegistry;
import com.gtnewhorizons.horizonqa.internal.InteractiveTestSession;
import com.gtnewhorizons.horizonqa.internal.InvalidTestDefinition;
import com.gtnewhorizons.horizonqa.item.ItemHorizonWand;
import com.gtnewhorizons.horizonqa.report.CaseResult;
import com.gtnewhorizons.horizonqa.report.ConsoleReporter;
import com.gtnewhorizons.horizonqa.report.IssueResult;
import com.gtnewhorizons.horizonqa.report.ReportPathPreflight;
import com.gtnewhorizons.horizonqa.report.RunReportWriter;
import com.gtnewhorizons.horizonqa.report.RunResult;
import com.gtnewhorizons.horizonqa.structure.StructureExporter;
import net.minecraft.world.gen.structure.template.Template;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HorizonQACommand extends CommandBase {

    private static final String[] SUBCOMMANDS = { "run", "runall", "runfailed", "tp", "runthis", "runthat", "pos",
        "clearall", "export", "clear" };
    private static final Set<String> LAST_REPORTED_FAILED_IDS = new LinkedHashSet<>();
    private static volatile boolean reportBatchRunning;

    @Override
    public String getName() {
        return "horizonqa";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/horizonqa <run|runall|runfailed|tp|runthis|runthat|pos|clearall|export|clear>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("qa");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Usage: " + getUsage(sender)));
            return;
        }
        switch (args[0]) {
            case "run":
                handleRun(sender, args);
                break;
            case "runall":
                handleRunAll(sender, args);
                break;
            case "runfailed":
                handleRunFailed(sender, args);
                break;
            case "tp":
                handleTeleport(sender, args);
                break;
            case "runthis":
                handleRunThis(sender, args);
                break;
            case "runthat":
                handleRunThat(sender, args);
                break;
            case "pos":
                handlePos(sender, args);
                break;
            case "clearall":
                handleClearAll(sender, args);
                break;
            case "export":
                handleExport(server, sender, args);
                break;
            case "clear":
                handleClear(sender, args);
                break;
            default:
                sender.sendMessage(
                    new TextComponentString(
                        TextFormatting.RED + "Unknown subcommand '" + args[0] + "'. " + getUsage(sender)));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBCOMMANDS);
        }
        if (args.length == 2) {
            if ("run".equals(args[0])) {
                List<GameTestDefinition> runnable = GameTestRegistry.getAllTests();
                String[] ids = new String[runnable.size()];
                for (int i = 0; i < runnable.size(); i++) ids[i] = runnable.get(i)
                    .getTestId();
                return getListOfStringsMatchingLastWord(args, ids);
            }
            if ("runall".equals(args[0])) {
                Set<String> namespaces = new LinkedHashSet<>();
                for (GameTestDefinition def : GameTestRegistry.getAllTests()) {
                    String id = def.getTestId();
                    int colon = id.indexOf(':');
                    if (colon > 0) namespaces.add(id.substring(0, colon));
                }
                return getListOfStringsMatchingLastWord(args, namespaces.toArray(new String[0]));
            }
            if ("tp".equals(args[0])) {
                return getListOfStringsMatchingLastWord(args, knownCellIds());
            }
        }
        return Collections.emptyList();
    }

    private void handleRun(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /horizonqa run <testId>"));
            return;
        }
        String testId = args[1];
        GameTestDefinition def = findDefinition(testId);
        if (def == null) {
            InvalidTestDefinition invalidTest = findInvalidTest(testId);
            if (invalidTest != null) {
                reportInvalidTest(sender, invalidTest);
                return;
            }
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "Unknown test: '"
                        + TextFormatting.YELLOW
                        + testId
                        + TextFormatting.RED
                        + "'. Use /horizonqa runall to list available tests."));
            return;
        }
        if (HorizonQAProperties.usesReportedCommandBatches()) {
            startReportedBatch(
                sender,
                Collections.singletonList(def),
                TextFormatting.GREEN + "Launched report batch: " + TextFormatting.YELLOW + def.getTestId());
            return;
        }
        if (rejectBatchRunning(sender)) return;
        int launched = InteractiveTestSession.get()
            .launchTest(def);
        if (launched > 0) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.GREEN + "Launched: " + TextFormatting.YELLOW + def.getTestId()));
        } else {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "Could not launch '"
                        + TextFormatting.YELLOW
                        + def.getTestId()
                        + TextFormatting.RED
                        + "'. Check the server log for details."));
        }
    }

    private void handleRunAll(ICommandSender sender, String[] args) {
        List<GameTestDefinition> tests;
        if (args.length >= 2) {
            String ns = args[1];
            tests = GameTestRegistry.getTestsForNamespace(ns);
            if (tests.isEmpty()) {
                sender.sendMessage(
                    new TextComponentString(
                        TextFormatting.RED + "No tests found for namespace '"
                            + TextFormatting.YELLOW
                            + ns
                            + TextFormatting.RED
                            + "'."));
                return;
            }
        } else {
            tests = GameTestRegistry.getAllTests();
            if (tests.isEmpty()) {
                sender.sendMessage(
                    new TextComponentString(
                        TextFormatting.YELLOW + "No tests discovered. Make sure your mod is loaded "
                            + "and annotated with @GameTestHolder."));
                return;
            }
        }
        if (HorizonQAProperties.usesReportedCommandBatches()) {
            startReportedBatch(
                sender,
                tests,
                TextFormatting.GREEN + "Launched report batch with "
                    + TextFormatting.YELLOW
                    + tests.size()
                    + TextFormatting.GREEN
                    + " test(s).");
            return;
        }
        if (rejectBatchRunning(sender)) return;
        int launched = InteractiveTestSession.get()
            .launchTests(tests);
        if (launched > 0) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.GREEN + "Launched "
                        + TextFormatting.YELLOW
                        + launched
                        + TextFormatting.GREEN
                        + " test(s)."));
        } else {
            sender. sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "Could not launch tests. The full test area could not be loaded."));
        }
    }

    private void handleRunFailed(ICommandSender sender, String[] args) {
        if (HorizonQAProperties.usesReportedCommandBatches() && reportBatchRunning) {
            reportBatchAlreadyRunning(sender);
            return;
        }
        Set<String> failedIds = failedIdsForCurrentMode();
        if (failedIds.isEmpty()) {
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "No failed tests to re-run."));
            return;
        }
        List<GameTestDefinition> defs = new ArrayList<>();
        for (String id : failedIds) {
            GameTestDefinition def = findDefinition(id);
            if (def != null) defs.add(def);
        }
        defs.sort(Comparator.comparing(GameTestDefinition::getTestId));
        if (defs.isEmpty()) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.YELLOW + "Could not find definitions for the failed tests — "
                        + "were they unloaded?"));
            return;
        }
        if (HorizonQAProperties.usesReportedCommandBatches()) {
            startReportedBatch(
                sender,
                defs,
                TextFormatting.GREEN + "Launched report batch with "
                    + TextFormatting.YELLOW
                    + defs.size()
                    + TextFormatting.GREEN
                    + " failed test(s).");
            return;
        }
        if (rejectBatchRunning(sender)) return;
        int launched = InteractiveTestSession.get()
            .launchTests(defs);
        if (launched > 0) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.GREEN + "Re-running "
                        + TextFormatting.YELLOW
                        + defs.size()
                        + TextFormatting.GREEN
                        + " failed test(s)."));
        } else {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "Could not re-run failed tests. The full test area could not be loaded."));
        }
    }

    private void handleTeleport(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /horizonqa tp <testId>"));
            return;
        }

        EntityPlayer player = requirePlayer(sender);
        if (player == null) return;
        if (!(player instanceof EntityPlayerMP serverPlayer)) {
            sender.sendMessage(
                new TextComponentString(TextFormatting.RED + "This command must be run by a server-side player."));
            return;
        }

        List<CellRecord> cells = new ArrayList<>(
            InteractiveTestSession.get()
                .getKnownCells());
        if (cells.isEmpty()) {
            sender.sendMessage(
                new TextComponentString(TextFormatting.RED + "No test cells found. Run /horizonqa runall first."));
            return;
        }

        String testId = args[1];
        CellRecord cell = HorizonQACommandUtils.findTestById(testId, cells);
        if (cell == null) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "No placed test cell for '"
                        + TextFormatting.YELLOW
                        + testId
                        + TextFormatting.RED
                        + "'. Use tab completion after /horizonqa runall."));
            return;
        }

        if (player.world == null || player.world.provider == null || player.world.provider.getDimension() != 0) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "Test cells are in the overworld. Go to dimension 0 first."));
            return;
        }

        BlockPos min = cell.minPos();
        BlockPos max = cell.maxPos();
        double targetX = (min.getX() + max.getX() + 1.0) * 0.5;
        double targetY = max.getY() + 2.0;
        double targetZ = (min.getZ() + max.getZ() + 1.0) * 0.5;

        if (serverPlayer.getRidingEntity() != null) {
            serverPlayer.dismountRidingEntity();
        }
        serverPlayer.connection
            .setPlayerLocation(targetX, targetY, targetZ, player.rotationYaw, player.rotationPitch);

        sender.sendMessage(
            new TextComponentString(
                TextFormatting.GREEN + "Teleported to: " + TextFormatting.YELLOW + cell.testId()));
        sender.sendMessage(
            new TextComponentString(
                TextFormatting.GRAY + String.format("Cell target: (%.1f, %.1f, %.1f)", targetX, targetY, targetZ)));

    }

    private static Set<String> failedIdsForCurrentMode() {
        if (!HorizonQAProperties.usesReportedCommandBatches()) {
            return InteractiveTestSession.get()
                .getFailedIds();
        }
        return new LinkedHashSet<>(LAST_REPORTED_FAILED_IDS);
    }

    private static void startReportedBatch(ICommandSender sender, List<GameTestDefinition> tests,
        String launchedMessage) {
        if (reportBatchRunning || GameTestBatchRunner.isBatchRunning()) {
            reportBatchAlreadyRunning(sender);
            return;
        }
        if (!preflightReportOutputs(sender)) {
            return;
        }
        try {
            GameTestBatchRunner batchRunner = new GameTestBatchRunner(
                tests,
                GameTestRegistry.getBeforeBatchMethods(),
                GameTestRegistry.getAfterBatchMethods(),
                Collections.emptyList(),
                result -> {
                    try {
                        rememberReportedBatchResult(result);
                    } finally {
                        reportBatchRunning = false;
                    }
                });
            reportBatchRunning = true;
            batchRunner.start();
        } catch (IllegalStateException e) {
            reportBatchRunning = false;
            reportBatchAlreadyRunning(sender);
            return;
        } catch (RuntimeException | Error e) {
            reportBatchRunning = false;
            throw e;
        }
        sender.sendMessage(new TextComponentString(launchedMessage));
    }

    private static boolean preflightReportOutputs(ICommandSender sender) {
        List<PropertyIssue> propertyIssues = HorizonQAProperties.reportInfrastructureIssues();
        if (!propertyIssues.isEmpty()) {
            logPropertyIssues(propertyIssues);
            File reportFile = HorizonQAProperties.junitReportFile();
            RunResult result = RunResult
                .preRun(HorizonQAProperties.modeName(), toPropertyIssueResults(propertyIssues), reportFile.getPath());
            RunReportWriter.write(result, HorizonQAMod.LOG);
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "Reported-batch configuration is invalid; tests were not launched. "
                        + "Check the report files and server log for details."));
            return false;
        }

        List<IssueResult> reportPathIssues = ReportPathPreflight
            .check(HorizonQAProperties.junitReportFile(), HorizonQAProperties.statusReportFile());
        if (reportPathIssues.isEmpty()) {
            return true;
        }

        HorizonQAMod.LOG.error("Report path preflight failed; reported batch was not launched.");
        for (IssueResult issue : reportPathIssues) {
            HorizonQAMod.LOG.error("Infrastructure issue [{}] {}: {}", issue.id(), issue.name(), issue.message());
        }
        File reportFile = HorizonQAProperties.junitReportFile();
        RunResult result = RunResult.preRun(HorizonQAProperties.modeName(), reportPathIssues, reportFile.getPath());
        ConsoleReporter.report(result);
        sender.sendMessage(
            new TextComponentString(
                TextFormatting.RED + "Report path preflight failed; tests were not launched. "
                    + "Check the server log for details."));
        return false;
    }

    public static void rememberReportedBatchResult(RunResult result) {
        LAST_REPORTED_FAILED_IDS.clear();
        if (result == null) {
            return;
        }
        for (CaseResult resultCase : result.cases()) {
            if (resultCase.failed() || resultCase.timedOut() || resultCase.error()) {
                LAST_REPORTED_FAILED_IDS.add(resultCase.id());
            }
        }
    }

    public static void resetReportBatchState() {
        reportBatchRunning = false;
        LAST_REPORTED_FAILED_IDS.clear();
    }

    private static void reportBatchAlreadyRunning(ICommandSender sender) {
        sender.sendMessage(
            new TextComponentString(
                TextFormatting.RED + "A GameTest batch is already running. Wait for it to finish first."));
    }

    private static boolean rejectBatchRunning(ICommandSender sender) {
        if (!GameTestBatchRunner.isBatchRunning()) {
            return false;
        }
        reportBatchAlreadyRunning(sender);
        return true;
    }

    private static boolean rejectInteractiveOnlyInNonInteractiveMode(ICommandSender sender, String replacement) {
        if (HorizonQAProperties.interactiveFeaturesEnabled()) {
            return false;
        }
        sender.sendMessage(
            new TextComponentString(
                TextFormatting.RED + "That command is only available in interactive mode. "
                    + "Use "
                    + replacement
                    + " for reported batches."));
        return true;
    }

    private static void logPropertyIssues(List<PropertyIssue> issues) {
        for (PropertyIssue issue : issues) {
            HorizonQAMod.LOG.error(
                "Infrastructure issue [{}] {} in {}: {}",
                issue.id(),
                issue.kind(),
                issue.property(),
                issue.message());
        }
    }

    private static List<IssueResult> toPropertyIssueResults(List<PropertyIssue> issues) {
        List<IssueResult> results = new ArrayList<>();
        for (PropertyIssue issue : issues) {
            results.add(IssueResult.property(issue));
        }
        return results;
    }

    private void handleRunThis(ICommandSender sender, String[] args) {
        if (rejectInteractiveOnlyInNonInteractiveMode(sender, "/horizonqa run <testId>")) return;
        if (rejectBatchRunning(sender)) return;
        EntityPlayer player;
        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (PlayerNotFoundException e) {
            return;
        }
        BlockPos playerPos = player.getPosition();

        CellRecord cell = HorizonQACommandUtils.findTestContaining(
            playerPos,
            InteractiveTestSession.get()
                .getKnownCells());
        if (cell == null) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "You are not inside any known test cell. "
                        + "Run /horizonqa runall first to place cells."));
            return;
        }
        relaunchCell(sender, cell);
    }

    private void handleRunThat(ICommandSender sender, String[] args) {
        if (rejectInteractiveOnlyInNonInteractiveMode(sender, "/horizonqa run <testId>")) return;
        if (rejectBatchRunning(sender)) return;
        EntityPlayer player;
        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (PlayerNotFoundException e) {
            return;
        }

        CellRecord cell = HorizonQACommandUtils.findTestAlongLook(
            player,
            InteractiveTestSession.get()
                .getKnownCells());
        if (cell == null) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "No test cell in your line of sight (within 64 blocks). "
                        + "Run /horizonqa runall first."));
            return;
        }
        relaunchCell(sender, cell);
    }

    private static void relaunchCell(ICommandSender sender, CellRecord cell) {
        GameTestDefinition def = findDefinition(cell.testId());
        if (def == null) {
            sender.sendMessage(
                new TextComponentString(TextFormatting.RED + "Definition not found for '" + cell.testId() + "'."));
            return;
        }
        boolean launched = InteractiveTestSession.get()
            .relaunchAtCell(def);
        if (launched) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.GREEN + "Re-running: " + TextFormatting.YELLOW + def.getTestId()));
        } else {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "Could not re-run '"
                        + TextFormatting.YELLOW
                        + def.getTestId()
                        + TextFormatting.RED
                        + "'. Check the server log for details."));
        }
    }

    private void handlePos(ICommandSender sender, String[] args) {
        if (rejectInteractiveOnlyInNonInteractiveMode(sender, "/horizonqa run <testId>")) return;
        EntityPlayer player;
        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (PlayerNotFoundException e) {
            return;
        }
        BlockPos pos = player.getPosition();

        CellRecord cell = HorizonQACommandUtils.findTestContaining(
            pos,
            InteractiveTestSession.get()
                .getKnownCells());
        if (cell == null) {
            cell = HorizonQACommandUtils.findNearestTest(
                pos,
                InteractiveTestSession.get()
                    .getKnownCells());
        }
        if (cell == null) {
            sender.sendMessage(
                new TextComponentString(TextFormatting.RED + "No test cells found. Run /horizonqa runall first."));
            return;
        }
        BlockPos relative = new BlockPos(
            pos.getX() - cell.origin().getX(),
            pos.getY() - cell.origin().getY(),
            pos.getZ() - cell.origin().getZ()
        );
        String call = String.format("helper.absolute(%d, %d, %d)", relative.getX(), relative.getY(), relative.getZ());

        sender.sendMessage(
            new TextComponentString(TextFormatting.AQUA + "Test: " + TextFormatting.YELLOW + cell.testId()));
        sender.sendMessage(
            new TextComponentString(
                TextFormatting.AQUA + "World:    "
                    + TextFormatting.WHITE
                    + String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())));
        sender.sendMessage(
            new TextComponentString(
                TextFormatting.AQUA + "Relative: "
                    + TextFormatting.WHITE
                    + String.format("(%d, %d, %d)", relative.getX(), relative.getY(), relative.getZ())));

        TextComponentString clickable = new TextComponentString(
            TextFormatting.GREEN + call + TextFormatting.GRAY + "  « click to copy to chat");
        clickable
            .setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, call)));
        sender.sendMessage(clickable);
    }

    private void handleClearAll(ICommandSender sender, String[] args) {
        if (rejectInteractiveOnlyInNonInteractiveMode(sender, "/horizonqa runall")) return;
        if (rejectBatchRunning(sender)) return;
        int count = InteractiveTestSession.get()
            .getKnownCells()
            .size();
        InteractiveTestSession.get()
            .clearAll();
        sender.sendMessage(
            new TextComponentString(
                TextFormatting.GREEN + "Cleared "
                    + TextFormatting.YELLOW
                    + count
                    + TextFormatting.GREEN
                    + " test cell(s)."));
    }

    private void handleExport(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /horizonqa export <name>"));
            return;
        }

        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(
                new TextComponentString(TextFormatting.RED + "This command must be run by a player."));
            return;
        }
        EntityPlayer player;
        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (PlayerNotFoundException e) {
            return;
        }
        ItemStack wand = findWand(player);

        if (wand.isEmpty()) {
            sender.sendMessage(
                new TextComponentString(TextFormatting.RED + "Hold (or have in inventory) a GameTest Wand first."));
            return;
        }

        NBTTagCompound nbt = wand.getTagCompound();
        if (nbt == null || !nbt.getBoolean(ItemHorizonWand.TAG_POS1_SET)
            || !nbt.getBoolean(ItemHorizonWand.TAG_POS2_SET)) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED
                        + "Wand selection incomplete — left-click Pos1 and right-click Pos2 first."));
            return;
        }

        String name = args[1];
        if (!name.matches("[A-Za-z0-9_-]+")) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "Name must contain only letters, digits, underscores, and hyphens."));
            return;
        }

        BlockPos pos1 = BlockPos.fromLong(nbt.getLong(ItemHorizonWand.TAG_POS1));
        BlockPos pos2 = BlockPos.fromLong(nbt.getLong(ItemHorizonWand.TAG_POS2));

        WorldServer world = (WorldServer) player.world;
        File outputDir = server.getFile("horizonqastructures");

        try {
            StructureExporter.export(world, pos1, pos2, outputDir, name);
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.GREEN + "Exported '"
                        + TextFormatting.YELLOW
                        + name
                        + TextFormatting.GREEN
                        + "' → "
                        + TextFormatting.WHITE
                        + outputDir.getAbsolutePath()));
            sender.sendMessage(
                new TextComponentString(TextFormatting.GRAY + "  " + name + ".json        (block layout)"));
            sender.sendMessage(
                new TextComponentString(TextFormatting.GRAY + "  " + name + "_tiles.nbt   (tile entities)"));
        } catch (IOException e) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Export failed: " + e.getMessage()));
            HorizonQAMod.LOG.error("StructureExporter failed for '{}'", name, e);
        }
    }

    private static void reportInvalidTest(ICommandSender sender, InvalidTestDefinition invalidTest) {
        sender.sendMessage(
            new TextComponentString(
                TextFormatting.RED + "Invalid test: '"
                    + TextFormatting.YELLOW
                    + invalidTest.intendedTestId()
                    + TextFormatting.RED
                    + "' was excluded during discovery."));

        List<DiscoveryIssue> issues = invalidTest.issues();
        if (!issues.isEmpty()) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + "Reason: "
                        + issues.get(0)
                            .message()));
            if (issues.size() > 1) {
                sender.sendMessage(
                    new TextComponentString(
                        TextFormatting.GRAY + "Also has "
                            + (issues.size() - 1)
                            + " other discovery issue(s). Check the server log for details."));
            }
        }
    }

    private void handleClear(ICommandSender sender, String[] args) {
        EntityPlayer player;
        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (PlayerNotFoundException e) {
            return;
        }
        ItemStack wand = findWand(player);
        if (wand.isEmpty()) {
            sender.sendMessage(
                new TextComponentString(
                    TextFormatting.RED + I18n.format("horizonqa.command.clear.no_wand")));
            return;
        }

        wand.setTagCompound(null);

        sender.sendMessage(
            new TextComponentString(
                TextFormatting.GREEN + I18n.format("horizonqa.command.clear.success")));
    }

    private static @Nullable GameTestDefinition findDefinition(String testId) {
        for (GameTestDefinition def : GameTestRegistry.getAllTests()) {
            if (def.getTestId()
                .equals(testId)) return def;
        }
        return null;
    }

    private static InvalidTestDefinition findInvalidTest(String testId) {
        for (InvalidTestDefinition invalidTest : GameTestRegistry.getInvalidTests()) {
            if (invalidTest.intendedTestId()
                .equals(testId)) return invalidTest;
        }
        return null;
    }

    private static String[] knownCellIds() {
        List<String> ids = new ArrayList<>();
        for (CellRecord cell : InteractiveTestSession.get()
            .getKnownCells()) {
            ids.add(cell.testId());
        }
        ids.sort(String::compareTo);
        return ids.toArray(new String[0]);
    }

    private static EntityPlayer requirePlayer(ICommandSender sender) {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(
                new TextComponentString(TextFormatting.RED + "This command must be run by a player."));
            return null;
        }
        return (EntityPlayer) sender;
    }

    @SuppressWarnings("ConstantConditions")
    private static ItemStack findWand(EntityPlayer player) {
        ItemStack held = player.getHeldItem(EnumHand.MAIN_HAND);
        if (!held.isEmpty() && held.getItem() == ItemRegistration.wand) {
            return held;
        }
        held = player.getHeldItem(EnumHand.OFF_HAND);
        if (!held.isEmpty() && held.getItem() == ItemRegistration.wand) {
            return held;
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == ItemRegistration.wand) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
