package com.nhajc.server.common.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record TypedItemResponse<T>(String type, @JsonUnwrapped T item) {}
