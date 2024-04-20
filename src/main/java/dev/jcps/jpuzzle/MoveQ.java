package dev.jcps.jpuzzle;
/*
 * MoveQ.java
 *
 * Licensed Materials - See the file license.txt.
 * Copyright (c) Joseph Bowbeer 1998, 1999. All rights reserved.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Provides Undo/Redo capability for Puzzle moves. A move is designated
 * by a piece id.
 * <p>
 * MoveQ manages two lists of moves: a pending list and a history stack.
 * New moves are pushed onto the tail of the pending list. When the next
 * move is popped from the head of the pending list it is automatically
 * pushed onto the top of the history stack. To undo a move, the top of
 * the history stack is pulled off and reinstated at the head of the
 * pending list.
 * <p>
 * Because moves are designated by piece ids, undoing a move is as simple
 * as repeating it. Note that any adjacent pair of identical moves
 * accomplishes nothing: amounting to moving a piece into the hole
 * followed by moving the same piece back again. The MoveQ can
 * automatically detect and <i>elide</i> these redundant pairs of moves
 * as they are pushed onto the pending list.
 *
 * @author <a href="mailto:jozart@csi.com">Joseph Bowbeer</a>
 * @version 1.2
 */
public class MoveQ implements java.io.Serializable {
    /**
     * Maximum length of move queue.
     */
    private static final int MAX_SIZE = Integer.MAX_VALUE;

    /**
     * List of moves.
     *
     * @serial
     */
    private List<Object> mq;

    /**
     * Index of the next pending move.
     * Entries {@code qNxt} to the end are pending moves.
     * Entries {@code 0} to {@code qNxt-1} are past moves, oldest first.
     *
     * @serial
     */
    private int qNxt;

    /**
     * Constructs an empty move queue.
     */
    public MoveQ() {
        reset();
    }

    /**
     * Resets this move queue. Clears the pending list and history stack.
     */
    public void reset() {
        mq = new ArrayList<>();
        qNxt = 0;
    }

    /**
     * Copy constructor for creating a deep copy of a MoveQ object.
     *
     * @param original The original MoveQ object to be copied.
     */
    public MoveQ(MoveQ original) {
        this.mq = new ArrayList<>(original.mq);
        this.qNxt = original.qNxt;
    }

    /**
     * Factory method for creating a deep copy of a MoveQ object.
     *
     * @param original The original MoveQ object to be copied.
     * @return A deep copy of the original MoveQ object.
     */
    public static MoveQ copy(MoveQ original) {
        return new MoveQ(original);
    }
    /**
     * Returns the number of moves on the pending list.
     *
     * @return moves left in the list
     */
    public int pendingSize() {
        return mq.size() - qNxt;
    }

    /**
     * Returns the number of moves on the history stack.
     *
     * @return number of moves on the stack
     */
    public int historySize() {
        return qNxt;
    }

    /**
     * Clears the pending list.
     */
    public void clearPending() {
        mq.subList(qNxt, mq.size()).clear();
    }

    /**
     * Clears the history stack.
     */
    public void clearHistory() {
        mq.subList(0, qNxt).clear();
        qNxt = 0;
    }

    /**
     * Pushes a move onto the tail of the pending list, optionally
     * eliding duplicate moves.
     * If {@code elide} is {@code true} and the new move would
     * duplicate the previous move on the list, the previous move is
     * removed instead!
     *
     * @param n     the move.
     * @param elide if {@code true} elide duplicate moves.
     * @throws IllegalArgumentException if {@code n} is {@code null}.
     * @throws RuntimeException         if the queue is completely full.
     */
    public void push(Object n, boolean elide) {
        if (elide) {
            int qEnd = mq.size() - 1;
            if (qNxt <= qEnd && mq.get(qEnd).equals(n)) {
                dequeue();
                return;
            }
        }

        push(n);
    }

    /**
     * Pushes a move onto the tail of the pending list without
     * attempting to elide. If the pending list is full, drop
     * the oldest item from the history stack to make room.
     *
     * @param n the move.
     * @throws IllegalArgumentException if {@code n} is {@code null}.
     * @throws RuntimeException         if the queue is completely full.
     */
    public void push(Object n) {
        if (n == null) {
            //throw new IllegalArgumentException("push(" + n + ")")
            return;
        }

        // handle overflow

        if (mq.size() == MAX_SIZE) {
            if (qNxt > 0) {
                mq.remove(0); // shrink history stack
                --qNxt;
            } else {
                //throw new RuntimeException("push overflow")
                return;
            }
        }

        mq.add(n);
    }

    /**
     * Removes the specified move from the tail of the pending list,
     * undoing an elision if necessary. If the specified move is not on
     * the list, it must have been elided. So, it is pushed back on the
     * list instead!
     *
     * @param n the move.
     * @return the dequeued move: {@code n}.
     * @throws NoSuchElementException if the pending list is empty.
     */
    public Object dequeue(Object n) {
        // elide handling

        int qEnd = mq.size() - 1;
        if (qNxt > qEnd || !mq.get(qEnd).equals(n)) {
            push(n);
            return n;
        }

        return dequeue();
    }

    /**
     * Removes one move from the tail of the pending list without
     * undoing any elision.
     *
     * @return the dequeued move.
     * @throws NoSuchElementException if the pending list is empty.
     */
    public Object dequeue() {
        int qEnd = mq.size() - 1;
        if (qNxt > qEnd) {
            throw new NoSuchElementException("dequeue underflow");
        }

        return mq.remove(qEnd);
    }

    /**
     * Pops a move from the head of the pending list and pushes it
     * onto the top of the history stack.
     *
     * @return the move, or {@code null} if the pending list was empty.
     */
    public Object pop() {
        Object n = null; // nothing pending
        if (qNxt < mq.size()) {
            n = mq.get(qNxt++);
        }
        return n;
    }

    /**
     * Pulls a move from the top of the history stack and reinstates it
     * at the head of the pending list.
     *
     * @return the reinstated move, or {@code null} if the history
     * stack was empty.
     */
    public Object pull() {
        Object n = null; // no history
        if (qNxt > 0) {
            n = mq.get(--qNxt);
        }
        return n;
    }
}
