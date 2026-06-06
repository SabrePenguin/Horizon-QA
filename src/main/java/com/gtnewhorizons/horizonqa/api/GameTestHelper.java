package com.gtnewhorizons.horizonqa.api;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.horizonqa.api.annotation.Experimental;
import com.gtnewhorizons.horizonqa.api.event.EventLog;
import com.gtnewhorizons.horizonqa.api.gt.GTNHGameTestHelper;
import com.gtnewhorizons.horizonqa.internal.GameTestInstance;
import com.gtnewhorizons.horizonqa.internal.GameTestSequence;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;


/**
 * Passed to every {@code @GameTest} method. Provides world interaction, assertions, and the fluent
 * sequence API.
 */
@Experimental
@SuppressWarnings("unused")
public class GameTestHelper {

    private final GameTestInstance instance;
    private final WorldServer world;
    private final int originX;
    private final int originY;
    private final int originZ;
    private GTNHGameTestHelper gtnh;
    private final BlockPos origin;

    public GameTestHelper(GameTestInstance instance, WorldServer world, BlockPos origin) {
        this.instance = instance;
        this.world = world;
        this.origin = origin;
        this.originX = origin.getX();
        this.originY = origin.getY();
        this.originZ = origin.getZ();
    }

    /**
     * Convert test-local block coordinates to an absolute {@link TestPos} in world space.
     */
    public BlockPos absolute(int x, int y, int z) {
        return absolute(new BlockPos(x, y, z));
    }

    public BlockPos absolute(BlockPos pos) {
        return new BlockPos(originX + pos.getX(), originY + pos.getY(), originZ + pos.getZ());
    }

    /**
     * Create and attach a new {@link GameTestSequence} to this test. Must be called at most once per
     * test method. Returns the sequence so the caller can chain step methods.
     */
    public GameTestSequence startSequence() {
        GameTestSequence seq = new GameTestSequence(instance);
        instance.setSequence(seq);
        return seq;
    }

    /**
     * Immediately mark this test as passed. Equivalent to {@code startSequence().thenSucceed()} but
     * without the one-tick delay.
     */
    public void succeed() {
        instance.succeed();
    }

    /** Polls {@code predicate} each tick; passes on the first {@code true}. At most once per test. */
    public void succeedWhen(BooleanSupplier predicate) {
        instance.setSucceedWhen(predicate);
    }

    /**
     * Pass at the end of {@code timeoutTicks} after the final tick's callbacks and sequence actions
     * have run.
     */
    public void succeedAtTimeout() {
        instance.setSucceedAtTimeout();
    }

    /**
     * Run {@code callback} once per test tick until the test ends (pass or fail). Useful for negative assertions that
     * must hold continuously.
     */
    public void onEachTick(Runnable callback) {
        instance.addEachTickCallback(callback);
    }

    /** Register {@code callback} to run once when this test ends, regardless of outcome. */
    public void afterTest(Runnable callback) {
        instance.addCleanup(callback);
    }

    /**
     * Immediately fail this test with {@code message}. Throws {@link GameTestAssertException} so that
     * any enclosing {@code thenExecute} lambda propagates the failure correctly.
     */
    public void fail(String message) {
        throw new GameTestAssertException(message, originX, originY, originZ);
    }

    /** Immediately fail this test with no message. */
    public void fail() {
        throw new GameTestAssertException("Test failed", originX, originY, originZ);
    }

