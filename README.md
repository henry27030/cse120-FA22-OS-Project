# UCSD CSE120 FA22 nachos starter code

- This repo contains the starter code for nachos for UCSD CSE 120 Principles of Operating Systems course for FA22 quarter.

- Please go through the README in the nachos directory for detailed information about nachos.

- Note that this code is the same as the starter code that is available as a tar file on ieng6 machines.

Project 3:
Henry
I worked on VMProcess, specifically loadSections, handleException, prepRequestedPage, and 
placed additional checks for the virtual page in read and write virtual memory fucntions.
Instead of loading the page in loadSections(), I saved the CoffSection number s 
in the same vpn index as its index in the pageTable.  Later in prepRequestedPage, 
which is given the argument badVPN, CoffSection[badVPN] would store s, and I 
used s to replicate the function calls to load the page within prepRequestedPage.
In prepRequestedPage, pages from the CoffSection and pages that were required to be 
zero-filled differed in the way that indexes that stored CoffSection pages stored s, 
a value greater than or equal to zero, and zero-filled pages stored -1.
In handleException, if a page fault occurred, we would call prepRequestedPage with the 
index of the page that faulted to handle the page fault.

Ethan
For the most part, I did the swap file implementation, but I also worked closely with Mark overall on part 2 and debugging. For the swap file, I simply used the TranslationEntry objects with a hash map to map everything out. I kept a list of free pages in the swap file, and simply added an extra page whenever we ran out of space; this should allow us to fill gaps. We all checked on each other’s work in literally every part of the project, and readVirtualMemory, writeVirtualMemory, unloadSections, and the exceptionHandler were all sort of a group effort. After writing this, Mark and I spent 2 days debugging, the vital flaw being unloadSections(). To test, we simply used the swap4, swap5, write10, and dungeon testing files. 

Mark
I worked on the section of the page fault handler that dealt with selecting a page for eviction and then evicting that page.  I created an inverted page table to keep track of all of the physical pages.  Each element was a Translation Entry to make the implementation easier.  Using the clock algorithm, I checked the invertedpagetable for pages that were recently useed and selected a page that wasn't recently used or currently being pinned as the page to evict.  This Translation Entry was then passed to an evict function that would use Ethan's work to write to the swapfile if the page was dirty (to update the swapfile with the newest version of the page) and also proceed to mark the owner process' translation entry as invalid.  In addition, we worked together to debug duplicate valid vpn's assigned to more than 1 vpn which ended up being an error in unloadSections().  We also modified readVirtualMemory() and writeVirtualMemory() to call the page fault handler for an invalid page when looping through the pages and also pinned each page while looking at that page.  To test, various print statements to test crash locations, methods to print the pageTable and printing the List of free pages, and the given swap4, swap5, and write10 tests helped to isolate the areas of broken code.  We tested with various memory sizes from 8 to 64 and testing dungeon with 8 pages gave me the confidence I needed to be assured that our code actually worked. :)  Thank you professor Voelker.
