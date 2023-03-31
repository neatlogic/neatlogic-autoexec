中文 / [English](README.en.md)

## 关于

neatlogic-autoexec是自动化管理模块，自带自定义工具、组合工具、作业管理等功能。

## 主要功能

### 工具

工具分为系统自带的工具和自定义工具两种，工具的执行方式支持本地执行（runner）和在目标机器执行（target、runner->target）。<br>
![img.png](README_IMAGES/img.png)
自定义工具内置多种脚本解析器，支持常用的脚本类型，如python、javascript、perl、sh等，支持定义输入和输入参数，参数值可配置默认值。

### 组合工具

基于工具和自定义工具的组合脚本，通过创建阶段框架，在阶段中添加工具并完成工具配置。
![img.png](README_IMAGES/img1.png)
-支持定义作业参数，作业参数可被工具参数和执行目标引用
-支持在阶段或阶段组中预设执行目标，也可以预设整个作业的执行目标，阶段执行目标优先级更高

### 作业

在组合工具的基础上发起自动化作业
![img.png](README_IMAGES/img2.png)
在作业管理页面，可根据需求过滤目标作业，并查看作业详情
![img.png](README_IMAGES/img3.png)