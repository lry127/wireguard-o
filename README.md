# wireguard-o

## 介绍

wireguard-o即wireguard-obfuscated, 通过对所有数据包执行xor计算以去除wireguard包特征，避免被GFW封锁。本repo运行在服务端。

## 使用

1. 前置要求：一台在墙外已经运行着**原版**wireguard的服务器 (可以使用[angristan/wireguard-install](https://github.com/angristan/wireguard-install) 安装)，假定其监听 127.0.0.1:6000 udp 端口。

2. 下载[jar包](https://github.com/lry127/wireguard-o/releases/download/v1.0/wireguard-o.jar), 并运行: `java -jar wireguard-o.jar your_secret_key_or_password 9123 127.0.0.1 6000`, 该命令会监听9123 udp端口。`your_secret_key_or_password` 是xor操作的种子，需保持服务端和客户端一致。

3. 前往[lry127/wireguard-android-o](https://github.com/lry127/wireguard-android-o) 下载Android客户端连接




