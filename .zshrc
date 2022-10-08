# $Id: zshrc,v 1.2 2010/07/18 21:47:22 rml Exp $
# zsh specific initialization, processed on new login
# and subsequent zsh invocations
[ -r ~/.acms.debug ] && echo ENTERED .zshrc >&2

[ -r /public/zshrc.adjunct ] && . /public/zshrc.adjunct

[ -r ~/.acms.debug ] && echo EXITING .zshrc >&2
