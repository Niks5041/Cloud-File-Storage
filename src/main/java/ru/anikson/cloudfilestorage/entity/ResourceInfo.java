package ru.anikson.cloudfilestorage.entity;

import lombok.Data;

@Data
public class ResourceInfo {
    private String path;
    private String name;
    private String size;
    private String type;
}
