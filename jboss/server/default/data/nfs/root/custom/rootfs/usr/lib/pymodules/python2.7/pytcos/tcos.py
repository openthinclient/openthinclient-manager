# -*- coding: utf_8 -*-

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
###############################################################################

import base64
import commands
# import gconf
import ldap
import ldap.filter
import ldapurl
import os
import re
import sys
import time
import subprocess
import syslog
import types
import urllib

# Classes
#
class Logger:
    def __init__(self):
        self.LOG = []

    def log(self, log_level, log_string):
        # remap our log levels:
        # 0=DEBUG, 1=INFO, 2=WARNING, 3=ERROR
        # according to syslog
        SYSLOG_LEVEL = [(7, "DEBUG"), (6, "INFO"), (4, "WARNING"), (3, "ERROR")]
        log_level_syslog = SYSLOG_LEVEL[log_level][0]
        log_level_string = SYSLOG_LEVEL[log_level][1]

        # prepare error string
        e = os.path.basename(sys.argv[0]) + ": " + \
            "[" + log_level_string + "] in: " + \
            self.__class__.__name__ + "." + \
            sys._getframe(1).f_code.co_name + "(): " + \
            log_string

        # add error string to self.LOG
        self.LOG.append(self.__class__.__name__ + "." + \
                     sys._getframe(1).f_code.co_name + "(): " + \
                     log_string)

        # write error to stderr and syslog
        sys.stderr.write(e + "\n")
        syslog.syslog(log_level_syslog, e)

class Util(Logger):
    def __init__(self):
        # self.LOG is filled and needed by Logger.log()
        self.LOG = []

    # private
    #
    def __getFileObject(self, filename, mode="r"):
        f = None
        try:
            if os.path.exists(filename) and mode != "r":
                os.rename(filename, filename + ".tcos-old")
            f = open(filename, mode)
        except IOError, (errno, strerror):
            if errno == 2 and mode != "r":
                try:
                    os.mkdir(os.path.dirname(filename))
                    f = open(filename, mode)
                except:
                    e = "Unable to get writable file object(" + \
                        str(sys.exc_info()[0]) + "): " + \
                        "filename: " + str(filename) + \
                        ", mode: " + str(mode)
                    self.log(2, e)
                    raise
            else:
                e = "Unable to get file object(" + \
                    str(sys.exc_info()[0]) + "): " + \
                    "filename: " + str(filename) + \
                    ", mode: " + str(mode)
                self.log(2, e)
                raise
        except:
            e = "Unable to get file object(" + \
                str(sys.exc_info()[0]) + "): " + \
                "filename: " + str(filename) + \
                ", mode: " + str(mode)
            self.log(2, e)
            raise

        return f

    def __closeFileObject(self, file_object):
        try:
            file_object.close()
        except:
            e = "Unable to close file object(" + \
                str(sys.exc_info()[0]) + "): " + \
                "file_object: " + str(file_object)
            self.log(2, e)

    # public
    #
    def isMountpoint(self, mountpoint):
        m = self.__getFileObject("/proc/mounts")
        is_mountpoint = False
        for line in m.readlines():
            if line.split()[1] == mountpoint:
                is_mountpoint = True
                break
        self.__closeFileObject(m)
        return is_mountpoint

    def isMounted(self, device):
        m = self.__getFileObject("/proc/mounts")
        is_mounted = False
        for line in m.readlines():
            if line.split()[0] == device:
                is_mounted = True
                break
        self.__closeFileObject(m)
        return is_mounted

    def isRunning(self, program):
        if commands.getstatusoutput("pidof -x " + program)[0] == 0:
            return True
        else:
            return False

    def mount(self, source, destination, options=None):
        if self.isMountpoint(destination):
            e = "Destination already mounted: " + \
                "source: " + str(source) + \
                ", destination: " + str(destination) + \
                ", options: " + str(options)
            self.log(2, e)
            return False

        if not os.path.isdir(destination):
            try:
                os.makedirs(destination)
            except:
                e = "Unable to create destinaton dir(" + \
                    str(sys.exc_info()[0]) + "): " + \
                    "source: " + str(source) + \
                    ", destination: " + str(destination) + \
                    ", options: " + str(options)
                self.log(2, e)
                return False

        if options:
            option_cmd = "-o " + str(options) + " "
        else:
            option_cmd = ""

        ret_val = os.system("mount " + option_cmd + source + " " + destination)

        if ret_val != 0:
            e = "Unable to mount: " + \
                "source: " + str(source) + \
                ", destination: " + str(destination) + \
                ", options: " + str(options)
            self.log(2, e)
            return False
        else:
            return True

    def getFullscreenDimensions(self, geometrystring=True):
        screen_geometry = ""
        xwininfo_cmd = "/usr/bin/xwininfo -root"

        if commands.getoutput("pidof mate-panel"):
            # c = gconf.client_get_default()
            # panel_auto_hide = c.get_bool("/apps/panel/toplevels/top_panel_screen0/auto_hide")
            panel_auto_hide = commands.getoutput("dconf read /org/mate/panel/toplevels/top/auto_hide")
            if panel_auto_hide == True:
                # panel_size = c.get_int("/apps/panel/toplevels/top_panel_screen0/auto_hide_size")
                panel_size = 0
            else:
                # gnome-panel lies about one pixel
                # panel_size = c.get_int("/apps/panel/toplevels/top_panel_screen0/size") + 1
                panel_size = int(commands.getoutput("dconf read /org/mate/panel/toplevels/top/size"))
                panel_size += 1 
        else:
            panel_size = 0

        try:
            xrootwininfo = os.popen(xwininfo_cmd)
            width, height, xoffset, yoffset = re.search(
                    "-geometry ([0-9]+)x([0-9]+)\+([0-9]+)\+([0-9])",
                    xrootwininfo.read()).groups()
            xrootwininfo.close()

            height = str(int(height) - panel_size)
            yoffset = str(int(yoffset) + panel_size)
            if geometrystring == True:
                screen_geometry = width + "x" + height + "+" + xoffset + "+" + yoffset
            else:
                screen_geometry = int(width), int(height), int(xoffset), int(yoffset)
        except:
            e = "Unable to get xrootwininfo(" + str(sys.exc_info()[0]) + "): " + \
                "xwininfo_cmd: " + str(xwininfo_cmd) + \
                ", panel_auto_hide: " + str(panel_auto_hide) + \
                ", panel_size: " + str(panel_size)
            self.log(3, e)

        return screen_geometry

    def getScreenDepth(self):
        screen_depth = ""
        xwininfo_cmd = "/usr/bin/xwininfo -root"
        try:
            xrootwininfo = os.popen(xwininfo_cmd)
            screen_depth = re.search(
                    'Depth: ([0-9]+)',
                    xrootwininfo.read()).groups()
            xrootwininfo.close()

        except:
            e = "Unable to get xrootwininfo(" + str(sys.exc_info()[0]) + "): " + \
                "xwininfo_cmd: " + str(xwininfo_cmd)
            self.log(3, e)

        return screen_depth

    def getDictionaryFromFile(self, filename, delimiter="="):
        entry_dict = {}
        f = self.__getFileObject(filename)
        for line in f.readlines():
            k, v = line.strip().split(delimiter)
            entry_dict[k] = v
        self.__closeFileObject(f)
        return entry_dict

    def writeDictionaryToFile(self, entry_dict, filename, delimiter="=", quote='"'):
        if type(entry_dict) != types.DictType:
            e = "No dictionary passed(TypeError): " + \
                "entry_dict: " + str(entry_dict)
            self.log(2, e)
            return

        try:
            f = self.__getFileObject(filename,"w")
            f.write("# TCOS modified by: " + \
                    os.path.abspath(sys.argv[0]) + \
                    " at: " + time.asctime()  + "\n")
            keys = entry_dict.keys()
            keys.sort()
            for key in keys:
                if entry_dict[key] != None:
                    f.write(key + delimiter + quote + entry_dict[key] + quote + "\n")
            self.__closeFileObject(f)
        except:
            e = "Unable to write dictionary to file(" + \
                str(sys.exc_info()[0]) + "): " + \
                "entry_dict: " + str(entry_dict) + \
                ", filename: " + str(filename)
            self.log(2, e)

    def symlinkSave(self, source, destination):
        try:
            if os.path.exists(destination):
                os.rename(destination, destination + ".tcos-old")
            os.symlink(source, destination)
        except:
            e = "Unable to symlink file(" + \
                str(sys.exc_info()[0]) + "): " + \
                "source: " + str(source) + \
                ", destination: " + str(destination)
            self.log(2, e)

    def unifyList(self, list):
        # Fastest order preserving
        # see: http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/52560
        set = {}
        return [set.setdefault(e,e) for e in list if e not in set]

    def shellQuote(self, arg):
        # Everything is safely quoted inside a ''-quoted string, except a ' itself,
        # which can be written as '\'' (a backslash-escaped ' outside of the
        # ''-quoted string)
        return "'" + arg.replace("'", "'\\''") + "'"

    def shellQuoteList(self, args):
        return " ".join([self.shellQuote(arg) for arg in args])

