import { cloneDeep, merge } from 'lodash-es';
import ExcelJS from 'exceljs';

// 定义表头项的类型
interface HeaderItem {
  key: string;
  header: string | string[];
  width?: number;
}

// 普通的json数据一页导出
const commonDownExcel = (headers: HeaderItem[], data: any[], fileName: string) => {
  const workbook = new ExcelJS.Workbook();
  workbook.created = new Date(); // 创建时间
  workbook.modified = new Date(); // 修改时间
  workbook.properties.date1904 = true;

  const sheet = workbook.addWorksheet('Sheet1', {
    views: [
      {
        state: 'frozen',
        ySplit: typeof headers[0].header === 'string' ? 1 : headers[0].header.length
      }
    ]
  });

  let columns = headers.map(s => {
    if (s.width) return s;
    s.width = typeof s.header === 'string' ? s.header.length * 2.5 + 3 : s.header[s.header.length - 1].length * 2.5 + 3;
    return s;
  });

  sheet.columns = columns;
  sheet.autoFilter = {
    from: {
      row: typeof headers[0].header === 'string' ? 1 : headers[0].header.length,
      column: 1
    },
    to: {
      row: typeof headers[0].header === 'string' ? 1 : headers[0].header.length,
      column: headers.length
    }
  };

  if (typeof headers[0].header !== 'string') {
    mergeSheetHead(sheet, headers);
  }

  // 表头设置颜色
  sheet.eachRow({ includeEmpty: true }, (row) => {
    row.eachCell((cell) => {
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: {
          argb: 'FFC0C0C0'
        },
        bgColor: {
          argb: 'FFC0C0C0'
        }
      };
      cell.font = {
        name: '宋体',
        family: 2,
        size: 14,
        bold: true
      };
    });
  });

  sheet.addRows(data);

  const color: { [key: string]: string } = {
    'red': 'ffff0000', // 红
    'green': 'ff00af50', // 绿
    'yellow': 'ffffff00' // 黄
  };

  sheet.eachRow({ includeEmpty: true }, (row, rowN) => {
    if (rowN > 3) {
      row.eachCell((cell, cellN) => {
        const cellNIndex = cellN - 1;
        const rowIndex = rowN - (headers[headers.length - 1].header.length + 1);
        if (
          data[rowIndex] &&
          headers[cellNIndex].key &&
          data[rowIndex][`${headers[cellNIndex].key}_Color`]
        ) {
          cell.fill = {
            type: 'pattern',
            pattern: 'solid',
            fgColor: {
              argb: color[data[rowIndex][`${headers[cellNIndex].key}_Color`]]
            }
          };
        }
      });
    }
  });

  // 居中和边框
  setCenterAndBorder(workbook);
  // 下载文件
  downExcel(workbook, fileName);
};

