package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.*;

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

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
    ThreadedKernel.fileSystem.remove("EMHSwapFile");
    swapFile = ThreadedKernel.fileSystem.open("EMHSwapFile", true);
    
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
    int spn = 0;
    if (pageLocations.get(entry) != null) {
        spn = pageLocations.get(entry);
    }
    else {
        spn = sfFreePages.removeFirst();
    }
    
    if(sfFreePages.isEmpty())
    {
      sfFreePages.addLast(pgsOnDisk);
      pgsOnDisk++;
    }
    
    byte[] memory = Machine.processor().getMemory();
    int pageSize = Machine.processor().pageSize;
    
    swapFile.write(spn*pageSize, memory, entry.ppn*pageSize, pageSize);
    //might need to do stuff to the translation entry?
    
    pageLocations.put(entry, spn);
	  return spn;
  }
  

  //paremeters: spn, ppn
  //sfRetrieve writes the data from an spn to the ppn
  //specified in the translationEntry
  public static void sfRetrieve(TranslationEntry entry)
  {
    byte[] memory = Machine.processor().getMemory();
    int pageSize = Machine.processor().pageSize;
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
			if (invertedPageTable[clockIndex] != null) {
				if (invertedPageTable[clockIndex].used) {
					invertedPageTable[clockIndex].used = false;
					clockIndex = (clockIndex+1) % Machine.processor().getNumPhysPages();
				}
				else {
					//we found a page to evict
					page = invertedPageTable[clockIndex];
					clockIndex = (clockIndex+1) % Machine.processor().getNumPhysPages();
					break;
				}
			}
			else {
				clockIndex = (clockIndex+1) % Machine.processor().getNumPhysPages();
				System.out.println("in an infinite loop " + clockIndex);
			}
		}
   
		lock.release();
		return page;
	}

	public static void evictPage(TranslationEntry page) {
		
    if (page.dirty) {
		VMKernel.sfSave(page);
		
			//if the page is not in the swapfile
			//if (page.valid == false) {
				//page.vpn = VMKernel.sfSave(page);
				//append a page to the swapfile or create it if it dun exist
				//what's that swapfile page's spn?
				//page.vpn = thatpage's spn
				//write to swap[vpn]
			//}
			//if the page is in the swapfile
			//else {
				//does the same thing
				//VMKernel.sfSave(page);
				//write to swap[vpn] the data currently in physical memory[page.vpn]
			//}
		}
    invertedPageTable[page.ppn].valid = false;
    freeAddrs.addLast(page.ppn);
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
 
  private static OpenFile swapFile;
  //spn
  private static LinkedList<Integer> sfFreePages;
  //entry, spn
  private static HashMap<TranslationEntry, Integer> pageLocations;
}
