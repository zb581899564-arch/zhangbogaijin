import fs from "node:fs/promises";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = "E:/学习/李明哲-毕业材料/张博改进";
const input = JSON.parse(await fs.readFile(`${root}/paper_evidence/I0/01_input/i0_input.json`, "utf8"));
const output = `${root}/outputs/019feb24-6dba-7651-be7c-54b0323631aa/I0_v2共同空档左移右移_本人手算空白模板.xlsx`;
const previewDir = `${root}/outputs/019feb24-6dba-7651-be7c-54b0323631aa/work_i1_left_right/previews_i0_v2`;

const wb = Workbook.create();
const colors = {
  navy: "#0E5261", teal: "#2F7A80", blue: "#DCEBF7", yellow: "#FFF4CC",
  green: "#E2F0D9", red: "#FCE4D6", gray: "#E7E6E6", white: "#FFFFFF",
  text: "#243746", line: "#B7C9D3", darkYellow: "#F4B183"
};

function title(sheet, text, endCol) {
  const range = sheet.getRange(`A1:${endCol}1`);
  range.merge();
  range.values = [[text]];
  range.format = {fill: colors.navy, font: {bold: true, color: colors.white, size: 16}, horizontalAlignment: "center", verticalAlignment: "center"};
  range.format.rowHeight = 32;
}

function header(range) {
  range.format = {fill: colors.teal, font: {bold: true, color: colors.white}, horizontalAlignment: "center", verticalAlignment: "center", wrapText: true, borders: {preset: "all", style: "thin", color: colors.line}};
}

function inputArea(range) {
  range.format = {fill: colors.yellow, font: {color: colors.text}, borders: {preset: "all", style: "thin", color: colors.line}, verticalAlignment: "center"};
  range.format.numberFormat = "0.000000000000";
}

function fixedArea(range) {
  range.format = {fill: colors.blue, font: {color: colors.text}, borders: {preset: "all", style: "thin", color: colors.line}, verticalAlignment: "center"};
}

function noteArea(range) {
  range.format = {fill: colors.gray, font: {color: colors.text}, borders: {preset: "all", style: "thin", color: colors.line}, wrapText: true, verticalAlignment: "top"};
}

function setupSheet(sheet) {
  sheet.showGridLines = false;
}

const start = wb.worksheets.add("从这里开始"); setupSheet(start); title(start, "I0 新版共同空档左移/右移——本人手算包（不含程序数值答案）", "H");
start.getRange("A3:B12").values = [
  ["你只需要做什么", "先算10道工序的S0，再完整重算1个FCLS后的S1，最后完整重算1个FCRS后的S2"],
  ["不用做什么", "不用手算程序内部42次完整传播；它只用于程序审计"],
  ["算例", "I0：5工件×2工厂×2阶段"],
  ["粒子", "新版X0；只公开编码与输入，不给开始/结束时间和目标答案"],
  ["移位语义", "fatigue-shift-v2-common-gap / LEFT_RIGHT"],
  ["FCLS", "机器空档∩工人空档；真实提前、可行且Cmax不变差才接受"],
  ["FCRS", "冻结S1的全局CmaxStar；提议延迟失败后最多减半回退10次"],
  ["每次移位", "必须从头重传播工件前序、机器、工人、恢复、疲劳、动态工时、能耗、成本和目标"],
  ["外部FE", "整个粒子解码仍只计1次jMetal FE；内部传播不加FE"],
  ["答案状态", "INPUT_ONLY；本人完成并提交前不生成程序对照答案"]
];
fixedArea(start.getRange("A3:A12")); noteArea(start.getRange("B3:B12"));
start.getRange("A14:D19").values = [
  ["顺序", "现在打开哪张表", "做到什么程度", "完成后再做什么"],
  [1, "粒子X0解码", "看懂5个位置分别是哪一个工件、工厂、机器、工人", "第一道工序练习"],
  [2, "第一道工序练习", "只算一行PT0、SET0、AT0、倍率、结束疲劳", "S0基础排程"],
  [3, "S0基础排程", "按资源状态连续算完10行", "FCLS共同空档"],
  [4, "FCLS共同空档 + S1左移后排程", "找到并接受第一个合格共同空档，然后重算10行", "FCRS减半回退"],
  [5, "FCRS减半回退 + S2右移后排程", "找到第一个合格右移并重算10行", "目标与提交"]
];
header(start.getRange("A14:D14")); fixedArea(start.getRange("A15:D19"));
start.getRange("F3:H10").values = [
  ["颜色", "含义", "你要不要填"],
  ["蓝色", "冻结输入/提示", "不要改"],
  ["黄色", "本人手算区", "要填"],
  ["绿色", "本人核对通过", "最后填"],
  ["灰色", "规则或口径", "只读"],
  ["精度", "中间值至少保留12位小数", "是"],
  ["容差", "最终程序对照绝对误差≤1e-9", "提交后检查"],
  ["最重要", "先做第一道工序，不要一口气看完整表", "是"]
];
header(start.getRange("F3:H3")); noteArea(start.getRange("F4:H10"));
start.getRange("A3:A19").format.columnWidth = 18; start.getRange("B3:B19").format.columnWidth = 58; start.getRange("C14:D19").format.columnWidth = 28; start.getRange("F3:H10").format.columnWidth = 22;

