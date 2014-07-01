(ns pallet.compute.ec2.static
  "Static data for EC2 (data not available in ec2 apis)")

(defn GiB [n] (* 1024 n))

(def instance-types
  {;;; Burstable Performance Instances

   :t2.micro
   {:ram (GiB 1)
    :cpus [{:cores 1 :speed 1}]
    :disks []
    :32-bit true
    :64-bit true
    :io :low
    :ebs-optimized false}

   :t2.small
   {:ram (GiB 2)
    :cpus [{:cores 1 :speed 1}]
    :disks []
    :32-bit true
    :64-bit true
    :io :low
    :ebs-optimized false}

   :t2.medium
   {:ram (GiB 4)
    :cpus [{:cores 2 :speed 1}]
    :disks []
    :32-bit true
    :64-bit true
    :io :low
    :ebs-optimized false}

   ;;; General Purpose

   :m3.medium
   {:ram (GiB 3.75)
    :cpus [{:cores 1 :speed 3}]
    :disks [{:size 4}]
    :64-bit true
    :io :moderate
    :ebs-optimized false}

   :m3.large
   {:ram (GiB 7.5)
    :cpus [{:cores 2 :speed 3.25}]
    :disks [{:size 32}]
    :64-bit true
    :io :moderate
    :ebs-optimized false}

   :m3.xlarge
   {:ram (GiB 15)
    :cpus [{:cores 4 :speed 3.25}]
    :disks [{:size 40} {:size 40}]
    :64-bit true
    :io :high
    :ebs-optimized 500}

   :m3.2xlarge
   {:ram (GiB 30)
    :cpus [{:cores 8 :speed 3.25}]
    :disks [{:size 80} {:size 80}]
    :64-bit true
    :io :high
    :ebs-optimized 1000}

   ;;; Compute Optimized

   :c3.large
   {:ram (GiB 3.75)
    :cpus [{:cores 2 :speed 3.5}]
    :disks [{:size 16} {:size 16}]
    :64-bit true
    :io :moderate
    :ebs-optimized false}

   :c3.xlarge
   {:ram (GiB 7.5)
    :cpus [{:cores 4 :speed 3.5}]
    :disks [{:size 40} {:size 40}]
    :64-bit true
    :io :moderate
    :ebs-optimized 500}

   :c3.2xlarge
   {:ram (GiB 15)
    :cpus [{:cores 8 :speed 3.5}]
    :disks [{:size 80} {:size 80}]
    :64-bit true
    :io :high
    :ebs-optimized 1000}

   :c3.4xlarge
   {:ram (GiB 30)
    :cpus [{:cores 16 :speed 3.4375}]
    :disks [{:size 160} {:size 160}]
    :64-bit true
    :io :high
    :ebs-optimized 2000}

   :c3.8xlarge
   {:ram (GiB 60)
    :cpus [{:cores 32 :speed 3.375}]
    :disks [{:size 320} {:size 320}]
    :64-bit true
    :io :very-high
    :ebs-optimized false}

   ;;; GPU Instances

   :g2.2xlarge
   {:ram (GiB 15)
    :cpus [{:cores 8 :speed 3.25}]
    :disks [{:size 60}]
    :64-bit true
    :io :high
    :ebs-optimized 1000}

   ;;; Memory Optimized

   :r3.large
   {:ram (GiB 15)
    :cpus [{:cores 2 :speed 3.25}]
    :disks [{:size 32}]
    :64-bit true
    :io :moderate
    :ebs-optimized false}

   :r3.xlarge
   {:ram (GiB 30.5)
    :cpus [{:cores 4 :speed 3.25}]
    :disks [{:size 80}]
    :64-bit true
    :io :moderate
    :ebs-optimized 500}

   :r3.2xlarge
   {:ram (GiB 61)
    :cpus [{:cores 8 :speed 3.25}]
    :disks [{:size 160}]
    :64-bit true
    :io :high
    :ebs-optimized 1000}

   :r3.4xlarge
   {:ram (GiB 122)
    :cpus [{:cores 16 :speed 3.25}]
    :disks [{:size 320}]
    :64-bit true
    :io :high
    :ebs-optimized 2000}

   :r3.8xlarge
   {:ram (GiB 244)
    :cpus [{:cores 32 :speed 3.25}]
    :disks [{:size 320} {:size 320}]
    :64-bit true
    :io :very-high
    :ebs-optimized false}

   ;;; Storage Optimized

   :i2.xlarge
   {:ram (GiB 30.5)
    :cpus [{:cores 4 :speed 3.5}]
    :disks [{:size 800}]
    :64-bit true
    :io :moderate
    :ebs-optimized 500}

   :i2.2xlarge
   {:ram (GiB 61)
    :cpus [{:cores 8 :speed 3.375}]
    :disks [{:size 800} {:size 800}]
    :64-bit true
    :io :high
    :ebs-optimized 1000}

   :i2.4xlarge
   {:ram (GiB 122)
    :cpus [{:cores 16 :speed 3.3125}]
    :disks [(repeat 4 {:size 800})]
    :64-bit true
    :io :high
    :ebs-optimized 2000}

   :i2.8xlarge
   {:ram (GiB 244)
    :cpus [{:cores 32 :speed 3.25}]
    :disks [(repeat 8 {:size 800})]
    :64-bit true
    :io :very-high
    :ebs-optimized false}

   :hs1.8xlarge
   {:ram (GiB 117)
    :cpus [{:cores 16 :speed 2.1875}]
    :disks [(repeat 24 {:size 2048})]
    :64-bit true
    :io :very-high
    :ebs-optimized false}

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;;; Previous generation instance types

   :m1.small
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
    :io :high
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
