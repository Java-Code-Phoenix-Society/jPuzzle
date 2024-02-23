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
public class MoveQ implements Cloneable, java.io.Serializable {
    /**
     * Maximum length of move queue.
     */
    private static final int MAX_SIZE = Integer.MAX_VALUE;

    /**
     * List of moves.
     *
     * @serial
     */
    private List mq;

    /**
     * Index of the next pending move.
     * Entries {@code qnxt} to the end are pending moves.
     * Entries {@code 0} to {@code qnxt-1} are past moves, oldest first.
     *
     * @serial
     */
    private int qnxt;

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
        mq = new ArrayList();
        qnxt = 0;
    }

    /**
     * Clones this move queue.
     *
     * @return a clone.
     */
    public Object clone() {
        MoveQ m = new MoveQ();
        m.mq = new ArrayList(mq);
        m.qnxt = qnxt;
        return m;
    }

    /**
     * Returns the number of moves on the pending list.
     *
     * @return moves left in the list
     */
    public int pendingSize() {
        return mq.size() - qnxt;
    }

    /**
     * Returns the number of moves on the history stack.
     *
     * @return number of moves on the stack
     */
    public int historySize() {
        return qnxt;
    }

    /**
     * Clears the pending list.
     */
    public void clearPending() {
        mq.subList(qnxt, mq.size()).clear();
    }

    /**
     * Clears the history stack.
     */
    public void clearHistory() {
        mq.subList(0, qnxt).clear();
        qnxt = 0;
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
            int qend = mq.size() - 1;
            if (qnxt <= qend && mq.get(qend).equals(n)) {
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
            throw new IllegalArgumentException("push(" + n + ")");
        }

        // handle overflow

        if (mq.size() == MAX_SIZE) {
            if (qnxt > 0) {
                mq.remove(0); // shrink history stack
                --qnxt;
            } else {
                throw new RuntimeException("push overflow");
            }
        }

        mq.add(n);
    }

    /**
     * Removes the specified move from the tail of the pending list,
     * undoing an elision if necessary. If the specified move is not on
     * the list, it must have been elided so it is pushed back on the
     * list instead!
     *
     * @param n the move.
     * @return the dequeued move: {@code n}.
     * @throws NoSuchElementException if the pending list is empty.
     */
    public Object dequeue(Object n) {
        // elide handling

        int qend = mq.size() - 1;
        if (qnxt > qend || !mq.get(qend).equals(n)) {
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
        int qend = mq.size() - 1;
        if (qnxt > qend) {
            throw new NoSuchElementException("dequeue underflow");
        }

        return mq.remove(qend);
    }

    /**
     * Pops a move from the head of the pending list and pushes it
     * onto the top of the history stack.
     *
     * @return the move, or {@code null} if the pending list was empty.
     */
    public Object pop() {
        Object n = null; // nothing pending
        if (qnxt < mq.size()) {
            n = mq.get(qnxt++);
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
        if (qnxt > 0) {
            n = mq.get(--qnxt);
        }
        return n;
    }
}
