# Server installation
The software has been installed on an Ubuntu 12.04.4 LTS. I took notes of the procedure, whose steps are given below.

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


# Using vmcatcher

##### Add image to cache, so that it will be downloaded next time you run vmcatcher_cache

```
$ vmcatcher_image -vv -a -u 91b05287-c42c-4777-ae40-e5f956d63131
```

##### Download images that were added to cache
```
$ vmcatcher_cache -vv
```

# External documentation
[Extra docs for images, vmcatcher etc](
http://www.yokel.org/pub/software/yokel.org/docbook/release/pdf/a4/)