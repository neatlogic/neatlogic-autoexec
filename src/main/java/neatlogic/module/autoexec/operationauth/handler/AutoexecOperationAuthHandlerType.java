/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.autoexec.operationauth.handler;

import neatlogic.framework.process.operationauth.core.IOperationAuthHandlerType;
import neatlogic.framework.util.I18nUtils;

/**
 * @author linbq
 * @since 2021/9/8 17:50
 **/
public enum AutoexecOperationAuthHandlerType implements IOperationAuthHandlerType {
    AUTOEXEC("autoexec", "enum.autoexec.autoexecoperationauthhandlertype.autoexec");

    AutoexecOperationAuthHandlerType(String value, String text){
        this.value = value;
        this.text = text;
    }

    private String value;
    private String text;

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getText() {
        return I18nUtils.getMessage(text);
    }
}