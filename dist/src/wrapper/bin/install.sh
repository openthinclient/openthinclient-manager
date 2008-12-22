#! /bin/sh

################################################################################
# openthinclient.org ThinClient suite
# 
# Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
# 
# 
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
# 
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
# details.
# 
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc., 59 Temple
# Place - Suite 330, Boston, MA 02111-1307, USA.
################################################################################


START_SCRIPT="%{INSTALL_PATH}/bin/start.sh"

RC_TOOLS="update-rc.d chkconfig rc-update"
RC_DIRS="/etc/init.d /etc/rc.d"


# link start script
for i in $RC_DIRS; do
	if [ -d "$i" ]; then
		case $i in
			'/etc/init.d')
				ln -sf "$START_SCRIPT" "$i/%{DIST_SHORTNAME}"
				break
				;;
			'/etc/rc.d')
				ln -sf "$START_SCRIPT" "$i/rc.%{DIST_SHORTNAME}"
				break
				;;
		esac
	fi
done

# install service
for i in $RC_TOOLS; do
	if which $i >/dev/null 2>&1; then
		case $i in
			'update-rc.d')
				update-rc.d "%{DIST_SHORTNAME}" defaults
				;;
			'chkconfig')
				chkconfig --add "%{DIST_SHORTNAME}"
				chkconfig "%{DIST_SHORTNAME}" on
				;;
			'rc-update')
				rc-update add "%{DIST_SHORTNAME}" default
				;;
		esac
	fi
done