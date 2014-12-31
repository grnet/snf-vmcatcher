### What is snf-vmcatcher?

`snf-vmcatcher` integrates with [`vmcatcher`](https://github.com/hepix-virtualisation/vmcatcher) via its event mechanism in order to handle the VM upload procedure to an IaaS provider. It does so via a message queue and specifically `RabbitMQ`. Currently we support the [Synnefo](https://www.synnefo.org) IaaS but the architecture is flexible enough to accomodate other user-cases with minimum additions. 

The architecture is asynchronous. When `vmcatcher` fires an event, a hook which can be specified in the environment as:

```shell
# This is what hooks up snf-vmcatcher to vmcatcher
export VMCATCHER_CACHE_EVENT="java -jar ${SNF_VMCATCHER_HOME}/snf-vmcatcher.jar -v enqueue-from-env -conf ${SNF_VMCATCHER_HOME}/application.conf"

```

is activated and posts the json-encoded event coming from `vmcatcer` to `RabbitMQ`. A daemon (part of `snf-vmcatcher`) running separately and listening for there messages then receives the event, extracts the VM image information and finally uploads the VM to a Synnefo installation.

So, in effect, `snf-vmcatcher` has two roles:

* To receive the `VMCATCHER_CACHE_EVENT` and post it to `RabbitMQ` for later processing.
* To receive an event from `RabbitMQ` and use the provided information for image uploading.

The latter is achieved by runnning `snf-vmcatcher` in daemon mode.

For the actual upload to Synnefo, we assume the installation of the relevant command-line tools.

### System Installation
Please read [Installation](INSTALLATION.md)

Also, we use [`snf-mkimage`](https://www.synnefo.org/docs/snf-image-creator/latest/) via `sudo`,
so this line must be added (via running `visudo`):

```
vmcatcher ALL = NOPASSWD: /usr/bin/snf-mkimage
```

where we assume that `snf-vmcatcher` runs under user `vmcatcher` and `snf-mkimage` is installed under `/usr/bin`.

### Using snf-vmcatcher

The command line options below give an overview of the functionality.

```
Usage: gr.grnet.egi.vmcatcher.Main [options] [command] [command options]
  Options:
    -h, -help, --help
       
       Default: false
    -v
       Be verbose
       Default: false
  Commands:
    usage      Show usage
      Usage: usage [options]

    show-env      Show environment variables
      Usage: show-env [options]

    show-conf      Show the contents of the configuration file. Its contents must be JSON-encoded of the form:
                    rabbitmq {
                      servers = ["localhost:5672"]
                      username = "vmcatcher"
                      password = "*****"
                      queue = "vmcatcher"
                      exchange = "vmcatcher"
                      routingKey = "vmcatcher"
                      vhost = "/"
                    }
      Usage: show-conf [options]
        Options:
        * -conf
             The configuration file the application uses

    enqueue-from-env      Use environment variables to enqueue a VM instance message to RabbitMQ. This is the original use-case and reflects how vmcatcher (the original software) works.
      Usage: enqueue-from-env [options]
        Options:
        * -conf
             The configuration file the application uses

    enqueue-from-image-list      Use a vmcatcher-compatible, JSON-encoded image list to enqueue a VM instance message to RabbitMQ
      Usage: enqueue-from-image-list [options]
        Options:
        * -conf
             The configuration file the application uses
          -image-identifier
             The 'dc:identifier' of the specific VM image you want to enqueue.
             If not given, then all VM images given in the list are enqueued.
        * -image-list-url
             The URL of the image list. You can use an http(s) or file URL.
          -token
             The Personal Access Token (from https://appdb.egi.eu) to use for
             protected image lists & images

    dequeue      Dequeue one message from RabbitMQ and register the corresponding VM instance
      Usage: dequeue [options]
        Options:
        * -conf
             The configuration file the application uses
          -handler
             The Java class that will handle a message from RabbitMQ. Use
             gr.grnet.egi.vmcatcher.image.handler.SynnefoVMRegistrationHandler for the standard behavior. Other values are
             gr.grnet.egi.vmcatcher.image.handler.JustLogHandler and gr.grnet.egi.vmcatcher.image.handler.ThrowingHandler
             Default: gr.grnet.egi.vmcatcher.image.handler.SynnefoVMRegistrationHandler
          -insecure-ssl, -insecure-SSL
             If set to true, SSL validation errors are ignored. This might be
             useful for images that are behind self-signed SSL certificates. Use at
             your own risk !
             Default: false
        * -kamaki-cloud
             The name of the cloud from ~/.kamakirc that will be used by kamaki
             for VM upload
          -server
             Run in server mode and dequeue a message at a time
             Default: false
          -sleepMillis
             Milliseconds to wait between RabbitMQ message consumption
             Default: 1000

    register-now      Directly register the corresponding VM instance. This is helpful in debugging
      Usage: register-now [options]
        Options:
          -format
             Use this VM format if none can be automatically discovered
          -insecure-ssl, -insecure-SSL
             If set to true, SSL validation errors are ignored. This might be
             useful for images that are behind self-signed SSL certificates. Use at
             your own risk !
             Default: false
        * -kamaki-cloud
             The name of the cloud from ~/.kamakirc that will be used by kamaki
             for VM upload
        * -osfamily
             The OS family, e.g. 'linux' or 'windows'
             Default: linux
        * -url
             The URL from where to fetch the VM.
        * -users
             The OS 'users' that will become a field in the metafile properties
             Default: root

    parse-image-list      Parses a vmcatcher-compatible, JSON-encoded image list. Helpful for debugging.
      Usage: parse-image-list [options]
        Options:
        * -image-list-url
             The URL of the image list. You can use an http(s) or file URL.
          -token
             The Personal Access Token (from https://appdb.egi.eu) to use for
             protected image lists & images

    get-image-list      Retrieves a vmcatcher-compatible, JSON-encoded image list. Helpful for debugging.
      Usage: get-image-list [options]
        Options:
        * -image-list-url
             The URL of the image list. You can use an http(s) or file URL.
          -token
             The Personal Access Token (from https://appdb.egi.eu) to use for
             protected image lists & images

    drain-queue      Remove all events from the queue and do nothing with them.
      Usage: drain-queue [options]
        Options:
        * -conf
             The configuration file the application uses

    transform      Transform a VM image to raw format
      Usage: transform [options]
        Options:
          -insecure-ssl, -insecure-SSL
             If set to true, SSL validation errors are ignored. This might be
             useful for images that are behind self-signed SSL certificates. Use at
             your own risk !
             Default: false
        * -url
             The URL from where to fetch the VM.
```