# TODO: move System class to Util class?
class System(Logger):
    def __init__(self):
        # self.LOG is filled and needed by Logger.log()
        self.LOG = []

    def getMac(self, iface="eth0"):
        # get mac via sysfs
        f_name = '/sys/class/net/' + iface + '/address'
        try:
            f = open(f_name)
            mac = f.read().strip()
            f.close()
            return mac
        except IOError, (errno, strerror):
            e = "I/O error(" + str(errno) + ")" + ": " + \
                str(strerror) + ": " + \
                str(f_name)
            self.log(3, e)
        except:
            e = "Unexpected error: " + str(sys.exc_info()[0])
            self.log(3, e)

    def getUsername(self):
        # get login name
        # ioctr trouble with gdm PostLogin
        #username = os.getlogin()
        username = os.getenv("USER")
        if username:
            return username
        else:
            e = "Unable to get user login name"
            self.log(3, e)

    def getCmdlineParam(self, param, cmdline="/proc/cmdline"):
        # read cmdline
        try:
            f = open(cmdline)
            c_data = f.read()
            # parse cmdline
            try:
                value = re.search(param + "=(.*?)[ \n]", c_data).group(1)
                return value
            except AttributeError:
                return ""
            except:
                e = "Unable to get " + \
                    "parameter: " + param + " in cmdline: " + \
                    str(cmdline) + ": " + \
                    str(c_data)
                self.log(3, e)
            f.close()
        except IOError, (errno, strerror):
            e = "I/O error(" + str(errno) + ")" + ": " + \
                str(strerror) + ": " + \
                str(cmdline)
            self.log(3, e)
        except:
            e = "Unexpected error: " + str(sys.exc_info()[0])
            self.log(3, e)

    def isLocalBoot(self):
        root_device = self.getCmdlineParam("root")
        if root_device.startswith("/dev/") and Util().isMounted(root_device):
            return True
        else:
            return False

    def getLdapUrl(self):
        ldap_url = self.getCmdlineParam("ldapurl")
        if self.isLocalBoot() and Util().isRunning("slapd"):
            lurl = ldapurl.LDAPUrl(ldap_url)
            ldap_url = ldap_url.replace(lurl.urlscheme + "://" + lurl.hostport,
                                        "ldap://127.0.0.1")
        return ldap_url

    def getNfsroot(self):
        return self.getCmdlineParam("nfsroot")

    def getNfsrootServer(self):
        return self.getNfsroot().split(':')[0]

    def getNfsrootPath(self):
        return self.getNfsroot().split(':')[1]

    def getNfshome(self):
        return self.getCmdlineParam("nfshome")

    def getNfshomeServer(self):
        return self.getNfshome().split(':')[0]

    def getNfshomePath(self):
        return self.getNfshome().split(':')[1]

