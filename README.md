# UCSD CSE120 FA22 nachos starter code

- This repo contains the starter code for nachos for UCSD CSE 120 Principles of Operating Systems course for FA22 quarter.

- Please go through the README in the nachos directory for detailed information about nachos.

- Note that this code is the same as the starter code that is available as a tar file on ieng6 machines.

Henry:

I wrote Condition2 according to the semaphore implementation.  I made KThreads release and acquire a lock in the form of a condition variable and sleep on condition variables until the condition variable it is sleeping on causes it or one of the other KThreads sleeping on it to wake.  Sleep() blocks the KThread by sleeping it and adds it to a linkedList that sleeps on the condition variable.

wake causes the first KThread sleeping on the condition variable in its linkedList to be put on the ready queue.
sleepFor should work in the way that it wakes up a specific KThread instead of making it wait in the linkedlist of a condition in the case that timeout is reached.
Tests were implemented to check if KThreads that sleep on a condition variable would wake up accordingly and produce a "ping-pong" result. 

sleepFor should work in the way that it wakes up a specific KThread instead of making it wait in the linkedlist of a condition in the case that timeout is reached.
Tests were implemented to check if KThreads that sleep on a condition variable would wake up accordingly and produce a "ping-pong" result.

Ethan:
I wrote Rendezvous's entire class structure basically. To implement exchange, there as an if else statement that determines if we are the first exchanger or the second exchanger. From there it's basically just a variable swap over a HashMap based on the tag.

To synchronize, I used a lock and a condition variable where the first swapper goes to sleep until the second swapper places the new value and wakes it up.

It worked basically as expected, but there were some problems when a third swapper would come in and wreck it after the second swapper had given up control. To get around this, I use a boolean value that sleeps the third swapper on the condition variable if an exchange is happening and release it after the first swapper is done. The problem persisted no more after that.

In order to test my code, I basically did what the project page said, the first test was a simple exchange, the second was two exchanges on two different tags. The third was two exchanges sequentially, on one tag. The fourth was just two different rendezvous objects being used at the same time.

Mark:
timerInterrupt() first disables interrupts, because calling ready requires interrupts disabled.  By checking that the Queue waitList for slept threads isn't empty and checking the first KThread's waitTime against the current time, we want to see if the current time matches or exceeds the KThread's waitTime to determine whether or not to transfer the KThread to the ready queue.  Interrupts are finally re-enabled and the current thread yields.

waitUntil() first disables interrupts and proceeds to grab the current machine time.  This is so that when adding a Pair<KThread, waitTime> to the Queue waitList, the KThread has a corresponding time so that other methods can compare against this time to know to wake this KThread.  After adding the KThread to the waitList, we call sleep and then re-enable interrupts.

sleepFor() goes hand in hand with cancel.  In Condition2, sleepFor will disable interrupts and relase the lock after confirming that condition is held by the current thread.  We also set a boolean value in KThread to note that this KThread has used sleepFor, which is important later for determining whether to ready() or cancel() in the method wake().  sleepFor() then proceeds to add the currentThread to the Condition variable's waitList and then calls Alarm's waitUntil() which also adds this KThread to the Alarm's waitList, keeping both waitLists updated as well as sleeping the thread.  The lock is then acquired and interrupts are restored.

join() in KThread first disables interrupts after confirming that this thread isn't the currentThread and its caller isnt null.  Inside KThread, like for sleepFor(), we have a field that holds join's caller so that the called thread can reference back to know how to context switch.  By checking if the status is finished, we return immediately; however if not finished, we then set the joinCaller to the currentThread and then sleep it to wait for the child thread to finish running.  And then interrupts are re-enabled.

As for testing, for Alarm I tested basic cases and manually traced execution to confirm.  For join, I tested both cases of the child thread with status finished and the child thread with not status finished.  For sleepFor, I relied on Henry's tests.
