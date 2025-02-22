package arch.session;

import jakarta.servlet.http.HttpSession;

public class MySession {
    private final HttpSession session;

    public MySession(HttpSession session) {
        this.session = session;
    }

    public void add(String key, Object object) {
        session.setAttribute(key, object);
    }

    public Object get(String key) {
        return session.getAttribute(key);
    }

    public void delete(String key) {
        session.removeAttribute(key);
    }
    
    public void invalidate() {
        session.invalidate();
    }
}