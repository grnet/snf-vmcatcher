#!/bin/sh

HERE="`dirname $0`"
SNF_VMCATCHER_HOME=${SNF_VMCATCHER_HOME:-$HERE}

java -jar $SNF_VMCATCHER_HOME/snf-vmcatcher.jar dequeue -conf application.conf -kamaki-cloud occi-test
