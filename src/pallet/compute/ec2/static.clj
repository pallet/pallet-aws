(ns pallet.compute.ec2.static
  "Static data for EC2 (data not available in ec2 apis)")

(def instance-types
  {:m1.small
   {:ram (* 1024 1.7)                   ; GiB memory
    :cpus [{:cores 1 :speed 1}]         ; 1 EC2 Compute Unit
    :disks [{:size 160}]                ; GB instance storage
    :32-bit true
    :64-bit true
    :io :moderate
    :ebs-optimised false}

   :m1.medium
   {:ram (* 1024 3.75)                  ;  GiB memory
    :cpus [{:cores 1 :speed 2}]         ; EC2 Compute Unit
    :disks [{:size 410}]                ; GB instance storage
    :32-bit true
    :64-bit true
    :io :moderate
    :ebs-optimised false}

   :m1.large
   {:ram (* 1024 7.5)                   ; GiB memory
    :cpus [{:cores 2 :speed 2}]         ; 4 Compute Units
    :disks [{:size 850}]                ; GB instance storage
    :64-bit true
    :io :high
    :ebs-optimised 500}                 ; Mbps

   :m1.xlarge
   {:ram (* 1024 15)                    ; GiB memory
    :cpus [{:cores 4 :speed 2}]         ; 8 Compute Units
    :disks [{:size 1690}]               ; GB instance storage
    :64-bit true
    :io :high
    :ebs-optimised 1000}                ; Mbps

   :m3.xlarge
   {:ram (* 1024 15)                    ;  GiB memory
    :cpus [{:cores 4 :speed 3.25}]      ; 13 Compute Units
    :disks []
    :64-bit true
    :io :moderate
    :ebs-optimised false}

   :m3.2xlarge
   {:ram (* 1024 30)                    ; GiB memory
    :cpus [{:cores 8 :speed 3.25}]      ; 26 Compute Units
    :disks []
    :64-bit true
    :io :high
    :ebs-optimised false}

   :t1.micro
   {:ram 613                            ; MiB memory
    :cpus [{:cores 1 :speed 1}]         ; Up to 2 EC2 Compute Units
    :disks []
    :32-bit true
    :64-bit true
    :io :low
    :ebs-optimised false}

   :m2.xlarge
   {:ram (* 1024 17.1)                  ; GiB of memory
    :cpus [{:cores 2 :speed 3.25}]      ; 6.5 Compute Units
    :disks [{:size 420}]                ; GB of instance storage
    :64-bit true
    :io :moderate
    :ebs-optimised false}

   :m2.2xlarge
   {:ram (* 1024 34.2)                  ; GiB of memory
    :cpus [{:cores 4 :speed 3.25}]      ; 13 Compute Units
    :disks [{:size 850}]                ; GB of instance storage
    :64-bit true
    :io :high
    :ebs-optimised false}

   :m2.4xlarge
   {:ram (* 1024 68.4)                  ; GiB of memory
    :cpus [{:cores 8 :speed 3.25}]      ; 26 Compute Units
    :disks [{:size 1690}]               ; GB of instance storage
    :64-bit true
    :io :High
    :ebs-optimised 1000}                ; Mbps

   :c1.medium
   {:ram (* 1024 1.7)                   ; GiB of memory
    :cpus [{:cores 2 :speed 2.5}]       ; 5 Compute Units
    :disks [{:size 350}]                ; GB of instance storage
    :32-bit true
    :64-bit true
    :io :moderate
    :ebs-optimised false}

   :c1.xlarge
   {:ram (* 1024 7)                     ; GiB of memory
    :cpus [{:cores 8 :speed 2.5}]       ; 20 EC2 Compute Units
    :disks [{:size 1690}]               ; GB of instance storage
    :64-bit true
    :io :high
    :ebs-optimised false}

   :cc1.4xlarge
   {:ram (* 1024 23)                               ; GiB of memory
    :cpus [{:cores 4 :speed 4}{:cores 4 :speed 4}] ; 33.5 EC2 Compute Units
    :disks [{:size 1690}]               ; GB of instance storage
    :64-bit true
    :io :very-high                      ; (10 Gigabit Ethernet)
    :ebs-optimised false}

   :cc2.8xlarge
   {:ram (* 1024 60.5)                             ; GiB of memory
    :cpus [{:cores 8 :speed 4}{:cores 8 :speed 4}] ; 88 EC2 Compute Units
    :disks [{:size 3370}]               ; GB of instance storage
    :64-bit true
    :io :very-high                      ; (10 Gigabit Ethernet)
    :ebs-optimised false}

   :cg1.4xlarge
   {:ram (* 1024 22)                               ; GiB of memory
    :cpus [{:cores 4 :speed 4}{:cores 4 :speed 4}] ; 33.5 EC2 Compute Units
    :disks [{:size 1690}]               ; GB of instance storage
    :64-bit true
    :io :very-high                      ; (10 Gigabit Ethernet)
    :ebs-optimised false}

   :hi1.4xlarge
   {:ram (* 1024 60.5)                  ; GiB of memory
    :cpus [{:cores 16 :speed 2.1875}]   ; 35 EC2 Compute Units
    :disks [{:size 1024}{:size 1024}] ; 2 SSD-based volumes each with 1024 GB
                                        ; of instance storage
    :64-bit true
    :io :very-high                      ; (10 Gigabit Ethernet)
    :ebs-optimised false}

   :hi1.8xlarge
   {:ram (* 1024 117)                   ; GiB of memory
    :cpus [{:cores 16 :speed 2.1875}]   ; 35 EC2 Compute Units
    :disks (repeat 24 {:size 1024}) ; 24 SSD-based volumes each with 1024 GB
                                        ; of instance storage
    :64-bit true
    :io :very-high                      ; (10 Gigabit Ethernet)
    :ebs-optimised false}
   })
