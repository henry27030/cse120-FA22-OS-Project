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
