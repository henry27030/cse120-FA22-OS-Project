package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.Arrays;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
    // use the code from UserProcess.loadSections without loading from COFF file
    // check for memory
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
    // initialize
    // some are in COFF AND pageTable while some are not in COFF but ARE in pageTable, but all are initialized as invalid
    // COFF is executable so ReadOnly
    pageTable = new TranslationEntry[numPages];
    CoffSections = new int[numPages];
    for (int i=0; i<numPages; i++) {
      pageTable[i] = new TranslationEntry(i, UserKernel.acquirePage(), true, false, false, false);
      pageTable[i].valid = false; //replace with setting as not valid
      pageTable[i].readOnly = false;
    }
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
        pageTable[vpn].readOnly = section.isReadOnly();
        CoffSections[vpn]=s;// to match to the pageTable index that is set to readOnly to load in the case of a page fault
        //set to s so that we can call loadPage later in a similar to way to initial implementation
      }
    }
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
    case Processor.exceptionPageFault:
      //prepare requested page
      int badVPN = Machine.processor().readRegister(Processor.regBadVAddr);
      prepRequestedPage(badVPN/pageSize);//regBadVAddr gives you the bad VA register, so divide by pageSize to get index, like in hw3
      break;
		default:
			super.handleException(cause);
			break;
		}
	}
  public void prepRequestedPage (int badVPN) {
    //pageTable[badVPN] is invalid
    //3 cases: fault on a code page, fault on a data page, fault on a stack page/args page
    //how to tell difference between these 3 pages?
    //first two cases, it is in the COFF file, and these files are readOnly true
    if (pageTable[badVPN].readOnly==true) {
      CoffSection section = coff.getSection(CoffSections[badVPN]);//CoffSections[badVPN]=s
      section.loadPage(badVPN-section.getFirstVPN(), pageTable[badVPN].ppn);//badVPN-section.getFirstVPN=i from UserProcess's loadPage(i, ppn)
    } 
    
    else {
      byte[] memory = Machine.processor().getMemory();
      Arrays.fill(memory, pageTable[badVPN].ppn*pageSize, pageTable[badVPN].ppn*pageSize + pageSize, (byte)0);
    }
    pageTable[badVPN].valid=true;
  }
	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
/*
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
*/
    //checking for valid virtual page conditions
    if (!(offset >= 0 && length >= 0 && offset + length <= data.length) ||
        data==null ||
        vaddr<0 ||
        vaddr+length>pageTable.length*pageSize) {
      return -1;
    }

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		//System.arraycopy(memory, vaddr, data, offset, amount);

    int iterable = 0;
    int page, diff;
    do
    {

      page = (vaddr + iterable) / pageSize; //getting virtual page
      diff = (vaddr + iterable) % pageSize; //getting offset within the page
      
      int spaceLeft = pageSize - diff; //the amount of space we can read from.
      
      //is the case where we have too much in our length to write
      if(spaceLeft < length - iterable)
      {
        //System.out.println(iterable + " " + pageSize + " " + spaceLeft + " " + (length - iterable));
        System.arraycopy(memory, pageTable[page].ppn*pageSize + diff, data, offset+iterable, spaceLeft);
        iterable += spaceLeft;
      }
      else //is the case where we can write all that's left within the page
      {
        System.arraycopy(memory, pageTable[page].ppn*pageSize + diff, data, offset+iterable, length - iterable);
        iterable += length - iterable;
      }
    }
    while(iterable < length);

		return iterable;
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 *
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
/*
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
*/
    //checking for valid virtual page conditions
    if (!(offset >= 0 && length >= 0 && offset + length <= data.length) ||
        data==null ||
        vaddr<0 ||
        vaddr+length>pageTable.length*pageSize) {
      return -1;
    }

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
    //System.out.println(pageTable[vaddr/pageSize].ppn + (vaddr%pageSize));
		//System.arraycopy(data, offset, memory, vaddr, amount);

    int iterable = 0;
    int page, diff;
    do
    {

      page = (vaddr + iterable) / pageSize; //getting virtual page
      diff = (vaddr + iterable) % pageSize; //getting offset within page
      
      int spaceLeft = pageSize - diff; //the amount of space we have left to write to.
      
      //is the case where we have too much in our length to write
      if(spaceLeft < length - iterable) //if we have to write more than we have space in the page for

      {
        //System.out.println(iterable + " " + pageSize + " " + spaceLeft + " " + (length - iterable));
        System.arraycopy(data, offset+iterable, memory, pageTable[page].ppn*pageSize + diff, spaceLeft);
        iterable += spaceLeft;
      }
      else //is the case where we can write all that's left within the page
      {
        System.arraycopy(data, offset+iterable, memory, pageTable[page].ppn*pageSize + diff, length - iterable);
        iterable += length - iterable;
      }
    }
    while(iterable < length);

		return iterable;
	}

  private int[] CoffSections;
  
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
