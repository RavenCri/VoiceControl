# 项目介绍
> 该项目为毕设的APP端控制程序，基于百度语音提供的demo进行二次开发！
> 可以通过语音实时控制您家居设备状态。
> 通信框架/协议：HTTP、Rabbitmq

### 运行流程：

硬件端配置了一个随机ID。

由于没有量产，只是做了一个硬件demo，所以产品的ID写死在了硬件的配置文件里。

WEB端：

管理员：对出厂设备进行管理（即做的demo硬件的相关信息添加到数据库）。

用户：登录web端，可以管理自己的设备。

#### 后端项目：[毕设后端](https://github.com/RavenCri/VoiceControlService)

该后端项目里面的**smart-vue**子工程即为WEB端。

### 硬件端：

arduino Mega 2560上电 ->ESP32 连接RabbitMQ ->监听消息队列。订阅topic为自己的ID->接收消息->执行相关指令->控制设备

### APP：

APP登录->获取设备列表，并默认选择一个自己拥有的设备ID(可手动要控制的设置ID)-> 连接RabbitMQ（MQTT协议） ->语音唤醒->语音控制->识别并发送数据(topic：为要控制的设备id)

给RabbitMQ 消息队列。

由于ESP32端使用MQTT协议连接了RabbitMQ，并监听了该topic，所以完成了消息的接收，并成功控制了硬件端。

## 1.相关配置：

InitConfig：配置HTTP的host。

assets/WakeUp.bin 唤醒文件，可以通过百度语音生成。默认是：曼拉曼拉即可唤醒APP。

AndroidManifest.xml：配置百度语音相关参数：APP_ID、API_KEY、SECRET_KEY



