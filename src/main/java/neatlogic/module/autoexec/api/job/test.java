/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package neatlogic.module.autoexec.api.job;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.auth.AUTOEXEC_JOB_MODIFY;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
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
