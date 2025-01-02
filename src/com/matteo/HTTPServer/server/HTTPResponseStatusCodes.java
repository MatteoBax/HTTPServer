package com.matteo.HTTPServer.server;

public class HTTPResponseStatusCodes {
    private static HTTPResponseStatusCode[] codes = new HTTPResponseStatusCode[]{
        new HTTPResponseStatusCode(100, "Continue"),
        new HTTPResponseStatusCode(101, "Switching Protocols"),
        new HTTPResponseStatusCode(102, "Processing"),
        new HTTPResponseStatusCode(200, "OK"),
        new HTTPResponseStatusCode(201, "Created"),
        new HTTPResponseStatusCode(202, "Accepted"),
        new HTTPResponseStatusCode(203, "Non-Authoritative Information"),
        new HTTPResponseStatusCode(204, "No Content"),
        new HTTPResponseStatusCode(205, "Reset Content"),
        new HTTPResponseStatusCode(206, "Partial Content"),
        new HTTPResponseStatusCode(207, "Multi-Status"),
        new HTTPResponseStatusCode(208, "Already Reported"),
        new HTTPResponseStatusCode(209, ""),
        new HTTPResponseStatusCode(210, ""),
        new HTTPResponseStatusCode(211, ""),
        new HTTPResponseStatusCode(212, ""),
        new HTTPResponseStatusCode(213, ""),
        new HTTPResponseStatusCode(214, ""),
        new HTTPResponseStatusCode(215, ""),
        new HTTPResponseStatusCode(216, ""),
        new HTTPResponseStatusCode(217, ""),
        new HTTPResponseStatusCode(218, ""),
        new HTTPResponseStatusCode(219, ""),
        new HTTPResponseStatusCode(220, ""),
        new HTTPResponseStatusCode(221, ""),
        new HTTPResponseStatusCode(222, ""),
        new HTTPResponseStatusCode(223, ""),
        new HTTPResponseStatusCode(224, ""),
        new HTTPResponseStatusCode(225, ""),
        new HTTPResponseStatusCode(226, "IM Used"),
        new HTTPResponseStatusCode(300, "Multiple Choices"),
        new HTTPResponseStatusCode(301, "Moved Permanently"),
        new HTTPResponseStatusCode(302, "Found"),
        new HTTPResponseStatusCode(303, "See Other"),
        new HTTPResponseStatusCode(304, "Not Modified"),
        new HTTPResponseStatusCode(305, "Use Proxy"),
        new HTTPResponseStatusCode(306, ""),
        new HTTPResponseStatusCode(307, "Temporary Redirect"),
        new HTTPResponseStatusCode(308, "Permanent Redirect"),
        new HTTPResponseStatusCode(400, "Bad Request"),
        new HTTPResponseStatusCode(401, "Unauthorized"),
        new HTTPResponseStatusCode(402, "Payment Required"),
        new HTTPResponseStatusCode(403, "Forbidden"),
        new HTTPResponseStatusCode(404, "Not Found"),
        new HTTPResponseStatusCode(405, "Method Not Allowed"),
        new HTTPResponseStatusCode(406, "Not Acceptable"),
        new HTTPResponseStatusCode(407, "Proxy Authentication Required"),
        new HTTPResponseStatusCode(408, "Request Timeout"),
        new HTTPResponseStatusCode(409, "Conflict"),
        new HTTPResponseStatusCode(410, "Gone"),
        new HTTPResponseStatusCode(411, "Length Required"),
        new HTTPResponseStatusCode(412, "Precondition Failed"),
        new HTTPResponseStatusCode(413, "Request Entity Too Large"),
        new HTTPResponseStatusCode(414, "Request-URI Too Long"),
        new HTTPResponseStatusCode(415, "Unsupported Media Type"),
        new HTTPResponseStatusCode(416, "Requested Range Not Satisfiable"),
        new HTTPResponseStatusCode(417, "Expectation Failed"),
        new HTTPResponseStatusCode(418, "I'm A Teapot"),
        new HTTPResponseStatusCode(419, ""),
        new HTTPResponseStatusCode(420, ""),
        new HTTPResponseStatusCode(421, "Misdirected Request"),
        new HTTPResponseStatusCode(422, "Unprocessable Entity"),
        new HTTPResponseStatusCode(423, "Locked"),
        new HTTPResponseStatusCode(424, "Failed Dependency"),
        new HTTPResponseStatusCode(425, "Too Early"),
        new HTTPResponseStatusCode(426, "Upgrade Required"),
        new HTTPResponseStatusCode(427, ""),
        new HTTPResponseStatusCode(428, "Precondition Required"),
        new HTTPResponseStatusCode(429, "Too Many Requests"),
        new HTTPResponseStatusCode(430, ""),
        new HTTPResponseStatusCode(431, "Request Header Fields Too Large"),
        new HTTPResponseStatusCode(432, ""),
        new HTTPResponseStatusCode(433, ""),
        new HTTPResponseStatusCode(434, ""),
        new HTTPResponseStatusCode(435, ""),
        new HTTPResponseStatusCode(436, ""),
        new HTTPResponseStatusCode(437, ""),
        new HTTPResponseStatusCode(438, ""),
        new HTTPResponseStatusCode(439, ""),
        new HTTPResponseStatusCode(440, ""),
        new HTTPResponseStatusCode(441, ""),
        new HTTPResponseStatusCode(442, ""),
        new HTTPResponseStatusCode(443, ""),
        new HTTPResponseStatusCode(444, ""),
        new HTTPResponseStatusCode(445, ""),
        new HTTPResponseStatusCode(446, ""),
        new HTTPResponseStatusCode(447, ""),
        new HTTPResponseStatusCode(448, ""),
        new HTTPResponseStatusCode(449, ""),
        new HTTPResponseStatusCode(450, ""),
        new HTTPResponseStatusCode(451, "Unavailable For Legal Reasons"),
        new HTTPResponseStatusCode(500, "Internal Server Error"),
        new HTTPResponseStatusCode(502, "Not Implemented"),
        new HTTPResponseStatusCode(503, "Service Unavailable"),
        new HTTPResponseStatusCode(504, "Gateway Timeout"),
        new HTTPResponseStatusCode(505, "HTTP Version Not Supported"),
        new HTTPResponseStatusCode(506, "Variant Also Negotiates"),
        new HTTPResponseStatusCode(507, "Insufficient Storage"),
        new HTTPResponseStatusCode(508, "Loop Detected"),
        new HTTPResponseStatusCode(509, ""),
        new HTTPResponseStatusCode(510, "Not Extended"),
        new HTTPResponseStatusCode(511, "Network Authentication Required")
    };

    public static HTTPResponseStatusCode find(int code) {
        int sup = codes.length -1;
        int inf = 0;
        int center;
        while(inf <= sup) {
            center = (sup + inf) / 2;
            HTTPResponseStatusCode el = codes[center];
            if(el.getCode() == code) {
                return el;
            } else if (el.getCode() > code) {
                sup = center - 1;
            } else {
                inf = center + 1;
            }
        }
        return null;
    }
}
