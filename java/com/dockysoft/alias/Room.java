package com.dockysoft.alias;

/**
 * Created by isaac on 6/2/2017.
 */

public class Room implements Comparable<Room> {
    private String name;
    private String desc;
    private String author;

    public Room(String name, String desc, String author) {
        this.name = name;
        this.desc = desc;
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getName() {
        return name;

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public int compareTo(Room other){
        return name.compareTo(other.getName());
    }
}
