package ru.anikson.cloudfilestorage.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceInfo {
    private String path;
    private String name;
    private Long size;
    private String type;
}
