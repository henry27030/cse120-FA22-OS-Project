package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
    
    processFileDescriptors = new OpenFile[16];//every file has tablesize 16 of file descriptors
    //index 0 for reading, index 1 for writing
    processFileDescriptors[0] = UserKernel.console.openForReading();
    processFileDescriptors[1] = UserKernel.console.openForWriting();
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
	        String name = Machine.getProcessClassName ();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Hack around it by hard-coding
		// creating new processes of the appropriate type.

		if (name.equals ("nachos.userprog.UserProcess")) {
		    return new UserProcess ();
		} else if (name.equals ("nachos.vm.VMProcess")) {
		    return new VMProcess ();
		} else {
		    return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		thread = new UThread(this);
		thread.setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
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
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(memory, vaddr, data, offset, amount);

		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
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
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(data, offset, memory, vaddr, amount);

		return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				section.loadPage(i, vpn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	/**
	 * Handle the exit() system call.
	 */
	private int handleExit(int status) {
	        // Do not remove this call to the autoGrader...
		Machine.autoGrader().finishingCurrentProcess(status);
		// ...and leave it as the top of handleExit so that we
		// can grade your implementation.

		Lib.debug(dbgProcess, "UserProcess.handleExit (" + status + ")");
		// for now, unconditionally terminate with just one process
		Kernel.kernel.terminate();

		return 0;
	}

/**
 * Attempt to open the named disk file, creating it if it does not exist,
 * and return a file descriptor that can be used to access the file. If
 * the file already exists, creat truncates it.
 *
 * Note that creat() can only be used to create files on disk; creat() will
 * never return a file descriptor referring to a stream.
 *
 * Returns the new file descriptor, or -1 if an error occurred.
 */
  private int handleCreat(int name) {
    String namee;
    boolean removeForTruncate = false;
    OpenFile fileToCreat;
    
    //name refers to virtual address, so use readVirtualMemoryString
    namee = readVirtualMemoryString(name,256); //maximum length for strings passed as args to syscalls is 256
    //namee refers to a file name if it's !=null, if it is null, return error
    if (namee == null) {
      return -1;
    }
    fileToCreat = ThreadedKernel.fileSystem.open(namee, true); //UserProcess.load uses this;
                                                               //creates new file if doesn't already exist
    
    //handle case that we don't have permission access to file(rwe)
    if (fileToCreat==null){
      return -1;
    }
    //the file exists already or has just been created
    else {
      //truncate the file in the case that it already exists
      removeForTruncate=ThreadedKernel.fileSystem.remove(namee);
      //in the case that we aren't able to remove the file
      if (removeForTruncate==false) {
        return -1;
      }
      fileToCreat = ThreadedKernel.fileSystem.open(namee, true);
      
      //add the fileToCreat to process's file descriptors
      for (int i=2; i<processFileDescriptors.length; i++) {//i=2 since we don't want to change reading/writing
        if (processFileDescriptors[i] == null) {
          processFileDescriptors[i]=fileToCreat;
          return i;
        }
      }
    }
    //in the case that this has been reached, processFileDescriptors is full, so return an error
    ThreadedKernel.fileSystem.remove(namee);
    return -1;
  }

/**
 * Attempt to open the named file and return a file descriptor.
 *
 * Note that open() can only be used to open files on disk; open() will never
 * return a file descriptor referring to a stream.
 *
 * Returns the new file descriptor, or -1 if an error occurred.
 */
  private int handleOpen(int name) {//same as handleCreat but doesn't create a new file nor truncate
    String namee;
    OpenFile fileToOpen;
    
    //name refers to virtual address, so use readVirtualMemoryString
    namee = readVirtualMemoryString(name,256); //maximum length for strings passed as args to syscalls is 256
    //namee refers to a file name if it's !=null, if it is null, return error
    if (namee == null) {
      return -1;
    }
    fileToOpen = ThreadedKernel.fileSystem.open(namee, false); //UserProcess.load uses this;
                                                               //doesn't create new file if doesn't already exist
    
    //handle case that we don't have permission access to file(rwe) or fileToOpen doesn't refer to a file after open()
    if (fileToOpen==null){
      return -1;
    }
    //the file exists already
    else {  
      //add the fileToOpen to process's file descriptors
      for (int i=2; i<processFileDescriptors.length; i++) {//i=2 since we don't want to change reading/writing
        if (processFileDescriptors[i] == null) {
          processFileDescriptors[i]=fileToOpen;
          return i;
        }
      }
    }
    //in the case that this has been reached, processFileDescriptors is full, so return an error
    ThreadedKernel.fileSystem.remove(namee);
    return -1;
  }

/**
 * Attempt to read up to size bytes into buffer from the file or stream
 * referred to by fileDescriptor.
 *
 * On success, the number of bytes read is returned. If the file descriptor
 * refers to a file on disk, the file position is advanced by this number.
 *
 * It is not necessarily an error if this number is smaller than the number of
 * bytes requested. If the file descriptor refers to a file on disk, this
 * indicates that the end of the file has been reached. If the file descriptor
 * refers to a stream, this indicates that the fewer bytes are actually
 * available right now than were requested, but more bytes may become available
 * in the future. Note that read() never waits for a stream to have more data;
 * it always returns as much as possible immediately.
 *
 * On error, -1 is returned, and the new file position is undefined. This can
 * happen if fileDescriptor is invalid, if part of the buffer is read-only or
 * invalid, or if a network stream has been terminated by the remote host and
 * no more data is available.
 */
  private int handleRead(int fd, int buffer, int size) {
    byte[] bytesReadFromFile = new byte[pageSize];//pagesize is the number of bytes of a page of the processor
    OpenFile fileToRead;
    int max = size;
    int currRead = 0;
    int finalRead = 0;
    int amountToRead = 0;
    //checking for valid buffer and size
    if (readVirtualMemoryString(buffer,256) == null || size>(32*1024)) {
      return -1;
    }
    if (size<0) {//check for negative size
      return -1;
    }
    if (fd!=0 && !(fd>=2 && fd<=15)) {//fd can be 0 or >2 && less than size of array, [0] is standard input, [1] is standard output
      return -1;
    }
    //no OpenFile has been stored at this index of processFileDescriptors
    if (processFileDescriptors[fd]==null) {
      return -1;
    }
    fileToRead = processFileDescriptors[fd];
    //read pageSize amount of bytes at a time from the file and write to buffer up to size amount of bytes
    while (max>0) {
      amountToRead=pageSize;//read in pageSize bytes each time
      if (max<pageSize) { //in the case that pageSize of bytes is more than the amount we want to read(size)
        amountToRead=max;
      }
      currRead = fileToRead.read(bytesReadFromFile, 0, amountToRead);
      if (currRead==-1) {
        return -1;//error in OpenFile.read();
      }
      if (currRead==0) { //in the case that the end of the file has been reached
        break;
      }
      writeVirtualMemory(buffer, bytesReadFromFile, 0, currRead);//write from bytesReadFromFile to buffer
      max-=currRead;
      finalRead+=currRead;
      buffer+=currRead;//move buffer
    }    
    return finalRead;
  }


/**
 * Attempt to write up to size bytes from buffer to the file or stream
 * referred to by fileDescriptor. write() can return before the bytes are
 * actually flushed to the file or stream. A write to a stream can block,
 * however, if kernel queues are temporarily full.
 *
 * On success, the number of bytes written is returned (zero indicates nothing
 * was written), and the file position is advanced by this number. It IS an
 * error if this number is smaller than the number of bytes requested. For
 * disk files, this indicates that the disk is full. For streams, this
 * indicates the stream was terminated by the remote host before all the data
 * was transferred.
 *
 * On error, -1 is returned, and the new file position is undefined. This can
 * happen if fileDescriptor is invalid, if part of the buffer is invalid, or
 * if a network stream has already been terminated by the remote host.
 */
  private int handleWrite(int fd, int buffer, int size){
    byte[] bytesReadFromVM = new byte[pageSize];//pagesize is the number of bytes of a page of the processor
    OpenFile fileToWrite;
    int max = size;
    int currWrote = 0;
    int finalWrote = 0;
    int amountToWrite = 0;
    int wroteToFile;
    //checking for valid buffer and size
    if (readVirtualMemoryString(buffer,256) == null || size>(32*1024)) {
      return -1;
    }
    if (size<0) {//check for negative size
      return -1;
    }
    if (!(fd>=1 && fd<=15)) {//fd must be file descriptors index 1->15
      return -1;
    }
    //no OpenFile has been stored at this index of processFileDescriptors
    if (processFileDescriptors[fd]==null) {
      return -1;
    }
    fileToWrite = processFileDescriptors[fd];
    //write pageSize amount of bytes at a time from buffer to file
    while (max>0) {
      amountToWrite=pageSize;//write pageSize bytes each time
      if (max<pageSize) { //in the case that pageSize of bytes is more than the amount, we want to write size bytes
        amountToWrite=max;
      }
      currWrote = readVirtualMemory(buffer, bytesReadFromVM, 0, amountToWrite);//read from virtual memory
      if (currWrote==0) {//nothing left to read from VM
        break;
      }
      wroteToFile = fileToWrite.write(bytesReadFromVM, 0, currWrote); //write to file
      //in this case we were not able to write everything from VM to the file or there's an error in OpenFile.write();
      if (wroteToFile==-1 || currWrote!=wroteToFile) {
        return -1;
      }
      max-=wroteToFile;
      finalWrote+=wroteToFile;
      buffer+=wroteToFile;//move buffer
    }
    return finalWrote;
  }
  
/**
 * Close a file descriptor, so that it no longer refers to any file or
 * stream and may be reused. The resources associated with the file
 * descriptor are released.
 *
 * Returns 0 on success, or -1 if an error occurred.
 */
  private int handleClose(int fd) {
    //checking for valid fd
    if (!(fd>=0 && fd<=processFileDescriptors.length-1)) {
      return -1;
    }
    //checking if trying to close null fd
    if (processFileDescriptors[fd]==null) {
      return -1;
    }
    //close the fd, null it so another fd can take its place
    processFileDescriptors[fd].close();
    processFileDescriptors[fd]=null;
    return 0;
  }

/**
 * Delete a file from the file system. 
 *
 * If another process has the file open, the underlying file system
 * implementation in StubFileSystem will cleanly handle this situation
 * (this process will ask the file system to remove the file, but the
 * file will not actually be deleted by the file system until all
 * other processes are done with the file).
 *
 * Returns 0 on success, or -1 if an error occurred.
 */
  private int handleUnlink(int name) {
    String namee;
    boolean removed=false;
    
    //name refers to virtual address, so use readVirtualMemoryString
    namee = readVirtualMemoryString(name,256); //maximum length for strings passed as args to syscalls is 256
    //namee refers to a file name if it's !=null, if it is null, return error
    if (namee == null) {
      return -1;
    }
    //if this process is unlinking this file, it is done with the file, so close it
    for (int i=0; i<processFileDescriptors.length; i++) {//i=0 since user process allowed to close these descriptors: [0], [1]
      if (processFileDescriptors[i] !=null && processFileDescriptors[i].getName()==namee) {
        handleClose(i);//close the fd at that index
      }
    }
    removed=ThreadedKernel.fileSystem.remove(namee);//remove the file
    if (removed==true) {//check if it's removed or not
      return 0;
    }
    //in the case that file was not able to be removed, we reach this
    return -1;
  }

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			return handleExit(a0);
    case syscallCreate:
      return handleCreat(a0);
    case syscallOpen:
      return handleOpen(a0);
    case syscallRead:
      return handleRead(a0, a1, a2);
    case syscallWrite:
      return handleWrite(a0, a1, a2);
    case syscallClose:
      return handleClose(a0);
    case syscallUnlink:
      return handleUnlink(a0);

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
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
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	/** The thread that executes the user-level program. */
        protected UThread thread;
    
	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';
 
  //added
  OpenFile[] processFileDescriptors;
}