const jobs = wb.worksheets.add("输入_工件"); setupSheet(jobs); title(jobs, "I0 冻结工件数据（蓝色，只读）", "E");
jobs.getRange("A3:E13").values = [["工件J", "阶段S", "ST", "SUT", "来源"], ...input.jobData.map(x => [x.job, x.stage, x.ST, x.SUT, "i0_input.json"] )];
header(jobs.getRange("A3:E3")); fixedArea(jobs.getRange("A4:E13")); jobs.getRange("A:E").format.columnWidth = 18;

const machines = wb.worksheets.add("输入_机器"); setupSheet(machines); title(machines, "I0 冻结机器数据（蓝色，只读）", "F");
machines.getRange("A3:F11").values = [["工厂F", "阶段S", "机器M", "速度MS", "功率", "来源"], ...input.machineData.map(x => [x.factory, x.stage, x.machine, x.speed, x.power, "i0_input.json"] )];
header(machines.getRange("A3:F3")); fixedArea(machines.getRange("A4:F11")); machines.getRange("A:F").format.columnWidth = 18;

const workers = wb.worksheets.add("输入_工人"); setupSheet(workers); title(workers, "I0 冻结工人数据（蓝色，只读）", "J");
workers.getRange("A3:J11").values = [["工厂F", "阶段S", "局部工人W", "全局工人", "效率WE", "成本率", "lambda", "mu", "r", "来源"], ...input.workerData.map(x => [x.factory, x.stage, x.localWorker, x.globalWorker, x.efficiency, x.costRate, x.lambda, x.mu, input.fatigueModel.maximumIncreaseR, "i0_input.json"] )];
header(workers.getRange("A3:J3")); fixedArea(workers.getRange("A4:J11")); workers.getRange("A:J").format.columnWidth = 16; workers.getRange("G4:I11").format.numberFormat = "0.000000000000";

