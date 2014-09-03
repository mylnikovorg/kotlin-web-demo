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

package org.jetbrains.webdemo.handlers;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.webdemo.ErrorWriter;
import org.jetbrains.webdemo.ResponseUtils;
import org.jetbrains.webdemo.Statistics;
import org.jetbrains.webdemo.log.LogDownloader;
import org.jetbrains.webdemo.session.SessionInfo;
import org.jetbrains.webdemo.session.UserInfo;
import org.jetbrains.webdemo.sessions.HttpSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Map;

public class ServerHandler {


    public void handle(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        if (request.getQueryString() != null && request.getQueryString().equals("test")) {
            try (PrintWriter out = response.getWriter()) {
                out.write("ok");
            } catch (Throwable e) {
                ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                        "TEST", request.getHeader("Origin"), "null");
            }
        } else if (!ServerResponseUtils.isOriginAccepted(request)) {
            ErrorWriter.ERROR_WRITER.writeInfo(request.getHeader("Origin") + " try to connect to server");
        } else {
            SessionInfo sessionInfo;

            String param = request.getRequestURI() + "?" + request.getQueryString();
            try {
                Map<String, String[]> parameters = request.getParameterMap();
                switch (parameters.get("type")[0]) {
                    case ("sendUserData"):
                        sessionInfo = setSessionInfo(request, parameters.get("sessionId")[0]);

                        break;
                   /* case ("getSessionId"):
                        sessionInfo = setSessionInfo(request, parameters.get("sessionId")[0]);
                        sendSessionId(request, response, sessionInfo, param);
                        break;
                    case ("getUserName"):
                        sessionInfo = setSessionInfo(request, parameters.get("sessionId")[0]);
                        sendUserName(request, response, sessionInfo, param);
                        break;
                    case ("authorization"):
                        sessionInfo = setSessionInfo(request, parameters.get("sessionId")[0]);
                        sendAuthorizationResult(request, response, parameters, sessionInfo);
                        break;*/
                   /* case ("updateExamples"):
                        updateExamples(request, response);
                        break;
                    case ("updateStatistics"):
                        ErrorWriterOnServer.LOG_FOR_INFO.info(SessionInfo.TypeOfRequest.GET_LOGS_LIST.name());
                        sessionInfo = setSessionInfo(request, parameters.get("sessionId")[0]);
                        sendListLogs(request, response, parameters.get("type")[0].equals("updateStatistics"), sessionInfo);
                        break;*/
//                    case ("showUserInfo"):
//                        ErrorWriterOnServer.LOG_FOR_INFO.info(SessionInfo.TypeOfRequest.GET_LOGS_LIST.name());
//                        sendUserInfoForStatistics(request, response);
//                        break;
                    /*case ("sortExceptions"):
                        ErrorWriterOnServer.LOG_FOR_INFO.info(SessionInfo.TypeOfRequest.DOWNLOAD_LOG.name());
                        sendSortedExceptions(request, response, parameters);
                        break;*/
                    /*case ("downloadLog"):
                        ErrorWriterOnServer.LOG_FOR_INFO.info(SessionInfo.TypeOfRequest.DOWNLOAD_LOG.name() + " " + param);
                        sendLog(request, response, parameters);
                        break;*/
                   /* case ("loadExampleHeaders"):
                        ErrorWriterOnServer.LOG_FOR_INFO.info(SessionInfo.TypeOfRequest.GET_EXAMPLES_LIST.name());
                        sendExamplesList(request, response);
                        break;*/
                    /*case ("loadHelpForWords"):
                        ErrorWriterOnServer.LOG_FOR_INFO.info(SessionInfo.TypeOfRequest.GET_HELP_FOR_WORDS.name());
                        sendHelpContentForWords(request, response);
                        break;*/
                    default: {
                        if (!parameters.get("type")[0].equals("writeLog")) {
                            sessionInfo = setSessionInfo(request, parameters.get("sessionId")[0]);
                        } else {
                            sessionInfo = new SessionInfo(request.getSession().getId());
                        }
                        /*if (!parameters.get("sessionId")[0].equals(sessionInfo.getId())) {
                            parameters.put("sessionId", new String[]{sessionInfo.getId()});
                        }*/
                        HttpSession session = new HttpSession(sessionInfo, parameters);
                        session.handle(request, response);
                    }
                }
            } catch (Throwable e) {
                //Do not stop server
                ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                        "UNKNOWN", "unknown", param);
                ServerResponseUtils.writeResponse(request, response, "Internal server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

   /* private void sendSessionId(HttpServletRequest request, HttpServletResponse response, SessionInfo sessionInfo, String param) {
        try {
            String id = sessionInfo.getId();
            ArrayNode array = new ArrayNode(JsonNodeFactory.instance);
            array.add(id);
            if (sessionInfo.getUserInfo().isLogin()) {
                array.add(URLEncoder.encode(sessionInfo.getUserInfo().getName(), "UTF-8"));
            }

            writeResponse(request, response, array.toString(), HttpServletResponse.SC_OK);
        } catch (Throwable e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                    "UNKNOWN", sessionInfo.getOriginUrl(), param);
        }
    }*/

    /*private void sendUserName(HttpServletRequest request, HttpServletResponse response, SessionInfo sessionInfo, String param) {
        try {
            ArrayNode array = new ArrayNode(JsonNodeFactory.instance);
            if (sessionInfo.getUserInfo().isLogin()) {
                array.add(URLEncoder.encode(sessionInfo.getUserInfo().getName(), "UTF-8"));
            } else {
                array.add("null");
            }
            writeResponse(request, response, array.toString(), HttpServletResponse.SC_OK);
        } catch (Throwable e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                    "UNKNOWN", sessionInfo.getOriginUrl(), param);
        }
    }*/

    /*private void sendAuthorizationResult(HttpServletRequest request, HttpServletResponse response, Map<String, String[]> parameters, SessionInfo sessionInfo) {
        if (parameters.get("args")[0].equals("logout")) {
            sessionInfo.getUserInfo().logout();
            request.getSession().setAttribute("userInfo", sessionInfo.getUserInfo());
        } else {
            AuthorizationHelper helper;
            if (parameters.get("args")[0].equals("twitter")) {
                helper = new AuthorizationTwitterHelper();
            } else if (parameters.get("args")[0].equals("google")) {
                helper = new AuthorizationGoogleHelper();
            } else {
                helper = new AuthorizationFacebookHelper();
            }
            if (parameters.containsKey("oauth_verifier") || parameters.containsKey("code")) {
                UserInfo info;
                if (parameters.containsKey("oauth_verifier")) {
                    info = helper.verify(parameters.get("oauth_verifier")[0]);
                } else {
                    info = helper.verify(parameters.get("code")[0]);
                }
                if (info != null) {
                    sessionInfo.setUserInfo(info);
                    MySqlConnector.getInstance().addNewUser(sessionInfo.getUserInfo());
                    request.getSession().setAttribute("userInfo", sessionInfo.getUserInfo());
                }
                try {
                    response.sendRedirect("http://" + ApplicationSettings.AUTH_REDIRECT);
                } catch (IOException e) {
                    ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                            "UNKNOWN", sessionInfo.getOriginUrl(), "cannot redirect to http://" + ApplicationSettings.AUTH_REDIRECT);
                }
            } else if (parameters.containsKey("denied")) {
                try {
                    response.sendRedirect("http://" + ApplicationSettings.AUTH_REDIRECT);
                } catch (IOException e) {
                    ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                            "UNKNOWN", sessionInfo.getOriginUrl(), "cannot redirect to http://" + ApplicationSettings.AUTH_REDIRECT);
                }
            } else {
                String verifyKey = helper.authorize();
                try (PrintWriter out = response.getWriter()) {
                    out.write(verifyKey);
                } catch (Throwable e) {
                    ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                            "UNKNOWN", sessionInfo.getOriginUrl(), request.getRequestURI() + "/" + request.getQueryString());
                }
            }
        }
    }*/

    private void sendUserInfoForStatistics(HttpServletRequest request, final HttpServletResponse response) {
        writeResponse(request, response, Statistics.getInstance().showMap(), HttpServletResponse.SC_OK);
    }


    /*private void updateExamples(HttpServletRequest request, final HttpServletResponse response) {
        String responseStr = ExamplesList.updateList();
        responseStr += HelpLoader.updateExamplesHelp();
        writeResponse(request, response, responseStr, HttpServletResponse.SC_OK);
    }*/

    private void sendSortedExceptions(final HttpServletRequest request, final HttpServletResponse response, Map<String, String[]> parameters) {
        if (parameters.get("args")[0].contains("download")) {
            response.addHeader("Content-type", "application/x-download");
        }
        String from = ResponseUtils.substringBetween(parameters.get("args")[0], "from=", "&to=");
        String to = ResponseUtils.substringAfter(parameters.get("args")[0], "&to=");

        writeResponse(request, response, new LogDownloader().getSortedExceptions(from, to), 200);
    }

    @Nullable
    private SessionInfo setSessionInfo(final HttpServletRequest request, String sessionId) {
        if (request.getSession().isNew()) {
            Statistics.incNumberOfUsers();
        }
        SessionInfo sessionInfo = new SessionInfo(request.getSession().getId());
        UserInfo userInfo = (UserInfo) request.getSession().getAttribute("userInfo");

        if (userInfo == null) {
            userInfo = new UserInfo();
            request.getSession().setAttribute("userInfo", userInfo);
        }
        sessionInfo.setUserInfo(userInfo);
        sessionInfo.setOriginUrl(request.getHeader("Origin"));
        return sessionInfo;
    }



   /* private void sendLog(final HttpServletRequest request, final HttpServletResponse response, Map<String, String[]> parameters) {
        String path;
        if (parameters.get("args")[0].contains("&download")) {
            response.addHeader("Content-type", "application/x-download");
        }
        if (parameters.get("args")[0].contains("&view")) {
            path = ResponseUtils.substringBefore(parameters.get("args")[0], "&view");
        } else {
            path = ResponseUtils.substringBefore(parameters.get("args")[0], "&download");
        }
        path = path.replaceAll("%5C", "/");
        writeResponse(request, response, new LogDownloader().download(path), 200);
    }*/

    private void sendListLogs(final HttpServletRequest request, final HttpServletResponse response, boolean updateStatistics, SessionInfo sessionInfo) {
        String responseStr = null;
        InputStream is = null;
        try {
            is = ServerHandler.class.getResourceAsStream("/logs.html");
            responseStr = ResponseUtils.readData(is, true);

        } catch (IOException e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                    SessionInfo.TypeOfRequest.GET_LOGS_LIST.name(), sessionInfo.getOriginUrl(), "Exception until downloading logs.html");
            writeResponse(request, response, "Cannot open this page", HttpServletResponse.SC_BAD_GATEWAY);
            return;
        } finally {
            ServerResponseUtils.close(is);
        }

        String links = new LogDownloader().getFilesLinks();
        responseStr = responseStr.replace("$LINKSTOLOGFILES$", links);
        responseStr = responseStr.replace("$CURRENTDATE$", ResponseUtils.getDate(Calendar.getInstance()));

        if (updateStatistics) {
            Statistics.getInstance().updateStatistics(true);
        }
        responseStr = Statistics.getInstance().writeStatistics(responseStr);
        writeResponse(request, response, responseStr, 200);
    }

    /*private void sendExamplesList(HttpServletRequest request, final HttpServletResponse response) {
        writeResponse(request, response, ExamplesList.getInstance().getListAsString(), HttpServletResponse.SC_OK);
    }*/

   /* private void sendUserInformation(final HttpServletRequest request, final HttpServletResponse response, SessionInfo info) {
        StringBuilder reqResponse = new StringBuilder();
        InputStream is = null;
        try {
            is = request.getInputStream();
            reqResponse.append(ResponseUtils.readData(is));
        } catch (IOException e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                    SessionInfo.TypeOfRequest.SEND_USER_DATA.name(), info.getOriginUrl(), info.getId());
            writeResponse(request, response, "Cannot read data from file", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } finally {
            ServerResponseUtils.close(is);
        }
        try {
            reqResponse = new StringBuilder(URLDecoder.decode(reqResponse.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e,
                    SessionInfo.TypeOfRequest.SEND_USER_DATA.name(), info.getOriginUrl(), info.getId());
        }
        ErrorWriterOnServer.LOG_FOR_INFO.info(ErrorWriter.getInfoForLog(SessionInfo.TypeOfRequest.INC_NUMBER_OF_REQUESTS.name(), info.getId(), SessionInfo.TypeOfRequest.SEND_USER_DATA.name()));
        ErrorWriterOnServer.LOG_FOR_INFO.info(ErrorWriter.getInfoForLogWoIp(SessionInfo.TypeOfRequest.SEND_USER_DATA.name(), info.getId(), ResponseUtils.substringAfter(reqResponse.toString(), "text=")));
        writeResponse(request, response, "OK", HttpServletResponse.SC_OK);
    }
*/

    //Send Response
    private void writeResponse(HttpServletRequest request, HttpServletResponse response, String responseBody, int errorCode) {
        try {
            ServerResponseUtils.writeResponse(request, response, responseBody, errorCode);
        } catch (IOException e) {
            //This is an exception we can't send data to client
            ErrorWriter.ERROR_WRITER.writeExceptionToExceptionAnalyzer(e, "UNKNOWN", request.getHeader("Origin"), "null");
        }
    }
}

