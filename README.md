# MyServer — 简化 HTTP/1.0 教学服务器

## 目录结构
myserver/
├── src/          # Java 源码
├── config/       # 服务器配置文件
├── www/          # 静态网页
├── cgi-bin/      # CGI 可执行脚本
├── README.md     # 本文档
└── .gitignore    # 不纳入版本控制的文件列表
## 如何运行
1. 编译  
   ```bash
   javac -d out src/*.java
启动
bash
复制
java -cp out Main config/server.conf
测试
浏览器访问 http://zzh:2345/index.html
作者
yt, 2025-10
