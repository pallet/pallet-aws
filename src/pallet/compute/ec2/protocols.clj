(ns pallet.compute.ec2.protocols)

(defprotocol AwsExecute
  (execute [_ command args] "Execute an aws comment"))
