/*
 * exit2.c
 *
 * It does not get simpler than this...
 */

#include "syscall.h"

int
main (int argc, char *argv[])
{
    printf("Just Before Basic Exiting");
    exit (123);
}
