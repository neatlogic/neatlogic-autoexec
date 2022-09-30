/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.autoexec.api.job;

/**
 * @author longrf
 * @date 2022/9/29 10:12
 */

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POI操作Excel工具类
 */
public class ExcelUtil {

    /**
     * 读取指定Sheet页的数据
     */
    public static List<Map<String,String>> readExcel3(File file, int sheetIndex) throws Exception {
        try (FileInputStream fs = new FileInputStream(file)) {
            XSSFWorkbook hw = new XSSFWorkbook(fs);
            XSSFSheet sheet = hw.getSheetAt(sheetIndex);

            ArrayList<Map<String,String>> list = new ArrayList<>();

            //读取表头
            List<String> headerList = new ArrayList<String>();
            XSSFRow headerRow = sheet.getRow(0);
            for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                String val = getCellValue(headerRow,headerRow.getCell(j));

                //数据为空
                if (StringUtils.isEmpty(val)) {
                    continue;
                }

                headerList.add(val);
            }

            //读取数据
            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                XSSFRow dataRow = sheet.getRow(i);

                if (dataRow == null) {
                    continue;
                }

                HashMap<String, String> map = new HashMap<>();
                for (int j = 0; j < headerList.size(); j++) {
                    String header = headerList.get(j);
                    String val = getCellValue(dataRow,dataRow.getCell(j));
                    map.put(header, val);
                }
                list.add(map);
            }
            return list;
        }
    }

    /**
     * 获取单元格内容
     */
    private static String getCellValue(XSSFRow dataRow, Cell cell){
        String cellvalue = "";
        if (cell!=null) {
            switch (cell.getCellType()) {
                case BOOLEAN:
                    cellvalue = String.valueOf(cell.getBooleanCellValue());
                    break;
                case NUMERIC:
                    cellvalue = String.valueOf(cell.getNumericCellValue()).split("\\.")[0];
                    if(cellvalue.toLowerCase().contains("e")){
                        cellvalue = new DecimalFormat("#").format(cell.getNumericCellValue());
                        if(cellvalue.toLowerCase().contains("e")){
                            throw new RuntimeException(dataRow.getCell(4) + "/数值带E！！！");
                        }
                    }
                    break;
                case STRING:
                    cellvalue = cell.getStringCellValue();
                    break;
                case BLANK:
                    break;
                case ERROR:
                    cellvalue = String.valueOf(cell.getErrorCellValue());
                    break;
                case FORMULA:
                    try {
                        cellvalue = String.valueOf(cell.getNumericCellValue());
                    } catch (IllegalStateException e) {
                        if (e.getMessage().contains("from a STRING cell")) {
                            try {
                                cellvalue = String.valueOf(cell.getStringCellValue());
                            } catch (IllegalStateException e2) {
                                throw new RuntimeException("公式计算出错");
                            }
                        }
                    }
                    break;
                default:
                    cellvalue = String.valueOf(cell.getBooleanCellValue());
                    break;
            }
        }
        return cellvalue;
    }


    /**
     * 只支持一级表头
     *
     * @param file   文件
     * @param titleName   表标题
     * @param columnNames 列名集合，key是用来设置填充数据时对应单元格的值，label就是对应的列名，生成Excel表时，
     *                    第一维数组下标0对应值为Excel表最左边的列的列名 例：{ { key,label },{ key,label } }
     * @param dataLists   数据集合，key对应的是列名集合的key，value是要填充到单元格的值 例：ArrayList<HashMap<String key, String vaule>>
     */
    public static String createExcelFile(File file,String titleName, String[][] columnNames, ArrayList<HashMap<String, String>> dataLists) {

        //创建HSSFWorkbook对象(excel的文档对象)
        HSSFWorkbook wb = new HSSFWorkbook();
        //建立新的sheet对象（excel的表单）
        HSSFSheet sheet = wb.createSheet(titleName);//设置表单名

        //1、标题名
        //创建标题行，参数为行索引(excel的行)，可以是0～65535之间的任何一个
        HSSFRow row1 = sheet.createRow(0);

        createCell(row1, 0, titleName);
        //合并单元格CellRangeAddress构造参数依次表示起始行，截至行，起始列， 截至列
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnNames.length - 1));

        //2、列名
        //创建列名行
        HSSFRow row2 = sheet.createRow(1);
        for (int i = 0; i < columnNames.length; i++) {
            //单元格宽度
            sheet.setColumnWidth(i, 20 * 256);
            createCell(row2, i, columnNames[i][1]);//例：[[key,label],[key,label]] 取label
        }

        //3、填充数据
        int index = 2;//标题行、列名行，所以数据行默认从第三行开始
        for (HashMap<String, String> map : dataLists) {
            //创建内容行
            HSSFRow row3 = sheet.createRow(index);
            for (int i = 0; i < columnNames.length; i++) {
                String val = map.get(columnNames[i][0]);
                createCell(row3, i, val == null ? "" : val);//例：[[key,label],[key,label]] 取key
            }
            index++;
        }

        try(FileOutputStream outputStream = new FileOutputStream(file)) {
            wb.write(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file.getName()+" 创建成功";
    }

    /**
     * 创建一个单元格
     */
    private static void createCell(Row row, int column, String text) {
        Cell cell = row.createCell(column);  // 创建单元格
        cell.setCellValue(text);  // 设置值
    }
}