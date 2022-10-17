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
    
    //this while thead is simply to wait if another exchange is happening   
    while(values.get(tag) != null && waitingThreads.get(tag) == null)
    {
      KThread.yield();
    }
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
    //clear out the spot in the threads hashmap here
		waitingThreads.remove(tag);

		//unlocking interrupts before returning value.
		Machine.interrupt().restore(intStatus);
		return val;
	}
  
    }

    private HashMap<Integer, Integer> values;
    private HashMap<Integer, KThread> waitingThreads;
    
    // Place Rendezvous test code inside of the Rendezvous class.

    public static void rendezTest1() {
  	  final Rendezvous r = new Rendezvous();
  
  	  KThread t1 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 0;
  		    int send = -1;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
    	t1.setName("t1");
    	KThread t2 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 0;
  		    int send = 1;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t2.setName("t2");
  
  	  t1.fork(); t2.fork();
  	  // assumes join is implemented correctly
    	t1.join(); t2.join();
    }
    
    public static void rendezTest3() {
      final Rendezvous r = new Rendezvous();
  
  	  KThread t1 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = -1;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
    	t1.setName("t1");
     
      KThread t2 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = 1;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t2.setName("t2");
     
      KThread t3 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = 2;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == -2, "Was expecting " + 4 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t3.setName("t3");
       
      KThread t4 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = -2;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == 2, "Was expecting " + 3 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t4.setName("t4");
       
      t1.fork(); t2.fork();
      t1.join(); t2.join();
      
      t3.fork(); t4.fork();
      t3.join(); t4.join();
    }
    
    public static void rendezTest2() {
      final Rendezvous r = new Rendezvous();
  
  	  KThread t1 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = -1;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
    	t1.setName("t1");
     
      KThread t2 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = 1;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t2.setName("t2");
     
      KThread t3 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 2;
  		    int send = 3;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == 4, "Was expecting " + 4 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t3.setName("t3");
       
      KThread t4 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 2;
  		    int send = 4;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r.exchange (tag, send);
  		    Lib.assertTrue (recv == 3, "Was expecting " + 3 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t4.setName("t4");
       
      t1.fork(); t2.fork();
      t1.join(); t2.join();
      
      t3.fork(); t4.fork();
      t3.join(); t4.join();
    }
    
    public static void rendezTest4() {
      final Rendezvous r1 = new Rendezvous();
      final Rendezvous r2 = new Rendezvous();
  
  	  KThread t1 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = -1;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r1.exchange (tag, send);
  		    Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
    	t1.setName("t1");
     
      KThread t2 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = 2;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r2.exchange (tag, send);
  		    Lib.assertTrue (recv == 4, "Was expecting " + 4 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t2.setName("t2");
     
      KThread t3 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = 1;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r1.exchange (tag, send);
  		    Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t3.setName("t3");
       
      KThread t4 = new KThread( new Runnable () {
  		public void run() {
  		    int tag = 1;
  		    int send = 4;
  
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send + " on tag " + tag);
  		    int recv = r2.exchange (tag, send);
  		    Lib.assertTrue (recv == 2, "Was expecting " + 2 + " but received " + recv);
  		    System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
  		}
  	    });
  	  t4.setName("t4");
       
      t1.fork(); t2.fork();
      t1.join(); t2.join();
      
      t3.fork(); t4.fork();
      t3.join(); t4.join();
    }        

    
    
    // Invoke Rendezvous.selfTest() from ThreadedKernel.selfTest()

    public static void selfTest() {
	    // place calls to your Rendezvous tests that you implement here
	    //rendezTest1();
      rendezTest4();
    }
}
