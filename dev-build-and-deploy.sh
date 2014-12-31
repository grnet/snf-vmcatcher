#!/bin/sh

TARGET=vmcatcher.vmcatcher:snf-vmcatcher

mvn package && \
scp -C target/snf-vmcatcher.jar $TARGET/snf-vmcatcher.jar && \
scp -C ./snf-vmcatcher $TARGET/ && \
scp -C ./dev-*.sh $TARGET/
