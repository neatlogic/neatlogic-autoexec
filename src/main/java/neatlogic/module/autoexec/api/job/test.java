/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_JOB_MODIFY;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.framework.util.word.WordBuilder;
import neatlogic.framework.util.word.enums.FontColor;
import neatlogic.framework.util.word.enums.FontFamily;
import neatlogic.framework.util.word.enums.ParagraphAlignmentType;
import neatlogic.framework.util.word.enums.TitleType;

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
//@Service
@Deprecated
@AuthAction(action = AUTOEXEC_JOB_MODIFY.class)
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
            Map<String, String> tableValueMap = new HashMap<>();
            tableValueMap.put("port", "23");
            tableValueMap.put("ip", "234.234.234.234");
            wordBuilder.addTitle(TitleType.TILE, "测试");
            wordBuilder.addTitle(TitleType.H1, "测试").setFontSize(24).setAlignmentType(ParagraphAlignmentType.RIGHT);
            wordBuilder.addTitle(TitleType.H2, "测试").setFontFamily(FontFamily.FANG_SONG.getValue());
            wordBuilder.addTitle(TitleType.H3, "测试").setBold(false).setColor(FontColor.RED.getValue());
            wordBuilder.addBlankRow().addParagraph().setText("测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试测试")
                    .setCustom(false, FontColor.RED.getValue(), FontFamily.BLACK.getValue(), 12, 2, true, ParagraphAlignmentType.CENTER);
            wordBuilder.addTable().addTableHeaderMap(tableHeaderMap).setSpacingBetween(1.5);
            wordBuilder.builder().write(os);
            wordBuilder.builder().close();
            os.flush();
            os.close();
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
