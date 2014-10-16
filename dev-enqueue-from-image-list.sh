#!/bin/sh

HERE="`dirname $0`"
SNF_VMCATCHER_HOME=${SNF_VMCATCHER_HOME:-$HERE}

java -jar $SNF_VMCATCHER_HOME/snf-vmcatcher.jar enqueue-from-image-list -conf application.conf -image-list-url file:fedcloud.egi.eu-image.list -image-identifier 800f345f-5278-5523-a1dc-8a98476006f8
