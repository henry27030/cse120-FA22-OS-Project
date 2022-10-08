################################################
#  /public/zshenv - Default ACS .zshenv file
[ -r ~/.acms.debug ] && echo ENTERED .zshenv >&2

# Your .zshenv  file is processed each time a new zsh shell is
# initialized.
#
#  /etc/zshenv
#  /etc/profile.d/*.sh (sourced in lexically sorted order for default locale)
#  ${HOME}/.zshenv
#  /etc/.zprofile
#  ........

[ -r /public/zshenv.adjunct ] && source /public/zshenv.adjunct
 
[ -r ~/.acms.debug ] && echo EXITING .zshenv >&2
