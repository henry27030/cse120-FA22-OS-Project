# UCSD CSE120 FA22 nachos starter code

- This repo contains the starter code for nachos for UCSD CSE 120 Principles of Operating Systems course for FA22 quarter.

- Please go through the README in the nachos directory for detailed information about nachos.

- Note that this code is the same as the starter code that is available as a tar file on ieng6 machines.

Project 2:

Henry: I worked on part 1 of the project.

When I implemented the functions of creat, open, read, write, close, and unlink, when a function call was able to return -1 or null, such as the OpenFile functions or UserProcess functions, I would check for that return value and return a -1 based on the context of the return value. This was done to "bullet-proof" the nachos kernel from user program errors. Afterwards, I tested the code against the given tests, changed around some values for the tests, and wrote additional checks for the parameter, such as returning -1 if the index argument accessed an incorrect index or an index that wasn't part of the 16 possible file descriptors.

For handleCreat, we're given a virtual address parameter, so we have to read the string that represents the name of the file from virtual memory. This was done using readVirtualMemoryString. To accommodate the instruction of truncating the file, I removed the file and created a new one regardless if the file was just created or if the file existed already with the use of the ThreadedKernel.filesystem's open() and remove().
handleOpen was created with the same logic as handleCreat, but, if the filesystem could not open the file, it wouldn't create a new file.

handleRead used a while loop to read in from a file size amount of bytes or until no more bytes could be read from the file. Each time the loop would read in pageSize(the max size of a page given by the processor) amount of bytes, or if the condition was met that we were approaching the size amount of written bytes where size<pageSize, we would instead read in the remainder of the amount of bytes required to read. The loop used OpenFile.read to read from the file at the fd argument index of the file descriptors into a buffer and writeVirtualMemory to write from that buffer into the virtual memory given by buffer argument.

handleWrite used a similar while loop as handleRead but the contents of the loop and the condition of the loop were different. This function would write from a file to a virtual address size amount of bytes. This loop would regularly terminate if there was nothing left to write from the file or if the number of bytes to write were met. However, if the number of bytes read from the file didn't match the number of bytes written to virtual memory, this would be an error, so we returned -1. The loop used readVirtualMemory to read from virtual memory given by buffer argument into a buffer, and used OpenFile.write to write to the file at the fd argument index of the file descriptors.
handleClose closed the file descriptor at the argument fd index of the file descriptors and nulled that index to allow another file descriptor to take its place.
handleUnlink made use of handleClose, since if the process is unlinking the file, the process must be done with the file as well, and additionally removed the file.

Ethan Wang: I did part 2 of the project.

To start with, I went into UserKernel and implemented a linked list of free addresses and a lock. I went through all the physical pages and added it to the linked list, then implemented a method to acquire a free page and one to release a free page.
In UserProcess, I added a loop creating the page table for virtual memory by simple acquiring addresses using my method in UserKernel, and then loading sections in via the page table. To unload, I simply freed all memory from the page table.
Lastly for read and write, I used the page table to translate virtual memory addresses to physical and then to read or write page by page. This was mechanically simple, just checking how far into the virtual page our address was and continuing from there in our physical equivalent and copying it for the rest of the page or the length, whichever was shorter.
In order to test, I switched my acquirePage() function in the UserKernel to grab the pages in reverse order, and then ran all the tests from Part1. Doing so ensured that I could read and write memory that was non contiguous physically, but do so contiguously virtually.

Mark: I did part 3 of the project.

In handleExit(), I first transfer the given status to a static HashMap<PID, status> and depending on whether a flag exitNormal is 0 or 1, a valid or a null status is inserted.  I unloadSections() and close all files.  Finally, a disown all children by checking a static HashMap<PID, child> for children whose parent’s PID matches the current processes’ PID.  If the current processes’ PID = 0, then it is the root process and terminate() is called.

In handleJoin(), I first check the static HashMap<PID, child> if the child of specific PID exists.  If it doesn’t, I return early with -1.  Otherwise, I continue and call join(), remove the child from the static Hashmap<PID, child>, and set its parent reference to null.  I then grab the exit status associated with this child from the static HashMap<PID, status> and write it to virtual memory with the given address.  If all is well, then return successfully.

In handleExec(), I check for valid inputs and for a valid filename when I readVirtualMemoryString() with the provided address.  I then proceed to read argc number of arguments and check their validity as well.  Finally, I create a new child process with unique PID, insert it into the static HashMap<PID, child> and call execute on the child with a valid filename and arguments.  I then return the child’s PID.

For testing, I developed my programs around the tests themselves and solidify my understanding of how these methods worked.  I used the given join test to confirm how exec(), exit(), and join() were all related.  With the except test, I traced execution back to the default case in handleException() and saw that handleExit() could handle the process’ exit so as not to cause an unhandled exception.
