# 配置中心

1. 简介
> 配置中心现在基本上是大型互联网公司的标配，用于存储管理公司内部各个系统的配置，降低维护成本。本配置中心提供了配置管理基本功能，提供配置更新推送能力，提供客户端配置缓存能力。

2. 环境要求：
> * 服务端：jdk1.8
> * 客户端：jdk1.8
> * zookeeper

> 注意：本系统已经上传到[maven中央库](http://search.maven.org/#search%7Cga%7C1%7Corg.antframework.configcenter)

### 1. 整体设计
配置就是不同应用在不同环境的一些键值对。本配置中心内的角色有：服务端、客户端、zookeeper。

服务端：管理不同应用在不同环境中的配置，配置数据落地到MySQL数据库。为客户端提供http查询某个应用在某个环境的配置。当一个应用在某个环境中的配置进行了更新（增删改），则服务端会将zookeeper上的对应的节点进行更新（客户端会监听对应节点）。

客户端：客户端刚启动时会通过http请求调用服务端读取当前应用在当前环境中的配置。如果从服务端查询失败，则客户端会尝试从本地缓存文件中读取配置，如果本地无缓存文件，则会抛出异常。客户端启动成功后，外部可以触发客户端向zookeeper上注册监听器，监听当前应用和当前应环境在zookeeper中的节点。当节点有更新，则客户端会通过http请求调用服务端读取最新配置，并且将最新配置和当前客户端中旧配置进行比较，将变化部分通知给当前应用。

zookeeper：仅仅作为通知工具，并不存储任何配置。当配置有变更，服务端会更新zookeeper的对应节点。节点有更新，则zookeeper会通知监听这个节点的监听器（客户端）。客户端收到通知后就会调用服务端查询最新配置。

应用编码为common的应用会作为公共配置，所有应用的配置都会继承公共配置。同时每个应用可以将部分配置设置为公开，这样其他应用可以读取这个应用的公开配置（对于一个应用需要开发客户端给其他应用使用时，这个特性特别好用）。

![image](http://note.youdao.com/yws/api/personal/file/4E2BD7EC88CD4DE18716157F592EC18D?method=download&shareKey=34b9f8760af2efc3d6dca89654fb814c)

### 2. 启动服务端
[下载服务端](https://repo.maven.apache.org/maven2/org/antframework/configcenter/configcenter-assemble/1.0.0.RELEASE/configcenter-assemble-1.0.0.RELEASE-exec.jar)。说明：
1. 服务端使用的springboot，直接命令启动下载好的jar包即可，无需部署tomcat。
2. 服务端使用hibernate自动生成表结构，无需导入sql。
3. 服务端在启动时会在"/var/apps/"下创建日志文件，请确保服务端对该目录拥有写权限。
4. 由于配置中心本身就是用来管理各个环境中的配置，所以大部分公司只需部署两套，一是线下环境配置中心（管理所有非线上环境配置）；二是线上环境配置中心（管理线上环境配置）。
5. 线下环境编码：offline，线上环境编码：online（可以根据各公司自己情况自己定义，这里只是根据我个人习惯推荐的两个编码）

启动命令模板：

    java -jar configcenter-assemble-1.0.0.RELEASE-exec.jar --spring.profiles.active="环境编码" --server.port="端口" --spring.datasource.url="数据库连接" --spring.datasource.username="数据库用户名" --spring.datasource.password="数据库密码" --configcenter.zk-urls="配置中心使用的zookeeper地址,如果存在多个zookeeper以英文逗号分隔"

比如我本地开发时启动命令：

    java -jar configcenter-assemble-1.0.0.RELEASE-exec.jar --spring.profiles.active="offline" --server.port="9090" --spring.datasource.url="jdbc:mysql://localhost:3306/configcenter-dev?useUnicode=true&characterEncoding=utf-8" --spring.datasource.username="root" --spring.datasource.password="root" --configcenter.zk-urls="localhost:2181"

### 3. 集成客户端

##### 1. 引入客户端依赖

        <dependency>
            <groupId>org.antframework.configcenter</groupId>
            <artifactId>configcenter-client</artifactId>
            <version>1.0.0.RELEASE</version>
        </dependency>

##### 2. 使用客户端

    客户端就是Java类，直接new就可以，只是需要传给它相应参数。


        // 设置初始化参数
        ConfigContext.InitParams initParams = new ConfigContext.InitParams();
        initParams.setProfileCode("dev");  // 环境编码（在服务端配置）
        initParams.setAppCode("demo");  // 应用编码（在服务端配置） 
        initParams.setQueriedAppCode("demo");  // 被查询应用编码（在服务端配置。当前应用可能需要查询其他应用的配置，需传对应应用的编码，只能查询到其他应用公开的配置）
        initParams.setServerUrl("http://localhost:8080");  // 服务端地址
        initParams.setCacheFilePath("/var/config/demo.properties");  // 配置缓存文件路径
        initParams.setZkUrls("localhost:2181");  // zookeeper地址。如果存在多个zookeeper地址，则以“,”相隔
        
        // 启动客户端（启动时会读取配置，读取不成功会抛异常。一个应用可以new多个客户端，各个客户端之间互不影响）
        ConfigContext configContext = new ConfigContext(initParams);
        
        // 注册配置监听器（用于监听配置变更，xxxListener是你自己定义的配置监听器）
        configContext.getListenerRegistrar().register(xxxListener);
        // 触发客户端向zookeeper注册监听器
        configContext.listenConfigModified();

        // 客户端启动好了，现在可以获取配置了，调用configContext.getProperties()
        // 比如需要和spring集成的话，可以在spring启动前将客户端包装成Environment的一个属性资源，这样配置中心里的配置就可以应用的spring了

        // 当要关闭当前系统时，需调用下面方法关闭客户端，用于释放相关资源（zookeeper链接，http客户端）。
        // 想省事的话，可以直接将客户端注入到spring容器，spring容器在关闭时会自动调用close方法.
        configContext.close();
        
##### 3. 与spring集成

上面介绍的是客户端的核心功能，使用这些功能进行开发是已经足够的。但是光使用这些核心功能进行开发是很繁琐的，为此，提供了一些附加能力：与spring集成的属性资源类（ConfigcenterPropertySource）、注解形式的配置监听器（@ConfigListener）。

###### 3.1 将配置中心加入到spring的environment中：

        // 将配置中心设置到environment中
        ConfigcenterPropertySource propertySource = new ConfigcenterPropertySource(ConfigcenterPropertySource.PROPERTY_SOURCE_NAME, configContext);
        environment.getPropertySources().addLast(propertySource);
        
###### 3.2 使用注解形式的配置监听器

使用时可以参考[ant-boot集成配置中心部分](https://github.com/zhongxunking/ant-boot/tree/master/ant-boot-starters/ant-boot-starter-config/src/main/java/org/antframework/boot/config/boot)

引入事件总线依赖（不了解事件总线也没有关系，如果想进一步了解可以查看[事件总线文档](https://github.com/zhongxunking/bekit)）：

        <dependency>
            <groupId>org.bekit</groupId>
            <artifactId>event</artifactId>
            <version>1.2.2.RELEASE</version>
        </dependency>

配置事件总线与配置监听器：

        @Configuration
        @Import(EventBusConfiguration.class)
        public class ConfigConfiguration {
            // 监听属性被修改触发器
            @Bean
            public ListenConfigModifiedTrigger listenConfigModifiedTrigger(DefaultConfigListener defaultConfigListener) {
                return new ListenConfigModifiedTrigger(ConfigContextHolder.get(), defaultConfigListener);
            }
        
            // 默认的配置监听器
            @Bean
            public DefaultConfigListener defaultConfigListener() {
                return new DefaultConfigListener();
            }
        }

使用注解形式的配置监听器：

        @ConfigListener
        public class ThreadPoolConfigListener {
        
            // 监听属性被修改，prefix表示需要监听的属性前缀。当以“pool.”开头的属性被修改时，会调用本方法，被修改的属性回座位入参。比如pool.aa、pool.aa.bb等被修改时都会调用本方法
            @ListenConfigModified(prefix = "pool")  
            public void listenPool(List<ModifiedProperty> modifiedProperties) {
                logger.info("监听到线程池配置被修改：" + modifiedProperties);
            }
        }


### 4. 管理配置
后台管理中管理员有两种：超级管理员、普通管理员。超级管理员可以管理所有配置，也可以管理其他管理员；普通管理员只能管理分配给他的应用的配置。

> 根据下面的介绍登陆成功后，请添加一个应用编码为“common”的应用，该应用会作为公共配置。

#### 页面挺丑的，但功能是全的。

##### 登录链接模板：http://IP地址:端口/html/login.html（比如我本地开发时的登录链接：http://localhost:8080/html/login.html ）
#### 第一次使用时会让你设置一个超级管理员：

![image](http://note.youdao.com/yws/api/personal/file/85F715EF5C574FAC866F327D7D35396E?method=download&shareKey=2dd73f83d6700c3651513834078e5739)

#### 然后进行登陆进入管理页面：

![image](http://note.youdao.com/yws/api/personal/file/BCC71043C36A4B1694DAFD6058652AA1?method=download&shareKey=e14cd88177df559477464d4a71f2c7eb)

#### 点击左侧按钮进入相应页面，在此仅展示配置管理页面（点击上图中的环境链接）

![image](http://note.youdao.com/yws/api/personal/file/EDEF433FBF2F4F109F44D952B2A43249?method=download&shareKey=ea2c3fc801049b76128c6b6ffc4ec261)
