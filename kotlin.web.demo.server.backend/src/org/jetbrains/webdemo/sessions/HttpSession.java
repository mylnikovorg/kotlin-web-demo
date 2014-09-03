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

package org.jetbrains.webdemo.sessions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.webdemo.*;
import org.jetbrains.webdemo.examplesLoader.ExampleObject;
import org.jetbrains.webdemo.examplesLoader.ExamplesList;
import org.jetbrains.webdemo.handlers.ServerHandler;
import org.jetbrains.webdemo.handlers.ServerResponseUtils;
import org.jetbrains.webdemo.responseHelpers.*;
import org.jetbrains.webdemo.session.SessionInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//import org.jetbrains.webdemo.database.MySqlConnector;

public class HttpSession {
    @NonNls
    private static final String[] REPLACES_REFS = {"&lt;", "&gt;", "&amp;", "&#39;", "&quot;"};
    @NonNls
    private static final String[] REPLACES_DISP = {"<", ">", "&", "'", "\""};
    private final SessionInfo sessionInfo;
    private final Map<String, String[]> parameters;
    protected Project currentProject;
    protected PsiFile currentPsiFile;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private ObjectMapper objectMapper = new ObjectMapper();

    public HttpSession(SessionInfo info, Map<String, String[]> parameters) {
        this.sessionInfo = info;
        this.parameters = parameters;
    }

    public static String unescapeXml(@Nullable final String text) {
        if (text == null) return null;
        return StringUtil.replace(text, REPLACES_REFS, REPLACES_DISP);
    }

