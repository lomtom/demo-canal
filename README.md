> 分享内容：**Canal**
>
> 分享人：**欧阳隆桐**
>
> 具体内容：
>
> 		1. Canal简介
> 		2. Canal原理
> 		3. Canal基于代码的实现





# Canal

官网：https://github.com/alibaba/canal/



[TOC]



## 1、简介

对于一个新的东西，我们就要提出我们的哲学三问：
<s>我是谁？我在哪？我要干嘛？</s>

**是什么？**

**干什么？**

**怎么做？**

### 1.1 是什么？

**Canal**是一个基于MySQL binlog的**高性能数据同步系统**，Canal被阿里巴巴（包括淘宝）都在广泛使用。（纯java开发）



**背景：**

在早期，大概在2010年，阿里巴巴由于双机房（杭州与美国）的服务器部署环境，未解决双机房带来的数据同步的需求，阿里巴巴就基于MySQL的的日志文件，实现数据增量操作的同步，所以衍生了数据库增量数据的订阅与消费的业务。

### 1.2 干什么？

基于日志增量订阅和消费的业务包括

- 数据库镜像
- 数据库实时备份（参考数据库主从模式）
- 索引构建和实时维护(拆分异构索引、倒排索引等)
- 业务 cache 刷新
- 带业务逻辑的增量数据处理

### 1.3 怎么做？

1. 配置MySQL
    - 开启binlog
2. 配置Canal
    - 在canal中配置MySQL

## 2、原理

### 2.1 Mysql原理

> 参考：
>
> 1. https://github.com/alibaba/canal/wiki/Introduction
>
> 2. https://www.cnblogs.com/rui517hua20/p/10422303.html

----> 主从复制模式

