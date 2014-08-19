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
 * Date: 3/30/12
 * Time: 3:37 PM
 */

var RunProvider = (function () {

    function RunProvider() {

        var instance = {
            onExecutionFinish: function (data) {
            },
            onFail: function (message) {
            },
            run: function (configuration, programText, args, example) {
                run(configuration, programText, args, example);
            }
        };

        function run(configuration, programText, args, example) {
            if (configuration.type.runner == ConfigurationType.runner.JAVA) {
                runJava(configuration, programText, args, example);
            } else {
                runJs(configuration, programText, args, example);
            }
        }


        function runJava(configuration, programText, args, example) {
            var confTypeString = Configuration.getStringFromType(configuration.type);
            $.ajax({
                url: generateAjaxUrl("run", confTypeString),
                context: document.body,
                success: function (data) {
                    if (checkDataForNull(data)) {
                        if (checkDataForException(data)) {
                            instance.onExecutionFinish(data);
                        } else {
                            instance.onFail(data);
                        }
                    } else {
                        instance.onFail("Incorrect data format.")
                    }
                },
                dataType: "json",
                type: "POST",
                data: {text: programText, consoleArgs: args, example: example},
                timeout: 10000,
                error: function (jqXHR, textStatus, errorThrown) {
                    instance.onFail(textStatus + " : " + errorThrown);
                }
            });

        }

        function runJs(configuration, programText, args) {
            Kotlin.modules = {};
            loadJsFromServer(configuration, programText, args);
        }

        function loadJsFromServer(configuration, i, arguments) {
            var confTypeString = Configuration.getStringFromType(configuration.type);
            $.ajax({
                url: generateAjaxUrl("run", confTypeString),
                context: document.body,
                success: function (data) {
                    if (checkDataForNull(data)) {
                        if (checkDataForException(data)) {
                            var dataJs;
                            try {
                                dataJs = eval(data[0].text);
                            } catch (e) {
                                instance.onFail(e);
                                return;
                            }
                            var output = [
                                {"text": safe_tags_replace(dataJs), "type": "out"},
                                {"text": data[0].text, "type": "toggle-info"}
                            ];
                            instance.onExecutionFinish(output);
                        } else {
                            instance.onFail(data);
                        }
                    } else {
                        instance.onFail("Incorrect data format.");
                    }
                },
                dataType: "json",
                type: "POST",
                data: {text: i, consoleArgs: arguments},
                timeout: 10000,
                error: function (jqXHR, textStatus, errorThrown) {
                    instance.onFail(textStatus + " : " + errorThrown);
                }
            });
        }

        return instance;
    }

    return RunProvider;
})();