const vector = wb.worksheets.add("粒子X0解码"); setupSheet(vector); title(vector, "新版I0粒子X0：先把位置翻译成工件身份资源", "H");
const x = input.goldenParticleX0;
vector.getRange("A3:F7").values = [
  ["向量/位置", 1, 2, 3, 4, 5],
  ["JS", ...x.JS], ["FA", ...x.FA], ["MA", ...x.MA], ["WA", ...x.WA]
];
header(vector.getRange("A3:F3")); fixedArea(vector.getRange("A4:F7"));
vector.getRange("A9:H14").values = [
  ["位置p", "工件J=JS[p]", "工厂F=FA[p]", "S1机器M=MA[p]", "S1工人W=WA[p]", "工件身份资源包", "核对", "提示"],
  [1, 3, 1, 1, 1, "J3→F1-S1-M1-W1", "", "按位置读资源"],
  [2, 1, 2, 1, 1, "J1→F2-S1-M1-W1", "", "按位置读资源"],
  [3, 2, 2, 1, 1, "J2→F2-S1-M1-W1", "", "按位置读资源"],
  [4, 4, 1, 1, 2, "J4→F1-S1-M1-W2", "", "按位置读资源"],
  [5, 5, 1, 2, 2, "J5→F1-S1-M2-W2", "", "按位置读资源"]
];
header(vector.getRange("A9:H9")); fixedArea(vector.getRange("A10:F14")); inputArea(vector.getRange("G10:G14")); noteArea(vector.getRange("H10:H14"));
vector.getRange("A16:H20").values = [
  ["工厂", "第一阶段JS顺序", "第二阶段顺序如何得到", "第二阶段机器", "第二阶段工人", "状态", "说明", "别做错"],
  ["F1", "J3→J4→J5", "按S1完工时间ECT排序；同刻FIFO；再按工件号", "首件M1，之后最早可用机器", "FM3选疲劳修正后最早完工者", "待手算", "由S0计算得到", "不要把MA/WA直接用于S2"],
  ["F2", "J1→J2", "同上", "同上", "同上", "待手算", "由S0计算得到", "资源编号是工厂阶段内局部编号"],
  ["FCLS", "机器空档与工人空档取交集", "每个可行候选都完整重传播", "序列可能改变", "序列可能改变", "新版", "接受门只锁可行+提前+Cmax不恶化", "不再要求TEC/TWC同时Pareto安全"],
  ["FCRS", "冻结S1的CmaxStar", "序列完全不变，只加release", "不变", "不变", "新版", "失败后proposal/2，最多10次", "TEC/TWC不恶化且至少一个改善"]
];
header(vector.getRange("A16:H16")); noteArea(vector.getRange("A17:H20")); vector.getRange("A:H").format.columnWidth = 24;

const first = wb.worksheets.add("第一道工序练习"); setupSheet(first); title(first, "只练第一道工序：J3-S1（先完成这一页）", "H");
first.getRange("A3:D12").values = [
  ["项目", "冻结输入/公式", "本人计算", "自查问题"],
  ["资源", "F1-S1-M1-W1", "", "来自X0位置1"],
  ["ST", 8, "", "见输入_工件"], ["SUT", 2, "", "见输入_工件"],
  ["MS", 1, "", "见输入_机器"], ["WE", 1, "", "见输入_工人"],
  ["lambda", 0.02, "", "见输入_工人"], ["mu", 0.05, "", "见输入_工人"],
  ["r", 0.3, "", "固定"], ["三项可用时间", "前序完工、机器可用、工人可用", "", "第一道工序均从初始状态判断"]
];
header(first.getRange("A3:D3")); fixedArea(first.getRange("A4:B12")); inputArea(first.getRange("C4:C12")); noteArea(first.getRange("D4:D12"));
first.getRange("A14:H23").values = [
  ["顺序", "要算什么", "公式（只作文字提示）", "本人结果", "单位", "上一状态", "更新后状态", "核对"],
  [1, "PT0", "ST/(MS×WE)", "", "时间", "—", "—", ""],
  [2, "SET0", "SUT/WE", "", "时间", "—", "—", ""],
  [3, "AT0", "PT0+SET0", "", "时间", "—", "—", ""],
  [4, "开始S", "max(前序完工,机器可用,工人可用)", "", "时间", "三项可用时间", "—", ""],
  [5, "恢复R", "若工人以前做过任务：S-上次结束；否则0", "", "时间", "上次人结束", "—", ""],
  [6, "开始疲劳Fstart", "Flast×exp(-mu×R)", "", "—", "Flast", "—", ""],
  [7, "倍率", "1+r/ln2×ln(1+Fstart)", "", "—", "Fstart", "—", ""],
  [8, "实际AT", "AT0×倍率", "", "时间", "—", "—", ""],
  [9, "结束C与结束疲劳", "C=S+实际AT；Fend=Fstart+(1-Fstart)(1-exp(-lambda×实际AT))", "", "时间/疲劳", "—", "更新工件/机器/工人/疲劳", ""]
];
header(first.getRange("A14:H14")); fixedArea(first.getRange("A15:C23")); inputArea(first.getRange("D15:D23")); noteArea(first.getRange("E15:H23"));
first.getRange("A:H").format.columnWidth = 23; first.getRange("C14:C23").format.columnWidth = 44;