// 普通的json数据一页导出
const commonDownColorExcel = (headers: HeaderItem[], data: any[], fileName: string) => {
  const workbook = new ExcelJS.Workbook();
  workbook.created = new Date(); // 创建时间
  workbook.modified = new Date(); // 修改时间
  workbook.properties.date1904 = true;

  const sheet = workbook.addWorksheet('Sheet1', {
    views: [
      {
        state: 'frozen',
        ySplit: typeof headers[0].header === 'string' ? 1 : headers[0].header.length
      }
    ]
  });

  let columns = headers.map(s => {
    if (s.width) return s;
    s.width = typeof s.header === 'string' ? s.header.length * 2.5 + 3 : s.header[s.header.length - 1].length * 2.5 + 3;
    return s;
  });

  sheet.columns = columns;
  sheet.autoFilter = {
    from: {
      row: typeof headers[0].header === 'string' ? 1 : headers[0].header.length,
      column: 1
    },
    to: {
      row: typeof headers[0].header === 'string' ? 1 : headers[0].header.length,
      column: headers.length
    }
  };

  if (typeof headers[0].header !== 'string') {
    mergeSheetHead(sheet, headers);
  }

  // 表头设置颜色
  sheet.eachRow({ includeEmpty: true }, (row) => {
    row.eachCell((cell) => {
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: {
          argb: 'FFC0C0C0'
        },
        bgColor: {
          argb: 'FFC0C0C0'
        }
      };
      cell.font = {
        name: '宋体',
        family: 2,
        size: 14,
        bold: true
      };
    });
  });

  sheet.addRows(data);

  const color: { [key: string]: string } = {
    'red': 'ffff0000', // 红
    'green': 'ff00af50', // 绿
    'yellow': 'ffffff00' // 黄
  };

  sheet.eachRow({ includeEmpty: true }, (row, rowN) => {
    if (rowN > 3) {
      row.eachCell((cell, cellN) => {
        const cellNIndex = cellN - 1;
        const rowIndex = rowN - (headers[headers.length - 1].header.length + 1);
        const piCode = headers[cellNIndex].key && headers[cellNIndex].key.split('_')[0] || null;
        if (
          data[rowIndex] &&
          piCode &&
          headers[cellNIndex].key.includes('ActualValue') &&
          data[rowIndex][`${piCode}_Color`]
        ) {
          cell.fill = {
            type: 'pattern',
            pattern: 'solid',
            fgColor: {
              argb: color[data[rowIndex][`${piCode}_Color`]]
            }
          };
        }
      });
    }
  });

  // 居中和边框
  setCenterAndBorder(workbook);
  // 下载文件
  downExcel(workbook, fileName);
};

const mergeSheetHead = (sheet: ExcelJS.Worksheet, headers: HeaderItem[]) => {
  // 这个方法主要是处理多行表头合并问题
  const mergeObj: { [key: string]: { startRow: number; startCol: number; endRow: number; endCol: number } } = {};
  for (let j = 0; j < headers[0].header.length; j++) {
    for (let i = 0; i < headers.length; i++) {
      let key = '';
      if (
        (j === 1 && Array.isArray(headers[i].header) && headers[i].header[1] === headers[i].header[0]) ||
        (j === 2 && Array.isArray(headers[i].header) && headers[i].header[2] === headers[i].header[1])
      ) {
        continue;
      }
      if (Array.isArray(headers[i].header)) {
        for (let m = 0; m <= j; m++) {
          if (headers[i].header[m]!== '') {
            key += '_' + headers[i].header[m];
          }
        }
      }
      mergeObj[key] = mergeObj[key] || {
        startRow: j + 1,
        startCol: i + 1,
        endRow: j + 1,
        endCol: i + 1
      };
      if (i < headers.length - 1 && j < (Array.isArray(headers[0].header)? headers[0].header.length : 0) - 1) {
        const curentHeaders = headers[i];
        const nextHeaders = headers[i + 1];
        if (
          Array.isArray(curentHeaders.header) &&
          Array.isArray(nextHeaders.header) &&
          curentHeaders.header[j] &&
          nextHeaders.header[j] &&
          curentHeaders.header[j] === nextHeaders.header[j]
        ) {
          mergeObj[key].endCol = i + 2;
        } else if (
          Array.isArray(curentHeaders.header) &&
          curentHeaders.header[j] &&
          curentHeaders.header[j + 1] &&
          curentHeaders.header[j] === curentHeaders.header[j + 1]
        ) {
          mergeObj[key].endRow = Array.isArray(headers[i].header)? headers[i].header.length : 0;
        }
      }

      if (Array.isArray(headers[i].header) && headers[i].header[j] === '') {
        mergeObj[key].endRow = j + 1;
      }
    }
  }

  for (const i in mergeObj) {
    const obj = mergeObj[i];
    if (obj.startRow!== obj.endRow || obj.startCol!== obj.endCol) {
      sheet.mergeCells(obj.startRow, obj.startCol, obj.endRow, obj.endCol); // 开始行，开始列，结束行 ，结束列
    }
  }
};

