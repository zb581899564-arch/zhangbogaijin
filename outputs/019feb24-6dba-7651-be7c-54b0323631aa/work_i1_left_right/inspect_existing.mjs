import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const inputPath = "E:/学习/李明哲-毕业材料/张博改进/outputs/019feb24-6dba-7651-be7c-54b0323631aa/I0_左移右移本人手算空白模板.xlsx";
const outputDir = "E:/学习/李明哲-毕业材料/张博改进/outputs/019feb24-6dba-7651-be7c-54b0323631aa/work_i1_left_right/previews_existing";
await fs.mkdir(outputDir, { recursive: true });
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
console.log((await wb.inspect({kind:"sheet", include:"id,name", maxChars:5000})).ndjson);
for (const name of ["README", "X0与移位规则", "FM3_S0基础解码", "FCLS候选", "FCRS候选", "S0_S1_S2目标"]) {
  const preview = await wb.render({sheetName:name, autoCrop:"all", scale:1, format:"png"});
  await fs.writeFile(`${outputDir}/${name}.png`, new Uint8Array(await preview.arrayBuffer()));
}
