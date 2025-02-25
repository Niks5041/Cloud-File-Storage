package ru.anikson.cloudfilestorage.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceInfo {
    private String path;
    private String name;
    private Long size;
    private String type;
}