    /** Fail if {@code condition} is {@code false}. */
    public void assertTrue(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message, originX, originY, originZ);
    }

    /** Fail if {@code condition} is {@code false}. */
    public void assertTrue(boolean condition) {
        if (!condition) throw new GameTestAssertException("Expected true but was false", originX, originY, originZ);
    }

    /** Fail if {@code condition} is {@code true}. */
    public void assertFalse(boolean condition, String message) {
        if (condition) throw new GameTestAssertException(message, originX, originY, originZ);
    }

    /** Fail if {@code condition} is {@code true}. */
    public void assertFalse(boolean condition) {
        if (condition) throw new GameTestAssertException("Expected false but was true", originX, originY, originZ);
    }

    /** Fail if {@code actual} is not {@code null}. */
    public void assertNull(Object actual, String message) {
        if (actual != null) throw new GameTestAssertException(message, originX, originY, originZ);
    }

    /** Fail if {@code actual} is not {@code null}. */
    public void assertNull(Object actual) {
        if (actual != null)
            throw new GameTestAssertException("Expected null but was: <" + actual + ">", originX, originY, originZ);
    }

    /** Fail if {@code actual} is {@code null}. */
    public void assertNotNull(Object actual, String message) {
        if (actual == null) throw new GameTestAssertException(message, originX, originY, originZ);
    }

    /** Fail if {@code actual} is {@code null}. */
    public void assertNotNull(Object actual) {
        if (actual == null) throw new GameTestAssertException("Expected non-null value", originX, originY, originZ);
    }

    /** Fail if {@code expected} and {@code actual} are not equal per {@link Objects#equals}. */
    public void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) throw new GameTestAssertException(
            message + ": expected <" + expected + "> but found <" + actual + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code expected} and {@code actual} are not equal per {@link Objects#equals}. */
    public void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new GameTestAssertException(
            "Expected <" + expected + "> but found <" + actual + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code expected != actual}. */
    public void assertEquals(long expected, long actual, String message) {
        if (expected != actual) throw new GameTestAssertException(
            message + ": expected <" + expected + "> but found <" + actual + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code expected != actual}. */
    public void assertEquals(long expected, long actual) {
        if (expected != actual) throw new GameTestAssertException(
            "Expected <" + expected + "> but found <" + actual + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code |expected - actual| > delta}. */
    public void assertEquals(double expected, double actual, double delta, String message) {
        if (Math.abs(expected - actual) > delta) throw new GameTestAssertException(
            message + ": expected <" + expected + "> but found <" + actual + "> (delta " + delta + ")",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code |expected - actual| > delta}. */
    public void assertEquals(double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) throw new GameTestAssertException(
            "Expected <" + expected + "> but found <" + actual + "> (delta " + delta + ")",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code unexpected} and {@code actual} are equal per {@link Objects#equals}. */
    public void assertNotEquals(Object unexpected, Object actual, String message) {
        if (Objects.equals(unexpected, actual)) throw new GameTestAssertException(
            message + ": expected anything except <" + unexpected + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code unexpected} and {@code actual} are equal per {@link Objects#equals}. */
    public void assertNotEquals(Object unexpected, Object actual) {
        if (Objects.equals(unexpected, actual)) throw new GameTestAssertException(
            "Expected anything except <" + unexpected + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code unexpected == actual}. */
    public void assertNotEquals(long unexpected, long actual, String message) {
        if (unexpected == actual) throw new GameTestAssertException(
            message + ": expected anything except <" + unexpected + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code unexpected == actual}. */
    public void assertNotEquals(long unexpected, long actual) {
        if (unexpected == actual) throw new GameTestAssertException(
            "Expected anything except <" + unexpected + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code |unexpected - actual| <= delta}. */
    public void assertNotEquals(double unexpected, double actual, double delta, String message) {
        if (Math.abs(unexpected - actual) <= delta) throw new GameTestAssertException(
            message + ": expected anything except <" + unexpected + "> (delta " + delta + ")",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code |unexpected - actual| <= delta}. */
    public void assertNotEquals(double unexpected, double actual, double delta) {
        if (Math.abs(unexpected - actual) <= delta) throw new GameTestAssertException(
            "Expected anything except <" + unexpected + "> (delta " + delta + ")",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code expected} and {@code actual} are not the same object reference ({@code ==}). */
    public void assertSame(Object expected, Object actual, String message) {
        if (expected != actual) throw new GameTestAssertException(
            message + ": expected same instance <" + expected + "> but found <" + actual + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code expected} and {@code actual} are not the same object reference ({@code ==}). */
    public void assertSame(Object expected, Object actual) {
        if (expected != actual) throw new GameTestAssertException(
            "Expected same instance <" + expected + "> but found <" + actual + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code unexpected} and {@code actual} are the same object reference ({@code ==}). */
    public void assertNotSame(Object unexpected, Object actual, String message) {
        if (unexpected == actual) throw new GameTestAssertException(
            message + ": expected different instances but both were <" + actual + ">",
            originX,
            originY,
            originZ);
    }

    /** Fail if {@code unexpected} and {@code actual} are the same object reference ({@code ==}). */
    public void assertNotSame(Object unexpected, Object actual) {
        if (unexpected == actual) throw new GameTestAssertException(
            "Expected different instances but both were <" + actual + ">",
            originX,
            originY,
            originZ);
    }

    /**
     * Fail if {@code actual} is not an instance of {@code type}. Returns {@code actual} cast to {@code T}.
     */
    @SuppressWarnings("unchecked")
    public <T> T assertInstanceOf(Class<T> type, Object actual, String message) {
        if (!type.isInstance(actual)) {
            String actualType = actual == null ? "null"
                : actual.getClass()
                    .getSimpleName();
            throw new GameTestAssertException(
                message + ": expected instance of " + type.getSimpleName() + " but was " + actualType,
                originX,
                originY,
                originZ);
        }
        return (T) actual;
    }

    /**
     * Fail if {@code actual} is not an instance of {@code type}. Returns {@code actual} cast to {@code T}.
     */
    public <T> T assertInstanceOf(Class<T> type, Object actual) {
        return assertInstanceOf(type, actual, "Type assertion failed");
    }

    /**
     * Assert that {@code action} throws an exception of exactly type {@code expectedType}.
     * Returns the thrown exception for further inspection. Fails if nothing is thrown or the
     * wrong type is thrown.
     */
    public <T extends Throwable> T assertThrows(Class<T> expectedType, Runnable action, String message) {
        try {
            action.run();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                return expectedType.cast(actual);
            }
            throw new GameTestAssertException(
                message + ": expected "
                    + expectedType.getSimpleName()
                    + " but got "
                    + actual.getClass()
                        .getSimpleName(),
                originX,
                originY,
                originZ);
        }
        throw new GameTestAssertException(
            message + ": expected " + expectedType.getSimpleName() + " to be thrown but nothing was thrown",
            originX,
            originY,
            originZ);
    }

    /**
     * Assert that {@code action} throws an exception of exactly type {@code expectedType}.
     * Returns the thrown exception for further inspection.
     */
    public <T extends Throwable> T assertThrows(Class<T> expectedType, Runnable action) {
        return assertThrows(expectedType, action, "assertThrows failed");
    }

    /**
     * Assert that two iterables contain equal elements in the same order (deep equality via
     * {@link Objects#equals}).
     */
    public void assertIterableEquals(Iterable<?> expected, Iterable<?> actual, String message) {
        Iterator<?> expIt = expected.iterator();
        Iterator<?> actIt = actual.iterator();
        int index = 0;
        while (expIt.hasNext() && actIt.hasNext()) {
            Object exp = expIt.next();
            Object act = actIt.next();
            if (!Objects.equals(exp, act)) throw new GameTestAssertException(
                message + ": element [" + index + "]: expected <" + exp + "> but found <" + act + ">",
                originX,
                originY,
                originZ);
            index++;
        }
        if (expIt.hasNext()) throw new GameTestAssertException(
            message + ": expected iterable has more elements at index " + index,
            originX,
            originY,
            originZ);
        if (actIt.hasNext()) throw new GameTestAssertException(
            message + ": actual iterable has more elements at index " + index,
            originX,
            originY,
            originZ);
    }

    /**
     * Assert that two iterables contain equal elements in the same order.
     */
    public void assertIterableEquals(Iterable<?> expected, Iterable<?> actual) {
        assertIterableEquals(expected, actual, "Iterable mismatch");
    }

    /**
     * Assert that {@code actualLines} matches {@code expectedLines} line by line. Each expected line is
     * first compared verbatim; on mismatch it is tried as a {@link java.util.regex.Pattern regex}.
     * Use {@code ">>"} to skip all remaining actual lines, or {@code ">> N >>"} to skip exactly N lines.
     */
    public void assertLinesMatch(List<String> expectedLines, List<String> actualLines, String message) {
        int ai = 0;
        int ei = 0;
        while (ei < expectedLines.size()) {
            String exp = expectedLines.get(ei);
            if (">>".equals(exp)) {
                ai = actualLines.size();
                ei++;
                continue;
            }
            if (exp.startsWith(">> ") && exp.endsWith(" >>")) {
                String inner = exp.substring(3, exp.length() - 3)
                    .trim();
                try {
                    ai += Integer.parseInt(inner);
                    ei++;
                    continue;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "assertLinesMatch: invalid skip directive '>> " + inner + " >>' - not a number",
                        e);
                }
            }
            if (ai >= actualLines.size()) throw new GameTestAssertException(
                message + ": line [" + ei + "] expected <" + exp + "> but actual output ended",
                originX,
                originY,
                originZ);
            String act = actualLines.get(ai);
            if (!exp.equals(act)) {
                boolean matched = false;
                try {
                    matched = act.matches(exp);
                } catch (java.util.regex.PatternSyntaxException e) {
                    throw new GameTestAssertException(
                        message + ": line [" + ei + "]: regex <" + exp + "> is invalid - " + e.getMessage(),
                        originX,
                        originY,
                        originZ);
                }
                if (!matched) throw new GameTestAssertException(
                    message + ": line [" + ei + "]: expected <" + exp + "> but found <" + act + ">",
                    originX,
                    originY,
                    originZ);
            }
            ei++;
            ai++;
        }
        if (ai < actualLines.size()) throw new GameTestAssertException(
            message + ": "
                + (actualLines.size() - ai)
                + " unmatched actual line(s) starting with <"
                + actualLines.get(ai)
                + ">",
            originX,
            originY,
            originZ);
    }

    /**
     * Assert that {@code actualLines} matches {@code expectedLines} — see
     * {@link #assertLinesMatch(List, List, String)}.
     */
    public void assertLinesMatch(List<String> expectedLines, List<String> actualLines) {
        assertLinesMatch(expectedLines, actualLines, "Lines mismatch");
    }

    public WorldServer getWorld() {
        return world;
    }

    /**
     * Event log for this test instance. Events are appended in emit order; filter with
     * {@code snapshot().stream()} to query specific event types.
     */
    public EventLog getRecorder() {
        return instance.getRecorder();
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }

    /**
     * Assert that the block at test-local position {@code (x, y, z)} is {@code expected}.
     * Optionally checks metadata when {@code meta >= 0}.
     */
    public void assertBlockPresent(Block expected, int x, int y, int z) {
        assertBlockPresent(expected, -1, x, y, z);
    }

    /**
     * Assert that the block at test-local position is {@code expected} with the given metadata.
     * Pass {@code meta < 0} to skip the meta check.
     */
    @SuppressWarnings("Deprecation")
    public void assertBlockPresent(Block expected, int meta, int x, int y, int z) {
        assertBlockPresent(meta != -1 ? expected.getStateFromMeta(meta) : expected.getDefaultState(), new BlockPos(x, y, z));
    }

    /**
     * Assert that the block at test-local position is {@code expected} with the given metadata.
     * Pass {@code meta < 0} to skip the meta check.
     */
    public void assertBlockPresent(IBlockState expected, BlockPos pos) {
        BlockPos absolute = absolute(pos);
        Block expectedBlock = expected.getBlock();
        int expectedMeta = expectedBlock.getMetaFromState(expected);
        IBlockState actualState = world.getBlockState(absolute);
        Block actual = actualState.getBlock();
        if (actual != expectedBlock) {
            throw new GameTestAssertException(
                "Expected " + expectedBlock.getRegistryName()
                    + " at "
                    + pos
                    + " but found "
                    + actual.getRegistryName(),
                absolute);
        }
        if (expectedMeta >= 0) {
            int actualMeta = actual.getMetaFromState(actualState);
            if (actualMeta != expectedMeta) {
                throw new GameTestAssertException(
                    "Expected meta " + expectedMeta + " at " + pos + " but found " + actualMeta,
                    absolute);
            }
        }
    }


    /**
     * Assert that the block at test-local position {@code (x, y, z)} is NOT {@code unexpected}.
     */
    public void assertBlockAbsent(Block unexpected, int x, int y, int z) {
        assertBlockAbsent(unexpected.getDefaultState(), new BlockPos(x, y, z));
    }

    /**
     * Assert that the block at test-local position {@code pos} is NOT {@code unexpected}.
     */
    public void assertBlockAbsent(IBlockState unexpected, BlockPos pos) {
        BlockPos absolute = absolute(pos);
        IBlockState actual = world.getBlockState(absolute);
        if (actual.getBlock() == unexpected.getBlock()) {
            throw new GameTestAssertException(
                "Expected anything except " + unexpected.getBlock().getRegistryName() + " at " + pos + " but found it",
                absolute);
        }
    }

    /**
     * Assert that a TileEntity exists at test-local position {@code (x, y, z)}.
     * Returns the TileEntity for further inspection.
     */
    public TileEntity assertTileEntityPresent(int x, int y, int z) {
        return assertTileEntityPresent(new BlockPos(x, y, z));
    }

    /**
     * Assert that a TileEntity exists at test-local position {@code (x, y, z)}.
     * Returns the TileEntity for further inspection.
     */
    public TileEntity assertTileEntityPresent(BlockPos relative) {
        BlockPos absolute = absolute(relative);
        TileEntity te = world.getTileEntity(absolute);
        if (te == null) {
            throw new GameTestAssertException("Expected a TileEntity at " + relative + " but found none", absolute);
        }
        return te;
    }

    /**
     * Assert that a TileEntity of a specific type exists at test-local position.
     * Returns the TileEntity cast to the requested type.
     */
    public <T extends TileEntity> T assertTileEntityPresent(Class<T> type, int x, int y, int z) {
        return assertTileEntityPresent(type, new BlockPos(x, y, z));
    }

    /**
     * Assert that a TileEntity of a specific type exists at test-local position.
     * Returns the TileEntity cast to the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T extends TileEntity> T assertTileEntityPresent(Class<T> type, BlockPos pos) {
        TileEntity te = assertTileEntityPresent(pos);
        if (!type.isInstance(te)) {
            BlockPos absolute = absolute(pos);
            throw new GameTestAssertException(
                "Expected TileEntity of type " + type.getSimpleName()
                    + " at "
                    + pos
                    + " but found "
                    + te.getClass().getSimpleName(),
                absolute);
        }
        return (T) te;
    }

    /**
     * Assert that the TileEntity NBT at test-local position contains all keys/values from
     * {@code expectedSubset}. Only the keys present in {@code expectedSubset} are checked.
     */
    public void assertTileNBT(int x, int y, int z, NBTTagCompound expectedSubset) {
        assertTileNBT(new BlockPos(x, y, z), expectedSubset);
    }

    /**
     * Assert that the TileEntity NBT at test-local position contains all keys/values from
     * {@code expectedSubset}. Only the keys present in {@code expectedSubset} are checked.
     */
    public void assertTileNBT(BlockPos pos, NBTTagCompound expectedSubset) {
        TileEntity te = assertTileEntityPresent(pos);
        NBTTagCompound actual = new NBTTagCompound();
        te.writeToNBT(actual);

        for (String key : expectedSubset.getKeySet()) {
            if (!actual.hasKey(key)) {
                throw new GameTestAssertException(
                    "TileEntity at " + pos + " missing NBT key '" + key + "'",
                    absolute(pos));
            }
            NBTBase expectedTag = expectedSubset.getTag(key);
            NBTBase actualTag = actual.getTag(key);
            if (!expectedTag.equals(actualTag)) {
                throw new GameTestAssertException(
                    "TileEntity at " + pos
                        + " NBT key '"
                        + key
                        + "': expected "
                        + expectedTag
                        + " but found "
                        + actualTag,
                    absolute(pos));
            }
        }
    }

    /**
     * Assert a specific value at a dotted NBT path (e.g. {@code "mInventory.0.id"}).
     * Comparison is done via the tag's string representation.
     */
    public void assertTileNBTPath(int x, int y, int z, String path, String expectedValue) {
        assertTileNBTPath(new BlockPos(x, y, z), path, expectedValue);
    }

    /**
     * Assert a specific value at a dotted NBT path (e.g. {@code "mInventory.0.id"}).
     * Comparison is done via the tag's string representation.
     */
    public void assertTileNBTPath(BlockPos relative, String path, String expectedValue) {
        NBTTagCompound nbt = getTileNBT(relative);
        String actual = NBTPathAccessor.resolveAsString(nbt, path);
        if (actual == null) {
            throw new GameTestAssertException(
                "TileEntity at " + relative + " has no NBT at path '" + path + "'",
                absolute(relative));
        }
        if (!actual.equals(expectedValue)) {
            throw new GameTestAssertException(
                "TileEntity at (" + relative
                    + ") NBT path '"
                    + path
                    + "': expected "
                    + expectedValue
                    + " but found "
                    + actual,
                absolute(relative));
        }
    }

    /**
     * Return a deep copy of the TileEntity's serialized NBT at test-local position.
     */
    public NBTTagCompound getTileNBT(int x, int y, int z) {
        return getTileNBT(new BlockPos(x, y, z));
    }

    /**
     * Return a deep copy of the TileEntity's serialized NBT at test-local position.
     */
    public NBTTagCompound getTileNBT(BlockPos relative) {
        TileEntity te = assertTileEntityPresent(relative);
        NBTTagCompound nbt = new NBTTagCompound();
        te.writeToNBT(nbt);
        return nbt.copy();
    }

    /**
     * Insert an ItemStack into the inventory at test-local position. Auto-detects
     * {@code ISidedInventory} vs plain {@code IInventory}.
     *
     * @throws GameTestAssertException if no inventory exists or the stack couldn't be fully inserted
     */
    public void insertItem(int x, int y, int z, ItemStack stack) {
        insertItem(new BlockPos(x, y, z), stack, 0);
    }

    /**
     * Insert an ItemStack into the inventory at test-local position. Auto-detects
     * {@code ISidedInventory} vs plain {@code IInventory}.
     *
     * @throws GameTestAssertException if no inventory exists or the stack couldn't be fully inserted
     */
    public void insertItem(BlockPos relative, ItemStack stack, int slot) {
        IItemHandler inventory = getItemHandlerAt(relative, null);
        ItemStack leftover = inventory.insertItem(slot, stack, false);
        if (leftover.getCount() > 0) {
            throw new GameTestAssertException(
                "Could not fully insert " + stack
                    .getDisplayName()
                    + " into slot "
                    + slot
                    + " at "
                    + relative
                    + ": "
                    + leftover.getCount()
                    + " items remaining",
                absolute(relative));
        }
    }

    /**
     * Insert an ItemStack into the inventory at a test-local (relative) TestPos.
     */
    public void insertItem(TestPos pos, ItemStack stack) {
        insertItem(pos.x(), pos.y(), pos.z(), stack);
    }

    /**
     * Assert that the inventory at test-local position contains at least the given stack
     * (item, damage, NBT match; stack size is minimum).
     */
    public void assertInventoryContains(int x, int y, int z, ItemStack expected) {
        assertInventoryContains(new BlockPos(x, y, z), expected);
    }

    /**
     * Assert that the inventory at test-local position contains at least the given stack
     * (item, damage, NBT match; stack size is minimum).
     */
    public void assertInventoryContains(BlockPos relative, ItemStack expected) {
        IItemHandler inventory = getItemHandlerAt(relative);
        if (!InventoryHelper.contains(inventory, expected)) {
            throw new GameTestAssertException(
                "Inventory at " + relative
                    + " does not contain "
                    + expected.getCount()
                    + "x "
                    + expected.getDisplayName(),
                absolute(relative));
        }
    }

    /**
     * Assert that every slot of the inventory at test-local position is empty.
     */
    public void assertInventoryEmpty(int x, int y, int z) {
        assertInventoryEmpty(new BlockPos(x, y, z));
    }

    /**
     * Assert that every slot of the inventory at test-local position is empty.
     */
    public void assertInventoryEmpty(BlockPos relative) {
        IItemHandler inventory = getItemHandlerAt(relative);
        if (!InventoryHelper.isEmpty(inventory)) {
            throw new GameTestAssertException(
                "Inventory at " + relative + " is not empty",
                absolute(relative));
        }
    }

    /**
     * Assert that a specific slot contains the given stack (exact item + damage + NBT + size).
     */
    public void assertSlot(int x, int y, int z, int slot, ItemStack expected) {
        assertSlot(new BlockPos(x, y, z), slot, expected);
    }

    /**
     * Assert that a specific slot contains the given stack (exact item + damage + NBT + size).
     */
    public void assertSlot(BlockPos relative, int slot, ItemStack expected) {
        IItemHandler inventory = getItemHandlerAt(relative);
        ItemStack actual = inventory.getStackInSlot(slot);
        if (expected == null) {
            if (!actual.isEmpty() && actual.getCount() > 0) {
                throw new GameTestAssertException(
                    "Slot " + slot
                        + " at "
                        + relative
                        + " expected empty but found "
                        + actual.getDisplayName(),
                    absolute(relative));
            }
            return;
        }
        if (!InventoryHelper.stacksMatch(actual, expected) || actual.getCount() != expected.getCount()) {
            String actualStr = !actual.isEmpty()? actual.getCount() + "x " + actual.getDisplayName() : "<empty>";
            throw new GameTestAssertException(
                "Slot " + slot
                    + " at ("
                    + relative
                    + ") expected "
                    + expected.getCount()
                    + "x "
                    + expected.getDisplayName()
                    + " but found "
                    + actualStr,
                absolute(relative));
        }
    }

    /**
     * Extract up to {@code maxAmount} items matching {@code template} from the inventory.
     * Returns the actual amount extracted.
     */
    public int extractItem(int x, int y, int z, ItemStack template, int maxAmount) {
        return extractItem(new BlockPos(x, y, z), template, maxAmount);
    }

    /**
     * Extract up to {@code maxAmount} items matching {@code template} from the inventory.
     * Returns the actual amount extracted.
     */
    public int extractItem(BlockPos relative, ItemStack template, int maxAmount) {
        IItemHandler itemHandler = getItemHandlerAt(relative);
        return InventoryHelper.extract(itemHandler, template, maxAmount);
    }

    private IItemHandler getItemHandlerAt(BlockPos relative) {
        return getItemHandlerAt(relative, null);
    }

    private IItemHandler getItemHandlerAt(BlockPos relative, EnumFacing facing) {
        TileEntity te = assertTileEntityPresent(relative);
        if (te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
            return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        } else if (te instanceof ISidedInventory sided) {
            return new SidedInvWrapper(sided, facing);
        } else if (te instanceof IInventory inventory) {
            return new InvWrapper(inventory);
        }
        throw new GameTestAssertException(
            "TileEntity at " + relative + "is not an IItemHandler (" + te.getClass().getSimpleName() + ")",
            absolute(relative));
    }

    private IInventory getInventoryAt(int x, int y, int z) {
        TileEntity te = assertTileEntityPresent(x, y, z);
        if (!(te instanceof IInventory inv)) {
            throw new GameTestAssertException(
                "TileEntity at (" + x
                    + ","
                    + y
                    + ","
                    + z
                    + ") is not an IInventory ("
                    + te.getClass()
                        .getSimpleName()
                    + ")",
                absolute(x, y, z));
        }
        return inv;
    }

    /**
     * Insert a FluidStack into the fluid handler at test-local position.
     *
     * @throws GameTestAssertException if no fluid handler exists or the fluid couldn't be fully inserted
     */
    public void insertFluid(int x, int y, int z, FluidStack fluid) {

    }

    /**
     * Insert a FluidStack into the fluid handler at test-local position.
     *
     * @throws GameTestAssertException if no fluid handler exists or the fluid couldn't be fully inserted
     */
    public void insertFluid(BlockPos relative, FluidStack fluid) {
        IFluidHandler handler = getFluidHandlerAt(relative);
        int filled = handler.fill(fluid, true);
        if (filled < fluid.amount) {
            throw new GameTestAssertException(
                "Could not fully insert " + fluid.amount
                    + "mB of "
                    + fluid.getLocalizedName()
                    + " at "
                    + relative
                    + ": only "
                    + filled
                    + "mB accepted",
                absolute(relative));
        }
    }

    /**
     * Assert that the fluid handler at test-local position contains at least {@code expected.amount}
     * mB of the expected fluid.
     */
    public void assertFluidTank(int x, int y, int z, FluidStack expected) {
        assertFluidTank(new BlockPos(x, y, z), expected);
    }

    /**
     * Assert that the fluid handler at test-local position contains at least {@code expected.amount}
     * mB of the expected fluid.
     */
    public void assertFluidTank(BlockPos relative, FluidStack expected) {
        IFluidHandler handler = getFluidHandlerAt(relative);
        FluidStack drained = handler.drain(expected.copy(), false);
        if (drained == null || drained.amount < expected.amount || drained.getFluid() != expected.getFluid()) {
            String actualStr = drained != null ? drained.amount + "mB " + drained.getLocalizedName() : "<empty>";
            throw new GameTestAssertException(
                "Fluid tank at " + relative
                    + " expected "
                    + expected.amount
                    + "mB "
                    + expected.getLocalizedName()
                    + " but found "
                    + actualStr,
                absolute(relative));
        }
    }

    /**
     * Assert that the fluid handler at test-local position has no fluid.
     */
    public void assertFluidTankEmpty(int x, int y, int z) {
        assertFluidTankEmpty(new BlockPos(x, y, z));
    }

    /**
     * Assert that the fluid handler at test-local position has no fluid.
     */
    public void assertFluidTankEmpty(BlockPos relative) {
        IFluidHandler handler = getFluidHandlerAt(relative);
        FluidStack drained = handler.drain(Integer.MAX_VALUE, false);
        if (drained != null && drained.amount > 0) {
            throw new GameTestAssertException(
                "Fluid tank at (" + relative
                    + ") expected empty but found "
                    + drained.amount
                    + "mB "
                    + drained.getLocalizedName(),
                absolute(relative));
        }
    }

    private IFluidHandler getFluidHandlerAt(int x, int y, int z) {
        return getFluidHandlerAt(new BlockPos(x, y, z));
    }

    private IFluidHandler getFluidHandlerAt(BlockPos relative) {
        TileEntity te = assertTileEntityPresent(relative);
        if (!(te instanceof IFluidHandler handler)) {
            throw new GameTestAssertException(
                "TileEntity at " + relative
                    + " is not an IFluidHandler ("
                    + te.getClass()
                        .getSimpleName()
                    + ")",
                absolute(relative));
        }
        return handler;
    }

    /**
     * Place a block at test-local position. Uses flag 3 (notify + send to client).
     */
    public void setBlock(int x, int y, int z, Block block, int meta) {
        setBlock(new BlockPos(x, y, z), block.getStateFromMeta(meta));
    }

    /**
     * Place a block at test-local position. Uses flag 3 (notify + send to client).
     */
    public void setBlock(BlockPos relative, IBlockState state) {
        BlockPos absolute = absolute(relative);
        world.setBlockState(absolute, state, 3);
    }

    public void setBlock(int x, int y, int z, Block block) {
        setBlock(x, y, z, block, 0);
    }

    /**
     * Destroy the block at test-local position (replace with air). Drops are not spawned.
     */
    public void destroyBlock(int x, int y, int z) {
        destroyBlock(new BlockPos(x, y, z));
    }

    /**
     * Destroy the block at test-local position (replace with air). Drops are not spawned.
     */
    public void destroyBlock(BlockPos relative) {
        BlockPos absolute = absolute(relative);
        world.setBlockState(absolute, Blocks.AIR.getDefaultState(), 3);
    }

    /**
     * Apply an NBT compound to the TileEntity at test-local position. The x/y/z keys in the compound
     * are overwritten with the absolute position so the TE doesn't teleport.
     */
    public void setTile(int x, int y, int z, NBTTagCompound nbt) {
        setTile(new BlockPos(x, y, z), nbt);
    }

    /**
     * Apply an NBT compound to the TileEntity at test-local position. The x/y/z keys in the compound
     * are overwritten with the absolute position so the TE doesn't teleport.
     */
    public void setTile(BlockPos relative, NBTTagCompound nbt) {
        BlockPos absolute = absolute(relative);
        TileEntity te = world.getTileEntity(absolute);
        if (te == null) {
            throw new GameTestAssertException(
                "No TileEntity at " + relative + " to apply NBT to",
                absolute);
        }
        NBTTagCompound copy = nbt.copy();
        copy.setInteger("x", absolute.getX());
        copy.setInteger("y", absolute.getY());
        copy.setInteger("z", absolute.getZ());
        te.readFromNBT(copy);
        world.notifyBlockUpdate(absolute, world.getBlockState(absolute), world.getBlockState(absolute), 3);
    }

    /**
     * Place a redstone block at the given test-local position for {@code durationTicks}, then remove it.
     * Uses a delayed sequence event on the test instance.
     */
    public void pulseRedstone(int x, int y, int z, int durationTicks) {
        pulseRedstone(new BlockPos(x, y, z), durationTicks);
    }

    /**
     * Place a redstone block at the given test-local position for {@code durationTicks}, then remove it.
     * Uses a delayed sequence event on the test instance.
     */
    public void pulseRedstone(BlockPos relative, int durationTicks) {
        setBlock(relative, Blocks.REDSTONE_BLOCK.getDefaultState());
        instance.scheduleDelayed(durationTicks, () -> destroyBlock(relative));
    }

    /**
     * Set a redstone signal source (repeater-like) at test-local position by placing a redstone block
     * (strength = 15) or air (strength = 0).
     *
     * @param strength 0 to clear, any positive value places a redstone block (full signal 15)
     */
    public void setRedstoneInput(int x, int y, int z, int strength) {
        setRedstoneInput(new BlockPos(x, y, z), strength);
    }

    /**
     * Set a redstone signal source (repeater-like) at test-local position by placing a redstone block
     * (strength = 15) or air (strength = 0).
     *
     * @param strength 0 to clear, any positive value places a redstone block (full signal 15)
     */
    public void setRedstoneInput(BlockPos relative, int strength) {
        if (strength > 0) {
            setBlock(relative, Blocks.REDSTONE_BLOCK.getDefaultState());
        } else {
            destroyBlock(relative);
        }
    }

    /**
     * Assert that the block at test-local position is receiving at least {@code minPower} redstone power.
     */
    public void assertRedstonePower(int x, int y, int z, int minPower) {
        assertRedstonePower(new BlockPos(x, y, z), minPower);
    }

    /**
     * Assert that the block at test-local position is receiving at least {@code minPower} redstone power.
     */
    public void assertRedstonePower(BlockPos relative, int minPower) {
        BlockPos absolute = absolute(relative);
        int power = world.getStrongPower(absolute);
        if (power < minPower) {
            throw new GameTestAssertException(
                "Expected redstone power >= " + minPower + " at " + relative + " but found " + power,
                absolute);
        }
    }

    /**
     * Disable random block ticks in this world (e.g. crop growth, leaf decay) for deterministic tests.
     * The original {@code randomTickSpeed} value is automatically restored after the test.
     */
    public void disableRandomTicks() {
        String original = world.getGameRules().getString("randomTickSpeed");
        world.getGameRules()
            .setOrCreateGameRule("randomTickSpeed", "0");
        afterTest(
            () -> world.getGameRules()
                .setOrCreateGameRule("randomTickSpeed", original));
    }

    /**
     * Fix the world time to a specific value and disable the daylight cycle.
     * The original time and {@code doDaylightCycle} value are automatically restored after the test.
     */
    public void fixWorldTime(long ticks) {
        String originalCycle = world.getGameRules().getString("doDaylightCycle");
        long originalTime = world.getWorldTime();
        world.setWorldTime(ticks);
        world.getGameRules()
            .setOrCreateGameRule("doDaylightCycle", "false");
        afterTest(() -> {
            world.setWorldTime(originalTime);
            world.getGameRules()
                .setOrCreateGameRule("doDaylightCycle", originalCycle);
        });
    }

    /**
     * Apply a weather preset and lock it for the duration of the test.
     * The original weather state is automatically restored after the test.
     */
    public void setWeather(Weather weather) {
        WorldInfo info = world.getWorldInfo();
        boolean wasRaining = info.isRaining();
        boolean wasThundering = info.isThundering();
        int rainTime = info.getRainTime();
        int thunderTime = info.getThunderTime();
        weather.applyTo(world);
        afterTest(() -> {
            info.setRaining(wasRaining);
            info.setThundering(wasThundering);
            info.setRainTime(rainTime);
            info.setThunderTime(thunderTime);
        });
    }

    /**
     * Spawn a fake player with the given profile name, positioned at the test's origin.
     */
    public FakePlayer spawnFakePlayer(String profileName) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(profileName.getBytes()), profileName);
        FakePlayer player = FakePlayerFactory.get(world, profile);
        player.setPosition(originX + 0.5, originY + 1.0, originZ + 0.5);
        return player;
    }

    /**
     * Simulate a right-click on the block at test-local position using the given player and held item.
     * Returns true if the activation was handled.
     */
    public boolean simulateRightClick(int x, int y, int z, FakePlayer player, ItemStack heldItem) {
        return simulateRightClick(new BlockPos(x, y, z), player, heldItem);
    }

    /**
     * Simulate a right-click on the block at test-local position using the given player and held item.
     * Returns true if the activation was handled.
     */
    //TODO: Facing and hand
    public boolean simulateRightClick(BlockPos relative, FakePlayer player, ItemStack heldItem) {
        BlockPos absolute = absolute(relative);
        player.inventory.mainInventory.set(player.inventory.currentItem, heldItem);
        IBlockState state = world.getBlockState(absolute);
        Block block = state.getBlock();
        int meta = block.getMetaFromState(state);
        return block.onBlockActivated(world, absolute, state, player, EnumHand.MAIN_HAND, EnumFacing.UP,0.5f, 0.5f, 0.5f);
    }

    /**
     * Simulate a left-click (block punch) at test-local position.
     */
    public void simulateLeftClick(int x, int y, int z, FakePlayer player) {

    }

    /**
     * Simulate a left-click (block punch) at test-local position.
     */
    public void simulateLeftClick(BlockPos relative, FakePlayer player) {
        BlockPos absolute = absolute(relative);
        IBlockState state = world.getBlockState(absolute);
        Block block = state.getBlock();
        block.onBlockClicked(world, absolute, player);
    }

    /**
     * Return the GTNH-specific helper that provides GregTech machine assertions, EU supply,
     * time-warp, and fluid-hatch utilities. The helper is created lazily on first call and
     * reused on subsequent calls within the same test instance.
     *
     * @throws IllegalStateException if GT5-Unofficial (mod ID {@code gregtech}) is not loaded
     */
    public GTNHGameTestHelper gtnh() {
        if (gtnh == null) {
            if (!Loader.isModLoaded("gregtech")) {
                throw new IllegalStateException(
                    "GT5-Unofficial (mod ID 'gregtech') is not loaded. Cannot use GTNHGameTestHelper.");
            }
            gtnh = new GTNHGameTestHelper(this, world, originX, originY, originZ);
        }
        return gtnh;
    }
}
