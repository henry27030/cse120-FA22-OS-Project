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
		// initialize
		// some are in COFF AND pageTable while some are not in COFF but ARE in pageTable, but all are initialized as invalid
		// COFF is executable so ReadOnly
		pageTable = new TranslationEntry[numPages];
   

		CoffSections = new int[numPages];
    Arrays.fill(CoffSections, -1);
   
		for (int i=0; i<numPages; i++) {
		  //vpn, spn, valid, readonly, used, dirty
		  pageTable[i] = new TranslationEntry(-1, -1, false, false, false, false);
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
			  //System.out.println("COFF READ ONLY?: " + pageTable[vpn].readOnly);
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
		//super.unloadSections();
		for(int i = 0; i < pageTable.length; i++)
		{
			if(pageTable[i].valid == true)
		{
			VMKernel.evictPage(pageTable[i]);
			//UserKernel.releasePage(pageTable[i].ppn);
			//pageTable[i].valid = false;
			//UserKernel.releasePage(-9);
		}
    }
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
				//pageTablePrinter();
			    pageFault(cause, Machine.processor().readRegister(Processor.regBadVAddr));
			
		      break;
		default:
			//System.out.println("DEFAULT EXCEPTION :(((((");
			super.handleException(cause);
			break;
		}
	}

	public void pageTablePrinter() {
		System.out.println("PAGETABLE START:");
		for (int i = 0; i < pageTable.length; i++) {
			System.out.println("vpn: " + i + " ppn: " + pageTable[i].ppn + " valid: " + pageTable[i].valid);
		}
		System.out.println("PAGETABLE END:");
	}
 
  public void pageFault(int cause, int badVPN)
  { 
          //System.out.println("PAGE FAULT!!!: " + cause);
			    //eviction here
			    //if there are no free pages, access from VMKernel's parent, UserKernel
			    if (VMKernel.freeAddrs.isEmpty()) {
				    VMKernel.evictPage(VMKernel.clockAlgorithm());
			    }
        //prepare requested page (once there is a free page)
		    prepRequestedPage(badVPN/pageSize);//regBadVAddr gives you the bad VA register that page faulted, so divide by pageSize to get index(rounds down since int), like in hw3
  }
  

	
	//call prepreqpage inside of read/write virt mem if vpn is invalid and check using .valid
	//use the virt addr field to check
	
	//pinn
	//if
	//super
	//unpin

  public void prepRequestedPage (int badVPN) {
	  //pageTable[badVPN] is invalid
	  //3 cases: fault on a code page, fault on a data page, fault on a stack page/args page
	  //how to tell difference between these 3 pages?
	  //first two cases, it is in the COFF file, and these files are readOnly true

  	//lock
  	//use the free pages list to get a valid ppn	
  	//
  	//unlock
  
  	//use for the loop over each coff and check first vpn and length to check if within the coff
  	//if vpn inn coff load from coff, if not then 0 fill it
    Lock lock = new Lock();
    lock.acquire();
	boolean readOnly = false;
    
    //System.out.println("Before:" + pageTable[badVPN].ppn);
	//System.out.println("Free Pages by ppn");
    //System.out.println(VMKernel.freeAddrs);
    pageTable[badVPN].ppn = VMKernel.acquirePage();
    //System.out.println("After:" + pageTable[badVPN].ppn);
	
  
    if(pageTable[badVPN].dirty == true)
    {
      VMKernel.sfRetrieve(pageTable[badVPN]);
    }
    else
    {
      if (CoffSections[badVPN] != -1) {  //if page in coff call sectionloadPage()
  		  CoffSection section = coff.getSection(CoffSections[badVPN]);//CoffSections[badVPN]=s
  		  section.loadPage(badVPN-section.getFirstVPN(), pageTable[badVPN].ppn);//badVPN-section.getFirstVPN=i from UserProcess's loadPage(i, ppn)
  	  	readOnly = section.isReadOnly();	
}
  	  else{
  		  byte[] memory = Machine.processor().getMemory();
  		  Arrays.fill(memory, pageTable[badVPN].ppn*pageSize, pageTable[badVPN].ppn*pageSize + pageSize, (byte)0);
   	  }
    }
	if (readOnly == true) {
		pageTable[badVPN].dirty = false;
	}
	else {
		pageTable[badVPN].dirty = true;
	}
    
 	  pageTable[badVPN].valid=true;
  	//System.out.println("about to fill the IPT");
  	VMKernel.invertedPageTable[pageTable[badVPN].ppn] = pageTable[badVPN];
	//pageTablePrinter();
	//problem the same ppn is being mapped to different vpn's
	//swap 4 only needs 19 pages
	//should have 19 page faults to fill those pages
    lock.release();
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
		//check validity -> use the vaddr to access the page number
		//thing is readvm can touch multiple pages
		//for each page that it CAN read
		//lenght/pagesize = number of pages = for loop iterations
			//we need to check validity
			//then if valid
				//call rvm
			//otherwise
				//this page is the bad vpn
				//pagefault with cause=1
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

		 //for now, just assume that virtual addresses equal physical addresses
		//if (vaddr < 0 || vaddr >= memory.length){
		//	System.out.println("RETURNING EARLY rvm");
		//	return 0;
		//}
		int amount = Math.min(length, memory.length - vaddr);
		//System.arraycopy(memory, vaddr, data, offset, amount);

    int iterable = 0;
    int page, diff;
    do
    {
      page = (vaddr + iterable) / pageSize; //getting virtual page
      diff = (vaddr + iterable) % pageSize; //getting offset within the page
      
      int spaceLeft = pageSize - diff; //the amount of space we can read from.
      
      if(pageTable[page].valid == false)
      {
        pageFault(1, page*pageSize);
      }
      
      VMKernel.pinnedPages[pageTable[page].ppn] = true;
      
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
      
      VMKernel.pinnedPages[pageTable[page].ppn] = false;
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
		
		//if (vaddr < 0 || vaddr >= memory.length){
		//	System.out.println("RETURNING EARLY wvm");
		//	return 0;
		//}
		int amount = Math.min(length, memory.length - vaddr);
    //System.out.println(pageTable[vaddr/pageSize].ppn + (vaddr%pageSize));
		//System.arraycopy(data, offset, memory, vaddr, amount);

    int iterable = 0;
    int page, diff;
    do
    {

      page = (vaddr + iterable) / pageSize; //getting virtual page
      diff = (vaddr + iterable) % pageSize; //getting offset within page
      
      
      if(pageTable[page].valid == false)
      {
        pageFault(1, page*pageSize);
      }
      
      VMKernel.pinnedPages[pageTable[page].ppn] = true;      
      
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
      
      VMKernel.pinnedPages[pageTable[page].ppn] = false;            
    }
    while(iterable < length);

		return iterable;
	}


	//create some global swap file
	//	in VMKernel? or a static variable in VMProcess dunno
	//create methods here to read/write swap file

  private int[] CoffSections;
  
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';


	public VMProcess parent;

}
