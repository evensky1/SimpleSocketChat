package main.entity;

import java.util.Arrays;

public class FileInfo {
    private final String name;
    private final byte[] data;

    public FileInfo(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "name='" + name + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}