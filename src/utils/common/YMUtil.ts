/**
 * 根据传入值获取当前年份前后多少年
 * @param beforeNum 往前查询多少年
 * @param afterNum 往后查询多少年
 * @returns 
 */
export const getYearArr = (beforeNum : number, afterNum : number) => {
    let yearArr = [] as any;
    for (let i = new Date().getFullYear() + afterNum; i > new Date().getFullYear() - beforeNum; i--) {
        yearArr.push({
            label: i.toString(),
            value: i.toString()
        })
    }
    return yearArr;
}

/**
 * 获取月份数组
 * @param isShow：月份前是否显示0 ，true是，false否
 * @returns 
 */
export const getMonthArr = (isShow) =>{
    let monthArr = [
        { label: "一月" },
        { label: "二月" },
        { label: "三月" },
        { label: "四月" },
        { label: "五月" },
        { label: "六月" },
        { label: "七月" },
        { label: "八月" },
        { label: "九月" },
        { label: "十月" },
        { label: "十一月" },
        { label: "十二月" },
    ] as any;
    monthArr.forEach((item, index) => {
        if(true === isShow){
            item.value = (index+1).toString().padStart(2, '0');;
        } else {
            item.value = index+1;
        }
    })
    return monthArr;
}