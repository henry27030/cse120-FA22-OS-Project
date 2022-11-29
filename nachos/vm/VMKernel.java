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
	}
	
	//Initialize the static variables here
	static {

	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
    ThreadedKernel.fileSystem.remove("EMHSwapFile");
    swapFile = ThreadedKernel.fileSystem.open("EMHSwapFile");
    
    sfFreePages = new LinkedList<Integer>();
    sfFreePages.addFirst(0);
    pgsOnDisk++;
    
    pageLocations = new HashMap<TranslationEntry, Integer>();
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
 
  //parameters: entry
  //return: spn
  public static int sfSave(TranslationEntry entry)
  {
    int spn = sfFreePages.removeFirst();
    
    if(SFreePages.isEmpty())
    {
      SFreePages.addLast(currPages);
      pgsOnDisk++;
    }
    
    byte[] memory = Machine.processor().getMemory();
    int pageSize = processor.pageSize;
    
    swapFile.write(spn*pageSize, memory, entry.ppn*pageSize, pageSize);
    //might need to do stuff to the translation entry?
    
    pageLocations.put(entry, spn);
  }
  

  //paremeters: spn, ppn
  //sfRetrieve writes the data from an spn to the ppn
  //specified in the translationEntry
  public static void sfRetrieve(TranslationEntry entry)
  {
    byte[] memory = Machine.processor().getMemory();
    int pageSize = processor.pageSize;
    int spn = pageLocations.get(entry);
    
    swapFile.read(spn*pageSize, memory, entry.ppn*pageSize, pageSize);
  }
  
  //spn to clear
  //clears a spot on the swapFile
  public static void sfClear(TranslationEntry entry)
  {
    sfFreePages.addFirst(pageLocations.get(entry));
    pageLocations.remove(entry);
  }

  public static TranslationEntry clockAlgorithm() {
		lock.acquire();
		//what type is a page
				

		TranslationEntry page = null;
	
		while (true) {
			if (numPinnedPages == Machine.processor().getNumPhysPages()) {
				cv.sleep();
			}
			if (pinnedPages[clockIndex]) {				
				clockIndex = (clockIndex+1) % Machine.processor().getNumPhysPages();
				continue;
			}
			if (invertedPageTable[clockIndex].used) {
				invertedPageTable[clockIndex].used = false;
				clockIndex = (clockIndex+1) % Machine.processor().getNumPhysPages();
			}
			else {
				if (numPinnedPages == Machine.processor().getNumPhysPages()) {
					cv.sleep();
				}
				else {
					//we found a page to evict
					page = invertedPageTable[clockIndex];
					clockIndex = (clockIndex+1) % Machine.processor().getNumPhysPages();
					break;
				}
			}
		}
		lock.release();
		return page;
	}

	public static void evictPage(TranslationEntry page) {
		if (page.dirty) {
			//if the page is not in the swapfile
			if (page.vpn == -1) {
				//append a page to the swapfile or create it if it dun exist
				//what's that swapfile page's spn?
				//page.vpn = thatpage's spn
			}
			//write to swap[vpn] the data currently in physical memory[page.vpn]
			invertedPageTable[page.vpn].valid = false;
		}	
	}

  
  	//used to iterate through the invertedPageTable, also serves as the ppn since the index of the invertedPageTable is the ppn
	private static int clockIndex = 0;

	//array of pinnedPages, with ppn as index, boolean as elements
	public static boolean[] pinnedPages = new boolean[Machine.processor().getNumPhysPages()];
	
	public static int numPinnedPages = 0;

	//invertedPageTable, with ppn as index, TranslationEntry as elements
	public static TranslationEntry[] invertedPageTable = new TranslationEntry[Machine.processor().getNumPhysPages()];



	public static Condition cv;
  
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';
 
  //number of pages on the disk
  private static int pgsOnDisk = 0;
 
  private OpenFile swapFile;
  //spn
  private LinkedList<Integer> sfFreePages;
  //entry, spn
  private HashMap<TranslationEntry, Integer> pageLocations;
}