class Ldap(Logger):
    class DirectoryType:
        UNKNOWN, RFC, ADS, OPENLDAP, OPENLDAP_LOCAL = range(5)

    class SecondaryLdapConnection:
        def __init__(self, ldap_url=None):
            if ldap_url:
                self.LDAP_URL = ldap_url
            else:
                self.LDAP_URL = System().getLdapUrl()

            client_dn = Ldap().getClientDn(System().getMac(), self.LDAP_URL)
            location_dn = Ldap().getLocationsDn(client_dn, self.LDAP_URL)

            self.LOCATION_ENTRY = Ldap().getNismapentry(location_dn, self.LDAP_URL)
            self.CONNECTION_ENTRY = Ldap().getRealmConfiguration(self.LDAP_URL)

            dsl = "Directory.Secondary.LDAPURLs"
            ud = "UserGroupSettings.DirectoryVersion"

            # check location for overwrite
            if self.LOCATION_ENTRY.has_key(dsl) and \
               self.LOCATION_ENTRY.has_key(ud):
                if self.LOCATION_ENTRY[ud] == "secondary":
                    self.CONNECTION_ENTRY = self.LOCATION_ENTRY

        def getLdapUrl(self):
            value = ""
            key = "Directory.Secondary.LDAPURLs"
            if self.CONNECTION_ENTRY.has_key(key):
                value = self.CONNECTION_ENTRY[key] + \
                    "????bindname=" + urllib.quote(self.getRoPrincipal()) + \
                    ",X-BINDPW=" + base64.b64encode(self.getRoSecret())

            return value

        def getRoPrincipal(self):
            value = ""
            key = "Directory.Secondary.ReadOnly.Principal"
            if self.CONNECTION_ENTRY.has_key(key):
                value = self.CONNECTION_ENTRY[key]

            return value

        def getRoSecret(self):
            value = ""
            key = "Directory.Secondary.ReadOnly.Secret"
            if self.CONNECTION_ENTRY.has_key(key):
                value = self.CONNECTION_ENTRY[key]

            return value

        def getType(self):
            value = ""
            key = "UserGroupSettings.Type"
            if self.CONNECTION_ENTRY.has_key(key):
                value = self.CONNECTION_ENTRY[key]

            return value

        def getDirectoryVersion(self):
            value = ""
            key = "UserGroupSettings.DirectoryVersion"
            if self.CONNECTION_ENTRY.has_key(key):
                value = self.CONNECTION_ENTRY[key]

            return value

    def __init__(self):
        # self.LOG is filled and needed by Logger.log()
        self.LOG = []

        # stats
        self.SEARCHCOUNT = 0
        self.SEARCHCOUNT_RECURSIVE = 0

    def trimDnToUpper(self, dn):
        ret = ""
        dn_esc = dn.replace("\,", "#%COMMA%#")
        ret_list = dn_esc.split(',')

        for i in range(len(ret_list)):
            ret_list[i] = ret_list[i].strip()
            if ret_list[i].startswith("cn="):
                ret_list[i] = ret_list[i].replace("cn=", "CN=", 1)
            elif ret_list[i].startswith("dc="):
                ret_list[i] = ret_list[i].replace("dc=", "DC=", 1)
            elif ret_list[i].startswith("ou="):
                ret_list[i] = ret_list[i].replace("ou=", "OU=", 1)
            elif ret_list[i].startswith("l="):
                ret_list[i] = ret_list[i].replace("l=", "L=", 1)
            ret = ret + ret_list[i].strip()
            if i + 1 < len(ret_list):
                ret = ret + ",";

        return ret.replace("#%COMMA%#", "\,")

    def getLdapObj(self, ldap_url):
        try:
            lurl = ldapurl.LDAPUrl(ldap_url)
            # bindname is urlencoded
            username = urllib.unquote(lurl.who)
            try:
                password = base64.b64decode(lurl.cred)
            except TypeError:
                password = lurl.cred
            ldap_obj = ldap.initialize(lurl.urlscheme + "://" + lurl.hostport)
            ldap_obj.simple_bind_s(username, password)
            return ldap_obj
        except:
            e = "Unable to get ldap object: ("  + \
                str(sys.exc_info()[0]) + "): " + \
                "ldap_url: " + str(ldap_url) + \
                ", hostport: " + str(lurl.hostport) + \
                ", username: " + str(username) + \
                ", password: " + str(password)
            self.log(3, e)

    def guessServerType(self, ldap_url):
        try:
            lurl = ldapurl.LDAPUrl(ldap_url)
            if lurl.hostport.startswith("127."):
                return self.DirectoryType.OPENLDAP_LOCAL

            l = self.getLdapObj(ldap_url)
            match = l.search_s("",
                               ldap.SCOPE_BASE,
                               "(objectclass=*)",
                               ["dsServiceName","vendorName","objectClass"])
            self.SEARCHCOUNT = self.SEARCHCOUNT + 1

            ret_list = match[0][1]
            if (ret_list.get("vendorName")):
                if (ret_list.get("vendorName")[0].upper().startswith("APACHE")):
                    return self.DirectoryType.RFC
            elif (ret_list.has_key("dsServiceName")):
                return self.DirectoryType.ADS
            elif (ret_list.has_key("objectClass")):
                for i in ret_list.get("objectClass"):
                    if i.upper().startswith("OPENLDAPROOTDSE"):
                        return self.DirectoryType.OPENLDAP
            else:
                raise
        except:
            e = "Unable to guess Server Type for ldap_url: " + str(ldap_url)
            self.log(3, e)
            return self.DirectoryType.UNKNOWN

    def getPamSaslMechanisms(self, ldap_url):
        try:
            l = self.getLdapObj(ldap_url)
            match = l.search_s("",
                               ldap.SCOPE_BASE,
                               "(objectclass=*)",
                               ["supportedSASLMechanisms"])
            self.SEARCHCOUNT = self.SEARCHCOUNT + 1

            try:
                return  match[0][1]["supportedSASLMechanisms"]
            except:
                return []

        except:
            e = "Unable to get supportet SASL mechanisms for ldap_url: " + str(ldap_url)
            self.log(3, e)
            return []

    def getRealmConfiguration(self, ldap_url):
        lurl = ldapurl.LDAPUrl(ldap_url)
        try:
            return self.getNismapentry("ou=RealmConfiguration," + lurl.dn,
                                       ldap_url)
        except:
            e = "Unable to get RealmConfiguration(" + \
                str(sys.exc_info()[0]) + "): " + \
                "ldap_url: " + str(ldap_url)

            return {}

    def getRealmConfigurationEntry(self, ldap_url, key):
        crc_dict = self.getRealmConfiguration(ldap_url)
        if crc_dict.has_key(key):
            return crc_dict[key]
        else:
            return ""

    def getClientDn(self, mac, ldap_url):
        try:
            l = self.getLdapObj(ldap_url)
            lurl = ldapurl.LDAPUrl(ldap_url)
            match = l.search_s("ou=clients," + lurl.dn,
                               ldap.SCOPE_ONELEVEL,
                               "(&(objectclass=ieee802Device)(macAddress=" +
                               mac + "))",
                               ['cn'])
            self.SEARCHCOUNT = self.SEARCHCOUNT + 1

            if len(match) > 0 and match[0][0] != None:
                #return self.trimDnToUpper(match[0][0])
                return match[0][0]
            else:
                # try default mac 00:00:00:00:00:00
                match = l.search_s("ou=clients," + lurl.dn,
                                   ldap.SCOPE_ONELEVEL,
                                   "(&(objectclass=ieee802Device)(macAddress=00:00:00:00:00:00))",
                                   ['cn'])
                self.SEARCHCOUNT = self.SEARCHCOUNT + 1

                if len(match) > 0 and match[0][0] != None:
                    return match[0][0]
                else:
                    e = "Unable to get CLIENT_DN for MAC(notExistent): " + \
                        "mac: " + str(mac) + \
                        ", ldap_url: " + str(ldap_url)

                    self.log(2, e)
                    return ""

        except:
            e = "Unable to get CLIENT_DN for MAC(" + \
                str(sys.exc_info()[0]) + "): " + \
                "mac: " + str(mac) + \
                ", ldap_url: " + str(ldap_url)

            self.log(2, e)
            return ""

    def getUserDn(self, username, ldap_url):
        try:
            slc = self.SecondaryLdapConnection(ldap_url)

            secondary_ldap_url = slc.getLdapUrl()
            usergroup_ldap_dir = slc.getDirectoryVersion()
            usergroup_type = slc.getType()

            use_secondary_ldap_url = False
            if (secondary_ldap_url and usergroup_ldap_dir == "secondary" and \
                    (usergroup_type == "Users" or usergroup_type == "UsersGroups")):
                use_secondary_ldap_url = True
                ldap_url = secondary_ldap_url

            server_type = self.guessServerType(ldap_url)
            if (server_type == self.DirectoryType.RFC or \
                    server_type == self.DirectoryType.OPENLDAP_LOCAL or \
                    use_secondary_ldap_url == False):
                lurl = ldapurl.LDAPUrl(ldap_url)
                #user_dn = 'CN=' + username + ',OU=users,' + lurl.dn
                user_dn = 'cn=' + username + ',ou=users,' + lurl.dn
                #return self.trimDnToUpper(user_dn)
                return user_dn

            elif (server_type == self.DirectoryType.ADS and \
                    use_secondary_ldap_url == True):
                l = self.getLdapObj(ldap_url)
                l.set_option(ldap.OPT_REFERRALS, 0)
                lurl = ldapurl.LDAPUrl(ldap_url)
                match = l.search_s(lurl.dn,
                                   ldap.SCOPE_SUBTREE,
                                   "(&(objectclass=person)(sAMAccountName=" +
                                   username + "))",
                                   ["sAMAccountName"],
                                   1)
                self.SEARCHCOUNT = self.SEARCHCOUNT + 1
                if len(match) > 0 and match[0][0] != None:
                    #return self.trimDnToUpper(match[0][0])
                    return match[0][0]

                e = "Unable to get USER_DN(notExistent): " + \
                    "username: " + str(username) + \
                    ", ldap_url: " + str(ldap_url)

                self.log(2, e)
                return ""

            elif (server_type == self.DirectoryType.OPENLDAP and \
                    use_secondary_ldap_url == True):
                l = self.getLdapObj(ldap_url)
                l.set_option(ldap.OPT_REFERRALS, 0)
                lurl = ldapurl.LDAPUrl(ldap_url)
                match = l.search_s(lurl.dn,
                                   ldap.SCOPE_SUBTREE,
                                   "(&(objectclass=posixAccount)(uid=" +
                                   username + "))",
                                   ["uid"],
                                   1)
                self.SEARCHCOUNT = self.SEARCHCOUNT + 1
                if len(match) > 0 and match[0][0] != None:
                    #return self.trimDnToUpper(match[0][0])
                    return match[0][0]

                e = "Unable to get USER_DN(notExistent): " + \
                    "username: " + str(username) + \
                    ", ldap_url: " + str(ldap_url)

                self.log(2, e)
                return ""
            else:
                raise
        except:
            e = "Unable to get USER_DN(" + str(sys.exc_info()[0]) + "): " + \
                "username: " + str(username) + \
                ", ldap_url: " + str(ldap_url)
            self.log(2, e)
            return ""

    def getPamLdapUrl(self, ldap_url):
        slc = self.SecondaryLdapConnection(ldap_url)

        secondary_ldap_url = slc.getLdapUrl()
        usergroup_ldap_dir = slc.getDirectoryVersion()
        usergroup_type = slc.getType()

        if secondary_ldap_url and \
           usergroup_ldap_dir == "secondary" and \
           (usergroup_type == "Users" or usergroup_type == "UsersGroups"):
            ldap_url = secondary_ldap_url

        return ldap_url

    def getGroupOfUniqueNamesDn(self,
                                organizational_unit, unique_member_dn_list, ldap_url):
        group_of_unique_names_dn =  ([])

        if type(unique_member_dn_list) == types.StringType:
            unique_member_dn_list = [unique_member_dn_list]

        search_pattern = "(|"
        for unique_member_dn in unique_member_dn_list:
            if unique_member_dn:
                unique_member_dn = ldap.filter.escape_filter_chars(unique_member_dn)
                search_pattern += "(uniquemember=" + unique_member_dn + ")"
        search_pattern += ")"

        if search_pattern == "(|)":
            return []

        try:
            l = self.getLdapObj(ldap_url)
            lurl = ldapurl.LDAPUrl(ldap_url)
            match = l.search_s("ou=" + organizational_unit + "," + lurl.dn,
                               ldap.SCOPE_ONELEVEL,
                               search_pattern,
                               ["cn"])
            self.SEARCHCOUNT = self.SEARCHCOUNT + 1

            for (dn, cn) in match:
                #group_of_unique_names_dn.append(self.trimDnToUpper(dn))
                group_of_unique_names_dn.append(dn)

            # the "who matches" is done by preserving order
            return Util().unifyList(group_of_unique_names_dn)
        except:
            e = "Unable to get DN(" + str(sys.exc_info()[0]) + "): " + \
                "organizational_unit: " + str(organizational_unit) + \
                ", unique_member_dn_list: " + str(unique_member_dn_list) + \
                ", search_pattern: " + str(search_pattern)

            self.log(2, e)
            return []

    def getGroupOfUniqueNamesDnRecursiv(self,
                                        organizational_unit,
                                        unique_member_dn_list,
                                        ldap_url):
        # TODO: port this crazy stuff to order preserving lists:
        #       who matches who, on recursive groups?!
        def __collectRecursiv(organizational_unit, unique_member_dn):
            child_set = set(self.__children)
            match_set = set([])
            try:
                l = self.getLdapObj(ldap_url)
                lurl = ldapurl.LDAPUrl(ldap_url)
                unique_member_dn = ldap.filter.escape_filter_chars(unique_member_dn)
                match = l.search_s("ou=" + organizational_unit + "," + lurl.dn,
                                   ldap.SCOPE_ONELEVEL,
                                   "(&(objectclass=groupOfUniqueNames)(uniquemember=" + unique_member_dn + "))",
                                   ["uniquemember"])
                self.SEARCHCOUNT = self.SEARCHCOUNT + 1
                self.SEARCHCOUNT_RECURSIVE = self.SEARCHCOUNT_RECURSIVE + 1
                for k, v in match:
                    if k != None:
                        match_set.add(k)
            except:
                e = "Unable to get DN(" + str(sys.exc_info()[0]) + "): " + \
                    "organizational_unit: " + str(organizational_unit) + \
                    ", unique_member_dn: " + str(unique_member_dn)

                self.log(2, e)
                match_set = set([])

            for i in match_set - child_set:
                if i == "DC=dummy": continue
                try:
                    if ldap.explode_dn(i, 1)[1] == organizational_unit:
                        self.__children += [i]
                        __collectRecursiv(organizational_unit, i)
                except:
                    e = "Unable to get DN(" + \
                        str(sys.exc_info()[0]) + "): " + \
                        "organizational_unit: " + \
                        str(organizational_unit) + \
                        ", unique_member_dn: " + str(i)

                    self.log(2, e)

            return set(self.__children)

        if type(unique_member_dn_list) == types.StringType:
            unique_member_dn_list = [unique_member_dn_list]

        self.__children = unique_member_dn_list
        all_children = set([])

        for unique_member_dn in unique_member_dn_list:
            all_children |= __collectRecursiv(organizational_unit, unique_member_dn)

        return list(all_children)

    def getGroupOfUniqueNamesInfo(self, group_of_unique_names_dn, ldap_url):
        description = {}
        try:
            l = self.getLdapObj(ldap_url)
            match = l.search_s(group_of_unique_names_dn,
                               ldap.SCOPE_BASE,
                               "(objectclass=groupOfUniqueNames)",
                               ["cn", "description"])
            self.SEARCHCOUNT = self.SEARCHCOUNT + 1
            description["name"] = match[0][1]["cn"][0]
            try:
                description["description"] = match[0][1]["description"][0]
            except:
                description["description"] = ''

            match = l.search_s("nismapname=profile," + group_of_unique_names_dn,
                               ldap.SCOPE_BASE,
                               "(objectclass=nisMap)",
                               ["description"])
            self.SEARCHCOUNT = self.SEARCHCOUNT + 1
            description["schema"] = match[0][1]["description"][0]

            for k, v in description.items():
                if not v:
                    del description[k]

            return description
        except:
            e = "Unable to get description for group of unique names DN(" + \
                str(sys.exc_info()[0]) + "): " + \
                "group_of_unique_names_dn: " + str(group_of_unique_names_dn)

            self.log(2, e)

    def getMemberOfDn(self, member_dn, ldap_url):
        l = self.getLdapObj(ldap_url)
        l.set_option(ldap.OPT_REFERRALS, 0)
        match = l.search_s(member_dn,
                           ldap.SCOPE_BASE,
                           "(memberOf=*)",
                           ["memberOf"])
        self.SEARCHCOUNT = self.SEARCHCOUNT + 1

        try:
            return match[0][1]["memberOf"]
        except:
            return []

    def getMemberOfDnRecursiv(self, member_dn_list, ldap_url):
        # TODO: port this crazy stuff to order preserving lists:
        #       who matches who, on recursive groups?!
        def __collectRecursiv(member_dn):
            child_set = set(self.__children)
            match_set = set([])
            try:
                l = self.getLdapObj(ldap_url)
                lurl = ldapurl.LDAPUrl(ldap_url)
                l.set_option(ldap.OPT_REFERRALS, 0)
                member_dn = ldap.filter.escape_filter_chars(member_dn)
                match = l.search_s(lurl.dn,
                                   ldap.SCOPE_SUBTREE,
                                   "(&(objectclass=group)(memberOf=" + member_dn + "))",
                                   ["NoAttributeNeeded"])

                self.SEARCHCOUNT = self.SEARCHCOUNT + 1
                self.SEARCHCOUNT_RECURSIVE = self.SEARCHCOUNT_RECURSIVE + 1
                for k, v in match:
                    if k != None:
                        match_set.add(k)
            except:
                e = "Unable to get DN(" + str(sys.exc_info()[0]) + "): " + \
                    "member_dn: " + str(member_dn)
                self.log(2, e)

            for i in match_set - child_set:
                try:
                    self.__children += [i]
                    __collectRecursiv(i)
                except:
                    e = "Unable to get DN(" + \
                        str(sys.exc_info()[0]) + "): " + \
                        "member_dn: " + str(i)

                    self.log(2, e)

            return set(self.__children)

        if type(member_dn_list) == types.StringType:
            member_dn_list = [member_dn_list]

        self.__children = member_dn_list
        all_children = set([])

        for member_dn in member_dn_list:
            all_children |= __collectRecursiv(member_dn)

        return list(all_children)

    def getMemberUidDn(self, username, ldap_url):
        member_uid_dn = ([])
        l = self.getLdapObj(ldap_url)
        lurl = ldapurl.LDAPUrl(ldap_url)
        l.set_option(ldap.OPT_REFERRALS, 0)
        match = l.search_s(lurl.dn,
                           ldap.SCOPE_SUBTREE,
                           "(&(objectclass=posixGroup)(memberUid=" + username + "))",
                           ["NoAttributeNeeded"])
        self.SEARCHCOUNT = self.SEARCHCOUNT + 1

        for dn, empty in match:
            member_uid_dn.append(dn)

        return member_uid_dn

    def getUsergroupsDn(self, user_dn, ldap_url):
        slc = self.SecondaryLdapConnection(ldap_url)

        secondary_ldap_url = slc.getLdapUrl()
        usergroup_ldap_dir = slc.getDirectoryVersion()
        usergroup_type = slc.getType()

        if (secondary_ldap_url and \
           usergroup_ldap_dir == "secondary"):
            ldap_url = secondary_ldap_url

        server_type = self.guessServerType(ldap_url)
        if (server_type == self.DirectoryType.RFC or \
                  server_type == self.DirectoryType.OPENLDAP_LOCAL):
            direct_usergroups = self.getGroupOfUniqueNamesDn("usergroups",
                                                             user_dn,
                                                             ldap_url)
            usergroups = self.getGroupOfUniqueNamesDnRecursiv("usergroups",
                                                              direct_usergroups,
                                                              ldap_url)
        elif (server_type == self.DirectoryType.ADS and \
                  usergroup_type == "UsersGroups"):
            direct_usergroups = self.getMemberOfDn(user_dn, ldap_url)
            usergroups = self.getMemberOfDnRecursiv(direct_usergroups, ldap_url)
        elif (server_type == self.DirectoryType.OPENLDAP and \
                  usergroup_type == "UsersGroups"):
            usergroups = self.getMemberUidDn(System().getUsername(), ldap_url)
        else:
            usergroups = []

        return usergroups

    def getAppgroupsDn(self, client_dn, user_dn, ldap_url):
        usergroups = self.getUsergroupsDn(user_dn, ldap_url)
        clientgroups = self.getClientgroupsDn(client_dn, ldap_url)
        direct_appgroups = self.getGroupOfUniqueNamesDn("appgroups",
                                                        usergroups +
                                                        clientgroups +
                                                        [user_dn, client_dn],
                                                        ldap_url)
        appgroups = self.getGroupOfUniqueNamesDnRecursiv("appgroups",
                                                         direct_appgroups,
                                                         ldap_url)
        return appgroups

    def getClientgroupsDn(self, client_dn, ldap_url):
        direct_clientgroups = self.getGroupOfUniqueNamesDn("clientgroups",
                                                           client_dn,
                                                           ldap_url)
        clientgroups = self.getGroupOfUniqueNamesDnRecursiv("clientgroups",
                                                            direct_clientgroups,
                                                            ldap_url)
        return clientgroups

    def getAppsDn(self, client_dn, user_dn, ldap_url):
        appgroups = self.getAppgroupsDn(client_dn, user_dn, ldap_url)
        clientgroups = self.getClientgroupsDn(client_dn, ldap_url)
        usergroups = self.getUsergroupsDn(user_dn, ldap_url)
        apps_for_appgroups = self.getGroupOfUniqueNamesDn("apps",
                                                          appgroups,
                                                          ldap_url)
        apps_for_clientgroups = self.getGroupOfUniqueNamesDn("apps",
                                                             clientgroups,
                                                             ldap_url)
        apps_for_usergroups = self.getGroupOfUniqueNamesDn("apps",
                                                           usergroups,
                                                           ldap_url)
        apps_for_client = self.getGroupOfUniqueNamesDn("apps",
                                                       [client_dn],
                                                       ldap_url)
        apps_for_user = self.getGroupOfUniqueNamesDn("apps",
                                                     [user_dn],
                                                     ldap_url)
        apps = apps_for_appgroups + \
	       apps_for_clientgroups + \
               apps_for_usergroups + \
               apps_for_client + \
               apps_for_user

        return Util().unifyList(apps)

    def getHwtypesDn(self, client_dn, ldap_url):
        hwtypes = self.getGroupOfUniqueNamesDn("hwtypes",
                                               client_dn,
                                               ldap_url)
        return hwtypes

    def getDevicesDn(self, client_dn, ldap_url):
        hwtypes = self.getHwtypesDn(client_dn, ldap_url)
        devices = self.getGroupOfUniqueNamesDn("devices",
                                               hwtypes + [client_dn],
                                               ldap_url)
        return devices

    def getLocationsDn(self, client_dn, ldap_url):
        try:
            l = self.getLdapObj(ldap_url)
            lurl = ldapurl.LDAPUrl(ldap_url)
            match = l.search_s(client_dn,
                               ldap.SCOPE_BASE,
                               "(objectclass=ieee802Device)",
                               ["l"])
            self.SEARCHCOUNT = self.SEARCHCOUNT + 1

            #return self.trimDnToUpper(match[0][1]["l"][0])
            return match[0][1]["l"][0]
        except:
            e = "Unable to get LOCATIONS_DN(" + str(sys.exc_info()[0]) + "): " + \
                "client_dn: " + str(client_dn)
            self.log(2, e)
            return ''

    def getPrintersDn(self, client_dn, user_dn, ldap_url):
        usergroups = self.getUsergroupsDn(user_dn, ldap_url)
        location = self.getLocationsDn(client_dn, ldap_url)
        printers = self.getGroupOfUniqueNamesDn("printers",
                                                [location] + usergroups +
                                                [user_dn, client_dn],
                                                ldap_url)
        return printers

    def getPrintersDnByClient(self, client_dn, ldap_url):
        location = self.getLocationsDn(client_dn, ldap_url)
        printers = self.getGroupOfUniqueNamesDn("printers",
                                                [location] + [client_dn],
                                                ldap_url)
        return printers

    def getPrintersDnByUser(self, user_dn, ldap_url):
        usergroups = self.getUsergroupsDn(user_dn, ldap_url)
        printers = self.getGroupOfUniqueNamesDn("printers",
                                                usergroups + [user_dn],
                                                ldap_url)
        return printers

    def getNismapentry(self, group_of_unique_names_dn, ldap_url):
        entry_dict = {}

        # accept list with one member as well
        if type(group_of_unique_names_dn) == types.ListType and \
           len(group_of_unique_names_dn) == 1:
            group_of_unique_names_dn = group_of_unique_names_dn[0]

        try:
            l = self.getLdapObj(ldap_url)
            lurl = ldapurl.LDAPUrl(ldap_url)
            match = l.search_s("nismapname=profile," + group_of_unique_names_dn,
                               ldap.SCOPE_ONELEVEL,
                               "(objectclass=nisObject)",
                               ["cn", "nismapentry"])
            for i in match:
                key = i[1]['cn'][0]
                try:
                    val = i[1]['nismapentry'][0]
                except KeyError:
                    val = i[1]['nisMapEntry'][0]

                val = os.path.expandvars(val)

                '''IMPORTANT: As there is no real type attribute within our xml
                scheme, we need to emulate it with the help of hashtags
                variable__BOOL -> boolean
                variable__INT  -> integer
                variable__FLOAT-> float

                - they will be part of the key
                - need to be cut from the variable name
                '''
                p = re.compile(r'__[A-Z]*$')
                m = p.search(key)
                
                if m:
                    if 'BOOL' in m.group():
                        if 'true' in val:
                            val = True
                        else:
                            val = False
                        key = key[:m.start()]
                    elif 'INT' in m.group():
                        val = int(val)
                        key = key[:m.start()]
                    elif 'FLOAT' in m.group():
                        val = float(val)
                        key = key[:m.start()]

                entry_dict[key] = val

            return entry_dict
        except:
            e = "Unable to get Nismapentry(" + str(sys.exc_info()[0]) + "): " + \
                "group_of_unique_names_dn: " + str(group_of_unique_names_dn) + \
                ", ldap_url: " + str(ldap_url)
            self.log(2, e)
            return {}

    def getCnByDn(self, dn):
        cn = ldap.explode_dn(dn)[0]
        if cn.startswith("cn="):
            cn = cn.split("cn=")[1]
        elif cn.startswith("CN="):
            cn = cn.split("CN=")[1]

        return cn