const scheduleHeaders = ["序号","工件J","阶段S","工厂F","机器M","工人W","前序完工","机器可用","工人可用","开始S","上次人结束","恢复R","恢复前Flast","开始F","ST","SUT","MS","WE","lambda","mu","r","PT0","SET0","AT0","倍率","实际PT","实际SET","实际AT","结束C","结束F","机器空闲","功率","能耗","成本","本人备注"];
function addSchedule(name, titleText) {
  const s = wb.worksheets.add(name); setupSheet(s); title(s, titleText, "AI");
  s.getRange("A3:AI3").values = [scheduleHeaders]; header(s.getRange("A3:AI3"));
  const rows = [];
  for (let i=1;i<=10;i++) rows.push([i, ...Array(34).fill("")]);
  s.getRange("A4:AI13").values = rows; fixedArea(s.getRange("A4:A13")); inputArea(s.getRange("B4:AI13"));
  s.getRange("A15:H20").values = [
    ["每算完一行立刻更新", "工件阶段完工", "机器可用", "工人可用", "工人最后疲劳", "机器累计工作", "机器首用/末用", "别忘了"],
    ["状态1", "C", "C", "C", "Fend", "+实际AT", "记录开始/结束", "同一工人跨阶段/机器也不能重叠"],
    ["状态2", "下一阶段只读前阶段C", "下一任务读取", "下一任务读取", "恢复从该Fend开始", "用于能耗", "用于机器空闲", "同一工件不能跨工厂"],
    ["第二阶段", "先按ECT/FIFO排顺序", "FAM选机器", "FM3逐工人比较候选结束", "候选使用自己的恢复与倍率", "—", "—", "不能只看工人可用时间"],
    ["能耗", "实际AT×功率", "+机器空闲×1", "—", "—", "—", "首用空闲为0", "按当前生产口径"],
    ["成本", "(实际AT+机器空闲)×成本率", "—", "—", "—", "—", "—", "按当前生产口径"]
  ];
  header(s.getRange("A15:H15")); noteArea(s.getRange("A16:H20"));
  s.freezePanes.freezeRows(3); s.freezePanes.freezeColumns(6);
  s.getRange("A:AI").format.columnWidth = 14; s.getRange("AG:AI").format.columnWidth = 20;
  s.getRange("G4:AH13").format.numberFormat = "0.000000000000";
  return s;
}

addSchedule("S0基础排程", "FM3基础调度S0：10道工序本人完整解码");

