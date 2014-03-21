# Pallet AWS Provider

A [pallet](http://palletops.com/) provider for [AWS](http://aws.amazon.com/), based on
the Amazon java SDK.

The EC2 provider uses the `:pallet-ec2` key.

## Usage

Add the following to your dependencies:

```clj
[com.palletops/pallet-aws "0.2.0"]
```
## Node-Spec Options

The `node-spec` can be used to control EC2 specific features:

### Image

The ami to use for the image is passed as a string to the `:image-id`
key in the node-spec's `:image`.

```clj
(node-spec ... :image {:image-id "ami-35792c5c"
                                 :os-family :ubuntu
                                 :os-version "13.10"
                                 :login-user "ubuntu"})
```

You must specify the `:os-family`, `:os-version` and `:login-user`
with ami specific information.

### Availability Zones

The availability zone can be specifed in the `:location` key of the node-spec.

```clj
(node-spec ... :location {:location-id "us-east-1d"})
```

### KeyPairs

By default, pallet creates a keypair based on the admin username and key credentials.

To reuse an existing EC2 keypair, pass the name of the keypair to the
`:key-name` key of the `:provider` specific map.

```clj
(node-spec ... :provider {:pallet-ec2 {:key-name "mykey"}})
```

This keypair is authorised on the node when it is created, and is used
by pallet to run the `:bootstrap` phase.

### Block devices and EBS

The provider specific `block-device-mapping` key can be used to set up block devices.
It takes a sequence of maps with the following keys:

`:device-name`
: The device name exposed to the instance (for example, "/dev/sdh").

`:virtual-name`
: The virtual device name (ephemeral[0..3]). The number of available
instance store volumes depends on the instance type. After you connect
to the instance, you must mount the volume.

`:ebs :snapshot-id`
: The ID of the snapshot.

`:ebs :volume-size`
: The size of the volume, in gigabytes.

`:ebs :delete-on-termination`
: Indicates whether to delete the volume on instance termination.

`:ebs :volume-type`
: The volume type ("standard" or "io1")

`:ebs :iops`
: For "io1" volumes, the number of I/O operations per second (IOPS)
that the volume supports. Range is 100 to 4000.

`:no-device`
: Suppresses the device mapping.

### Network Interfaces

The provider specific `:network-interface` key takes a sequence of
maps with the following keys (and key paths):

`:network-interface-id`
: An existing interface to attach to a single instance. Requires n=1
instances.

`:device-index`
: The device index. Applies both to attaching an existing network
interface and creating a network interface.  If you are specifying a
network interface in the request, you must provide the device index.

`:subnet-id`
: The subnet ID. Applies only when creating a network interface.

`:description`
: A description. Applies only when creating a network interface.

`:private-ip-address`
: The primary private IP address. Applies only when creating a network
interface. Requires n=1 network interfaces in launch.

`:security-group-id`
: The ID of the security group. Applies only when creating a network interface.

`:delete-on-termination`
: Indicates whether to delete the network interface on instance termination.

`:private-ip-addresses [] :private-ip-address`
: The private IP address. `:private-ip-addresses` takes a sequence of
maps to specify explicit private IP addresses for a network interface,
but only one private IP address can be designated as primary.  Only
one private IP address can be designated as primary. Therefore, you
can't specify this parameter if `:private-ip-addresses :primary` is set
to true and `:private-ip-address` is set to an IP address.

`:private-ip-addresses [] :primary`
: Indicates whether the private IP address is the primary private IP address.

`:secondary-private-ip-addresscount`
: The number of private IP addresses to assign to the network
interface. For a single network interface, you can't specify this
option and specify more than one private IP address using
`:private-ip-address`.

`:associate-public-ip-address`
: Indicates whether to assign a public IP address to an instance in a
VPC. The public IP address is associated with a specific network
interface. If set to true, the following rules apply: Can only be
associated with a single network interface with the device index
of 0. You can't associate a public IP address with a second network
interface, and you can't associate a public IP address if you are
launching more than one network interface.  Can only be associated
with a new network interface, not an existing one. Default: If
launching into a default subnet, the default value is true. If
launching into a nondefault subnet, the default value is false.

### IAM Roles

An IAM profile can be specified on the `:iam-instance-profile`
provider specific key. It is a map with one of the following keys:

`:arn`
: The Amazon Resource Name (ARN) of the IAM instance profile to
associate with the instances.

`:name`
: The name of the IAM Instance Profile (IIP) to associate with the
instances.

### Other

The `[:provider :pallet-ec2]` path can be used to specify other EC2
specific functionality.

`:user-data`
: The Base64-encoded MIME user data for the instances.


`:placement :group-name`
: The name of an existing placement group

`:placement :tenancy`
: The tenancy of the instance. An instance with a tenancy of `"dedicated"`
runs on single-tenant hardware and can only be launched into a VPC.

`:kernel-id`
: The ID of the kernel.  It is recommended that you use PV-GRUB
instead of kernels and RAM disks. For more information, see PV-GRUB: A
New Amazon Kernel Image in the Amazon Elastic Compute Cloud User
Guide.

`:ramdisk-id`
: The ID of the RAM disk.

`:monitoring :enabled`
: Enables monitoring for the instance.

`:subnet-id`
: the ID of the subnet to launch the instance into.

`:disable-api-termination`
: If you set this parameter to true, you can't terminate the instance
using the Amazon EC2 console, CLI, or API; otherwise, you can. If you
set this parameter to true and then later want to be able to terminate
the instance, you must first change the value of the
disableApiTermination attribute to false using
ModifyInstanceAttribute. Alternatively, if you set
InstanceInitiatedShutdownBehavior to terminate, you can terminate the
instance by running the shutdown command from the instance.

`:instance-initiated-shutdown-behavior`
: Indicates whether an instance stops or terminates when you initiate
shutdown from the instance (using the operating system command for
system shutdown). Either "stop" or "terminate".

`::private-ip-address`
: The primary IP address. You must specify a value from the IP address
range of the subnet.  Only one private IP address can be designated as
primary. Therefore, you can't specify this parameter if
PrivateIpAddresses.n.Primary is set to true and
PrivateIpAddresses.n.PrivateIpAddress is set to an IP address.

`:client-token`
: Unique, case-sensitive identifier you provide to ensure idempotency
of the request. For more information, see How to Ensure Idempotency in
the Amazon Elastic Compute Cloud User Guide.

`:ebs-optimized`
: Indicates whether the instance is optimized for EBS I/O. This optimization provides dedicated throughput to Amazon EBS and an optimized configuration stack to provide optimal Amazon EBS I/O performance. This optimization isn't available with all instance types. Additional usage charges apply when using an EBS-optimized instance.



## Access to Arbitrary AWS SDK Funtions

The provide provides an `execute` function that can be used to execute
arbitrary functions, described as [`awaze`](https://github.com/pallet/awaze) data maps.


## License

Copyright © 2013 Hugo Duncan

Distributed under the Eclipse Public License.
