package nachos.threads;

import nachos.machine.*;

import java.util.*;
import java.util.Comparator;
import java.util.PriorityQueue;



/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {

	//define Pair Comparatable for waitList
	private class Pair implements Comparable<Pair> {
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

		@Override
		public int compareTo(Pair pair1) {
      return (int)this.getWaitTime()-(int)pair1.getWaitTime();
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

		//for each thread in the waitList, compare to the parameter thread
		//if it's the same thread then
			//remove it from the waitList and transfer it into the readyQueue
			//return true;
		boolean intStatus = Machine.interrupt().disable();
		//queue use a get method/remove method
		for (Pair KThreadPair : waitList) {
			if (KThreadPair.KThread == thread) {
				waitList.remove(KThreadPair); //remove from waitList in alarm
				thread.ready();
				Machine.interrupt().restore(intStatus);
				return true;
			}
		}

		Machine.interrupt().restore(intStatus);
		return false;
	}
  public static void alarmTest1() {
		int durations[] = {1000, 10*1000, 100*1000};
		long t0, t1;

		for (int d : durations) {
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil (d);
			t1 = Machine.timer().getTime();
			System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
	}

	// Implement more test methods here ...

	// Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
	public static void selfTest() {
		alarmTest1();

		// Invoke your other test methods here ...
	}
}
