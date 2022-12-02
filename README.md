# UCSD CSE120 FA22 nachos starter code

- This repo contains the starter code for nachos for UCSD CSE 120 Principles of Operating Systems course for FA22 quarter.

- Please go through the README in the nachos directory for detailed information about nachos.

- Note that this code is the same as the starter code that is available as a tar file on ieng6 machines.

Project 3:

Ethan
For the most part, I did the swap file implementation, but I also worked closely with Mark overall on part 2 and debugging. For the swap file, I simply used the TranslationEntry objects with a hash map to map everything out. I kept a list of free pages in the swap file, and simply added an extra page whenever we ran out of space; this should allow us to fill gaps. We all checked on each other’s work in literally every part of the project, and readVirtualMemory, writeVirtualMemory, unloadSections, and the exceptionHandler were all sort of a group effort. After writing this, Mark and I spent 2 days debugging, the vital flaw being unloadSections(). To test, we simply used the swap4, swap5, write10, and dungeon testing files. 
