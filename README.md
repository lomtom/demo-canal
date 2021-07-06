# Table of Contents

* [Canal](#canal)
  * [1、简介](#1、简介)
    * [1.1 是什么？](#11-是什么？)
    * [1.2 干什么？](#12-干什么？)
    * [1.3 怎么做？](#13-怎么做？)
  * [2、原理](#2、原理)
    * [2.1 Mysql原理](#21-mysql原理)
    * [2.2 运用到Canal](#22-运用到canal)
  * [3、使用场景](#3、使用场景)
  * [4、使用示例](#4、使用示例)
    * [4.1 MySQL主从模式理解同步机制](#41-mysql主从模式理解同步机制)
    * [4.2 配置Canal与MySQL的通信](#42-配置canal与mysql的通信)
    * [4.3 实现Canal发送消息至kafka](#43-实现canal发送消息至kafka)
    * [4.4 基于java直接消费Canal](#44-基于java直接消费canal)
  * [知识补充：](#知识补充：)



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





## 1、简介

对于一个新的东西，我们就要提出我们的哲学三问：
<s>我是谁？我在哪？我要干嘛？</s>

**是什么？**

**干什么？**

**怎么做？**

### 1.1 是什么？

**Canal**是一个基于MySQL binlog的**高性能数据同步（增量）系统**，Canal被阿里巴巴（包括淘宝）都在广泛使用。（纯java开发）



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
> 
> 3. 高性能MySQL（第3版）第十章第一节

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

1. 同步缓存redis/全文搜索ES

2. 数据库实时备份

3. 业务cache刷新

4. 带业务逻辑的增量数据处理

## 4、使用示例

### 4.1 MySQL主从模式理解同步机制

搭建一个主从（k8s环境下）

>  参考：
>
> https://zhuanlan.zhihu.com/p/113003682
>
> https://blog.csdn.net/qq_41929184/article/details/112306554
>
> https://www.cnblogs.com/xiaoit/p/4489643.html
>
> https://www.cnblogs.com/l-hh/p/9922548.html

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

### 4.3 实现Canal发送消息至kafka

1. 安装zookeeper
2. 安装kafka

第一步：修改canal的config/example/instance.properties
```properties
# mq config
canal.mq.topic=user
# dynamic topic route by schema or table regex
#canal.mq.dynamicTopic=mytest1.user,mytest2\\..*,.*\\..*
canal.mq.partition=0
```

第二步：修改canal的config/server.properties
```properties
1.修改消费模式
# tcp, kafka, rocketMQ, rabbitMQ
canal.serverMode = kafka

2.新增
# kafka/rocketmq 集群配置: 192.168.1.117:9092,192.168.1.118:9092,192.168.1.119:9092
canal.mq.servers = localhost:9092
canal.mq.retries = 0
# flagMessage模式下可以调大该值, 但不要超过MQ消息体大小上限
canal.mq.batchSize = 16384
canal.mq.maxRequestSize = 1048576
# flatMessage模式下请将该值改大, 建议50-200
canal.mq.lingerMs = 1
canal.mq.bufferMemory = 33554432
# Canal的batch size, 默认50K, 由于kafka最大消息体限制请勿超过1M(900K以下)
canal.mq.canalBatchSize = 50
# Canal get数据的超时时间, 单位: 毫秒, 空为不限超时
canal.mq.canalGetTimeout = 100
# 是否为flat json格式对象
canal.mq.flatMessage = true
canal.mq.compressionType = none
canal.mq.acks = all
# kafka消息投递是否使用事务
canal.mq.transaction = true
```
第三步：运行kafka
```cmd
bin/windows/kafka-server-start.bat config/server.properties
```

第四步：查看消费记录
```cmd
bin/windows/kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic 名字 --from-beginning
```

或使用代码
```java

<!--引入kafka依赖-->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

@KafkaListener(topics = "user")
public void consumer(ConsumerRecord consumerRecord){
    System.out.println("消费。。。。。。。");
    Optional<Object> kafkaMassage = Optional.ofNullable(consumerRecord.value());
    if(kafkaMassage.isPresent()){
        Object o = kafkaMassage.get();
        System.out.println(o);
    }
}
```
修改数据库后，查看记录：
```json
{
  "data": [
    {
      "id": "11",
      "username": "123",
      "password": "123",
      "tel_new": "123"
    }
  ],
  "database": "canal",
  "es": 1625469394000,
  "id": 3,
  "isDdl": false,
  "mysqlType": {
    "id": "int",
    "username": "varchar(255)",
    "password": "varchar(255)",
    "tel_new": "varchar(100)"
  },
  "old": [
    {
      "username": "1",
      "password": "1"
    }
  ],
  "pkNames": [
    "id"
  ],
  "sql": "",
  "sqlType": {
    "id": 4,
    "username": 12,
    "password": 12,
    "tel_new": 12
  },
  "table": "user",
  "ts": 1625469308716,
  "type": "UPDATE"
}

```
### 4.4 基于java直接消费Canal
前提：
需要将消费模式改回tcp
```properties
# tcp, kafka, rocketMQ, rabbitMQ
canal.serverMode = tcp
```
示例代码
```java
@Component
public class CanalClient implements InitializingBean {

    private final static int BATCH_SIZE = 1000;

    @Override
    public void afterPropertiesSet() {
        // 创建链接
        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress("localhost", 11111), "example", "", "");
        try {
            //打开连接
            connector.connect();
            //订阅数据库表,全部表
            connector.subscribe(".*\\..*");
            //回滚到未进行ack的地方，下次fetch的时候，可以从最后一个没有ack的地方开始拿
            connector.rollback();
            while (true) {
                // 获取指定数量的数据
                Message message = connector.getWithoutAck(BATCH_SIZE);
                //获取批量ID
                long batchId = message.getId();
                //获取批量的数量
                int size = message.getEntries().size();
                //如果没有数据
                if (batchId == -1 || size == 0) {
                    try {
                        //线程休眠2秒
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    //如果有数据,处理数据
                    printEntry(message.getEntries());
                }
                //进行 batch id 的确认。确认之后，小于等于此 batchId 的 Message 都会被确认。
                connector.ack(batchId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connector.disconnect();
        }
    }

    /**
     * 打印canal server解析binlog获得的实体类信息
     */
    private static void printEntry(List<Entry> entrys) {
        for (CanalEntry.Entry entry : entrys) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND) {
                //开启/关闭事务的实体类型，跳过
                continue;
            }
            //RowChange对象，包含了一行数据变化的所有特征
            //比如isDdl 是否是ddl变更操作 sql 具体的ddl sql beforeColumns afterColumns 变更前后的数据字段等等
            RowChange rowChage;
            try {
                rowChage = RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(), e);
            }
            //获取操作类型：insert/update/delete类型
            EventType eventType = rowChage.getEventType();
            //打印Header信息
            System.out.println(String.format("================》; binlog[%s:%s] , name[%s,%s] , eventType : %s",
                    entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                    entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),
                    eventType));
            //判断是否是DDL语句
            if (rowChage.getIsDdl()) {
                System.out.println("------->;isDdl: true    ------->   sql:" + rowChage.getSql());
            }
            //获取RowChange对象里的每一行数据，打印出来
            for (RowData rowData : rowChage.getRowDatasList()) {
                //如果是删除语句
                if (eventType == EventType.DELETE) {
                    printColumn(rowData.getBeforeColumnsList());
                    //如果是新增语句
                } else if (eventType == EventType.INSERT) {
                    printColumn(rowData.getAfterColumnsList());
                    //如果是更新的语句
                } else {
                    //变更前的数据
                    System.out.println("------->; before");
                    printColumn(rowData.getBeforeColumnsList());
                    //变更后的数据
                    System.out.println("------->; after");
                    printColumn(rowData.getAfterColumnsList());
                }
            }
        }
    }

    private static void printColumn(List<Column> columns) {
        for (Column column : columns) {
            System.out.println(column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
        }
    }
}

```


## 知识补充：
1. DDL和DML

https://www.jb51.net/article/40359.htm
>DML（data manipulation language）：
>
> 它们是SELECT、UPDATE、INSERT、DELETE，就象它的名字一样，这4条命令是用来对数据库里的数据进行操作的语言
>
>DDL（data definition language）：
>
>DDL比DML要多，主要的命令有CREATE、ALTER、DROP等，DDL主要是用在定义或改变表（TABLE）的结构，数据类型，表之间的链接和约束等初始化工作上，他们大多在建立表时使用
>
>DCL（Data Control Language）：
>
>是数据库控制功能。是用来设置或更改数据库用户或角色权限的语句，包括（grant,deny,revoke等）语句。在默认状态下，只有sysadmin,dbcreator,db_owner或db_securityadmin等人员才有权力执行DCL


2. 运行结果记录
```bash
1. 新增

================》; binlog[mysql-bin.000004:10218] , name[canal,user] , eventType : INSERT
id : 11    update=true
username : 1    update=true
password : 1    update=true

2.更新
================》; binlog[mysql-bin.000004:9905] , name[canal,user] , eventType : UPDATE
------->; before
id : 1    update=false
username : 123    update=false
password : 123    update=false
------->; after
id : 1    update=false
username : 1234    update=true
password : 123    update=false


3.删除
================》; binlog[mysql-bin.000004:10510] , name[canal,user] , eventType : DELETE
id : 22    update=false
username : 123453    update=false
password : 212    update=false

4.DDl操作
================》; binlog[mysql-bin.000004:11223] , name[canal,user] , eventType : ALTER
------->;isDdl: true    ------->   sql:ALTER TABLE user CHANGE tel tel_new varchar(100)
```

3. MySQl新增、更新和 删除字段
   
>参考:
>
>https://www.cnblogs.com/ningqing2015/articles/9799727.html
```sql
1.新增字段
ALTER TABLE 表名 ADD 字段名 字段类型(字段长度) DEFAULT 默认值 COMMENT '注释';
例如：ALTER TABLE user ADD tel CHAR(11) DEFAULT NULL COMMENT '手机号';

2.更新字段
ALTER TABLE 表名 CHANGE 旧字段名  新字段名 新数据类型;
例如：ALTER TABLE user CHANGE tel tel_new varchar(100);;
    
    
3.删除字段
ALTER TABLE 表名 DROP 字段名;
例如：ALTER TABLE user DROP tel_new ;
```

4. 数据同步全量与增量
> 参考：
> 
> https://www.cnblogs.com/big1987/p/8522884.html
