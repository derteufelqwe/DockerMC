# Node network performance analysis
## Setup
This setup consists of two Ubuntu VMs on the same host.
The data is gathered using Iperf3 and sent using 10MB packets. 
Higher bandwidth gets achieved by running up to 100 parallel streams.
For bandwidths higher than 10 GB two Iperf instances with 100 streams each are used.


## Native connection
| Bandwith | Streams | UDP | TCP |
| ------ | --- | --------- | -------- |
| 10 MB  | 1   | 10 MB/s   | 10 MB/s
| 100 MB | 10  | 100 MB/s  | 100 MB/s
| 1 GB   | 100 | 1 GB/s    | 1 GB/s
| 2 GB   | 100 | 2 GB/s    | 2 GB/s
| 5 GB   | 100 | 4.1 GB/s  | 5 GB/s
| 10 GB  | 100 | 4.1 GB/s  | 10 GB/s
| 20 GB  | 100 | -         | 10 GB/s


## Default docker overlay network
| Bandwith | Streams | UDP | TCP |
| ------ | --- | --------- | -------- |
| 10 MB  | 1   | 10 MB/s   | 10 MB/s
| 100 MB | 10  | 100 MB/s  | 100 MB/s
| 1 GB   | 100 | 1 GB/s    | 1 GB/s
| 2 GB   | 100 | 1.9 GB/s  | 2 GB/s
| 5 GB   | 100 | 2 GB/s    | 2.35 GB/s
| 10 GB  | 100 | 2 GB/s    | 2.65 GB/s
| 20 GB  | 100 | -         | -


## Weavenet overlay network
| Bandwith | Streams | UDP | TCP |
| ------ | --- | --------- | -------- |
| 10 MB  | 1   | 10 MB/s   | 10 MB/s
| 100 MB | 10  | 100 MB/s  | 100 MB/s
| 1 GB   | 100 | 1 GB/s    | 1 GB/s
| 2 GB   | 100 | 1.9 GB/s  | 2 GB/s
| 5 GB   | 100 | ?         | 2.2 GB/s
| 10 GB  | 100 | ?         | 2.25 GB/s
| 20 GB  | 100 | -         | -