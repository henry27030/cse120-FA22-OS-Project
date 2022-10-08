# $Id: login,v 1.2 2010/07/09 04:43:23 rml Exp $
# - Standard ACS Linux .login 
if ( -r ~/.acms.debug ) echo ENTERED .login

if (-r /public/login.adjunct) source /public/login.adjunct

# Since "modules" modifies (adds or removes additions made by modules
# PATH modification can be made before of after the above.  Please
# consider using "modules" yourself to modify your environment.

if ( -r ~/.acms.debug ) echo EXITING .login