class Desktop(Logger):
    def __init__(self, client_dn=None, user_dn=None, ldap_url=None):
        # self.LOG is filled and needed by Logger.log()
        self.LOG = []

        # set self.LDAP_URL
        if ldap_url:
            self.LDAP_URL = ldap_url
        else:
            self.LDAP_URL = System().getLdapUrl()

        # set self.CLIENT_DN
        if client_dn:
            self.CLIENT_DN = client_dn
        else:
            mac = System().getMac()
            self.CLIENT_DN = Ldap().getClientDn(mac, self.LDAP_URL)

        # set self.USER_DN
        if user_dn:
            self.USER_DN = user_dn
        else:
            username = System().getUsername()
            self.USER_DN = Ldap().getUserDn(username, self.LDAP_URL)

    def getDesktopFileEntries(self, desktop_filename):
        entries = {}
        try:
            f = open(desktop_filename)
            for line in f:
                split_data = line.split("=", 1)
                if len(split_data) == 2:
                    entries[split_data[0]] = split_data[1].strip()
            return entries
        except IOError, (errno, strerror):
            e = "I/O error(" + str(errno) + ")" + ": " + \
                str(strerror) + ": " + \
                str(desktop_filename)
            self.log(1, e)
            return {}

    def getMergedDesktopFileEntries(self, app_dn, app_dn_info_dict, merge_desktop_filename):
        # Prefer "description" as "name"
        app_name = app_dn_info_dict.get("description", None)
        app_comment = app_dn_info_dict.get("name", None)

        if not app_name:
            app_name = app_comment

        app_schema = app_dn_info_dict.get("schema", None)
        # if there is a General.custom_icon set up within the xml, override the icon setting
        try:
            l = Ldap()
            app_icon_custom =  l.getNismapentry(app_dn, self.LDAP_URL).get('General.custom_icon')
        except IOError, e:
            print "*** error: " + e


        if app_schema and app_dn:
            app_path = "/opt/" + app_schema
            app_tryexec = app_path + "/tcos/launcher"
            app_exec = app_tryexec + " " + base64.b16encode(app_dn)
            app_icon = app_path + "/tcos/launcher.icon"
        else:
            app_path = None
            app_tryexec = None
            app_exec = None
            app_icon = None
        if app_icon_custom:
            app_icon = "/var/tcos/custom/icons/" + app_icon_custom

        new_desktop_file_entries = self.getDesktopFileEntries(merge_desktop_filename)
        if new_desktop_file_entries.has_key("X-TCOS-EXECFIELDCODE"):
            app_exec = app_exec + " " + new_desktop_file_entries["X-TCOS-EXECFIELDCODE"]

        desktop_file_entries = {"Name" : app_name,
                                "Comment" : app_comment,
                                "Path" : app_path,
                                #"TryExec" : app_tryexec,
                                "Exec" : app_exec,
                                "Icon" : app_icon,
                                "X-TCOS-DN" : app_dn,
                                "Type" : "Application",
                                "Categories" : "Application;TCOS;"}

        for k, v in desktop_file_entries.items():
            if not v:
                del desktop_file_entries[k]

        desktop_file_entries.update(new_desktop_file_entries)

        if not desktop_file_entries.has_key("Exec"):
            e = "Unable to merge desktop file(no key: Exec): " + \
                "app_dn: " + str(app_dn) + \
                ", app_dn_info_dict: " + str(app_dn_info_dict) + \
                ", merge_desktop_filename: " + str(merge_desktop_filename)
            self.log(2, e)
        elif not desktop_file_entries.has_key("Name"):
            e = "Unable to merge desktop file(no key: Name): " + \
                "app_dn: " + str(app_dn) + \
                ", app_dn_info_dict: " + str(app_dn_info_dict) + \
                ", merge_desktop_filename: " + str(merge_desktop_filename)
            self.log(2, e)
        else:
            return desktop_file_entries

    def writeDesktopFile(self, desktop_file_entry_dict, desktop_file_foldername):
        if not desktop_file_entry_dict.has_key("Exec"):
            e = "Unable to write desktop file(no key: Exec): " + \
                "desktop_file_entry_dict: " + str(desktop_file_entry_dict) + \
                ", desktop_file_foldername: " + str(desktop_file_foldername)
            self.log(2, e)
            return
        elif not desktop_file_entry_dict.has_key("Name"):
            e = "Unable to write desktop file(no key: Name): " + \
                "desktop_file_entry_dict: " + str(desktop_file_entry_dict) + \
                ", desktop_file_foldername: " + str(desktop_file_foldername)
            self.log(2, e)
            return

        desktop_file = os.path.join(desktop_file_foldername,
                                    desktop_file_entry_dict["Comment"] + ".desktop")

        try:
            f = open(desktop_file, "w")
        except IOError, (errno, strerror):
            if errno == 2:
                try:
                    os.makedirs(desktop_file_foldername)
                    f = open(desktop_file, "w")
                except:
                    e = "Unable to write desktop file(" + \
                        str(sys.exc_info()[0]) + "): " + \
                        "desktop_file_entry_dict: " + \
                        str(desktop_file_entry_dict) + \
                        ", desktop_file_foldername: " + \
                       str(desktop_file_foldername) + \
                        ", desktop_file: " + str(desktop_file)
                    self.log(2, e)
                    raise
            else:
                e = "I/O error(" + str(errno) + ")" + ": " + \
                    str(strerror) + ": " + \
                    str(desktop_file)
                self.log(2, e)
                raise

        f.write("[Desktop Entry]\n")
        for k, v in desktop_file_entry_dict.iteritems():
            f.write(k + "=" + v + "\n")
        f.close()

    def writeDesktopFiles(self, desktop_file_foldernames=[], autostart_desktop_file_foldernames=[]):
        if desktop_file_foldernames == []:
            homedir = os.getenv("HOME")
            desktop_file_foldernames = [homedir + "/Desktop",
                                        homedir + "/.local/share/applications/"]
        elif type(desktop_file_foldernames) == types.StringType:
            desktop_file_foldernames = [desktop_file_foldernames]

        if autostart_desktop_file_foldernames == []:
            homedir = os.getenv("HOME")
            autostart_desktop_file_foldernames = [homedir + "/.config/autostart/"]
        elif type(autostart_desktop_file_foldernames) == types.StringType:
            autostart_desktop_file_foldernames = [autostart_desktop_file_foldernames]

        l = Ldap()
        apps_dn_list = l.getAppsDn(self.CLIENT_DN,
                                   self.USER_DN,
                                   self.LDAP_URL)

        for app_dn in apps_dn_list:
            entry = l.getNismapentry(app_dn, self.LDAP_URL)
            app_dn_info_dict = l.getGroupOfUniqueNamesInfo(app_dn, self.LDAP_URL)
            app_schema = app_dn_info_dict["schema"]
            if not os.path.exists(os.path.join(os.path.sep, "opt", app_schema, "tcos", ".nodesktop")):
                merge_desktop_filename = os.path.join(os.path.sep,
                                                      "opt",
                                                      app_schema,
                                                      "tcos",
                                                      app_schema + ".desktop")


                desktop_entry_dict = self.getMergedDesktopFileEntries(
                                         app_dn,
                                         app_dn_info_dict,
                                         merge_desktop_filename)

                for desktop_file_foldername in desktop_file_foldernames:
                    self.writeDesktopFile(desktop_entry_dict, desktop_file_foldername)

                for autostart_desktop_file_foldername in autostart_desktop_file_foldernames:
                    if entry.get('General.Autostart') == "Yes":
                        self.writeDesktopFile(desktop_entry_dict, autostart_desktop_file_foldername)

            if os.path.exists(os.path.join(os.path.sep, "opt", app_schema, "tcos", app_schema + ".desktop-reload")):
                merge_desktop_filename = os.path.join(os.path.sep,
                                                      "opt",
                                                      app_schema,
                                                      "tcos",
                                                      app_schema + ".desktop")

                desktop_entry_dict = self.getMergedDesktopFileEntries(
                                         app_dn,
                                         app_dn_info_dict,
                                         merge_desktop_filename)

                # check if desktop already up and running
                if commands.getoutput('ps ax | grep /etc/gdm/PostLogin/Default | grep -v grep'):
                    for autostart_desktop_file_foldername in autostart_desktop_file_foldernames:
                        self.writeDesktopFile(desktop_entry_dict, autostart_desktop_file_foldername)
                else:
                    subprocess.Popen(desktop_entry_dict["Exec"], shell=True)

    def removeDesktopFiles(self, desktop_file_foldernames=[]):
        desktop_file_filenames = []

        if desktop_file_foldernames == []:
            homedir = os.getenv("HOME")
            desktop_file_foldernames = [homedir + "/Desktop/",
                                        homedir + "/.local/share/applications/",
                                        homedir + "/.config/autostart/"]
        elif type(desktop_file_foldernames) == types.StringType:
            desktop_file_foldernames = [desktop_file_foldernames]

        for foldername in desktop_file_foldernames:
            if os.path.isdir(foldername):
                for filename in os.listdir(foldername):
                    if filename.endswith(".desktop"):
                        desktop_file_filenames.append(foldername + filename)

        for filename in desktop_file_filenames:
            # let the custom made menu files alive (mozo)
            if not "mozo" in filename:
                os.remove(filename)

