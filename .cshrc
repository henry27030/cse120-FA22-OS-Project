################################################
#  /public/cshrc - Default ACS .cshrc file
if ( -r .acms.debug ) echo ENTERED .cshrc

# Your .cshrc (or .tcshrc) file is processed each time a new c-shell (csh)
# is initialized.  In the case of a login c-shell, the initialization is
# done by processing files in the following order:
#
#  /etc/csh.cshrc
#  /etc/profile.d/*.csh (sourced in lexically sorted order for default locale)
#  ${HOME}/.tcshrc OR ${HOME}/.cshrc
#  /etc/csh.login
#  ${HOME}/.login

# On ACMS Linux systems, PATH is initially set by /etc/csh.cshrc,
# and then modified by the script fragments in /etc/profile.d/*.csh.
# The acspath.csh script initializes the "modules" package, and establishes
# PATH, MANPATH, and miscelaneous other environment variables by sourcing
# /public/modulerc.
 
# All the usual cshrc setup is done by the following line.  Any additions
# you make should come after it.

if (-r /public/cshrc.adjunct) source /public/cshrc.adjunct
 
# CAUTION: if you choose to make adjustments to PATH, it is usually advisable
# to *modify* the existing PATH rather than resetting PATH completely.  By
# modifying, there is less chance of inadvertently losing important elements.
# For example:  set path = ( $path ${HOME}/bin )

# As of July 2010, the "Modules" package is installed, and used to establish
# your initial PATH, and to modify your PATH as required when you use "prep".
# We suggest using the "modules" package to modify your path.  For example,
# to prepend your ~/bin directory to PATH:
#	% module load my
# or, to remove the "tex" and "acroread" components of PATH (and MANPATH):
#	% module unload acroread tex
# You may still modify your path the traditional way, but using "modules"
# is infinitely easier for making dynamic changes.  To see which modules
# are in use, issue the command "module list";  for more info, "module help".

if ( -r .acms.debug ) echo EXITING .cshrc
