package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static util.HttpRequestUtils.parseHeader;
import static util.HttpRequestUtils.parseQueryString;
import static util.IOUtils.readData;

import util.RequestLine;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            BufferedReader rawRequest = new BufferedReader(new InputStreamReader(in));
            RequestLine requestLine = new RequestLine(rawRequest.readLine());
            DataOutputStream dos = new DataOutputStream(out);
            if (requestLine.getMethod().equals("POST") && requestLine.getPath().startsWith("user/create")) {
                String rawContentLength;
                while (!(rawContentLength = rawRequest.readLine()).startsWith("Content-Length:"));
                while (!(rawRequest.readLine().equals(""))) ;
                Integer contentLength = Integer.parseInt(parseHeader(rawContentLength).getValue());
                String rawRequestBody = readData(rawRequest, contentLength);
                Map<String, String> requestBody = parseQueryString(rawRequestBody);
                User user = new User(requestBody.get("userId"), requestBody.get("password"), requestBody.get("name"), requestBody.get("email"));
                log.debug("New {} has been created", user);
                response302Header(dos);
                return;
            }
            byte[] body = Files.readAllBytes(new File(String.format("./webapp/%s", requestLine.getPath())).toPath());
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found\r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("Content-Length: 0\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
