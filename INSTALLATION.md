# Server installation
The software has been installed on an Ubuntu 12.04.4 LTS. I took notes of the procedure, whose steps are given below.


### Note
You should create a new user `vmcatcher` to host the vmcatcher software.
```
$ adduser vmcatcher
```

### Let's go
First some preliminaries

```
$ aptitude install -y python-pip git gcc swig fetch-crl openjdk-7-jdk
$ aptitude install -y qemu-utils
$ pip install sqlalchemy
$ pip install M2Crypto
```

Then install the following software

* `https://github.com/hepix-virtualisation/hepixvmitrust.git`
* `https://github.com/hepix-virtualisation/smimeX509validation.git`
* `https://github.com/hepix-virtualisation/vmcatcher.git`

Following the procedure mentioned in [EGI IGTF Release](https://wiki.egi.eu/wiki/EGI_IGTF_Release), run this command

```
$ wget -q -O - \
     https://dist.eugridpma.info/distribution/igtf/current/GPG-KEY-EUGridPMA-RPM-3 \
     | apt-key add -
```

Add to `/etc/apt/sources.list`:

```
#### EGI Trust Anchor Distribution ####
deb http://repository.egi.eu/sw/production/cas/1/current egi-igtf core
```

Then run these:

```
$ aptitude update
$ aptitude install -y ca-policy-egi-core

$ aptitude install -y python-software-properties
$ apt-add-repository ppa:grnet/synnefo
$ aptitude update
$ aptitude install -y snf-image-creator
```

Install latest [rabbitmq](http://www.rabbitmq.com/install-debian.html) by first adding this to `/etc/apt/sources.list`

```
deb http://www.rabbitmq.com/debian/ testing main
```

and then running

```
$ wget http://www.rabbitmq.com/rabbitmq-signing-key-public.asc
$ apt-key add rabbitmq-signing-key-public.asc
$ aptitude update
$ aptitude install -y rabbitmq-server
```
The default user in rabbitmq is `quest`. Configure a dedicated user for `snf-vmcatcher`

```
$ rabbitmqctl add_user vmcatcher PASSWORD
$ rabbitmqctl set_permissions -p / vmcatcher ".*" ".*" ".*"
```
where you have to provide a good `PASSWORD`

This is for debugging, not needed for the `vmcatcher` system

```
$ aptitude install sqlite3
```

In order to upgrade the `qemu-utils` package, you first need to

```
$ aptitude install libglib2.0-dev dh-autoreconf
$ aptitude install flex bison
```

This will also grab a ton of other dependencies. Then get the latest version `2.0.0` of `qemu`
 
 ```
 $ wget http://wiki.qemu-project.org/download/qemu-2.0.0.tar.bz2
 ```
 
 unpack it and then, inside the created folder, run 
 
 ```
 $ ./configure && ./make install
 ```
 

# Using vmcatcher

##### Add image to cache, so that it will be downloaded next time you run vmcatcher_cache

```
$ vmcatcher_image -vv -a -u 91b05287-c42c-4777-ae40-e5f956d63131
```

##### Download images that were added to cache
```
$ vmcatcher_cache -vv
```

# Shell configuration

`vmcatcher` can be configured via a number of environment variables. We rely on them for the operation of our automatic
image registration to ~okeanos. For the following, we assume that

* `vmcatcher` is installed in `/root/vmcatcher`
* `snf-vmcatcher` is installed in `/root/snf-vmcatcher/snf-vmcatcher.jar`

The environement variables we use are as 

```
export VMCATCHER_RDBMS="sqlite:////var/lib/vmcatcher/vmcatcher.db"
export VMCATCHER_CACHE_DIR_CACHE=/root/vmcatcher/cache
export VMCATHCER_CACHE_DIR_DOWNLOAD=/root/vmcatcher/cache/partial
export VMCATCHER_CACHE_DIR_EXPIRE=/root/vmcatcher/cache/expired
export VMCATCHER_CACHE_EVENT="python /root/vmcatcher/vmcatcher_eventHndlExpl --output_file=/root/vmcatcher_event.log --datetime"
export VMCATCHER_CACHE_EVENT="java -jar /root/snf-vmcatcher/snf-vmcatcher.jar -v -conf /root/snf-vmcatcher/application.conf enqueue"
```


# External documentation
[Extra docs for images, vmcatcher etc](
http://www.yokel.org/pub/software/yokel.org/docbook/release/pdf/a4/)

# Problems encountered during development and testing
##### qemu-img: error while reading sector 131072: Invalid argument
This first appeared when trying to convert a `vmdk` image to `raw` using `qemu-img`.
The command line was something like this:

```
$ qemu-img convert -f vmdk -O raw SL-6.5-x86_64-minimal-disk1.vmdk SL-6.5-x86_64-minimal-disk1.vmdk.raw
```

The problem seems to be with the version of `qemu-img` being old on Ubuntu 12.04.4 LTS;
see this [SO question](http://askubuntu.com/questions/406365/qemu-img-error-while-reading-sector-327680-invalid-argument).
The problem has been resolved by manually installing the latest version of `qemu`, as described above.