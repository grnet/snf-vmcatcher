#!/bin/sh

TARGET=vmcatcher.vmcatcher:snf-vmcatcher

mvn package && \
scp -C target/snf-vmcatcher.jar $TARGET/snf-vmcatcher.jar && \
scp ./snf-vmcatcher $TARGET/
scp ./dev-*.sh $TARGET/
