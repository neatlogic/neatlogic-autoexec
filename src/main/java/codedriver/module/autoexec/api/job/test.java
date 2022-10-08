/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.autoexec.api.job;

import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.framework.util.FileUtil;
import codedriver.framework.util.word.WordBuilder;
import codedriver.framework.util.word.enums.FontColor;
import codedriver.framework.util.word.enums.FontFamily;
import codedriver.framework.util.word.enums.ParagraphAlignmentType;
import codedriver.framework.util.word.enums.TitleType;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author longrf
 * @date 2022/9/23 16:34
 */
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class test extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getName() {
        return "testWord";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {


        String fileName = FileUtil.getEncodedFileName("222" + ".docx");
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");

        try {
            OutputStream os = response.getOutputStream();
            int i = 1;
            WordBuilder wordBuilder = new WordBuilder();

            Map<Integer, String> tableHeaderMap = new HashMap<>();
            tableHeaderMap.put(1, "ip");
            tableHeaderMap.put(2, "port");
            tableHeaderMap.put(3, "port2");
            tableHeaderMap.put(4, "port44");
            tableHeaderMap.put(5, "测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试");
            //222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222
            Map<String, String> tableValueMap = new HashMap<>();
//            tableValueMap.put("port", "22");
            tableValueMap.put("port", "23");
            tableValueMap.put("ip", "234.234.234.234");
//            tableValueMap.put("ip", "192");

            wordBuilder.addTitle(TitleType.TILE, "测试");
            wordBuilder.addTitle(TitleType.H1, "测试").setFontSize(24).setAlignmentType(ParagraphAlignmentType.RIGHT);
            wordBuilder.addTitle(TitleType.H2, "测试").setFontFamily(FontFamily.FANG_SONG.getValue());
            wordBuilder.addTitle(TitleType.H3, "测试").setBold(false).setColor(FontColor.RED.getValue());

            wordBuilder.addBlankRow().addParagraph().setText("测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试")
                    .setCustom(false, FontColor.RED.getValue(), FontFamily.BLACK.getValue(), 12, 2, true, ParagraphAlignmentType.CENTER);


            // 当前JVM占用的内存总数(M)
            double total = (Runtime.getRuntime().totalMemory()) / (1024.0 * 1024);
            System.out.println("当前JVM占用的内存总数(M)：" + total);
            // JVM最大可用内存总数(M)
            double max = (Runtime.getRuntime().maxMemory()) / (1024.0 * 1024);
            System.out.println("JVM最大可用内存总数(M)：" + max);
            // JVM空闲内存(M)
            double free = (Runtime.getRuntime().freeMemory()) / (1024.0 * 1024);
            System.out.println("JVM空闲内存(M)：" + free);
            // 可用内存内存(M)
            double mayuse = (max - total + free);
            System.out.println("可用内存内存(M)：" + mayuse);
            // 已经使用内存(M)
            double used = (total - free);
            System.out.println("已经使用内存(M)：" + used);

//            wordBuilder.builder().write(os);
//            wordBuilder.builder().close();
//            wordBuilder.builder().getProperties().commit();
            //            wordBuilder.addParagraph().setContext("测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试")
//                    .setCustom(false, FontColor.BLUE.getValue(), FontFamily.SONG.getValue(), 12, 25, false, ParagraphAlignmentType.RIGHT);
//            os.close();

//            wordBuilder.addTable();

//            wordBuilder.addParagraph("1213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213").setText("1213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213");
//            wordBuilder.builder().write(os);


//            OutputStream os1 = response.getOutputStream();
//            WordBuilder wordBuilder1 = new WordBuilder();
//            wordBuilder1.addBlankRow();
            wordBuilder.addTable().addTableHeaderMap(tableHeaderMap).setSpacingBetween(1.5);
//                    .addRow(tableValueMap).addRow(tableValueMap);
//
//
//            wordBuilder1.addParagraph("1213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213").setText("1213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213121312131213");
//
            wordBuilder.builder().write(os);
            wordBuilder.builder().close();

//            os1.close();
//            stream.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getToken() {
        return "word/test";
    }
}
