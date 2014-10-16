#!/bin/sh

HERE="`dirname $0`"
SNF_VMCATCHER_HOME=${SNF_VMCATCHER_HOME:-$HERE}

java -jar $SNF_VMCATCHER_HOME/snf-vmcatcher.jar register-now -kamaki-cloud occi-test -url http://appliance-repo.egi.eu/images/base/UbuntuServer-12.04-x86_64-base/1.0/UbuntuServer-12.04-x86_64-base.ova -users root -osfamily linux
