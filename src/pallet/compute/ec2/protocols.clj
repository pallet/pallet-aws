(ns pallet.compute.ec2.protocols)

(defprotocol AwsExecute
  (execute [_ command args] "Execute an aws comment"))

(defprotocol AwsParseAMI
  (ami-info [_ ami-id] "Try parsing info from the ami-id"))
