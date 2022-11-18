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
      pageTable[i].valid = false; //replace loadPage with setting as not valid
      pageTable[i].readOnly = false;
    }
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
        pageTable[vpn].readOnly = section.isReadOnly();
        CoffSections[vpn]=s;//to match to the pageTable index to load in the case of a page fault
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
      prepRequestedPage(badVPN);
      break;
		default:
			super.handleException(cause);
			break;
		}
	}
  protected void prepRequestedPage (int badVPN) {
    //pageTable[badVPN] is invalid
    //3 cases: fault on a code page, fault on a data page, fault on a stack page/args page
    //how to tell difference between these 3 pages?
    //first two cases, it is in the COFF file, and these files are readOnly true
    if (pageTable[badVPN].readOnly==true) {
      CoffSection section = coff.getSection(CoffSections[badVPN]);
      section.loadPage(badVPN-section.getFirstVPN(), pageTable[badVPN].ppn);//badVPN-section.getFirstVPN=i from loadSection's loadPage usage
    } 
    else {
      byte[] memory = Machine.processor().getMemory();
      Arrays.fill(memory, pageTable[badVPN].ppn*pageSize, pageTable[badVPN].ppn*pageSize + pageSize, (byte)0);
    }
    pageTable[badVPN].valid=true;
  }

  private int[] CoffSections;
  
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
