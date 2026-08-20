# 已取代的中间运行

- 首次尝试使用旧Surefire不支持的`Class#method1+method2`过滤语法，Surefire 2.12.4自身抛出`StringIndexOutOfBoundsException`；改为运行完整测试类后消除，该失败不属于产品代码。
- P6.1.1完成门以最终`TEST_PDDR_INTEGRATION_2000FE.log`和P6.2目录中的定向回归/构建日志为准。

