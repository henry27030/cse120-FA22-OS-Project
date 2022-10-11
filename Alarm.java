package nachos.threads;

import nachos.machine.*;

import java.util.Comparator;
import java.util.PriorityQueue;



/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {

	//define Pair Comparator for waitList
	private class Pair implements Comparator<Pair> {
		private long waitTime;
		private KThread KThread;

		public Pair(KThread KThread, long waitTime) {
			this.KThread = KThread;
			this.waitTime = waitTime;
		}

		public long getWaitTime() {
			return this.waitTime;
		}

		public KThread getKThread() {
			return this.KThread;
		}

		public int compare(Pair pair1, Pair pair2) {
			return Long.valueOf(pair1.getWaitTime()).compareTo(Long.valueOf(pair2.getWaitTime()));
		}
	}


	//a list of threads that are currently asleep (blocked) with their wait timers
	private PriorityQueue<Pair> waitList;
	//prolly actually make a priority queue that sorts based on waketime

	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		waitList = new PriorityQueue<Pair>();
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean intStatus = Machine.interrupt().disable();

		//transfer eligible threads (based on waittime) from waitList to readyQueue
		while (!(waitList.isEmpty()) && waitList.peek().getWaitTime() <= Machine.timer().getTime()) {
			waitList.peek().getKThread().ready();
			waitList.poll();
		}

		Machine.interrupt().restore(intStatus);
		KThread.currentThread().yield(); //yield calls ready() which needs interrupts disabled
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 *
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 *
	 * @param x the minimum number of clock ticks to wait.
	 *
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		if (x <= 0) {
			return;
		}
		boolean intStatus = Machine.interrupt().disable();
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x; //time to match with global timer to know to wake
		//add pair current thread with wake time to waitList
		waitList.add(new Pair(KThread.currentThread(), wakeTime));
		KThread.sleep();
		Machine.interrupt().restore(intStatus);
	}

        /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 *
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
        public boolean cancel(KThread thread) {
		return false;
	}
}