    public void handle(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            this.request = request;
            this.response = response;
            String param = request.getRequestURI() + "?" + request.getQueryString();

            ErrorWriterOnServer.LOG_FOR_INFO.info("request: " + param + " ip: " + sessionInfo.getId());

            switch (parameters.get("type")[0]) {
                case("run"):
                    ErrorWriterOnServer.LOG_FOR_INFO.info(ErrorWriter.getInfoForLog(SessionInfo.TypeOfRequest.INC_NUMBER_OF_REQUESTS.name(), sessionInfo.getId(), sessionInfo.getType()));
                    sendExecutorResult();
                    break;
                /*case("loadExample"):
                    sessionInfo.setType(SessionInfo.TypeOfRequest.LOAD_EXAMPLE);
                    ErrorWriterOnServer.LOG_FOR_INFO.info(ErrorWriter.getInfoForLog(SessionInfo.TypeOfRequest.INC_NUMBER_OF_REQUESTS.name(), sessionInfo.getId(), sessionInfo.getType()));
                    sendExampleContent();
                    break;*/
                case("highlight"):
                    sendHighlightingResult();
                    break;
                case("test"):
                    sendExecutorResultTest();
                    break;
                /*case("writeLog"):
                    sessionInfo.setType(SessionInfo.TypeOfRequest.WRITE_LOG);
                    sendWriteLogResult();
                    break;*/
                case("convertToKotlin"):
                    sessionInfo.setType(SessionInfo.TypeOfRequest.CONVERT_TO_KOTLIN);
                    sendConversationResult();
                    break;
                /*case("saveFile"):
                    sendSaveFileResult();
                    break;
                case("addProject"):
                    MySqlConnector.getInstance().addProject(sessionInfo.getUserInfo(), parameters.get("args")[0]);
                    break;
                case("addExampleProject"):
                    addExampleProject();
                    break;
                case ("deleteProject"):
                    MySqlConnector.getInstance().deleteProject(sessionInfo.getUserInfo(), parameters.get("args")[0], parameters.get("name")[0]);
                    break;
                case ("saveProject"):
                    sendSaveProjectResult();
                    break;
                case("loadProject"):
                    sendLoadProjectResult();
                    break;
                case("addFile"):
                    MySqlConnector.getInstance().addFile(sessionInfo.getUserInfo(), parameters.get("args")[0], parameters.get("name")[0], parameters.get("filename")[0]);
                case("deleteFile"):
                    sendDeleteProgramResult();
                    break;
                case("generatePublicLink"):
                    sendGeneratePublicLinkResult();
                    break;*/
                case("complete"):
                    sessionInfo.setType(SessionInfo.TypeOfRequest.COMPLETE);
                    ErrorWriterOnServer.LOG_FOR_INFO.info(ErrorWriter.getInfoForLog(SessionInfo.TypeOfRequest.INC_NUMBER_OF_REQUESTS.name(), sessionInfo.getId(), sessionInfo.getType()));
                    sendCompletionResult();
                    break;
                default:
                    ErrorWriterOnServer.LOG_FOR_INFO.info(SessionInfo.TypeOfRequest.GET_RESOURCE.name() + " " + param);
                    sendResourceFile(request, response);
                    break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (sessionInfo != null && sessionInfo.getType() != null && currentPsiFile != null && currentPsiFile.getText() != null) {
                ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, sessionInfo.getType(), sessionInfo.getOriginUrl(), currentPsiFile.getText());
            } else {
                ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, "UNKNOWN", "unknown", "null");
            }
            writeResponse(ResponseUtils.getErrorInJson("Internal server error"), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void sendConversationResult() {
        PostData data = getPostDataFromRequest();
        writeResponse(new JavaToKotlinConverter(sessionInfo).getResult(data.text), HttpServletResponse.SC_OK);
    }

    private void sendWriteLogResult() {
        String type = parameters.get("args")[0];
        if (type.equals("info")) {
            String tmp = getPostDataFromRequest(true).text;
            ErrorWriterOnServer.LOG_FOR_INFO.info(tmp);
        } else if (type.equals("errorInKotlin")) {
            String tmp = getPostDataFromRequest(true).text;
            tmp = unescapeXml(unescapeXml(tmp));
            List<String> list = ErrorWriter.parseException(tmp);
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(list.get(2), list.get(3), list.get(1), "unknown", list.get(4));
        } else {
            String tmp = getPostDataFromRequest(true).text;
            List<String> list = ErrorWriter.parseException(tmp);
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(list.get(2), list.get(3), list.get(1), "unknown", list.get(4));
        }
        writeResponse("Data sent", HttpServletResponse.SC_OK);
    }


    private void sendExecutorResultTest() {
        PostData data = new PostData("/**\n" +
                " * This is an example of a Type-Safe Groovy-style Builder\n" +
                " *\n" +
                " * Builders are good for declaratively describing data in your code.\n" +
                " * In this example we show how to describe an HTML page in Kotlin.\n" +
                " *\n" +
                " * See this page for details:\n" +
                " * http://confluence.jetbrains.net/display/Kotlin/Type-safe+Groovy-style+builders\n" +
                " */\n" +
                "package html\n" +
                "\n" +
                "import java.util.*\n" +
                "\n" +
                "fun main(args: Array<String>) {\n" +
                "  val result =\n" +
                "      html {\n" +
                "        head {\n" +
                "          title { +\"XML encoding with Kotlin\" }\n" +
                "        }\n" +
                "        body {\n" +
                "          h1 { +\"XML encoding with Kotlin\" }\n" +
                "          p { +\"this format can be used as an alternative markup to XML\" }\n" +
                "\n" +
                "          // an element with attributes and text content\n" +
                "          a(href = \"http://jetbrains.com/kotlin\") { +\"Kotlin\" }\n" +
                "\n" +
                "          // mixed content\n" +
                "          p {\n" +
                "            +\"This is some\"\n" +
                "            b { +\"mixed\" }\n" +
                "            +\"text. For more see the\"\n" +
                "            a(href = \"http://jetbrains.com/kotlin\") { +\"Kotlin\" }\n" +
                "            +\"project\"\n" +
                "          }\n" +
                "          p { +\"some text\" }\n" +
                "\n" +
                "          // content generated from command-line arguments\n" +
                "          p {\n" +
                "            +\"Command line arguments were:\"\n" +
                "            ul {\n" +
                "              for (arg in args)\n" +
                "                li { +arg }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "  println(result)\n" +
                "}\n" +
                "\n" +
                "trait Element {\n" +
                "  fun render(builder: StringBuilder, indent: String)\n" +
                "\n" +
                "  override fun toString(): String {\n" +
                "    val builder = StringBuilder()\n" +
                "    render(builder, \"\")\n" +
                "    return builder.toString()\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "class TextElement(val text: String): Element {\n" +
                "  override fun render(builder: StringBuilder, indent: String) {\n" +
                "    builder.append(\"$indent$text\\n\")\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "abstract class Tag(val name: String): Element {\n" +
                "  val children: ArrayList<Element> = ArrayList<Element>()\n" +
                "  val attributes = HashMap<String, String>()\n" +
                "\n" +
                "  protected fun initTag<T: Element>(tag: T, init: T.() -> Unit): T {\n" +
                "    tag.init()\n" +
                "    children.add(tag)\n" +
                "    return tag\n" +
                "  }\n" +
                "\n" +
                "  override fun render(builder: StringBuilder, indent: String) {\n" +
                "    builder.append(\"$indent<$name${renderAttributes()}>\\n\") \n" +
                "    for (c in children) {\n" +
                "      c.render(builder, indent + \"  \")\n" +
                "    } \n" +
                "    builder.append(\"$indent</$name>\\n\") \n" +
                "  }\n" +
                "\n" +
                "  private fun renderAttributes(): String? {\n" +
                "    val builder = StringBuilder()\n" +
                "    for (a in attributes.keySet()) {\n" +
                "      builder.append(\" $a=\\\"${attributes[a]}\\\"\")\n" +
                "    }\n" +
                "    return builder.toString()\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "abstract class TagWithText(name: String): Tag(name) {\n" +
                "  fun String.plus() {\n" +
                "    children.add(TextElement(this))\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "class HTML(): TagWithText(\"html\") {\n" +
                "  fun head(init: Head.() -> Unit) = initTag(Head(), init)\n" +
                "\n" +
                "  fun body(init: Body.() -> Unit) = initTag(Body(), init)\n" +
                "}\n" +
                "\n" +
                "class Head(): TagWithText(\"head\") {\n" +
                "  fun title(init: Title.() -> Unit) = initTag(Title(), init)\n" +
                "}\n" +
                "\n" +
                "class Title(): TagWithText(\"title\")\n" +
                "\n" +
                "abstract class BodyTag(name: String): TagWithText(name) {\n" +
                "  fun b(init: B.() -> Unit) = initTag(B(), init)\n" +
                "  fun p(init: P.() -> Unit) = initTag(P(), init)\n" +
                "  fun h1(init: H1.() -> Unit) = initTag(H1(), init)\n" +
                "  fun ul(init: UL.() -> Unit) = initTag(UL(), init)\n" +
                "  fun a(href: String, init: A.() -> Unit) {\n" +
                "    val a = initTag(A(), init)\n" +
                "    a.href = href\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "class Body(): BodyTag(\"body\")\n" +
                "class UL(): BodyTag(\"ul\") {\n" +
                "  fun li(init: LI.() -> Unit) = initTag(LI(), init)\n" +
                "}\n" +
                "\n" +
                "class B(): BodyTag(\"b\")\n" +
                "class LI(): BodyTag(\"li\")\n" +
                "class P(): BodyTag(\"p\")\n" +
                "class H1(): BodyTag(\"h1\")\n" +
                "class A(): BodyTag(\"a\") {\n" +
                "  public var href: String\n" +
                "    get() = attributes[\"href\"]!!\n" +
                "    set(value) {\n" +
                "      attributes[\"href\"] = value\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "fun html(init: HTML.() -> Unit): HTML {\n" +
                "  val html = HTML()\n" +
                "  html.init()\n" +
                "  return html\n" +
                "}\n");

        /*String consoleArgs = ResponseUtils.substringBefore(data.arguments, "&example");

        ExampleObject example = ExamplesList.getExampleObject(ResponseUtils.substringAfter(data.arguments, "&example=").replaceAll("_", " "));*/
        String consoleArgs="";
        ExampleObject example = ExamplesList.getExampleObject("Simplest version" , "Hello, world!");


        sessionInfo.setRunConfiguration(example.confType);
        if (sessionInfo.getRunConfiguration().equals(SessionInfo.RunConfiguration.JAVA) || sessionInfo.getRunConfiguration().equals(SessionInfo.RunConfiguration.JUNIT)) {
            sessionInfo.setType(SessionInfo.TypeOfRequest.RUN);
            List<PsiFile> psiFiles = createProjectPsiFiles(example);

            CompileAndRunExecutor responseForCompilation = new CompileAndRunExecutor(psiFiles, currentProject, sessionInfo, example);
            writeResponse(responseForCompilation.getResult(), HttpServletResponse.SC_OK);
        } else {
            sessionInfo.setType(SessionInfo.TypeOfRequest.CONVERT_TO_JS);
            writeResponse(new JsConverter(sessionInfo).getResult(data.text, consoleArgs), HttpServletResponse.SC_OK);
        }
    }




    private void sendExecutorResult() {
        try{

            ExampleObject example = addUnmodifiableDataToExample(objectMapper.readValue(parameters.get("project")[0], ExampleObject.class));

            sessionInfo.setRunConfiguration(example.confType);
            if (sessionInfo.getRunConfiguration().equals(SessionInfo.RunConfiguration.JAVA) || sessionInfo.getRunConfiguration().equals(SessionInfo.RunConfiguration.JUNIT)) {
                sessionInfo.setType(SessionInfo.TypeOfRequest.RUN);
                List<PsiFile> psiFiles = createProjectPsiFiles(example);

                CompileAndRunExecutor responseForCompilation = new CompileAndRunExecutor(psiFiles, currentProject, sessionInfo, example);
                writeResponse(responseForCompilation.getResult(), HttpServletResponse.SC_OK);
            } else {
                sessionInfo.setType(SessionInfo.TypeOfRequest.CONVERT_TO_JS);
//                writeResponse(new JsConverter(sessionInfo).getResult(data.text, consoleArgs), HttpServletResponse.SC_OK);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void sendExampleContent() {
        writeResponse(ExamplesList.loadExample(parameters.get("args")[0].replaceAll("_", " "), parameters.get("name")[0].replaceAll("_", " ")), HttpServletResponse.SC_OK);
    }

    private List<PsiFile> createProjectPsiFiles(ExampleObject example) {
        currentProject = Initializer.INITIALIZER.getEnvironment().getProject();
        return example.files.stream().map(file -> JetPsiFactoryUtil.createFile(currentProject, file.name, file.content)).collect(Collectors.toList());
    }

    public void sendCompletionResult() {
        try{
            String fileName = parameters.get("filename")[0];
            int line = Integer.parseInt(parameters.get("line")[0]);
            int ch = Integer.parseInt(parameters.get("ch")[0]);
            ExampleObject example = addUnmodifiableDataToExample(objectMapper.readValue(parameters.get("project")[0], ExampleObject.class));

            List<PsiFile> psiFiles = createProjectPsiFiles(example);
            sessionInfo.setRunConfiguration(parameters.get("runConf")[0]);

            JsonResponseForCompletion jsonResponseForCompletion = new JsonResponseForCompletion(psiFiles, sessionInfo, fileName, line, ch);
            writeResponse(jsonResponseForCompletion.getResult(), HttpServletResponse.SC_OK);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendHighlightingResult() {
        sessionInfo.setType(SessionInfo.TypeOfRequest.HIGHLIGHT);
        sessionInfo.setRunConfiguration(parameters.get("args")[0]);
        try{
            ExampleObject example = addUnmodifiableDataToExample(objectMapper.readValue(parameters.get("project")[0], ExampleObject.class));

            List<PsiFile> psiFiles = createProjectPsiFiles(example);
            JsonResponseForHighlighting responseForHighlighting = new JsonResponseForHighlighting(psiFiles, sessionInfo, currentProject);
            String response = responseForHighlighting.getResult();
            response = response.replaceAll("\\n", "");
            writeResponse(response, HttpServletResponse.SC_OK);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private PostData getPostDataFromRequest() {
        return getPostDataFromRequest(false);
    }

    private PostData getPostDataFromRequest(boolean withNewLines) {
        StringBuilder reqResponse = new StringBuilder();
        try (InputStream is = request.getInputStream()) {
            reqResponse.append(ResponseUtils.readData(is, withNewLines));
        } catch (IOException e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, sessionInfo.getType(), sessionInfo.getOriginUrl(), request.getQueryString());
            writeResponse("Cannot read data from file", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new PostData("", "");
        }

        String finalResponse;

        try {
            finalResponse = TextUtils.decodeUrl(reqResponse.toString());
        } catch (UnsupportedEncodingException e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, sessionInfo.getType(), sessionInfo.getOriginUrl(), "null");
            return new PostData("", "");
        } catch (IllegalArgumentException e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, sessionInfo.getType(), sessionInfo.getOriginUrl(), reqResponse.toString());
            return new PostData("", "");
        }
        finalResponse = finalResponse.replaceAll("<br>", "\n");
        String[] parts = finalResponse.split("&");
        PostData out = new PostData("fun main(args : Array<String>) {" +
                "  println(\"Hello, world!\")\n" +
                "}");

        Map Request = new HashMap<>();
        for (String tmp : parts) {
            Request.put(ResponseUtils.substringBefore(tmp, "="), ResponseUtils.substringAfter(tmp, "="));
        }

        if (Request.containsKey("text"))
            out.text = (String) Request.get("text");

        if (Request.containsKey("consoleArgs"))
            out.arguments = (String) Request.get("consoleArgs");

        if (Request.containsKey("example"))
            out.exampleFolder = (String) Request.get("example");

        if (Request.containsKey("name"))
            out.example = (String) Request.get("name");
        return out;

        /*
        if (finalResponse != null) {
            finalResponse = finalResponse.replaceAll("<br>", "\n");
            if (finalResponse.length() >= 5) {
                if (finalResponse.contains("&consoleArgs=")) {
                    return new PostData(ResponseUtils.substringBetween(finalResponse, "text=", "&consoleArgs="), ResponseUtils.substringAfter(finalResponse, "&consoleArgs="));
                } else {
                    return new PostData(ResponseUtils.substringAfter(finalResponse, "text="));
                }
            } else {
                writeResponse("Post request is too short", HttpServletResponse.SC_BAD_REQUEST);
                return new PostData("", "");
            }
        } else {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(
                    new UnsupportedOperationException("Cannot read data from post request"),
                    sessionInfo.getType(), sessionInfo.getOriginUrl(), currentPsiFile.getText());
            writeResponse("Cannot read data from post request: ", HttpServletResponse.SC_BAD_REQUEST);
        }

        return new PostData("", "");
        */
    }


    //Send Response
    private void writeResponse(String responseBody, int errorCode) {
        try {
            ServerResponseUtils.writeResponse(request, response, responseBody, errorCode);
            ErrorWriterOnServer.LOG_FOR_INFO.info(ErrorWriter.getInfoForLogWoIp(sessionInfo.getType(),
                    sessionInfo.getId(), "ALL " + sessionInfo.getTimeManager().getMillisecondsFromStart() + " request=" + request.getRequestURI() + "?" + request.getQueryString()));
        } catch (IOException e) {
            //This is an exception we can't send data to client
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, sessionInfo.getType(), sessionInfo.getOriginUrl(), currentPsiFile.getText());
        }
    }

    private ExampleObject addUnmodifiableDataToExample(ExampleObject exampleObject) {
        ExampleObject storedExample = ExamplesList.getExampleObject(exampleObject.name, exampleObject.parent);
        exampleObject.files.addAll(storedExample.files.stream().filter((file) -> !file.modifiable).collect(Collectors.toList()));
        exampleObject.testClasses = storedExample.testClasses;
        return exampleObject;
    }

    private class PostData {
        public String text;
        public String arguments = null;
        public String example = null;
        public String exampleFolder = null;

        private PostData(String text) {
            this.text = text;
        }

        private PostData(String text, String arguments) {
            this.text = text;
            this.arguments = arguments;
        }

        private PostData(String text, String arguments, String example) {
            this.text = text;
            this.arguments = arguments;
            this.example = example;
        }
    }


    private void sendResourceFile(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI() + "?" + request.getQueryString();
        path = ResponseUtils.substringAfterReturnAll(path, "resources");
        ErrorWriterOnServer.LOG_FOR_INFO.error(ErrorWriter.getInfoForLogWoIp(SessionInfo.TypeOfRequest.GET_RESOURCE.name(), "-1", "Resource doesn't downloaded from nginx: " + path));
        if (path.equals("")) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(
                    new UnsupportedOperationException("Empty path to resource"),
                    SessionInfo.TypeOfRequest.GET_RESOURCE.name(), request.getHeader("Origin"), path);
            writeResponse(request, response, "Path to the file is incorrect.", HttpServletResponse.SC_NOT_FOUND);
            return;
        } else if (path.startsWith("/messages/")) {
            writeResponse(request, response, "", HttpServletResponse.SC_OK);
            return;
        } else if (path.equals("/") || path.equals("/index.html")) {
            path = "/index.html";
            StringBuilder responseStr = new StringBuilder();
            InputStream is = null;
            try {
                is = ServerHandler.class.getResourceAsStream(path);
                responseStr.append(ResponseUtils.readData(is, true));
            } catch (FileNotFoundException e) {
                ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                        SessionInfo.TypeOfRequest.GET_RESOURCE.name(), request.getHeader("Origin"), "index.html not found");
                writeResponse(request, response, "Cannot open this page", HttpServletResponse.SC_BAD_GATEWAY);
                return;
            } catch (IOException e) {
                ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                        SessionInfo.TypeOfRequest.GET_RESOURCE.name(), request.getHeader("Origin"), "index.html not found");
                writeResponse(request, response, "Cannot open this page", HttpServletResponse.SC_BAD_GATEWAY);
                return;
            } finally {
                ServerResponseUtils.close(is);
            }

            OutputStream os = null;
            try {
                os = response.getOutputStream();
                os.write(responseStr.toString().getBytes());
            } catch (IOException e) {
                //This is an exception we can't send data to client
            } finally {
                ServerResponseUtils.close(os);
            }
            return;
        }

        InputStream is = ServerHandler.class.getResourceAsStream(path);
        if (is == null) {
            if (request.getQueryString() != null) {
                ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(
                        new UnsupportedOperationException("Broken path to resource"),
                        SessionInfo.TypeOfRequest.GET_RESOURCE.name(), request.getHeader("Origin"), request.getRequestURI() + "?" + request.getQueryString());
            }
            writeResponse(request, response, ("Resource not found. " + path), HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            FileUtil.copy(is, response.getOutputStream());
        } catch (IOException e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                    SessionInfo.TypeOfRequest.GET_RESOURCE.name(), request.getHeader("Origin"), request.getRequestURI() + "?" + request.getQueryString());
            writeResponse(request, response, "Could not load the resource from the server", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void writeResponse(HttpServletRequest request, HttpServletResponse response, String responseBody, int errorCode) {
        try {
            ServerResponseUtils.writeResponse(request, response, responseBody, errorCode);
        } catch (IOException e) {
            //This is an exception we can't send data to client
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, "UNKNOWN", request.getHeader("Origin"), "null");
        }
    }
}
