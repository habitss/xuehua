
export function getColor(key: string) {
    switch (key) {
        //红
        case 'red':
            return '#FE0000';

        //绿
        case 'green':
            return '#00FF00';

        //黄
        case 'yellow':
            return '#FFFF00';
            
        //灰
        case 'grey':
            return '#D0D3D4';

        default:
            return '';
    }
}