![](https://gitee.com/lomtom/mark/raw/master/images/%E5%88%86%E4%BA%AB/Canal/202107020001.png)

**主从模式原理：**

1. Master服务器记录binlog的变化（这些记录称为binlog事件，可以通过查看`show binlog events`）
2. 从服务器将主服务器的二进制日志事件复制到其中继日志。
3. 中继日志中的从服务器重做事件将随后更新其旧数据。



**作用（适用场景）：**

      1. 读写分离
      2. 数据库备份
      3. 数据库负载均衡
      4. 业务拆分访问

### 2.2 运用到Canal

> 参考：
>
> 1. https://github.com/alibaba/canal/wiki



![](https://gitee.com/lomtom/mark/raw/master/images/%E5%88%86%E4%BA%AB/Canal/202107020002.png)

原理：

1. Canal 模拟 MySQL 从机的交互协议，伪装成 mysql 从机，将 dump 协议发送到 MySQL Master 服务器。
2. MySQL Master 收到转储请求并开始将二进制日志推送到 slave（即 canal）。
3. Canal 将二进制日志对象解析为自己的数据类型（最初是字节流）

## 3、使用场景

1. Docker

2. RabbitMQ/kafka消息队列

3. 阿里云数据库

4. Prometheus：https://github.com/alibaba/canal/wiki/Prometheus-QuickStart

## 4、使用实例

### 4.1 搭建一个主从（k8s环境下）

搭建k8s主从

>  参考：
>
>  https://zhuanlan.zhihu.com/p/113003682
>
>  https://blog.csdn.net/qq_41929184/article/details/112306554
>
>  https://www.cnblogs.com/xiaoit/p/4489643.html
>
>  https://www.cnblogs.com/l-hh/p/9922548.html

```cmd
1. 查看主机状态
show master status;

File				position
mysql-bin.000004	382	

2.查看主机server id（要求主从不一致） 
show variables like '%server_id%';

3.查看mysql端口号与ip
ouyang-mysql-master-svc              NodePort    20.98.206.210    <none>        3306:32308/TCP                  33s
ouyang-mysql-slave-svc               NodePort    20.111.229.221   <none>        3306:32309/TCP                  28s

4.在从机设置主机配置
change master to master_host='20.98.206.210',master_port=3306,master_user='root',master_password='123456',master_log_file='binlog.000004',master_log_pos=382;

5.开启从机
start slave;

6.查看从机状态
show slave status;
SLAVE_IO_RUNNING ,SLAVE_MYSQL_RUNNING两个值为YES即为正确启动，否则自己根据下方的错误提示修改配置
```



**查看日志**

```
show binlog events;

show master status;

show binlog events in 'binlog.000002';
```

### 4.2 配置Canal与MySQL的通信

> 参考：
>
> https://github.com/alibaba/canal/wiki/QuickStart

1. 开启MySQl的binlog功能（一般都会开启）、配置server_id

```sql
1. 查看是否开启
show variables like 'log_%';

2.修改mysql.cnf(一般处于/etc/mysql/mysql.cnf.d或/etc/mysql/cnf.d)
 # 开启 binlog
log-bin=mysql-bin
# 选择 ROW 模式
binlog-format=ROW 
# 配置 MySQL replaction 需要定义，不要和 canal 的 slaveId 重复
server_id=1 

3.或者创建MySQL直接修改参数
apiVersion: v1
# 副本控制器
kind: ReplicationController
metadata:
  # RC的名字，全局唯一
  name: ouyang-mysql-master
  labels:
    name: ouyang-mysql-master
spec:
  # Pod 副本的期待数量
  replicas: 1
  selector:
    name: ouyang-mysql-pod-master
  #根据此模板创建Pod的副本(实例)
  template:
    metadata:
      labels:
        name: ouyang-mysql-pod-master
    spec:
      containers:
        - name: ouyang-mysql
          image: mysql
          ports:
            - containerPort: 3306
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: "123456" 
          args:
            - --binlog-ignore-db=mysql
            - --server-id=1
            # 禁用软连接
            - --symbolic-links=0
```

2. 创建一个用户用来连接MySQL，并且进行授权

```sql
CREATE USER canal IDENTIFIED BY 'canal';  
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
-- GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ;
FLUSH PRIVILEGES;
```

3. 下载canal

- 下载 canal, 访问 [release 页面](https://github.com/alibaba/canal/releases) , 选择需要的包下载, 如以 1.0.17 版本为例

  ```cmd
  wget https://github.com/alibaba/canal/releases/download/canal-1.0.17/canal.deployer-1.0.17.tar.gz
  ```

- 解压缩

  ```cmd
  mkdir /tmp/canal
  tar zxvf canal.deployer-$version.tar.gz  -C /tmp/canal
  ```

    - 解压完成后，进入 /tmp/canal 目录，可以看到如下结构

  ```cmd
  drwxr-xr-x 2 jianghang jianghang  136 2013-02-05 21:51 bin
  drwxr-xr-x 4 jianghang jianghang  160 2013-02-05 21:51 conf
  drwxr-xr-x 2 jianghang jianghang 1.3K 2013-02-05 21:51 lib
  drwxr-xr-x 2 jianghang jianghang   48 2013-02-05 21:29 logs
  ```

4. 配置canal

   ```properties
   编辑配置文件
   vi conf/example/instance.properties
   
   
   ## mysql serverId
   canal.instance.mysql.slaveId = 1234
   #position info，需要改成自己的数据库信息
   canal.instance.master.address = 127.0.0.1:3306 
   canal.instance.master.journal.name = 
   canal.instance.master.position = 
   canal.instance.master.timestamp = 
   #canal.instance.standby.address = 
   #canal.instance.standby.journal.name =
   #canal.instance.standby.position = 
   #canal.instance.standby.timestamp = 
   #username/password，需要改成自己的数据库信息
   canal.instance.dbUsername = canal  
   canal.instance.dbPassword = canal
   canal.instance.defaultDatabaseName =
   canal.instance.connectionCharset = UTF-8
   #table regex
   canal.instance.filter.regex = .\*\\\\..\*
   ```

5. 启动

   ```bash
   点击startup.bat或startup.sh
   
   2021-07-02 16:13:22.010 [destination = example , address = /8.16.0.211:32308 , EventParser] WARN  c.a.o.c.p.inbound.mysql.rds.RdsBinlogEventParserProxy - prepare to find start position just last position
    {"identity":{"slaveId":-1,"sourceAddress":{"address":"8.16.0.211","port":32308}},"postion":{"gtid":"","included":false,"journalName":"mysql-bin.000004","position":5440,"serverId":1,"timestamp":1625212991000}}
   2021-07-02 16:13:22.261 [destination = example , address = /8.16.0.211:32308 , EventParser] WARN  c.a.o.c.p.inbound.mysql.rds.RdsBinlogEventParserProxy - ---> find start position successfully, EntryPosition[included=false,journalName=mysql-bin.000004,position=5440,serverId=1,gtid=,timestamp=1625212991000] cost : 267ms , the next step is binlog dump
   ```

   