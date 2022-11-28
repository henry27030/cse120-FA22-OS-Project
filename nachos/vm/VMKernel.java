package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
		cv = new Condition(lock);
		invertedPageTable = new ArrayList<Pair<Integer, TranslationEntry>>();

	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	public static int clockAlgorithm() {
		lock.acquire();
		//what type is a page
				

		int page = 0;
	
		while (true) {
			if (pinnedPages[invertedPageTable.get(startIndex).getKey()]) {				
				startIndex = (startIndex+1) % Machine.processor().getNumPhysPages();
				continue;
			}
			if (invertedPageTable.get(startIndex).getValue().used == true) {
				invertedPageTable.get(startIndex).getValue().used = false;
				startIndex = (startIndex+1) % Machine.processor().getNumPhysPages();
			}
			else {
				if (numPinnedPages == Machine.processor().getNumPhysPages()) {
					cv.sleep();
				}
				else {
					//we found a page to evict
					page = invertedPageTable.get(startIndex).getKey();
					startIndex = (startIndex+1) % Machine.processor().getNumPhysPages();
					break;
				}
			}
		}
		lock.release();
		return page;
	}

	public static void evictPage(int page) {
		HashMap<Integer, TranslationEntry> hmIPT = new HashMap<Integer, TranslationEntry>();
		for (Pair p : invertedPageTable) {
			hmIPT.put(p.getKey(), p.getValue());
		}

		if (hmIPT.get(page).dirty == true) {
			
		}		
	}

	private static int startIndex = 0;

	//ppn, if pinned
	private static HashMap<Integer, bool> pinnedPages;
	
	public static int numPinnedPages = 0;

	//ppn, process

	public static ArrayList<Pair<Integer, TranslationEntry>> invertedPageTable;

	public Condition cv;




	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';
}