const fcls = wb.worksheets.add("FCLS共同空档"); setupSheet(fcls); title(fcls, "FCLS：机器—工人共同空档左移（新版）", "N");
fcls.getRange("A3:N10").values = [
  ["步骤", "目标工序", "原机器位置", "原工人位置", "目标机器槽", "目标工人槽", "机器空档", "工人空档", "共同空档交集", "预测开始", "预测结束", "是否真实提前", "是否可放入", "本人说明"],
  [1,"","","","","","[      ,      ]","[      ,      ]","[      ,      ]","","","","","先从S0资源时间线找交集"],
  [2,"","","","","","","","","","","","","按最早预测开始稳定排序"],
  [3,"","","","","","","","","","","","","最多检查冻结门记录的6个候选"],
  ["接受门1","","","","","","","","","","","开始必须严格提前","",""],
  ["接受门2","","","","","","","","","","","","完整重传播后可行",""],
  ["接受门3","","","","","","","","","","","","新Cmax≤旧Cmax+1e-9",""],
  ["结果","","","","","","","","","","","","","首个通过者形成S1"]
];
header(fcls.getRange("A3:N3")); inputArea(fcls.getRange("A4:N10")); fcls.getRange("A:N").format.columnWidth = 18; fcls.getRange("G:N").format.columnWidth = 23;
fcls.getRange("A12:H16").values = [
  ["重要", "共同空档只用于提出候选", "真正接受前必须怎么做", "重算内容1", "重算内容2", "重算内容3", "外部FE", "程序审计"],
  ["规则", "机器空档∩工人空档", "从头完整传播全部10道工序", "恢复与疲劳", "动态PT/SET/AT", "能耗/成本/Cmax/TEC/TWC", "不增加", "内部可能传播多次"],
  ["你本人", "只记录首个接受候选", "把S1十行重新填一遍", "不能平移一条甘特条", "后续工序也可能变化", "目标必须重算", "写1次粒子评价", "不用手算全部42次"],
  ["I0门", "新版固定粒子存在真实FCLS接受", "不在此表给答案", "本人先算", "提交后程序对照", "误差≤1e-9", "—", "左候选总数6"],
  ["I1旁证", "I1真实1次FCLS接受", "I1只作工程证据", "—", "—", "—", "—", "不替代I0本人手算"]
];
header(fcls.getRange("A12:H12")); noteArea(fcls.getRange("A13:H16"));

addSchedule("S1左移后排程", "FCLS接受后S1：按新资源序列完整重传播10道工序");

const fcrs = wb.worksheets.add("FCRS减半回退"); setupSheet(fcrs); title(fcrs, "FCRS：冻结CmaxStar后的右移与最多10次减半回退", "P");
fcrs.getRange("A3:P15").values = [
  ["目标工序","反向拓扑序号","原开始","实际AT","冻结CmaxStar","后继最早开始","proposalUpper","初始延迟Δ0","attempt","本次延迟Δ0/2^(attempt-1)","release","传播后开始","传播后Cmax","传播后TEC","传播后TWC","接受/拒绝原因"],
  ["","","","","","","","",1,"","","","","","",""],
  ["","","","","","","","",2,"","","","","","",""],
  ["","","","","","","","",3,"","","","","","",""],
  ["","","","","","","","",4,"","","","","","",""],
  ["","","","","","","","",5,"","","","","","",""],
  ["","","","","","","","",6,"","","","","","",""],
  ["","","","","","","","",7,"","","","","","",""],
  ["","","","","","","","",8,"","","","","","",""],
  ["","","","","","","","",9,"","","","","","",""],
  ["","","","","","","","",10,"","","","","","",""],
  ["接受门","开始必须严格推迟","—","—","新Cmax≤CmaxStar","—","—","—","—","—","—","是","是","≤前一状态","≤前一状态","且TEC/TWC至少一项严格改善"],
  ["最终","首个通过的attempt","","","","","","","","","","","","","","形成S2"]
];
header(fcrs.getRange("A3:P3")); inputArea(fcrs.getRange("A4:P15")); fcrs.getRange("A:P").format.columnWidth = 19; fcrs.getRange("J:J").format.columnWidth = 30;
fcrs.getRange("A17:H20").values = [
  ["牢记", "序列是否改变", "CmaxStar何时冻结", "失败怎么办", "每次attempt", "外部FE", "I0门", "I1旁证"],
  ["规则", "工件/机器/工人顺序全部不变", "S1完成后只冻结一次", "延迟减半再试，最多10次", "完整重传播全部10道工序", "不增加", "存在真实FCRS接受", "I1真实3次FCRS接受"],
  ["你本人", "不要改JS/资源序", "先填在E列", "逐行写拒绝原因", "不是只改目标工序", "写1次粒子评价", "只需讲首个接受事件", "75次内部传播只作程序审计"],
  ["公式", "proposalUpper=min(CmaxStar,所有后继开始)-实际AT", "Δ0=proposalUpper-原开始", "Δa=Δ0/2^(a-1)", "release=原开始+Δa", "—", "—", "—"]
];
header(fcrs.getRange("A17:H17")); noteArea(fcrs.getRange("A18:H20"));

