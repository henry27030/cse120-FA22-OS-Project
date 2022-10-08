# $Id: kshrc,v 1.3 2010/07/13 19:21:44 rml Exp $
# /public/kshrc -- prototype for ${HOME}/.kshrc
public=${public:-/public}
[ -r .acms.debug ] && echo ENTERED .kshrc >&2

# Your .kshrc file is processed each time a new, non-login ksh.
# Changes made here will have no effect on the login shell;
# such changes should be made in .profile. 
# 
# All the usual setup is done by the following line.  Any additions 
# you make should come after it. 
#
# You may add commands to the end of this file as needed. 
# 

# echo processing ksh specific shell initialization
[ -r $public/kshrc.adjunct ] && . $public/kshrc.adjunct

# When this file was first placed in your home directory, a
# pre-existing .kshrc file may have been moved to a file named 
# "${HOME}/.kshrc.old". Check the commands in that file be 
# sure they are still needed.

if [ -r ${HOME}/.kshrc.old ]
then
	echo "----------------------------------------------" >&2
	echo "About to run commands in your old kshrc file."  >&2
	echo "Delete the file .kshrc.old, or edit .kshrc if these commands" >&2
	echo "should not be run anymore." 		      >&2
	echo "----------------------------------------------" >&2
	. ${HOME}/.kshrc.old
	echo "----------------------------------------------" >&2
	echo "Done running commands in your old kshrc file."  >&2
	echo "----------------------------------------------" >&2
fi

# CAUTION: if you choose to make adjustments to PATH,
# it is usually advisable to *add* to the existing PATH
# rather than resetting PATH completely.  By adding, there
# is less chance of inadvertently losing important elements.
# For example:  set path = ( $path ${HOME}/bin )
[ -r .acms.debug ] && echo EXITING .kshrc >&2
