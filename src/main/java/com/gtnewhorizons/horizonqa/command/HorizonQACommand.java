package com.gtnewhorizons.horizonqa.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.gtnewhorizons.horizonqa.item.ItemRegistration;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
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
import com.gtnewhorizons.horizonqa.command.HorizonQACommandUtils.CellRecord;
import com.gtnewhorizons.horizonqa.internal.DiscoveryIssue;
import com.gtnewhorizons.horizonqa.internal.GameTestDefinition;
import com.gtnewhorizons.horizonqa.internal.GameTestRegistry;
import com.gtnewhorizons.horizonqa.internal.InteractiveTestSession;
import com.gtnewhorizons.horizonqa.internal.InvalidTestDefinition;
import com.gtnewhorizons.horizonqa.item.ItemHorizonWand;
import com.gtnewhorizons.horizonqa.structure.StructureExporter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HorizonQACommand extends CommandBase {

    private static final String[] SUBCOMMANDS = { "run", "runall", "runfailed", "runthis", "runthat", "pos", "clearall",
        "export", "clear" };

    @Override
    public String getName() {
        return "horizonqa";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/horizonqa <run|runall|runfailed|runthis|runthat|pos|clearall|export|clear>";
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
        Set<String> failedIds = InteractiveTestSession.get()
            .getFailedIds();
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

    private void handleRunThis(ICommandSender sender, String[] args) {
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
        int relX = pos.getX() - cell.originX();
        int relY = pos.getY() - cell.originY();
        int relZ = pos.getZ() - cell.originZ();
        String call = String.format("helper.absolute(%d, %d, %d)", relX, relY, relZ);

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
                    + String.format("(%d, %d, %d)", relX, relY, relZ)));

        TextComponentString clickable = new TextComponentString(
            TextFormatting.GREEN + call + TextFormatting.GRAY + "  « click to copy to chat");
        clickable
            .setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, call)));
        sender.sendMessage(clickable);
    }

    private void handleClearAll(ICommandSender sender, String[] args) {
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

        // TODO: BlockPos sort
        int minX = Math.min(pos1.getX(), pos2.getX()), minY = Math.min(pos1.getY(), pos2.getY()), minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()), maxY = Math.max(pos1.getY(), pos2.getY()), maxZ = Math.max(pos1.getZ(), pos2.getZ());

        WorldServer world = (WorldServer) player.world;
        File outputDir = server.getFile("horizonqastructures");

        try {
            StructureExporter.export(world, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), outputDir, name);
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
