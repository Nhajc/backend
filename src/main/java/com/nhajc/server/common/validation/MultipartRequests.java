package com.nhajc.server.common.validation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
@Component
public class MultipartRequests {
    private static final Set<String> PARTS=Set.of("data","file");
    public void validateParts(HttpServletRequest request) {
        for(var entry:request.getParameterMap().entrySet()) {
            if(!PARTS.contains(entry.getKey())||entry.getValue().length!=1)throw invalid();
        }
        var multipart=org.springframework.web.util.WebUtils.getNativeRequest(request,MultipartHttpServletRequest.class);
        if(multipart!=null) {
            for(var entry:multipart.getMultiFileMap().entrySet()) {
                if(!PARTS.contains(entry.getKey())||entry.getValue().size()!=1)throw invalid();
            }
        }
        if(request.getParameterMap().containsKey("file")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "file must be a File part in multipart/form-data, not text or a file path.");
        }
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Multipart requires one JSON data part and at most one file part.");
    }
}
