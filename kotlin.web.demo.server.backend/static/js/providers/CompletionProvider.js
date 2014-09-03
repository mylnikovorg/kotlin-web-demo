/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created with IntelliJ IDEA.
 * User: Natalia.Ukhorskaya
 * Date: 3/29/12
 * Time: 1:56 PM
 */

var CompletionProvider = (function () {

    function CompletionProvider(onSuccess, onFail) {

        var instance = {
            getCompletion: function (confType, project, filename, cursorLine, cursorCh) {
                var confTypeString = Configuration.getStringFromType(confType);
                getCompletion(confTypeString, project, filename, cursorLine, cursorCh);
            }
        };

        var isLoadingCompletion = false;

        function getCompletion(confTypeString, project, filename, cursorLine, cursorCh) {
            if (!isLoadingCompletion) {
                isLoadingCompletion = true;
                $.ajax({
                    url: generateAjaxUrl("complete", "&runConf=" + confTypeString),
                    context: document.body,
                    success: function (data) {
                        isLoadingCompletion = false;
                        if (checkDataForNull(data)) {
                            if (checkDataForException(data)) {
                                onSuccess(data);
                            } else {
                                onFail(data);
                            }
                        } else {
                            onFail("Incorrect data format.");
                        }
                    },
                    dataType: "json",
                    type: "POST",
                    data: { project: JSON.stringify(project), filename: filename, line : cursorLine, ch : cursorCh},
                    timeout: 10000,
                    error: function (jqXHR, textStatus, errorThrown) {
                        isLoadingCompletion = false;
                        onFail(textStatus + " : " + errorThrown);
                    }
                });
            }
        }

        return instance;
    }


    return CompletionProvider;
})();