package com.gtnewhorizons.gametest.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Fluent builder for a tick-based sequence of test actions. Each step is scheduled at an absolute
 * test-tick count. Terminal steps ({@link #thenSucceed()}, {@link #thenFail(String)}) mark the owning
 * instance as done.
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * {@code
 * helper.startSequence()
 *     .thenIdle(10)
 *     .thenExecute(() -> helper.assertBlockPresent(Blocks.stone, pos))
 *     .thenSucceed();
 * }
 * </pre>
 */
public class GameTestSequence {

    private final GameTestInstance instance;
    private final Deque<SequenceEvent> events = new ArrayDeque<>();
    /** Next tick at which a newly-added event will be scheduled. */
    private long currentScheduledTick = 0;

    public GameTestSequence(GameTestInstance instance) {
        this.instance = instance;
    }

    /** Advance the schedule by {@code ticks} without adding an event. */
    public GameTestSequence thenIdle(int ticks) {
        currentScheduledTick += ticks;
        return this;
    }

    /** Execute {@code action} at the current scheduled tick. Non-conditional — if it throws the test fails. */
    public GameTestSequence thenExecute(Runnable action) {
        events.add(new SequenceEvent(currentScheduledTick, action, false));
        return this;
    }

    /**
     * Execute {@code action} every tick for {@code ticks} consecutive ticks starting at the current
     * scheduled tick, then advance the schedule.
     */
    public GameTestSequence thenExecuteFor(int ticks, Runnable action) {
        for (int i = 0; i < ticks; i++) {
            events.add(new SequenceEvent(currentScheduledTick + i, action, false));
        }
        currentScheduledTick += ticks;
        return this;
    }

    /**
     * Retry {@code condition} every tick from the current scheduled tick until it stops throwing, then
     * advance. If the condition never passes the test will eventually time out.
     */
    public GameTestSequence thenWaitUntil(Runnable condition) {
        events.add(new SequenceEvent(currentScheduledTick, condition, true));
        return this;
    }

    /**
     * Like {@link #thenWaitUntil(Runnable)} but also advances the schedule by {@code maxTicks} so
     * subsequent events are delayed by that amount regardless of when the condition passes.
     */
    public GameTestSequence thenWaitUntil(int maxTicks, Runnable condition) {
        events.add(new SequenceEvent(currentScheduledTick, condition, true));
        currentScheduledTick += maxTicks;
        return this;
    }

    /** Mark the test as passed. Must be the last call in a chain. */
    public void thenSucceed() {
        events.add(new SequenceEvent(currentScheduledTick, instance::succeed, false));
    }

    /** Mark the test as failed with {@code message}. Must be the last call in a chain. */
    public void thenFail(String message) {
        events.add(new SequenceEvent(currentScheduledTick, () -> instance.fail(message), false));
    }

    /**
     * Called each tick by the owning {@link GameTestInstance}. Processes all events whose scheduled
     * tick has been reached.
     */
    void tick(long testTick) {
        while (!events.isEmpty() && !instance.isDone()) {
            SequenceEvent head = events.peek();
            if (testTick < head.scheduledTick) break;

            if (head.conditional) {
                try {
                    head.action.run();
                    events.poll();
                } catch (AssertionError e) {
                    break;
                }
            } else {
                events.poll();
                head.action.run();
            }
        }
    }

    private static final class SequenceEvent {

        final long scheduledTick;
        final Runnable action;
        /** If true the action is retried each tick until it stops throwing. */
        final boolean conditional;

        SequenceEvent(long scheduledTick, Runnable action, boolean conditional) {
            this.scheduledTick = scheduledTick;
            this.action = action;
            this.conditional = conditional;
        }
    }
}
