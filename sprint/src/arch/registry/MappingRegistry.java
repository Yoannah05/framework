package arch.registry;

import arch.Mapping;
import arch.exception.DuplicateUrlMappingException;
import arch.exception.UrlMappingNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MappingRegistry {
    private final Map<String, Mapping> urlMappings = new HashMap<>();

    public void registerMapping(String url, Mapping mapping) {
        if (urlMappings.containsKey(url)) {
            throw new DuplicateUrlMappingException(url);
        }
        urlMappings.put(url, mapping);
    }

    public Mapping getMapping(String url) {
        Mapping mapping = urlMappings.get(url);
        if (mapping == null) {
            throw new UrlMappingNotFoundException(url);
        }
        return mapping;
    }

    public Set<String> getRegisteredUrls() {
        return urlMappings.keySet();
    }
}