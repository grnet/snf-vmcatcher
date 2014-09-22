# You can use these as a template for setting up vmcatcher

# This is where vmcatcher itself is installed
export VMCATCHER_HOME=${HOME}/vmcatcher

# This is where snf-vmcatcher is installed
# You can find snf-vmcatcher at https://github.com/grnet/snf-vmcatcher
export SNF_VMCATCHER_HOME=${HOME}/snf-vmcatcher

# These environment variables are known to vmcatcher python script
export VMCATCHER_RDBMS="sqlite:////var/lib/vmcatcher/vmcatcher.db"
export VMCATCHER_CACHE_DIR_CACHE=${VMCATCHER_HOME}/cache
export VMCATHCER_CACHE_DIR_DOWNLOAD=${VMCATHCER_HOME}/cache/partial
export VMCATCHER_CACHE_DIR_EXPIRE=${VMCATCHER_HOME}/cache/expired
export VMCATCHER_CACHE_EVENT="python ${VMCATCHER_HOME}/vmcatcher_eventHndlExpl --output_file=$HOME/vmcatcher_event.log --datetime"

# This is what hooks up snf-vmcatcher to vmcatcher
export VMCATCHER_CACHE_EVENT="java -jar ${SNF_VMCATCHER_HOME}/snf-vmcatcher.jar -v enqueue-from-env -conf ${SNF_VMCATCHER_HOME}/application.conf"

