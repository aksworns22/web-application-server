package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static util.HttpRequestUtils.parseQueryString;
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
            BufferedReader rawRequestHeader = new BufferedReader(new InputStreamReader(in));
            RequestLine requestLine = new RequestLine(rawRequestHeader.readLine());
            DataOutputStream dos = new DataOutputStream(out);
            if (requestLine.getMethod().equals("GET") && requestLine.getPath().startsWith("user/create")) {
                String rawQuery = requestLine.getPath().replace("user/create?", "");
                Map<String, String> query = parseQueryString(rawQuery);
                User user = new User(query.get("userId"), query.get("password"), query.get("name"), query.get("email"));
                log.debug("New {} has been created", user);
                byte[] body = user.toString().getBytes(StandardCharsets.UTF_8);
                response200Header(dos, body.length);
                responseBody(dos, body);
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

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
