package nachos.threads;

import nachos.machine.*;
import java.util.HashMap;
import java.lang.Integer;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
	values = new HashMap<Integer, Integer>();
	waitingThreads = new HashMap<Integer, KThread>();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */
    public int exchange (int tag, int value) {
	//locking interrupts.
	boolean intStatus = Machine.interrupt().disable();
	
	//this if statement will ensure there is not another thread waiting to exchange.
	if(values.get(tag) == null)
	{
		//this is for thread A.

		//store the values into a hashmap, then sleep thread A.
		values.put(tag, value);
		//KThread.currentThread().sleep(); <- we can just sleep this thread.
		//map this thread to a hashmap and go to sleep
		waitingThreads.put(tag, KThread.currentThread());
		KThread.sleep();

		//after being woken up the new value should be in the hashmap

		//store the value locally and then remove it from the hashmap
		//so that we may use it again in the future
		int val = values.get(tag);
		values.remove(tag);

		//unlocking interrupts before returning value.
		Machine.interrupt().restore(intStatus);
		return val; 
	}
	//if there is already a thread waiting for exchange
	else
	{
		//this is for thread B.

		//retrieve the value and replace it with our current value
		int val = values.get(tag);
		values.put(tag, value);
		
		//reawaken blocked thread A and
		waitingThreads.get(tag).ready();
		//clear out the spot in the threads hashmap
		waitingThreads.remove(tag);

		//unlocking interrupts before returning value.
		Machine.interrupt().restore(intStatus);
		return val;
	}
    }

    private HashMap<Integer, Integer> values;
    private HashMap<Integer, KThread> waitingThreads;
}