const setCenterAndBorder = (wb: ExcelJS.Workbook) => {
  wb.eachSheet((sheet) => {
    sheet.eachRow(
      {
        includeEmpty: true
      },
      (row) => {
        row.eachCell(
          {
            includeEmpty: true
          },
          (cell) => {
            cell.alignment = {
              vertical: 'middle',
              horizontal: 'center'
            };
            cell.border = {
              top: {
                style: 'thin'
              },
              left: {
                style: 'thin'
              },
              bottom: {
                style: 'thin'
              },
              right: {
                style: 'thin'
              }
            };
          }
        );
      }
    );
  });
};

const downExcel = (workbook: ExcelJS.Workbook, fileName: string) => {
  workbook.xlsx.writeBuffer().then((buffer) => {
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });
    const ele = document.createElement('a');
    ele.style.display = 'none';
    ele.href = URL.createObjectURL(blob);
    ele.download = fileName;
    document.body.appendChild(ele);
    ele.click();
    document.body.removeChild(ele);
    URL.revokeObjectURL(ele.href);
    // @ts-ignore
    workbook = null;
  });
};

interface ColumnItem {
  key: string;
  title: string;
  children?: ColumnItem[];
}

const changeColumnsToHeader = (columnsArr: ColumnItem[], parentTitle: string[] = []) => {
  const coloumnRet: HeaderItem[] = [];

  const loop = (columns: ColumnItem[], parentTitle: string[]) => {
    columns.forEach((item, index) => {
      if (item.children && item.children.length > 0) {
        loop(item.children, [...parentTitle, item.title]);
      } else {
        let headerItem: HeaderItem = {
          key: item.key ?? item.dataIndex,
          header: [...parentTitle, item.title || ''],
          width: 10
        };
        if (parentTitle.length > 0 && parentTitle[parentTitle.length - 1]) {
          const titleLast = parentTitle[parentTitle.length - 1];
          let width = String(item.title).length * 2.5 + 3;

          if (String(titleLast).length > String(item.title).length) {
            width = String(titleLast).length * 2.5 + 3;
          }

          headerItem.width = width;
        }
        coloumnRet.push(deepCloneByJSON(headerItem));
      }
    });
  };

  loop(columnsArr, parentTitle);
  return coloumnRet;
};

/**
 * 
 * @param columnsArr 表头数组
 * @param mergeArr 需要合并的列
 */
const changeColumnsToHeaderV2 = (columnsArr, mergeArr) => {
  let headersTemp = cloneDeep(columnsArr);
  let headers = [] as any;
  headersTemp.forEach((item, index) => {
    let headerArr = [] as  any;
    if (mergeArr.includes(index)) {
      headerArr = [item.title, item.title]
      headers.push({
        key: item.dataIndex,
        header: headerArr,
        width: String(item.title).length * 2.5 + 3
      })
    } else {
      if (item.children && item.children.length > 0) {
        item.children.forEach(item => {
          headerArr = [item.title, item.title]
          headers.push({
            key: item.dataIndex ,
            header: headerArr,
            width: String(item.title).length * 3.5 + 3
          })
        })
      } else {
        headers.push({
          key: item.children ? item.children[0].dataIndex : item.dataIndex,
          header: headerArr,
          width: String(item.title).length * 2.5 + 3
        })
      }
    }
  })
  return headers;
};

//深度拷贝
const deepCloneByJSON = (obj) => {
  return JSON.parse(JSON.stringify(obj));
}

// @ts-ignore
ExcelJS.commonDownExcel = commonDownExcel;
// @ts-ignore
ExcelJS.commonDownColorExcel = commonDownColorExcel;
// @ts-ignore
ExcelJS.downExcel = downExcel;
// @ts-ignore
ExcelJS.setHeader = changeColumnsToHeader;
// @ts-ignore
ExcelJS.getNewHeaders = changeColumnsToHeaderV2;

export default ExcelJS;