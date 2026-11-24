package com.nhajc.server.admin.controller;

import com.nhajc.server.admin.service.ReleaseSongOrderService;
import com.nhajc.server.release.dto.ReorderSongsRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/releases")
public class ReleaseSongOrderController {
    private final ReleaseSongOrderService order;

    public ReleaseSongOrderController(ReleaseSongOrderService order) { this.order = order; }

    @PutMapping("/{id}/songs/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@PathVariable UUID id, @RequestBody @Valid ReorderSongsRequest request) {
        order.reorder(id, request.songIds());
    }
}
