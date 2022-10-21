package nachos.threads;

import java.util.HashMap;
import nachos.machine.*;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
	    values = new HashMap<Integer, Integer>();
      myLock = new Lock();
      myCondition = new Condition2(myLock);
      inUse = false;
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
      //acquire the lock
      myLock.acquire();
      
      //if an exchange is happening
      if(inUse)
      {
        myCondition.sleep(); //add to the backlog of myCondition
              //lock is automatically reacquired after waking up
      }
      
  	  //this if statement will ensure there is not another thread waiting to exchange.
	    if(values.get(tag) == null)
	    {
    		//this is for thread A.
	    	values.put(tag, value);//store the values into a hashmap,
   	    myCondition.sleep();//then sleep thread A.
        //lock returns after waking up and 
        //we will release it later after waking up other threads
        
	    	//after being woken up the new value should be in the hashmap
                
	    	int val = values.get(tag); //store the value locally
	    	values.remove(tag); //and then remove it from the hashmap
                            //so that we may use it again in the future
                      
        inUse = false; //exchange is completed at this point 
        myCondition.wake(); //wake up any threads that tried to interrupt when we
                            //were doing the exchange
        myLock.release();//and release the lock

	  	  return val; 
  	  }
	    else //if there is already a thread waiting for exchange
    	{
		    //this is for thread B.
	  	   
	  	  int val = values.get(tag); //retrieve the value
	  	  values.put(tag, value);    //and replace it with our current value
                 
        inUse = true; //indicate an exchange is happening
        myCondition.wake(); //wake up stopped thread
        myLock.release(); //and release the lock
        
	  	  return val;
	    }
  
    }

    private HashMap<Integer, Integer> values;
    private Lock myLock;
    private Condition2 myCondition;
    private boolean inUse;
    
    // Place Rendezvous test code inside of the Rendezvous class.

    public static void rendezTest1() {
      //testing a simple exchange
      System.out.println("Test 1 Starting");
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
      System.out.println("Test 1 Complete");
    }
    
    public static void rendezTest3() {
      //testing running on the same tag consecatively
      System.out.println("Test 3 Starting");
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
      System.out.println("Test 3 Complete");
    }
    
    public static void rendezTest2() {
      //tests exchanging on multiple tags
      System.out.println("Test 2 Starting");
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
      System.out.println("Test 2 Complete");
    }
    
    public static void rendezTest4() {
      //testing different rendezvous objects
      System.out.println("Test 4 Starting");
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
      t3.fork(); t4.fork();
      
      t1.join(); t2.join();
      t3.join(); t4.join();
      
      System.out.println("Test 4 Complete");
    }        
    
    
    // Invoke Rendezvous.selfTest() from ThreadedKernel.selfTest()
 
    public static void selfTest() {
	    // place calls to your Rendezvous tests that you implement here
      rendezTest1();
      rendezTest2();
      rendezTest3();
      rendezTest4();
    }
}
