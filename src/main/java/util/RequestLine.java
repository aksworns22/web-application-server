package util;

public class RequestLine {
    private final String path;
    private final String method;
    private final String protocol;

    public RequestLine(String requestLine) {
        String[] tokens = requestLine.split(" ");
        this.method = tokens[0];
        this.path = tokens[1].replaceFirst("^/", "");
        this.protocol = tokens[2];
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }
}
