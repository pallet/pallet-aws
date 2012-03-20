(ns pallet.compute.ec2.ami
  "Functions for parsing ami data"
  (:require
   [clojure.core.match :refer [match]]
   [clojure.core.match.regex]
   [clojure.string :as string]))

;;; ### OS parsing
(def ^{:doc "Map of non-canonical os-versions to canonical versions"}
  os-versions
  {"debian" {"lenny" "5.0" "squeeze" "6.0" "5" "5.0" "6" "6.0"}
   "centos" {"5" "5.0" "6" "6.0"}
   "rhel" {"5" "5.0" "6" "6.0"}
   "ubuntu" {"hardy" "8.04", "karmic" "9.10", "lucid" "10.04",
             "maverick" "10.10", "natty" "11.04", "oneiric" "11.10",
             "precise" "12.04"}
   "windows" {"2008 Server R2" "2008 R2",
              "Server 2008" "2008",
              "Server 2008 SP2" "2008 SP2",
              "2008 Web" "2008",
              "2008 R2" "2008 R2",
              "2008 SP2" "2008 SP2",
              "2003 Standard" "2003",
              "Server 2008 R2" "2008 R2",
              "2008 Server" "2008"}})

(def ^{:doc "Map of non-canonical os-families to canonical versions"}
  os-families
  {})

;; this is based on jclouds' AWSEC2ReviseParsedImage
(defn default-parser-result
  [match]
  (when-let [[_ family version] match]
    (let [family (string/lower-case family)
          versions (os-versions family {})]
      {:os-family (keyword (os-families family family))
       :os-version (versions version version)})))

(def os-parsers
  {:amzn-linux
   #".*amzn-(?:hvm-)?ami-(?:pv-)?(.*)\.(i386|x86_64)(-ebs|\.manifest.xml)?"
   :amazon #"amazon/EC2 ([^ ]+) ([^ ]+).*"
   :canonical #".*/([^-]*)-([^-]*)-.*-(.*)(\\.manifest.xml)?"
   :rightimage
   #"[^/]*/RightImage[_ ]([^_]*)_([^_]*)_[^vV]*[vV](.*)(\.manifest.xml)?"
   :rightscale #"[^/]*/([^_]*)_([^_]*)_[^vV]*[vV](.*)(\.manifest.xml)?"})

(defn parse
  "Best guess os family and version from the image name"
  [{:keys [name] :as image}]
  (match [image]
    [{:owner-id "137112412989"
      :description #"Amazon Linux AMI.*"}]
    (let [[m version] (re-find (:amzn-linux os-parsers) name)]
      {:os-family :amzn-linux
       :os-version version
       :user {:username "ec2-user"}})

    [{:owner-id "137112412989"}]
    (default-parser-result (re-find (:amazon os-parsers) name))

    [{:owner-id "099720109477"}]
    (assoc (default-parser-result (re-find (:canonical os-parsers) name))
      :user {:username "ubuntu"})

    [{:owner-id "411009282317"
      :name #".*RightImage.*"}]
    (default-parser-result (re-find (:rightimage os-parsers) name))

    [{:owner-id "411009282317"}]
    (default-parser-result (re-find (:rightscale os-parsers) name))))

;; local Variables:
;; mode: clojure
;; eval: (define-clojure-indent (match 1))
;; End:
