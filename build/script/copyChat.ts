const fs = require('fs-extra');
const path = require('path');

const sourceDir = path.join(__dirname, '../../src/views/super/airag/aiapp/chat/js'); // 源目录
const destDir = path.join(__dirname, '../../dist', 'chat'); // 目标目录

// airag(AI 流程)模块未迁移到本项目，源目录不存在时跳过复制
if (!fs.existsSync(sourceDir)) {
  console.log(`[copyChat] 源目录不存在，跳过：${sourceDir}`);
  process.exit(0);
}

// 复制目录
fs.copy(sourceDir, destDir)
  .then(() => {
    console.log(`成功将 ${sourceDir} 复制到 ${destDir}`);
  })
  .catch(err => {
    console.error(`复制目录失败: ${err.message}`);
  });