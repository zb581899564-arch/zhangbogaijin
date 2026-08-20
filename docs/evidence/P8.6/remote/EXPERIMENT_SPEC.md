# P8.6 单实例双算法100k对照

```text
instance=20_2_3_1
seed=20260808
population=100
maxFEs=100000
algorithms=ZHANGBO-FULL,HMOPSO-QGS-F(B1)
decoder=FM3
shift=LEFT_RIGHT
shiftSemantics=fatigue-shift-v2-common-gap
execution=serial
cpuAffinity=20-23
jvm=-Xmx4g
```

两条算法必须使用相同实例、SUT、疲劳参数、初始四向量种群和移位配置。该运行是当前语义下的单实例单seed性能/链路门，不属于正式多实例矩阵或论文统计实验。