addSchedule("S2右移后排程", "FCRS接受后S2：固定资源顺序并完整重传播10道工序");

const summary = wb.worksheets.add("目标与提交"); setupSheet(summary); title(summary, "S0→S1→S2目标重建与本人提交", "I");
summary.getRange("A3:I16").values = [
  ["指标", "S0", "S1", "S2", "S1-S0", "S2-S1", "计算口径", "本人结论", "状态"],
  ["Cmax","","","","","","所有第二阶段结束时间最大值","",""],
  ["加工能耗","","","","","","Σ(实际AT×机器功率)","",""],
  ["待机能耗","","","","","","Σ(机器空闲×1)","",""],
  ["TEC","","","","","","加工能耗+待机能耗","",""],
  ["TWC","","","","","","Σ((实际AT+机器空闲)×工人成本率)","",""],
  ["Fmax","","","","","","所有工序结束疲劳最大值","",""],
  ["Favg","","","","","","所有工序结束疲劳平均值","",""],
  ["FE","","","","","","高于Fwarn部分的解析积分","",""],
  ["Var(Fw)","","","","","","Cmax时合法工人疲劳总体方差","",""],
  ["高疲劳比例","","","","","","高于Fwarn的工人时间/(人数×Cmax)","",""],
  ["最长连续工作","","","","","","同一工人零间隙连续实际AT之和最大值","",""],
  ["自然恢复总时长","","","","","","任务间正空闲和；不含初始等待和尾部","",""],
  ["外部jMetal FE",1,1,1,0,0,"内部传播不加FE","应保持1次","待填"]
];
header(summary.getRange("A3:I3")); fixedArea(summary.getRange("A4:A16")); inputArea(summary.getRange("B4:F16")); noteArea(summary.getRange("G4:I16"));
summary.getRange("A18:E27").values = [
  ["提交检查", "本人填写", "要求", "为什么", "状态"],
  ["X0位置解释", "", "5个位置全部完成", "证明编码理解", ""],
  ["第一道工序", "", "9个中间量完成", "先过最小门", ""],
  ["S0", "", "10道工序完整", "基础解码", ""],
  ["FCLS", "", "共同空档、接受门、S1完整", "新版左移", ""],
  ["FCRS", "", "CmaxStar、减半回退、S2完整", "新版右移", ""],
  ["目标", "", "S0/S1/S2全部重建", "不能只看甘特", ""],
  ["约束", "", "前序/机器/工人/工厂均通过", "可行性", ""],
  ["本人声明", "", "这些数值由本人手算", "满足导师要求", ""],
  ["提交文件SHA-256", "", "提交后冻结", "随后才允许程序逐项对照", ""]
];
header(summary.getRange("A18:E18")); inputArea(summary.getRange("B19:B27")); noteArea(summary.getRange("A19:A27")); fixedArea(summary.getRange("C19:E27"));
summary.getRange("A:I").format.columnWidth = 22; summary.getRange("G:G").format.columnWidth = 42;

for (const sheet of wb.worksheets.items) {
  const used = sheet.getUsedRange();
  if (used) used.format.font = {...(used.format.font ?? {}), name: "Microsoft YaHei", size: 10};
}

await fs.mkdir(previewDir, { recursive: true });
for (const sheet of wb.worksheets.items) {
  const p = await wb.render({sheetName: sheet.name, autoCrop: "all", scale: 1, format: "png"});
  await fs.writeFile(`${previewDir}/${sheet.name}.png`, new Uint8Array(await p.arrayBuffer()));
}
const xlsx = await SpreadsheetFile.exportXlsx(wb);
await xlsx.save(output);

console.log((await wb.inspect({kind:"sheet", include:"id,name", maxChars:5000})).ndjson);
console.log((await wb.inspect({kind:"formula", maxChars:5000, options:{maxResults:100}})).ndjson);
console.log((await wb.inspect({kind:"match", searchTerm:"#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A", options:{useRegex:true,maxResults:100}, maxChars:5000})).ndjson);
console.log(output);
