/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.autoexec.api.job;

import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/9/23 16:34
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class test2 extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getName() {
        return "testWord2";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {

        try {
            XWPFDocument xdoc = new XWPFDocument();
            HashMap<String, List<Map<String, String>>> hashMap = new HashMap<>();

            //获取数据
            /*
                -- mysql查询表名、表注释、表字段数据
                SELECT
                    t.table_name AS '表名称',
                    t.table_comment AS '表注释',
                    c.column_name AS '字段名称',
                    c.column_type AS '数据类型',
                    c.column_comment AS '字段注释',
                    c.column_key AS '是否主键',
                    c.is_nullable AS '是否允许NULL'
                FROM
                    information_schema.COLUMNS c
                    JOIN information_schema.TABLES t ON c.table_name = t.table_name
                WHERE
                    c.table_schema = (
                SELECT DATABASE
                    ());
             */
            File file = new File("/Users/longrf/Desktop/004.xlsx");
            List<Map<String, String>> list = ExcelUtil.readExcel3(file, 0);

            //处理数据，调整成下面的格式
            /*
                [
                    {"表名称":[
                        {},//一条条字段信息
                        {},//一条条字段信息
                        {},//一条条字段信息
                    ]}
                ]
             */
            ArrayList<Map<String, String>> arrayList = new ArrayList<>();
            String tableName = "";
            for (int i = 0; i < list.size(); i++) {
                Map<String, String> map = list.get(i);
                String tName = String.valueOf(map.get("表名称"));
                if(tableName.equals(tName)){
                    arrayList.add(map);
                }else{
                    hashMap.put(tableName,arrayList);
                    tableName = tName;
                    arrayList = new ArrayList<>();
                    arrayList.add(map);
                }

                if(list.size() - i == 1){
                    hashMap.put(tableName,arrayList);
                }
            }


            //生成内容
            for (String tName : hashMap.keySet()) {
                if("".equals(tName)){
                    continue;
                }
                List<Map<String, String>> maps = hashMap.get(tName);
                String tZs = String.valueOf(maps.get(0).get("表注释"));

                //设置文字，对表格进行描述
                XWPFParagraph xp = xdoc.createParagraph();
                xp.setSpacingBefore(0);
                XWPFRun r1 = xp.createRun();
                r1.setFontFamily("宋体");
                r1.setFontSize(12);
                r1.setTextPosition(0);

                r1.addBreak(); // 换行
                r1.setText("表名称："+tName);
                r1.addBreak(); // 换行
                r1.setText("表注释："+tZs);


                //表格标题
                String[] titles = {
                        "字段名称",
                        "字段类型",
                        "字段注释",
                        "允许空值",
                };

                //表格内容
                List<Map<String, String>> values = new ArrayList<>();
                for (Map<String, String> stringStringMap : maps) {
                    String cName = stringStringMap.get("字段名称");
                    String cType = stringStringMap.get("数据类型");
                    String cZs = stringStringMap.get("字段注释");
                    String isPri = stringStringMap.get("是否主键");
                    String isNull = stringStringMap.get("是否允许NULL");

                    //按照表格标题格式进行封装
                    HashMap<String, String> stringStringHashMap = new HashMap<>();
                    stringStringHashMap.put("字段名称",cName);
                    stringStringHashMap.put("字段类型",cType);
                    stringStringHashMap.put("字段注释",cZs);
                    stringStringHashMap.put("允许空值",isNull);

                    values.add(stringStringHashMap);
                }


                WordUtil.createSimpleTable(xdoc, titles, values);
            }

            //保存word文件
            FileOutputStream fos = new FileOutputStream("/Users/longrf/Desktop/002.docx");
            xdoc.write(fos);
            fos.close();

            System.out.println("操作完成！");

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getToken() {
        return "word/test2";
    }


    public static void main(String[] args) throws IOException {
        XWPFDocument xdoc = new XWPFDocument();
        //表格标题
        String[] titles = {
                "port1",
                "port2",
                "测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试",
                "port3",
        };
        List<Map<String, String>> list = new ArrayList<>();

        Map<String, String> map = new HashMap<>();
        map.put("字段名称", "3");
        map.put("字段类型", "字段名称");
        map.put("测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试", "字段名称");
        map.put("允许空值", "字段名称");
        list.add(map);


        WordUtil.createSimpleTable(xdoc, titles, list);
        //保存word文件
        FileOutputStream fos = new FileOutputStream("/Users/longrf/Desktop/002.docx");
        xdoc.write(fos);
        fos.close();

    }
}
