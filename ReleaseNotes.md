## 0.2.7

- Add t2.large static information

## 0.2.6

- Update to pallet-aws-ops 0.2.2

- Enable use of :network-interfaces
  The subnet ID and groups were not being handled correctly.

- Report private DNS if public DNS not set

- Fix block device doc in README

## 0.2.5

- Fix generation of signed s3 requests

## 0.2.4

- Make provider work on non-default VPC
  Use security group ids rahter than security group names.

- Update to pallet-aws-ops 0.2.1

- Add jcl-over-slf4j to default dependencies in readme

- Add the new generation ec2 instance types

## 0.2.3

- Fix bug in destroy-nodes-in-group
  The bug would remove all noes in an account when any group was converged
  to 0 nodes.

  Closes #7

## 0.2.2

- Fix destroy-node implementation
  Was incorrectly using :instance-id rather than :instance-ids.

- Remove hardware-name
  The name is translated in awaze, so can be passed directly here.

  Closes #1

- Update to pallet-aws-ops 0.2.0

## 0.2.1

- Add blobstore implementation
  Adds the :pallet-s3 provider for blobstore.

  Closes #4

# 0.2.0

- Update to pallet 0.8.0-RC.9

- Tags on new instances reflected in Nodes

- Tag instance names

- Add ami-info function to parse ami descriptions

- Remove AMI description parsing for Node info

- Implement ComputeServiceProperties

# 0.1.1

- Change describe-key-pairs to use :key-names

- Split Execute protocol into separate namespace

- Fix creating of launch keypair

- Fix link to awaze in the readme

# 0.1.0

- Initial version
