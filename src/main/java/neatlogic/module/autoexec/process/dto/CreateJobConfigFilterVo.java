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

package neatlogic.module.autoexec.process.dto;

public class CreateJobConfigFilterVo {
    private String leftMappingMode;
    private String leftValue;
    private String leftColumn;
    private String expression;
    private String rightMappingMode;
    private String rightValue;
    private String rightColumn;

    public String getLeftMappingMode() {
        return leftMappingMode;
    }

    public void setLeftMappingMode(String leftMappingMode) {
        this.leftMappingMode = leftMappingMode;
    }

    public String getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(String leftValue) {
        this.leftValue = leftValue;
    }

    public String getLeftColumn() {
        return leftColumn;
    }

    public void setLeftColumn(String leftColumn) {
        this.leftColumn = leftColumn;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getRightMappingMode() {
        return rightMappingMode;
    }

    public void setRightMappingMode(String rightMappingMode) {
        this.rightMappingMode = rightMappingMode;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }

    public String getRightColumn() {
        return rightColumn;
    }

    public void setRightColumn(String rightColumn) {
        this.rightColumn = rightColumn;
    }
}
