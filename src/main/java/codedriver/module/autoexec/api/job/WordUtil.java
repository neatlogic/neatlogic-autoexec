/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.autoexec.api.job;

/**
 * @author longrf
 * @date 2022/9/29 10:11
 */

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * POI操作Word工具类
 */
public class WordUtil {

    /**
     * 简单表格生成
     * @param xdoc XWPFDocument对象
     * @param titles 表头表头
     * @param values 表内容
     */
    public static void createSimpleTable(XWPFDocument xdoc,String[] titles,List<Map<String, String>> values){
        //行高
        int rowHeight = 300;

        //开始创建表格（默认有一行一列）
        XWPFTable xTable = xdoc.createTable();
        CTTbl ctTbl = xTable.getCTTbl();
        CTTblPr tblPr = ctTbl.getTblPr() == null ? ctTbl.addNewTblPr() : ctTbl.getTblPr();
        CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblWidth.setType(STTblWidth.DXA);
//        tblWidth.setW(new BigInteger("8310"));//表格宽度

        // 创建表头数据
        XWPFTableRow titleRow = xTable.getRow(0);
        titleRow.setHeight(rowHeight);
        for (int i = 0; i < titles.length; i++) {
            setCellText(i == 0 ? titleRow.getCell(0) :titleRow.createCell(), titles[i]);
        }

        // 创建表格内容
        for (int i = 0; i < values.size(); i++) {
            Map<String, String> stringStringMap = values.get(i);

            //设置列内容
            XWPFTableRow row = xTable.insertNewTableRow(i + 1);
            row.setHeight(rowHeight);
            for (String title : titles) {
                setCellText(row.createCell(), stringStringMap.get(title));
            }
        }
    }

    /**
     * 设置列内容
     */
    private static void setCellText(XWPFTableCell cell,String text) {
        CTTc cttc = cell.getCTTc();
        CTTcPr cellPr = cttc.addNewTcPr();
        cellPr.addNewTcW().setW(new BigInteger("2100"));
        cell.setColor("FFFFFF");
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        CTTcPr ctPr = cttc.addNewTcPr();
        ctPr.addNewVAlign().setVal(STVerticalJc.CENTER);
        cttc.getPList().get(0).addNewPPr().addNewJc().setVal(STJc.CENTER);
        cell.setText(text);
    }
}