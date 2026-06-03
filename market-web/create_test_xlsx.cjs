const XLSX = require('xlsx');
const data = [['classifyCode', 'classifyName', 'path'], ['TEST99', 'Test Classify', '/test']];
const wb = XLSX.utils.book_new();
const ws = XLSX.utils.aoa_to_sheet(data);
XLSX.utils.book_append_sheet(wb, ws, 'Sheet1');
XLSX.writeFile(wb, 'test99.xlsx');