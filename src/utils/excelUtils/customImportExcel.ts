export function importExcel(data){
  const excelData = data.returnExcel[0];
  //初始化表格大小
  //最大列
  let maxCol = excelData['maxCol'] || 30;
  //最大行
  let maxRow = excelData['maxRow'] || 30;
  //设置列
  let colList = [] as any;
  for(let colNum = 0;colNum <= maxCol;colNum ++){
    let colOption = {} as any;
    colOption.field = colNum;
    colOption.title = '';
    colOption.width = 'auto';
    colList.push(colOption);
  }

  //数据信息
  let rowData = excelData['sheet'];
  //设置行
  let newArray = [] as any;
  for(let rowNum = 0; rowNum <= maxRow; rowNum++){
    let colTextList = {};
    for(let colNum = 0;colNum <= maxCol;colNum ++){
      let colText = rowData[rowNum][colNum] == undefined ? "" : rowData[rowNum][colNum];
      colTextList[colNum+''] = colText;
    }
    newArray.push(colTextList);
  }

  //合并单元格信息
  let merges = excelData['merges'] || [];
  let mergesList = [] as any;
  merges.forEach((item) => {
    //合并单元格配置
    let mergesHr = {} as any;
    //合并单元格开始列
    mergesHr.startCol = item['s']['c'];
    //合并单元格结束列
    mergesHr.endCol = item['e']['c'];
    //合并单元格开始行
    mergesHr.startRow = item['s']['r'];
    //合并单元格结束行
    mergesHr.endRow = item['e']['r'];
    //合并单元格数据值
    mergesHr.dataValue = newArray[item['s']['r']][item['s']['c']];
    //添加配置
    mergesList.push(mergesHr);
    //将合并单元格第一个值赋值给所有合并单元格，避免编辑点击出现空白
    for(let row = mergesHr.startRow; row <= mergesHr.endRow; row++){
      for(let col = mergesHr.startCol; col <= mergesHr.endCol; col++){
        newArray[row][col] = mergesHr.dataValue;
      }
    }
  });
  return {
    newArray,
    colList,
    mergesList
  }
}