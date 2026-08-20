# P6.2随机性说明

P6新增Qg/CFVF事件使用配置seed `20260808`创建的可注入`PseudoRandomGenerator`。P6.2档案组件本身不调用随机源。

B2P与B3影子等价测试采用同一份显式初始种群，并在两次算法运行前分别调用`JMetalRandom.getInstance().setSeed(20260808L)`。这是为了隔离作者派生代码仍共享jMetal全局随机单例的事实；没有修改默认Runner，也没有把普通作者算法运行描述为全程可重放。

