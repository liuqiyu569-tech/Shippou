package com.fudan.shorturl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessEvent implements Serializable {

    private String shortCode;
    private long timestamp;
}
