package com.gtnewhorizons.horizonqa.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.FMLCommonHandler;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.internal.DiscoveryIssue;
import com.gtnewhorizons.horizonqa.internal.DiscoveryResult;
import com.gtnewhorizons.horizonqa.internal.DuplicateTestId;
import com.gtnewhorizons.horizonqa.internal.GameTestDefinition;
import com.gtnewhorizons.horizonqa.internal.GameTestRegistry;
import com.gtnewhorizons.horizonqa.internal.InvalidBatchHook;
import com.gtnewhorizons.horizonqa.internal.InvalidTestDefinition;

public class HorizonQACommandTest {

    @After
    public void clearRegistry() throws Exception {
        seedRegistry(Collections.emptyList(), Collections.emptyList());
    }

    @Test
    public void tabCompletionListsOnlyRunnableTests() throws Exception {
        seedRegistry(
            Collections.singletonList(definition("good:Suite.valid")),
            Collections.singletonList(invalid("bad:Broken.invalid")));

        HorizonQACommand command = new HorizonQACommand();

        List<String> runCompletions = command
            .getTabCompletions(FMLCommonHandler.instance().getMinecraftServerInstance(), new RecordingSender(), new String[] { "run", "" }, null);
        assertTrue(runCompletions.contains("good:Suite.valid"));
        assertFalse(runCompletions.contains("bad:Broken.invalid"));

        List<String> runAllCompletions = command
            .getTabCompletions(FMLCommonHandler.instance().getMinecraftServerInstance(), new RecordingSender(), new String[] { "runall", "" }, null);
        assertTrue(runAllCompletions.contains("good"));
        assertFalse(runAllCompletions.contains("bad"));
    }

    @Test
    public void runKnownInvalidTestReportsInvalidInsteadOfUnknown() throws Exception {
        String invalidId = "bad:Broken.invalid";
        seedRegistry(
            Collections.singletonList(definition("good:Suite.valid")),
            Collections.singletonList(invalid(invalidId)));

        RecordingSender sender = new RecordingSender();

        new HorizonQACommand().execute(FMLCommonHandler.instance().getMinecraftServerInstance(), sender, new String[] { "run", invalidId });

        String messages = sender.messages();
        assertTrue(messages.contains("Invalid test"));
        assertTrue(messages.contains(invalidId));
        assertTrue(messages.contains("must be public static"));
        assertFalse(messages.contains("Unknown test"));
    }

    private static GameTestDefinition definition(String testId) throws Exception {
        return new GameTestDefinition(testId, dummyMethod(), "", 20, "", true, Rotation.NONE);
    }

    private static InvalidTestDefinition invalid(String testId) throws Exception {
        DiscoveryIssue issue = new DiscoveryIssue(
            "discovery:invalidTest:" + testId + ":modifiers",
            "DISCOVERY_ERROR",
            "Skipping @GameTest method 'invalid' in 'DummyTests': must be public static.");
        return new InvalidTestDefinition(testId, dummyMethod(), Collections.singletonList(issue));
    }

    private static Method dummyMethod() throws Exception {
        return DummyTests.class.getMethod("test", GameTestHelper.class);
    }

    @SuppressWarnings("unchecked")
    private static void seedRegistry(List<GameTestDefinition> validTests, List<InvalidTestDefinition> invalidTests)
        throws Exception {

        List<GameTestDefinition> allTests = (List<GameTestDefinition>) field("ALL_TESTS").get(null);
        allTests.clear();
        allTests.addAll(validTests);

        ((Map<String, List<Method>>) field("BEFORE_BATCH_METHODS").get(null)).clear();
        ((Map<String, List<Method>>) field("AFTER_BATCH_METHODS").get(null)).clear();

        field("lastDiscoveryResult").set(
            null,
            new DiscoveryResult(
                validTests,
                Collections.emptyMap(),
                Collections.emptyMap(),
                invalidTests,
                Collections.<InvalidBatchHook>emptyList(),
                Collections.<DuplicateTestId>emptyList(),
                Collections.<DiscoveryIssue>emptyList()));
    }

    private static Field field(String name) throws Exception {
        Field field = GameTestRegistry.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static final class DummyTests {

        public static void test(GameTestHelper helper) {}
    }

    private static final class RecordingSender implements ICommandSender {

        private final List<String> messages = new ArrayList<>();

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public ITextComponent getDisplayName() {
            return new TextComponentString(getName());
        }

        @Override
        public void sendMessage(ITextComponent component) {
            messages.add(component.getUnformattedText());
        }

        @Override
        public boolean canUseCommand(int permissionLevel, String commandName) {
            return true;
        }

        @Override
        public BlockPos getPosition() {
            return new BlockPos(0, 0, 0);
        }

        @Override
        public World getEntityWorld() {
            return null;
        }

        @Override
        public @Nullable MinecraftServer getServer() {
            return null;
        }

        String messages() {
            StringBuilder out = new StringBuilder();
            for (String message : messages) {
                out.append(message)
                    .append('\n');
            }
            return out.toString();
        }
    }
}