class Launcher(Logger):
    def __init__(self, ldap_url=None):
        # self.LOG is filled and needed by Logger.log()
        self.LOG = []

        self.ENTRY = {}

        self.DN = self.getDn()

        if ldap_url:
            self.LDAP_URL = ldap_url
        else:
            s = System()
            self.LDAP_URL = s.getLdapUrl()

        if self.LDAP_URL:
            self.ENTRY = self.getEntry()

    def getDn(self):
        if len(sys.argv) >= 2:
            try:
                dn_encode = base64.b16decode(sys.argv[1])
                return dn_encode
            except TypeError:
                return sys.argv[1]
        else:
            e = "No Application DN passed via argv[1]"
            self.log(3, e)

    def getEntry(self):
        entry = {}

        l = Ldap()
        entry = l.getNismapentry(self.DN, self.LDAP_URL)

        if entry:
            return entry
        else:
            e = "Unable to get application entries (entry={}): " + \
                "DN: " + str(self.DN) + \
                ", LDAP_URL: " + str(self.LDAP_URL)

            e_user = "Anwendung nicht gefunden!" + "\n\n" + \
                     "Der Anwdendungseintrag hat sich geändert." +"\n" + \
                     "Melden Sie sich neu an, " + \
                     "damit die geänderten Einstellungen wirksam werden." + "\n\n" + \
                     "Details:" + "\n" + \
                     "DN: " + str(self.DN) + "\n" + \
                     "LDAP_URL: " + str(self.LDAP_URL)

            self.log(2, e)
            os.system("zenity --error --text '" + e_user + "'")

    def getAppInfo(self):
        info = {}

        l = Ldap()
        info = l.getGroupOfUniqueNamesInfo(self.DN, self.LDAP_URL)

        if info:
            return info

        else:
            # do something about the missing stuff
            return {}
