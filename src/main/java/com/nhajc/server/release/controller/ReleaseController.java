package com.nhajc.server.release.controller;
import com.nhajc.server.common.dto.IdResponse;
import com.nhajc.server.common.validation.MultipartRequests;
import com.nhajc.server.release.dto.ReleaseRequest;
import com.nhajc.server.release.service.ReleaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/v1/admin/releases")
public class ReleaseController {
    private final ReleaseService releases;
    private final MultipartRequests multipart;
    public ReleaseController(ReleaseService releases,MultipartRequests multipart){this.releases=releases;this.multipart=multipart;}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public IdResponse create(@RequestPart("data") @Valid @NotNull ReleaseRequest data,
            @RequestPart(value="file",required=false) MultipartFile file,HttpServletRequest request) {
        multipart.validateParts(request);
        return new IdResponse(releases.create(data,file));
    }
}
